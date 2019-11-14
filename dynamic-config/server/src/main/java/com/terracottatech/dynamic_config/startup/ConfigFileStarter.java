/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.startup;

import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.dynamic_config.model.ClusterFactory;
import com.terracottatech.dynamic_config.model.Node;
import com.terracottatech.dynamic_config.parsing.Options;
import com.terracottatech.dynamic_config.util.IParameterSubstitutor;
import com.terracottatech.dynamic_config.util.ParameterSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;

public class ConfigFileStarter implements NodeStarter {
  private static final Logger LOGGER = LoggerFactory.getLogger(ConfigFileStarter.class);
  private static final IParameterSubstitutor PARAMETER_SUBSTITUTOR = new ParameterSubstitutor();

  private final Options options;
  private final ClusterFactory clusterCreator;
  private final NodeStarter nextStarter;
  private final StartupManager startupManager;

  ConfigFileStarter(Options options, ClusterFactory clusterCreator,
                    StartupManager startupManager, NodeStarter nextStarter) {
    this.options = options;
    this.clusterCreator = clusterCreator;
    this.nextStarter = nextStarter;
    this.startupManager = startupManager;
  }

  @Override
  public void startNode() {
    if (options.getConfigFile() == null) {
      // If config file wasn't specified - pass the responsibility to the next starter
      nextStarter.startNode();
    }

    Path substitutedConfigFile = Paths.get(PARAMETER_SUBSTITUTOR.substitute(options.getConfigFile()));
    LOGGER.info("Starting node from config file: {}", substitutedConfigFile);
    Cluster cluster = clusterCreator.create(substitutedConfigFile, options.getClusterName());
    Node node = startupManager.getMatchingNodeFromConfigFile(options.getNodeHostname(), options.getNodePort(), options.getConfigFile(), cluster);

    if (options.getLicenseFile() != null) {
      if (cluster.getNodeCount() > 1) {
        //TODO [DYNAMIC-CONFIG] TRACK #6: relax this constraint
        throw new UnsupportedOperationException("License file option can be used only with a one-node cluster config file");
      }
      startupManager.startActivated(cluster, node, options.getLicenseFile(), options.getNodeRepositoryDir());
    } else {
      startupManager.startUnconfigured(cluster, node, options.getNodeRepositoryDir());
    }
  }
}
