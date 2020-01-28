/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.server.nomad;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.terracottatech.dynamic_config.api.model.NodeContext;
import com.terracottatech.dynamic_config.api.service.DynamicConfigListener;
import com.terracottatech.dynamic_config.api.service.IParameterSubstitutor;
import com.terracottatech.dynamic_config.api.service.PathResolver;
import com.terracottatech.dynamic_config.api.service.XmlConfigMapper;
import com.terracottatech.dynamic_config.api.service.XmlConfigMapperDiscovery;
import com.terracottatech.dynamic_config.server.nomad.persistence.ConfigStorageAdapter;
import com.terracottatech.dynamic_config.server.nomad.persistence.ConfigStorageException;
import com.terracottatech.dynamic_config.server.nomad.persistence.DefaultHashComputer;
import com.terracottatech.dynamic_config.server.nomad.persistence.FileConfigStorage;
import com.terracottatech.dynamic_config.server.nomad.persistence.InitialConfigStorage;
import com.terracottatech.dynamic_config.server.nomad.persistence.SanskritNomadServerState;
import com.terracottatech.dynamic_config.server.nomad.repository.NomadRepositoryManager;
import com.terracottatech.json.Json;
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

import java.nio.file.Paths;

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

    // This path resolver is used when converting a model to XML.
    // It makes sure to resolve any relative path to absolute ones based on the working directory.
    // This is necessary because if some relative path ends up in the XML exactly like they are in the model,
    // then platform will rebase these paths relatively to the config XML file which is inside a sub-folder in
    // the config repository: repository/config.
    // So this has the effect of putting all defined directories inside such as repository/config/logs, repository/config/user-data, repository/metadata, etc
    // That is why we need to force the resolving within the XML relatively to the user directory.
    PathResolver userDirResolver = new PathResolver(Paths.get("%(user.dir)"), parameterSubstitutor::substitute);
    XmlConfigMapper xmlConfigMapper = new XmlConfigMapperDiscovery(userDirResolver).find()
        .orElseThrow(() -> new AssertionError("No " + XmlConfigMapper.class.getName() + " service implementation found on classpath"));

    InitialConfigStorage<NodeContext> configStorage = new InitialConfigStorage<>(new ConfigStorageAdapter<NodeContext>(new FileConfigStorage(repositoryManager.getConfigPath(), nodeName, xmlConfigMapper)) {
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