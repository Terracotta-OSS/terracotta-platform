/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.migration.nomad;

import com.terracottatech.dynamic_config.nomad.ConfigMigrationNomadChange;
import com.terracottatech.dynamic_config.nomad.NomadJson;
import com.terracottatech.dynamic_config.nomad.persistence.FileConfigStorage;
import com.terracottatech.dynamic_config.nomad.persistence.InitialConfigStorage;
import com.terracottatech.dynamic_config.nomad.persistence.SanskritNomadServerState;
import com.terracottatech.migration.util.FileUtility;
import com.terracottatech.nomad.client.change.NomadChange;
import com.terracottatech.nomad.server.ChangeApplicator;
import com.terracottatech.nomad.server.NomadException;
import com.terracottatech.nomad.server.NomadServer;
import com.terracottatech.nomad.server.NomadServerImpl;
import com.terracottatech.nomad.server.PotentialApplicationResult;
import com.terracottatech.persistence.sanskrit.Sanskrit;
import com.terracottatech.persistence.sanskrit.file.FileBasedFilesystemDirectory;

import java.nio.file.Path;

public class NomadServerProvider {

  public static NomadServer getNomadServer(Path repositoryRoot, String nodeName) throws Exception {

    Path sanskritPath = repositoryRoot.resolve("sanskrit");
    Path configPath = repositoryRoot.resolve("config");

    createDirectory(sanskritPath);
    createDirectory(configPath);

    ChangeApplicator changeApplicator = new ChangeApplicator() {

      @Override
      public PotentialApplicationResult canApply(final String existing, final NomadChange change) {
        return PotentialApplicationResult.allow(((ConfigMigrationNomadChange) change).getConfiguration());
      }

      @Override
      public void apply(final NomadChange change) throws NomadException {

      }
    };
    return new NomadServerImpl(new SanskritNomadServerState(
        Sanskrit.init(
            new FileBasedFilesystemDirectory(sanskritPath),
            NomadJson.buildObjectMapper()
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