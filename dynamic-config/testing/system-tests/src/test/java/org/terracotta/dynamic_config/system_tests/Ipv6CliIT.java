/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.system_tests;

import org.junit.Test;

import java.util.Arrays;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.stringContainsInOrder;

@ClusterDefinition(nodesPerStripe = 2)
public class Ipv6CliIT extends DynamicConfigIT {

  @Override
  protected void startNode(int stripeId, int nodeId) {
    String uniqueId = combine(stripeId, nodeId);
    startNode(stripeId, nodeId,
        "--node-name", "node" + uniqueId,
        "--node-hostname", "::1",
        "--node-bind-address", "::",
        "--node-group-bind-address", "::",
        "--node-port", String.valueOf(getNodePort(stripeId, nodeId)),
        "--node-group-port", String.valueOf(getNodeGroupPort(stripeId, nodeId)),
        "--node-log-dir", "terracotta" + uniqueId + "/logs",
        "--node-backup-dir", "terracotta" + uniqueId + "/backup",
        "--node-metadata-dir", "terracotta" + uniqueId + "/metadata",
        "--node-repository-dir", "terracotta" + uniqueId + "/repository",
        "--data-dirs", "main:terracotta" + uniqueId + "/data-dir"
    );
  }

  @Test
  public void testSingleNodeStartupFromCliParamsAndActivateCommand() {
    waitUntil(out::getLog, containsString("Started the server in diagnostic mode"));

    configToolInvocation("activate", "-s", "[::1]:" + getNodePort(), "-n", "tc-cluster");
    waitUntil(out::getLog, containsString("Moved to State[ ACTIVE-COORDINATOR ]"));
  }

  @Test
  public void testMultiNodeStartupFromCliParamsAndActivateCommand() {
    waitUntil(out::getLog, stringContainsInOrder(
        Arrays.asList("Started the server in diagnostic mode", "Started the server in diagnostic mode")
    ));

    configToolInvocation("attach", "-d", "[::1]:" + getNodePort(), "-s", "[::1]:" + getNodePort(1, 2));
    assertCommandSuccessful();

    configToolInvocation("activate", "-s", "[::1]:" + getNodePort(), "-n", "tc-cluster");
    waitUntil(out::getLog, containsString("Moved to State[ ACTIVE-COORDINATOR ]"));
    waitUntil(out::getLog, containsString("Moved to State[ PASSIVE-STANDBY ]"));
  }
}
