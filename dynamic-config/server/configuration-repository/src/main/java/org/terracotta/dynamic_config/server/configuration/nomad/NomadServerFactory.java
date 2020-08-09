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

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.server.api.DynamicConfigEventFiring;
import org.terracotta.dynamic_config.server.configuration.nomad.persistence.ClusterConfigFilename;
import org.terracotta.dynamic_config.server.configuration.nomad.persistence.ConfigStorageAdapter;
import org.terracotta.dynamic_config.server.configuration.nomad.persistence.ConfigStorageException;
import org.terracotta.dynamic_config.server.configuration.nomad.persistence.DefaultHashComputer;
import org.terracotta.dynamic_config.server.configuration.nomad.persistence.FileConfigStorage;
import org.terracotta.dynamic_config.server.configuration.nomad.persistence.InitialConfigStorage;
import org.terracotta.dynamic_config.server.configuration.nomad.persistence.NomadConfigurationManager;
import org.terracotta.dynamic_config.server.configuration.nomad.persistence.SanskritNomadServerState;
import org.terracotta.json.ObjectMapperFactory;
import org.terracotta.nomad.messages.AcceptRejectResponse;
import org.terracotta.nomad.messages.CommitMessage;
import org.terracotta.nomad.messages.PrepareMessage;
import org.terracotta.nomad.messages.RollbackMessage;
import org.terracotta.nomad.server.ChangeApplicator;
import org.terracotta.nomad.server.NomadChangeInfo;
import org.terracotta.nomad.server.NomadException;
import org.terracotta.nomad.server.NomadServerImpl;
import org.terracotta.nomad.server.SingleThreadedNomadServer;
import org.terracotta.nomad.server.UpgradableNomadServer;
import org.terracotta.nomad.server.UpgradableNomadServerAdapter;
import org.terracotta.persistence.sanskrit.Sanskrit;
import org.terracotta.persistence.sanskrit.SanskritException;
import org.terracotta.persistence.sanskrit.file.FileBasedFilesystemDirectory;

import java.nio.file.Path;

public class NomadServerFactory {
  private static final Logger LOGGER = LoggerFactory.getLogger(NomadServerFactory.class);

  private final ObjectMapperFactory objectMapperFactory;

  public NomadServerFactory(ObjectMapperFactory objectMapperFactory) {
    this.objectMapperFactory = objectMapperFactory;
  }

  public UpgradableNomadServer<NodeContext> createServer(NomadConfigurationManager configurationManager,
                                                         ChangeApplicator<NodeContext> changeApplicator,
                                                         String nodeName,
                                                         DynamicConfigEventFiring dynamicConfigEventFiring) throws SanskritException, NomadException {

    FileBasedFilesystemDirectory filesystemDirectory = new FileBasedFilesystemDirectory(configurationManager.getChangesPath());

    // Creates a json mapper with indentation for human readability, but forcing all EOL to be LF like Sanskrit
    // The sanskrit files should be portable from Lin to Win and still work.
    ObjectMapper objectMapper = objectMapperFactory.pretty().create();
    DefaultIndenter indent = new DefaultIndenter("  ", "\n");
    objectMapper.writer(new DefaultPrettyPrinter()
        .withObjectIndenter(indent)
        .withArrayIndenter(indent));

    Sanskrit sanskrit = Sanskrit.init(filesystemDirectory, objectMapper);

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

    SanskritNomadServerState serverState = new SanskritNomadServerState(sanskrit, configStorage, new DefaultHashComputer(objectMapper));
    long currentVersion = serverState.getCurrentVersion();
    if (currentVersion != 0) {
      String filename = ClusterConfigFilename.with(nodeName, currentVersion).getFilename();
      LOGGER.info("Loading version: {} of saved configuration from: {}", currentVersion, clusterDir.resolve(filename));
    }

    return new SingleThreadedNomadServer<>(new UpgradableNomadServerAdapter<NodeContext>(new NomadServerImpl<>(serverState, changeApplicator)) {
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
  }
}