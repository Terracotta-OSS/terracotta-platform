/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.startup;

import com.terracottatech.dynamic_config.parsing.Options;

import java.util.Map;

public class NodeProcessor {
  private final Options options;
  private final Map<String, String> paramValueMap;
  private final StartupManager startupManager;
  private final ClusterCreator clusterCreator;

  public NodeProcessor(Options options, Map<String, String> paramValueMap, ClusterCreator clusterCreator, StartupManager startupManager) {
    this.options = options;
    this.paramValueMap = paramValueMap;
    this.clusterCreator = clusterCreator;
    this.startupManager = startupManager;
  }

  public void process() {
    // Each NodeStarter either handles the startup itself or hands over to the next NodeStarter, following the chain-of-responsibility pattern
    NodeStarter third = new ConsoleParamsStarter(options, paramValueMap, clusterCreator, startupManager);
    NodeStarter second = new ConfigFileStarter(options, clusterCreator, startupManager, third);
    NodeStarter first = new ConfigRepoStarter(options, startupManager, second);
    first.startNode();
  }
}
