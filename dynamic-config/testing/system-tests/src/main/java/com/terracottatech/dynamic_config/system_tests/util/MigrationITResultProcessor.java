/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.system_tests.util;

import com.terracottatech.dynamic_config.api.model.NodeContext;
import com.terracottatech.dynamic_config.server.migration.RepositoryStructureBuilder;
import com.terracottatech.nomad.server.NomadServer;

import java.nio.file.Path;
import java.util.Map;

public class MigrationITResultProcessor extends RepositoryStructureBuilder {
  private final Map<String, NomadServer<NodeContext>> serverMap;

  public MigrationITResultProcessor(Path outputFolderPath, Map<String, NomadServer<NodeContext>> serverMap) {
    super(outputFolderPath);
    this.serverMap = serverMap;
  }

  @Override
  protected NomadServer<NodeContext> getNomadServer(final int stripeId, final String nodeName) throws Exception {
    NomadServer<NodeContext> nomadServer = super.getNomadServer(stripeId, nodeName);
    serverMap.put("stripe" + stripeId + "_" + nodeName, nomadServer);
    return nomadServer;
  }
}