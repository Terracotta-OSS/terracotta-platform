/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.server.nomad;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.service.ConfigRepositoryMapper;
import org.terracotta.dynamic_config.api.service.ConfigRepositoryMapperDiscovery;
import org.terracotta.dynamic_config.api.service.DynamicConfigListener;
import org.terracotta.dynamic_config.api.service.IParameterSubstitutor;
import org.terracotta.dynamic_config.api.service.PathResolver;
import org.terracotta.dynamic_config.server.nomad.persistence.ConfigStorageAdapter;
import org.terracotta.dynamic_config.server.nomad.persistence.ConfigStorageException;
import org.terracotta.dynamic_config.server.nomad.persistence.DefaultHashComputer;
import org.terracotta.dynamic_config.server.nomad.persistence.FileConfigStorage;
import org.terracotta.dynamic_config.server.nomad.persistence.InitialConfigStorage;
import org.terracotta.dynamic_config.server.nomad.persistence.NomadRepositoryManager;
import org.terracotta.dynamic_config.server.nomad.persistence.SanskritNomadServerState;
import org.terracotta.json.Json;
import org.terracotta.nomad.messages.AcceptRejectResponse;
import org.terracotta.nomad.messages.CommitMessage;
import org.terracotta.nomad.messages.PrepareMessage;
import org.terracotta.nomad.messages.RollbackMessage;
import org.terracotta.nomad.server.ChangeApplicator;
import org.terracotta.nomad.server.NomadException;
import org.terracotta.nomad.server.NomadServerImpl;
import org.terracotta.nomad.server.SingleThreadedNomadServer;
import org.terracotta.nomad.server.UpgradableNomadServer;
import org.terracotta.nomad.server.UpgradableNomadServerAdapter;
import org.terracotta.persistence.sanskrit.Sanskrit;
import org.terracotta.persistence.sanskrit.SanskritException;
import org.terracotta.persistence.sanskrit.file.FileBasedFilesystemDirectory;

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
    ConfigRepositoryMapper configRepositoryMapper = new ConfigRepositoryMapperDiscovery(userDirResolver).find()
        .orElseThrow(() -> new AssertionError("No " + ConfigRepositoryMapper.class.getName() + " service implementation found on classpath"));

    InitialConfigStorage<NodeContext> configStorage = new InitialConfigStorage<>(new ConfigStorageAdapter<NodeContext>(new FileConfigStorage(repositoryManager.getConfigPath(), nodeName, configRepositoryMapper)) {
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