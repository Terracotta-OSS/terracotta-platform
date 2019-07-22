/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.startup;

import com.terracottatech.dynamic_config.diagnostic.LicensingService;
import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.dynamic_config.model.Node;
import com.terracottatech.dynamic_config.parsing.Options;

import java.util.Map;

import static com.terracottatech.utilities.Assertion.assertNonNull;

public class CliParamsStarter extends NodeStarter {
  private final Options options;
  private final Map<String, String> paramValueMap;
  private final LicensingService licensingService;

  CliParamsStarter(Options options, Map<String, String> paramValueMap, LicensingService licensingService) {
    this.options = options;
    this.paramValueMap = paramValueMap;
    this.licensingService = licensingService;
  }

  @Override
  public void startNode(Cluster cluster, Node node) {
    logger.info("Starting node from command-line parameters");
    cluster = ClusterCreator.createCluster(paramValueMap);
    node = cluster.getSingleNode().get(); // Cluster object will have only 1 node, just get that

    if (options.getLicenseFile() != null) {
      assertNonNull(options.getClusterName(), "clusterName must not be null and must be validated in " + Options.class.getName());
      startPreactivated(cluster, node, licensingService, options.getLicenseFile());
    } else {
      startUnconfigured(cluster, node);
    }
    // If we're here, we've failed in our attempts to start the node
    throw new AssertionError("Tried all methods of starting the node. Giving up!");
  }
}
