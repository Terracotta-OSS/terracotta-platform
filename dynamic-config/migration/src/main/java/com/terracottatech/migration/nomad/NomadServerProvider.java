/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.migration.nomad;

import com.terracottatech.migration.util.FileUtility;
import com.terracottatech.nomad.server.ChangeApplicator;
import com.terracottatech.nomad.server.NomadException;
import com.terracottatech.nomad.server.NomadServer;
import com.terracottatech.nomad.server.NomadServerImpl;
import com.terracottatech.nomad.server.PotentialApplicationResult;
import com.terracottatech.persistence.sanskrit.Sanskrit;
import com.terracottatech.persistence.sanskrit.file.FileBasedFilesystemDirectory;

import com.terracottatech.tools.server.nomad.persistence.FileConfigStorage;
import com.terracottatech.tools.server.nomad.persistence.InitialConfigStorage;
import com.terracottatech.tools.server.nomad.persistence.SanskritNomadServerState;

import java.nio.file.Path;

public class NomadServerProvider {

  public static NomadServer getNomadServer(Path repositoryRoot, String nodeName) throws Exception {

    Path sanskritPath = repositoryRoot.resolve("sanskrit");
    Path configPath = repositoryRoot.resolve("config");

    createDirectory(sanskritPath);
    createDirectory(configPath);

    ChangeApplicator changeApplicator = new ChangeApplicator() {

      @Override
      public PotentialApplicationResult canApply(final String existing, final String change, final String summary) {
        return PotentialApplicationResult.allow(change);
      }

      @Override
      public void apply(final String change, final String summary) throws NomadException {

      }
    };
    return new NomadServerImpl(new SanskritNomadServerState(
        Sanskrit.init(
            new FileBasedFilesystemDirectory(
                sanskritPath
            )
        ),
        new InitialConfigStorage(
            new FileConfigStorage(
                configPath,
                version -> "cluster-config." + nodeName + "." + version + ".xml"
            )
        )
    ), changeApplicator);
  }


  protected static void createDirectory(Path directory) throws Exception {
    FileUtility.createDirectory(directory);
  }
}