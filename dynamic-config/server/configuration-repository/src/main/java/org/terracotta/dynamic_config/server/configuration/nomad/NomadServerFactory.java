/*
 * Copyright Terracotta, Inc.
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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.Version;
import org.terracotta.dynamic_config.api.model.nomad.FormatUpgradeNomadChange;
import org.terracotta.dynamic_config.api.service.ClusterValidator;
import org.terracotta.dynamic_config.server.api.DynamicConfigEventFiring;
import org.terracotta.dynamic_config.server.configuration.nomad.persistence.ClusterConfigFilename;
import org.terracotta.dynamic_config.server.configuration.nomad.persistence.Config;
import org.terracotta.dynamic_config.server.configuration.nomad.persistence.ConfigStorageAdapter;
import org.terracotta.dynamic_config.server.configuration.nomad.persistence.ConfigStorageException;
import org.terracotta.dynamic_config.server.configuration.nomad.persistence.DefaultHashComputer;
import org.terracotta.dynamic_config.server.configuration.nomad.persistence.FileConfigStorage;
import org.terracotta.dynamic_config.server.configuration.nomad.persistence.InitialConfigStorage;
import org.terracotta.dynamic_config.server.configuration.nomad.persistence.NomadConfigurationManager;
import org.terracotta.dynamic_config.server.configuration.nomad.persistence.SanskritNomadServerState;
import org.terracotta.json.ObjectMapperFactory;
import org.terracotta.nomad.NomadEnvironment;
import org.terracotta.nomad.client.NomadClient;
import org.terracotta.nomad.client.NomadEndpoint;
import org.terracotta.nomad.client.results.NomadFailureReceiver;
import org.terracotta.nomad.messages.AcceptRejectResponse;
import org.terracotta.nomad.messages.CommitMessage;
import org.terracotta.nomad.messages.PrepareMessage;
import org.terracotta.nomad.messages.RollbackMessage;
import org.terracotta.nomad.server.ChangeApplicator;
import org.terracotta.nomad.server.NomadChangeInfo;
import org.terracotta.nomad.server.NomadException;
import org.terracotta.nomad.server.NomadServer;
import org.terracotta.nomad.server.NomadServerImpl;
import org.terracotta.nomad.server.SingleThreadedNomadServer;
import org.terracotta.nomad.server.UpgradableNomadServer;
import org.terracotta.nomad.server.UpgradableNomadServerAdapter;
import org.terracotta.persistence.sanskrit.ObjectMapperSupplier;
import org.terracotta.persistence.sanskrit.Sanskrit;
import org.terracotta.persistence.sanskrit.SanskritException;
import org.terracotta.persistence.sanskrit.file.FileBasedFilesystemDirectory;

import java.nio.file.Path;
import java.time.Clock;
import java.util.List;

import static java.util.Collections.singletonList;

public class NomadServerFactory {
  private static final Logger LOGGER = LoggerFactory.getLogger(NomadServerFactory.class);

  private final ObjectMapperFactory objectMapperFactory;

  public NomadServerFactory(ObjectMapperFactory objectMapperFactory) {
    this.objectMapperFactory = objectMapperFactory;
  }

  public UpgradableNomadServer<NodeContext> createServer(NomadConfigurationManager configurationManager,
                                                         ChangeApplicator<NodeContext> changeApplicator,
                                                         String nodeName,
                                                         DynamicConfigEventFiring dynamicConfigEventFiring,
                                                         ClusterValidator clusterValidator) throws SanskritException, NomadException, ConfigStorageException {

    FileBasedFilesystemDirectory filesystemDirectory = new FileBasedFilesystemDirectory(configurationManager.getChangesPath());

    // Creates a json mapper with indentation for human readability, but forcing all EOL to be LF like Sanskrit
    // The sanskrit files should be portable from Lin to Win and still work.
    // do not use pretty() or it will mess up the EOL and sanskrit hashes. It is also harder to keep backward compat with that
    ObjectMapper objectMapper = objectMapperFactory.create();

    ObjectMapper objectMapperV1 = createDeprecatedV1Mapper();

    ObjectMapperSupplier objectMapperSupplier = ObjectMapperSupplier.versioned(objectMapper, Version.CURRENT.getValue())
        .withVersions(objectMapperV1, "", Version.V1.getValue())
        .withVersions(objectMapper, Version.V2.getValue());

    Sanskrit sanskrit = Sanskrit.init(filesystemDirectory, objectMapperSupplier);

    Path clusterDir = configurationManager.getClusterPath();
    InitialConfigStorage configStorage = new InitialConfigStorage(new ConfigStorageAdapter(new FileConfigStorage(clusterDir, nodeName, clusterValidator)) {
      @Override
      public void saveConfig(long version, NodeContext config) throws ConfigStorageException {
        super.saveConfig(version, config);
        if (dynamicConfigEventFiring != null) {
          dynamicConfigEventFiring.onNewConfigurationSaved(config, version);
        }
      }
    });

    SanskritNomadServerState serverState = new SanskritNomadServerState(sanskrit, configStorage, new DefaultHashComputer());

    SingleThreadedNomadServer<NodeContext> nomadServer = new SingleThreadedNomadServer<>(new UpgradableNomadServerAdapter<NodeContext>(new NomadServerImpl<>(serverState, changeApplicator)) {
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
        NomadChangeInfo changeInfo = getNomadChange(message.getChangeUuid()).get();
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
    });

    long currentVersion = serverState.getCurrentVersion();
    if (currentVersion != 0) {
      upgrade(configStorage, nomadServer, currentVersion);

    }

    currentVersion = serverState.getCurrentVersion();
    if (currentVersion != 0) {
      Config config = configStorage.getConfig(currentVersion);
      String filename = ClusterConfigFilename.with(nodeName, currentVersion).getFilename();
      LOGGER.info("Using configuration version: {} with format version: {} at: {}", currentVersion, config.getVersion(), clusterDir.resolve(filename));
    }

    return nomadServer;
  }

  @SuppressWarnings("deprecation")
  private ObjectMapper createDeprecatedV1Mapper() {
    return objectMapperFactory.withModules(
        new org.terracotta.dynamic_config.api.json.DynamicConfigApiJsonModuleV1(),
        new org.terracotta.dynamic_config.api.json.DynamicConfigModelJsonModuleV1()).create();
  }

  private void upgrade(InitialConfigStorage configStorage, NomadServer<NodeContext> nomadServer, long currentVersion) throws ConfigStorageException {
    final Config config = configStorage.getConfig(currentVersion);
    final Node node = config.getTopology().getNode();
    final String filename = ClusterConfigFilename.with(node.getName(), currentVersion).getFilename();
    final Version to = Version.CURRENT;

    if (config.getVersion().is(to)) {
      return;
    }

    LOGGER.info("Upgrading configuration version: {} stored in: {} from format version: {} to format version: {}", currentVersion, filename, config.getVersion(), to);

    NomadEnvironment environment = new NomadEnvironment();

    List<NomadEndpoint<NodeContext>> endpoints = singletonList(new NomadEndpoint<>(node.getInternalAddress(), nomadServer));
    // Note: do NOT close this nomad client - it would close the server and sanskrit!
    NomadClient<NodeContext> nomadClient = new NomadClient<>(endpoints, environment.getHost(), environment.getUser(), Clock.systemUTC());
    NomadFailureReceiver<NodeContext> failureRecorder = new NomadFailureReceiver<>();
    nomadClient.tryApplyChange(failureRecorder, new FormatUpgradeNomadChange(config.getVersion(), to));

    // this is important to rethrow eagerly in the nomad server creation flow to avoid starting a server ending with a prepared change,
    // which cannot be migrated sadly since we cannot alter the append log entries.
    failureRecorder.reThrowErrors();
  }
}