/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.server.startup;

import org.terracotta.dynamic_config.api.model.Setting;
import org.terracotta.dynamic_config.api.service.ClusterFactory;
import org.terracotta.dynamic_config.api.service.IParameterSubstitutor;

import java.util.Map;

public class NodeProcessor {
  private final Options options;
  private final Map<Setting, String> paramValueMap;
  private final StartupManager startupManager;
  private final ClusterFactory clusterCreator;
  private final IParameterSubstitutor parameterSubstitutor;

  public NodeProcessor(Options options, Map<Setting, String> paramValueMap,
                       ClusterFactory clusterCreator,
                       StartupManager startupManager,
                       IParameterSubstitutor parameterSubstitutor) {
    this.options = options;
    this.paramValueMap = paramValueMap;
    this.clusterCreator = clusterCreator;
    this.startupManager = startupManager;
    this.parameterSubstitutor = parameterSubstitutor;
  }

  public void process() {
    // Each NodeStarter either handles the startup itself or hands over to the next NodeStarter, following the chain-of-responsibility pattern
    NodeStarter third = new ConsoleParamsStarter(options, paramValueMap, clusterCreator, startupManager, parameterSubstitutor);
    NodeStarter second = new ConfigFileStarter(options, clusterCreator, startupManager, third);
    NodeStarter first = new ConfigRepoStarter(options, startupManager, second, parameterSubstitutor);
    boolean started = first.startNode();

    if (!started) {
      // If we're here, we've failed in our attempts to start the node
      throw new AssertionError("Exhausted all methods of starting the node. Giving up!");
    }
  }
}
