/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.startup;

import com.terracottatech.dynamic_config.diagnostic.LicensingService;
import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.dynamic_config.model.Node;
import com.terracottatech.dynamic_config.parsing.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static java.util.Objects.requireNonNull;

public class CliParamsStarter implements NodeStarter {
  private static final Logger LOGGER = LoggerFactory.getLogger(CliParamsStarter.class);

  private final Options options;
  private final Map<String, String> paramValueMap;
  private final ClusterCreator clusterCreator;
  private final LicensingService licensingService;
  private final StartupManager startupManager;

  CliParamsStarter(Options options, Map<String, String> paramValueMap, LicensingService licensingService,
                   ClusterCreator clusterCreator, StartupManager startupManager) {
    this.options = options;
    this.paramValueMap = paramValueMap;
    this.clusterCreator = clusterCreator;
    this.licensingService = licensingService;
    this.startupManager = startupManager;
  }

  @Override
  public void startNode() {
    LOGGER.info("Starting node from command-line parameters");
    Cluster cluster = clusterCreator.create(paramValueMap);
    Node node = cluster.getSingleNode().get(); // Cluster object will have only 1 node, just get that

    if (options.getLicenseFile() != null) {
      requireNonNull(options.getClusterName(), "Cluster name is required with license file");
      startupManager.startPreactivated(cluster, node, licensingService, options.getLicenseFile());
    } else {
      startupManager.startUnconfigured(cluster, node);
    }

    // If we're here, we've failed in our attempts to start the node
    throw new AssertionError("Exhausted all methods of starting the node. Giving up!");
  }
}
