/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.nomad;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.terracottatech.dynamic_config.json.Json;
import com.terracottatech.dynamic_config.model.NodeContext;
import com.terracottatech.dynamic_config.nomad.persistence.ConfigStorageAdapter;
import com.terracottatech.dynamic_config.nomad.persistence.ConfigStorageException;
import com.terracottatech.dynamic_config.nomad.persistence.DefaultHashComputer;
import com.terracottatech.dynamic_config.nomad.persistence.FileConfigStorage;
import com.terracottatech.dynamic_config.nomad.persistence.InitialConfigStorage;
import com.terracottatech.dynamic_config.nomad.persistence.SanskritNomadServerState;
import com.terracottatech.dynamic_config.repository.NomadRepositoryManager;
import com.terracottatech.dynamic_config.service.api.DynamicConfigListener;
import com.terracottatech.dynamic_config.util.IParameterSubstitutor;
import com.terracottatech.nomad.messages.AcceptRejectResponse;
import com.terracottatech.nomad.messages.CommitMessage;
import com.terracottatech.nomad.messages.PrepareMessage;
import com.terracottatech.nomad.messages.RollbackMessage;
import com.terracottatech.nomad.server.ChangeApplicator;
import com.terracottatech.nomad.server.NomadException;
import com.terracottatech.nomad.server.NomadServerImpl;
import com.terracottatech.nomad.server.SingleThreadedNomadServer;
import com.terracottatech.nomad.server.UpgradableNomadServer;
import com.terracottatech.nomad.server.UpgradableNomadServerAdapter;
import com.terracottatech.persistence.sanskrit.Sanskrit;
import com.terracottatech.persistence.sanskrit.SanskritException;
import com.terracottatech.persistence.sanskrit.file.FileBasedFilesystemDirectory;

public class UpgradableNomadServerFactory {
  public static UpgradableNomadServer<NodeContext> createServer(NomadRepositoryManager repositoryManager,
                                                                ChangeApplicator<NodeContext> changeApplicator,
                                                                String nodeName,
                                                                IParameterSubstitutor parameterSubstitutor) throws SanskritException, NomadException {
    return createServer(repositoryManager, changeApplicator, nodeName, parameterSubstitutor, new DynamicConfigListener() {});
  }

  public static UpgradableNomadServer<NodeContext> createServer(NomadRepositoryManager repositoryManager,
                                                                ChangeApplicator<NodeContext> changeApplicator,
                                                                String nodeName,
                                                                IParameterSubstitutor parameterSubstitutor,
                                                                DynamicConfigListener listener) throws SanskritException, NomadException {
    ObjectMapper objectMapper = Json.copyObjectMapper(true);
    FileBasedFilesystemDirectory filesystemDirectory = new FileBasedFilesystemDirectory(repositoryManager.getSanskritPath());
    Sanskrit sanskrit = Sanskrit.init(filesystemDirectory, objectMapper);

    InitialConfigStorage<NodeContext> configStorage = new InitialConfigStorage<>(new ConfigStorageAdapter<NodeContext>(new FileConfigStorage(repositoryManager.getConfigPath(), nodeName, parameterSubstitutor)) {
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
        listener.onNomadCommit(message, response);
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