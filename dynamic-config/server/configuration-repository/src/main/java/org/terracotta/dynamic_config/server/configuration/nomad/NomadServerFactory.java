/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.dynamic_config.server.configuration.nomad;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.Version;
import org.terracotta.dynamic_config.api.model.nomad.DynamicConfigNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.FormatUpgradeNomadChange;
import org.terracotta.dynamic_config.api.service.FormatUpgrade;
import org.terracotta.dynamic_config.server.api.DynamicConfigEventFiring;
import org.terracotta.dynamic_config.server.api.DynamicConfigNomadServer;
import org.terracotta.dynamic_config.server.configuration.nomad.persistence.ClusterConfigFilename;
import org.terracotta.dynamic_config.server.configuration.nomad.persistence.Config;
import org.terracotta.dynamic_config.server.configuration.nomad.persistence.ConfigStorageAdapter;
import org.terracotta.dynamic_config.server.configuration.nomad.persistence.ConfigStorageException;
import org.terracotta.dynamic_config.server.configuration.nomad.persistence.DefaultHashComputer;
import org.terracotta.dynamic_config.server.configuration.nomad.persistence.FileConfigStorage;
import org.terracotta.dynamic_config.server.configuration.nomad.persistence.InitialConfigStorage;
import org.terracotta.dynamic_config.server.configuration.nomad.persistence.NomadConfigurationManager;
import org.terracotta.dynamic_config.server.configuration.nomad.persistence.json.SanskritJsonModule;
import org.terracotta.dynamic_config.server.configuration.nomad.persistence.sanskrit.SanskritJsonMapper;
import org.terracotta.dynamic_config.server.configuration.nomad.persistence.sanskrit.SanskritNomadServerState;
import org.terracotta.json.Json;
import org.terracotta.nomad.NomadEnvironment;
import org.terracotta.nomad.client.NomadClient;
import org.terracotta.nomad.client.NomadEndpoint;
import org.terracotta.nomad.client.results.NomadFailureReceiver;
import org.terracotta.nomad.messages.AcceptRejectResponse;
import org.terracotta.nomad.messages.CommitMessage;
import org.terracotta.nomad.messages.PrepareMessage;
import org.terracotta.nomad.messages.RollbackMessage;
import org.terracotta.nomad.server.ChangeApplicator;
import org.terracotta.nomad.server.ChangeState;
import org.terracotta.nomad.server.NomadException;
import org.terracotta.persistence.sanskrit.Sanskrit;
import org.terracotta.persistence.sanskrit.SanskritException;
import org.terracotta.persistence.sanskrit.SanskritMapper;
import org.terracotta.persistence.sanskrit.file.FileBasedFilesystemDirectory;

import java.nio.file.Path;
import java.time.Clock;
import java.util.List;

import static java.util.Collections.singletonList;

public class NomadServerFactory {
  private static final Logger LOGGER = LoggerFactory.getLogger(NomadServerFactory.class);

  private final Json.Factory jsonFactory;

  public NomadServerFactory(Json.Factory jsonFactory) {
    this.jsonFactory = jsonFactory.withModule(new SanskritJsonModule());
  }

  public DynamicConfigNomadServer createServer(NomadConfigurationManager configurationManager,
                                               String nodeName,
                                               DynamicConfigEventFiring dynamicConfigEventFiring) throws SanskritException, NomadException, ConfigStorageException {

    FileBasedFilesystemDirectory filesystemDirectory = new FileBasedFilesystemDirectory(configurationManager.getChangesPath());

    SanskritMapper mapper = new SanskritJsonMapper(jsonFactory);

    Sanskrit sanskrit = Sanskrit.init(filesystemDirectory, mapper);

    Path clusterDir = configurationManager.getClusterPath();
    InitialConfigStorage configStorage = new InitialConfigStorage(new ConfigStorageAdapter(new FileConfigStorage(clusterDir, nodeName)) {
      @Override
      public void saveConfig(long version, NodeContext config) throws ConfigStorageException {
        super.saveConfig(version, config);
        if (dynamicConfigEventFiring != null) {
          dynamicConfigEventFiring.onNewConfigurationSaved(config, version);
        }
      }
    });

    SanskritNomadServerState serverState = new SanskritNomadServerState(sanskrit, configStorage, new DefaultHashComputer());

    DynamicConfigNomadServer nomadServer = new SingleThreadedNomadServer(new DynamicConfigNomadServerImpl(serverState)) {
      @Override
      public AcceptRejectResponse prepare(PrepareMessage message) throws NomadException {
        AcceptRejectResponse response = super.prepare(message);
        if (dynamicConfigEventFiring != null) {
          dynamicConfigEventFiring.onNomadPrepare(message, response);
        }
        return response;
      }

      @Override
      public AcceptRejectResponse commit(CommitMessage message) throws NomadException {
        AcceptRejectResponse response = super.commit(message);
        ChangeState<NodeContext> changeInfo = getConfig(message.getChangeUuid()).get();
        if (dynamicConfigEventFiring != null) {
          dynamicConfigEventFiring.onNomadCommit(message, response, changeInfo);
        }
        return response;
      }

      @Override
      public AcceptRejectResponse rollback(RollbackMessage message) throws NomadException {
        AcceptRejectResponse response = super.rollback(message);
        if (dynamicConfigEventFiring != null) {
          dynamicConfigEventFiring.onNomadRollback(message, response);
        }
        return response;
      }

      @Override
      public void close() {
        try {
          sanskrit.close();
        } catch (SanskritException e) {
          LOGGER.warn("Error closing Sanskrit: " + e.getMessage(), e);
        }
      }
    };

    long currentVersion = serverState.getCurrentVersion();
    if (currentVersion != 0) {
      upgrade(configStorage, nomadServer, currentVersion);
    }

    currentVersion = serverState.getCurrentVersion();
    if (currentVersion != 0) {
      Config config = configStorage.getConfig(currentVersion);
      String filename = ClusterConfigFilename.with(nodeName, currentVersion).getFilename();
      LOGGER.info("Using configuration version: {} with format version: {} from file: {}", currentVersion, config.getVersion(), filename);
    }

    return nomadServer;
  }

  private void upgrade(InitialConfigStorage configStorage, DynamicConfigNomadServer nomadServer, long currentVersion) throws ConfigStorageException {
    final Config config = configStorage.getConfig(currentVersion);
    final Node node = config.getTopology().getNode();
    final String filename = ClusterConfigFilename.with(node.getName(), currentVersion).getFilename();
    final Version to = Version.CURRENT;

    if (config.getVersion().is(to)) {
      return;
    }

    LOGGER.info("Upgrading configuration version: {} stored in: {} from format version: {} to format version: {}", currentVersion, filename, config.getVersion(), to);

    // upgrade
    Cluster upgraded = new FormatUpgrade().upgrade(config.getTopology().getCluster(), config.getVersion());

    // push in Nomad
    try {
      // prepare server with a change applicator that will always apply
      nomadServer.setChangeApplicator(ChangeApplicator.allow((nodeContext, change) -> nodeContext.withCluster(((DynamicConfigNomadChange) change).apply(nodeContext.getCluster())).get()));

      NomadEnvironment environment = new NomadEnvironment();

      List<NomadEndpoint<NodeContext>> endpoints = singletonList(new NomadEndpoint<>(node.getInternalHostPort(), nomadServer));
      // Note: do NOT close this nomad client - it would close the server and sanskrit!
      NomadClient<NodeContext> nomadClient = new NomadClient<>(endpoints, environment.getHost(), environment.getUser(), Clock.systemUTC());
      NomadFailureReceiver<NodeContext> failureRecorder = new NomadFailureReceiver<>();
      nomadClient.tryApplyChange(failureRecorder, new FormatUpgradeNomadChange(config.getVersion(), to, upgraded));

      // this is important to rethrow eagerly in the nomad server creation flow to avoid starting a server ending with a prepared change,
      // which cannot be migrated sadly since we cannot alter the append log entries.
      failureRecorder.reThrowErrors();

    } finally {
      // remove the change applicator, because the factory needs to return a nomad server without any
      nomadServer.setChangeApplicator(null);
    }

    LOGGER.debug("Successfully completed upgradeForWrite procedure");
  }
}