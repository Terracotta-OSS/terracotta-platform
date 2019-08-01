/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.startup;

import com.terracottatech.dynamic_config.parsing.Options;

import java.nio.file.Path;

public class ConfigRepoStarter implements NodeStarter {
  private final Options options;
  private final NodeStarter nextStarter;
  private final StartupManager startupManager;

  ConfigRepoStarter(Options options, StartupManager startupManager, NodeStarter nextStarter) {
    this.options = options;
    this.nextStarter = nextStarter;
    this.startupManager = startupManager;
  }

  @Override
  public void startNode() {
    Path nonNullConfigDir = startupManager.getOrDefaultConfigDir(options.getNodeConfigDir());
    startupManager.findNodeName(nonNullConfigDir).ifPresent(nodeName -> startupManager.startUsingConfigRepo(nonNullConfigDir, nodeName));

    // Couldn't start node - pass the responsibility to the next starter
    nextStarter.startNode();
  }
}
