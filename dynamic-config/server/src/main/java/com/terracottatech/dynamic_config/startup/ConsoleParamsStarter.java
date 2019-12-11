/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.startup;

import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.dynamic_config.model.ClusterFactory;
import com.terracottatech.dynamic_config.model.Node;
import com.terracottatech.dynamic_config.model.Setting;
import com.terracottatech.dynamic_config.parsing.Options;
import com.terracottatech.dynamic_config.util.IParameterSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static java.util.Objects.requireNonNull;

public class ConsoleParamsStarter implements NodeStarter {
  private static final Logger LOGGER = LoggerFactory.getLogger(ConsoleParamsStarter.class);

  private final Options options;
  private final Map<Setting, String> paramValueMap;
  private final ClusterFactory clusterCreator;
  private final StartupManager startupManager;
  private final IParameterSubstitutor parameterSubstitutor;

  ConsoleParamsStarter(Options options, Map<Setting, String> paramValueMap,
                       ClusterFactory clusterCreator,
                       StartupManager startupManager,
                       IParameterSubstitutor parameterSubstitutor) {
    this.options = options;
    this.paramValueMap = paramValueMap;
    this.clusterCreator = clusterCreator;
    this.startupManager = startupManager;
    this.parameterSubstitutor = parameterSubstitutor;
  }

  @Override
  public boolean startNode() {
    LOGGER.info("Starting node from command-line parameters");
    Cluster cluster = clusterCreator.create(paramValueMap, parameterSubstitutor);
    Node node = cluster.getSingleNode().get(); // Cluster object will have only 1 node, just get that

    if (options.getLicenseFile() != null) {
      requireNonNull(options.getClusterName(), "Cluster name is required with license file");
      return startupManager.startActivated(cluster, node, options.getLicenseFile(), options.getNodeRepositoryDir());
    } else {
      return startupManager.startUnconfigured(cluster, node, options.getNodeRepositoryDir());
    }
  }
}
