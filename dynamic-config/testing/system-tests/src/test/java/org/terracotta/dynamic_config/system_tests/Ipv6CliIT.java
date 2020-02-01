/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.system_tests;

import org.junit.Test;
import org.terracotta.dynamic_config.cli.config_tool.ConfigTool;
import org.terracotta.dynamic_config.system_tests.util.NodeProcess;

import java.util.Arrays;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.stringContainsInOrder;

@ClusterDefinition(nodesPerStripe = 2)
public class Ipv6CliIT extends DynamicConfigIT {

  @Override
  protected NodeProcess startNode(int stripeId, int nodeId, int port, int groupPort) {
    return startNode(stripeId, nodeId,
        "--node-name", "node-" + nodeId,
        "--node-hostname", "::1",
        "--node-bind-address", "::",
        "--node-group-bind-address", "::",
        "--node-port", String.valueOf(port),
        "--node-group-port", String.valueOf(groupPort),
        "--node-log-dir", "logs/stripe" + stripeId + "/node-" + nodeId,
        "--node-backup-dir", "backup/stripe" + stripeId,
        "--node-metadata-dir", "metadata/stripe" + stripeId,
        "--node-repository-dir", "repository/stripe" + stripeId + "/node-" + nodeId,
        "--data-dirs", "main:user-data/main/stripe" + stripeId
    );
  }

  @Test
  public void testSingleNodeStartupFromCliParamsAndActivateCommand() {
    waitUntil(out::getLog, containsString("Started the server in diagnostic mode"));

    ConfigTool.start("activate", "-s", "[::1]:" + getNodePort(), "-n", "tc-cluster");
    waitUntil(out::getLog, containsString("Moved to State[ ACTIVE-COORDINATOR ]"));
  }

  @Test
  public void testMultiNodeStartupFromCliParamsAndActivateCommand() {
    waitUntil(out::getLog, stringContainsInOrder(
        Arrays.asList("Started the server in diagnostic mode", "Started the server in diagnostic mode")
    ));

    ConfigTool.start("attach", "-d", "[::1]:" + getNodePort(), "-s", "[::1]:" + getNodePort(1, 2));
    assertCommandSuccessful();

    ConfigTool.start("activate", "-s", "[::1]:" + getNodePort(), "-n", "tc-cluster");
    waitUntil(out::getLog, containsString("Moved to State[ ACTIVE-COORDINATOR ]"));
    waitUntil(out::getLog, containsString("Moved to State[ PASSIVE-STANDBY ]"));
  }
}
