/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.nomad;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.terracottatech.dynamic_config.model.NodeContext;
import com.terracottatech.dynamic_config.nomad.persistence.ConfigStorageAdapter;
import com.terracottatech.dynamic_config.nomad.persistence.ConfigStorageException;
import com.terracottatech.dynamic_config.nomad.persistence.DefaultHashComputer;
import com.terracottatech.dynamic_config.nomad.persistence.FileConfigStorage;
import com.terracottatech.dynamic_config.nomad.persistence.InitialConfigStorage;
import com.terracottatech.dynamic_config.nomad.persistence.SanskritNomadServerState;
import com.terracottatech.dynamic_config.repository.NomadRepositoryManager;
import com.terracottatech.dynamic_config.util.IParameterSubstitutor;
import com.terracottatech.nomad.server.ChangeApplicator;
import com.terracottatech.nomad.server.NomadException;
import com.terracottatech.nomad.server.NomadServerImpl;
import com.terracottatech.nomad.server.SingleThreadedNomadServer;
import com.terracottatech.nomad.server.UpgradableNomadServer;
import com.terracottatech.persistence.sanskrit.Sanskrit;
import com.terracottatech.persistence.sanskrit.SanskritException;
import com.terracottatech.persistence.sanskrit.file.FileBasedFilesystemDirectory;
import com.terracottatech.utilities.Json;

import java.util.function.Consumer;

public class UpgradableNomadServerFactory {
  public static UpgradableNomadServer<NodeContext> createServer(NomadRepositoryManager repositoryManager,
                                                                ChangeApplicator<NodeContext> changeApplicator,
                                                                String nodeName,
                                                                IParameterSubstitutor parameterSubstitutor) throws SanskritException, NomadException {
    return createServer(repositoryManager, changeApplicator, nodeName, parameterSubstitutor, nodeContext -> {
    });
  }

  public static UpgradableNomadServer<NodeContext> createServer(NomadRepositoryManager repositoryManager,
                                                                ChangeApplicator<NodeContext> changeApplicator,
                                                                String nodeName,
                                                                IParameterSubstitutor parameterSubstitutor,
                                                                Consumer<NodeContext> changeCommitted) throws SanskritException, NomadException {
    ObjectMapper objectMapper = Json.copyObjectMapper(true);
    FileBasedFilesystemDirectory filesystemDirectory = new FileBasedFilesystemDirectory(repositoryManager.getSanskritPath());
    Sanskrit sanskrit = Sanskrit.init(filesystemDirectory, objectMapper);

    InitialConfigStorage<NodeContext> configStorage = new InitialConfigStorage<>(new ConfigStorageAdapter<NodeContext>(new FileConfigStorage(repositoryManager.getConfigPath(), nodeName, parameterSubstitutor)) {
      @Override
      public void saveConfig(long version, NodeContext config) throws ConfigStorageException {
        super.saveConfig(version, config);
        changeCommitted.accept(config);
      }
    });

    SanskritNomadServerState<NodeContext> serverState = new SanskritNomadServerState<>(sanskrit, configStorage, new DefaultHashComputer(objectMapper));

    return new SingleThreadedNomadServer<>(new NomadServerImpl<>(serverState, changeApplicator));
  }
}