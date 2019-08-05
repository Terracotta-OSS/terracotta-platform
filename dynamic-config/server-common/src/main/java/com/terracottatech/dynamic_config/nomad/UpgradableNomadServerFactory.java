/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.nomad;

import com.terracottatech.dynamic_config.nomad.persistence.FileConfigStorage;
import com.terracottatech.dynamic_config.nomad.persistence.InitialConfigStorage;
import com.terracottatech.dynamic_config.nomad.persistence.SanskritNomadServerState;
import com.terracottatech.dynamic_config.repository.NomadRepositoryManager;
import com.terracottatech.nomad.server.ChangeApplicator;
import com.terracottatech.nomad.server.NomadException;
import com.terracottatech.nomad.server.NomadServerImpl;
import com.terracottatech.nomad.server.UpgradableNomadServer;
import com.terracottatech.persistence.sanskrit.HashUtils;
import com.terracottatech.persistence.sanskrit.Sanskrit;
import com.terracottatech.persistence.sanskrit.SanskritException;
import com.terracottatech.persistence.sanskrit.file.FileBasedFilesystemDirectory;

import static com.terracottatech.dynamic_config.repository.RepositoryConstants.FILENAME_EXT;
import static com.terracottatech.dynamic_config.repository.RepositoryConstants.FILENAME_PREFIX;

public class UpgradableNomadServerFactory {
  public static UpgradableNomadServer<String> createServer(NomadRepositoryManager repositoryManager,
                                                           ChangeApplicator<String> changeApplicator,
                                                           String nodeName) throws SanskritException, NomadException {
    return new NomadServerImpl<>(
        new SanskritNomadServerState<>(
            Sanskrit.init(
                new FileBasedFilesystemDirectory(repositoryManager.getSanskritPath()),
                NomadJson.buildObjectMapper()
            ),
            new InitialConfigStorage<>(
                new FileConfigStorage(
                    repositoryManager.getConfigurationPath(),
                    version -> FILENAME_PREFIX + "." + nodeName + "." + version + "." + FILENAME_EXT
                )
            ),
            HashUtils::generateHash
        ),
        changeApplicator
    );
  }
}