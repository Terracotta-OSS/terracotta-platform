/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.system_tests.util;

import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.server.conversion.ConfigRepoProcessor;
import org.terracotta.nomad.server.NomadServer;

import java.nio.file.Path;
import java.util.Map;

public class ConversionITResultProcessor extends ConfigRepoProcessor {
  private final Map<String, NomadServer<NodeContext>> serverMap;

  public ConversionITResultProcessor(Path outputFolderPath, Map<String, NomadServer<NodeContext>> serverMap) {
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