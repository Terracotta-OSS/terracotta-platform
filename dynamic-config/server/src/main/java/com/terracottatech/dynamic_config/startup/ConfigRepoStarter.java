/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.startup;

import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.dynamic_config.model.Node;
import com.terracottatech.dynamic_config.parsing.Options;

import java.nio.file.Path;

import static com.terracottatech.dynamic_config.repository.NomadRepositoryManager.findNodeName;

public class ConfigRepoStarter extends NodeStarter {
  private final Options options;
  private final NodeStarter nextStarter;

  ConfigRepoStarter(Options options, NodeStarter nextStarter) {
    this.options = options;
    this.nextStarter = nextStarter;
  }

  @Override
  public void startNode(Cluster cluster, Node node) {
    Path nonNullConfigDir = getOrDefaultConfigDir(options.getNodeConfigDir());
    findNodeName(nonNullConfigDir).ifPresent(nodeName -> startUsingConfigRepo(nonNullConfigDir, nodeName));

    nextStarter.startNode(cluster, node);
  }
}
