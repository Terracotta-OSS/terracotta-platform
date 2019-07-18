/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.test.util;

import com.terracottatech.migration.nomad.RepositoryStructureBuilder;
import com.terracottatech.nomad.server.NomadServer;

import java.nio.file.Path;
import java.util.Map;

public class MigrationITResultProcessor extends RepositoryStructureBuilder {
  final Map<String, NomadServer> serverMap;

  public MigrationITResultProcessor(Path outputFolderPath, Map<String, NomadServer> serverMap) {
    super(outputFolderPath);
    this.serverMap = serverMap;
  }

  @Override
  protected NomadServer getNomadServer(final String nodeName) throws Exception {
    NomadServer nomadServer = super.getNomadServer(nodeName);
    serverMap.put(nodeName, nomadServer);
    return nomadServer;
  }

  @Override
  protected NomadServer getNomadServer(final int stripeId, final String nodeName) throws Exception {
    NomadServer nomadServer = super.getNomadServer(stripeId, nodeName);
    serverMap.put("stripe" + stripeId + "_" + nodeName, nomadServer);
    return nomadServer;
  }
}