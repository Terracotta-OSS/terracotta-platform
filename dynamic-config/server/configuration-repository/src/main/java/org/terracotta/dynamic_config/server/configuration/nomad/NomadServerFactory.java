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
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.server.api.DynamicConfigListener;
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

public class NomadServerFactory {

  private final ObjectMapperFactory objectMapperFactory;

  public NomadServerFactory(ObjectMapperFactory objectMapperFactory) {
    this.objectMapperFactory = objectMapperFactory;
  }

  public UpgradableNomadServer<NodeContext> createServer(NomadConfigurationManager configurationManager,
                                                         ChangeApplicator<NodeContext> changeApplicator,
                                                         String nodeName,
                                                         DynamicConfigListener listener) throws SanskritException, NomadException {

    FileBasedFilesystemDirectory filesystemDirectory = new FileBasedFilesystemDirectory(configurationManager.getChangesPath());
    ObjectMapper objectMapper = objectMapperFactory.create();
    Sanskrit sanskrit = Sanskrit.init(filesystemDirectory, objectMapper);

    InitialConfigStorage<NodeContext> configStorage = new InitialConfigStorage<>(new ConfigStorageAdapter<NodeContext>(new FileConfigStorage(configurationManager.getClusterPath(), nodeName)) {
      @Override
      public void saveConfig(long version, NodeContext config) throws ConfigStorageException {
        super.saveConfig(version, config);
        listener.onNewConfigurationSaved(config, version);
      }
    });

    SanskritNomadServerState<NodeContext> serverState = new SanskritNomadServerState<>(sanskrit, configStorage, new DefaultHashComputer(objectMapper));

    return new SingleThreadedNomadServer<>(new UpgradableNomadServerAdapter<NodeContext>(new NomadServerImpl<>(serverState, changeApplicator)) {
      @Override
      public AcceptRejectResponse prepare(PrepareMessage message) throws NomadException {
        AcceptRejectResponse response = super.prepare(message);
        listener.onNomadPrepare(message, response);
        return response;
      }

      @Override
      public AcceptRejectResponse commit(CommitMessage message) throws NomadException {
        AcceptRejectResponse response = super.commit(message);
        NomadChangeInfo changeInfo = getNomadChange(message.getChangeUuid()).get();
        listener.onNomadCommit(message, response, changeInfo);
        return response;
      }

      @Override
      public AcceptRejectResponse rollback(RollbackMessage message) throws NomadException {
        AcceptRejectResponse response = super.rollback(message);
        listener.onNomadRollback(message, response);
        return response;
      }
    });
  }
}