/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.server.startup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.Setting;
import org.terracotta.dynamic_config.api.service.ClusterFactory;
import org.terracotta.dynamic_config.api.service.IParameterSubstitutor;
import org.terracotta.dynamic_config.server.parsing.Options;

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

  @SuppressWarnings("OptionalGetWithoutIsPresent")
  @Override
  public boolean startNode() {
    LOGGER.info("Starting node from command-line parameters");
    Cluster cluster = clusterCreator.create(paramValueMap, parameterSubstitutor);
    Node node = cluster.getSingleNode().get(); // Cluster object will have only 1 node, just get that

    if (options.getLicenseFile() != null) {
      requireNonNull(cluster.getName(), "Cluster name is required with license file");
    }
    if (cluster.getName() != null) {
      return startupManager.startActivated(cluster, node, options.getLicenseFile(), options.getNodeRepositoryDir());
    } else {
      return startupManager.startUnconfigured(cluster, node, options.getNodeRepositoryDir());
    }
  }
}
