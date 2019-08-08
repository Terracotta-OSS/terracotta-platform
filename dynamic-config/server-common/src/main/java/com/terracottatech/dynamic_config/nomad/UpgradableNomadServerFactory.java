/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.nomad;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.terracottatech.dynamic_config.model.NodeContext;
import com.terracottatech.dynamic_config.nomad.persistence.DefaultHashComputer;
import com.terracottatech.dynamic_config.nomad.persistence.FileConfigStorage;
import com.terracottatech.dynamic_config.nomad.persistence.InitialConfigStorage;
import com.terracottatech.dynamic_config.nomad.persistence.SanskritNomadServerState;
import com.terracottatech.dynamic_config.repository.NomadRepositoryManager;
import com.terracottatech.nomad.server.ChangeApplicator;
import com.terracottatech.nomad.server.NomadException;
import com.terracottatech.nomad.server.NomadServerImpl;
import com.terracottatech.nomad.server.UpgradableNomadServer;
import com.terracottatech.persistence.sanskrit.Sanskrit;
import com.terracottatech.persistence.sanskrit.SanskritException;
import com.terracottatech.persistence.sanskrit.file.FileBasedFilesystemDirectory;

public class UpgradableNomadServerFactory {
  public static UpgradableNomadServer<NodeContext> createServer(NomadRepositoryManager repositoryManager,
                                                             ChangeApplicator<NodeContext> changeApplicator,
                                                             String nodeName) throws SanskritException, NomadException {
    ObjectMapper objectMapper = NomadJson.buildObjectMapper();
    return new NomadServerImpl<>(
        new SanskritNomadServerState<>(
            Sanskrit.init(
                new FileBasedFilesystemDirectory(repositoryManager.getSanskritPath()),
                objectMapper
            ),
            new InitialConfigStorage<>(
                new FileConfigStorage(
                    repositoryManager.getConfigPath(),
                    nodeName                )
            ),
            new DefaultHashComputer(objectMapper)
        ),
        changeApplicator
    );
  }
}