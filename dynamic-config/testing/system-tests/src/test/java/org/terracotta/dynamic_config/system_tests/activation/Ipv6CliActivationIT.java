/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.system_tests.activation;

import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.SystemOutRule;
import org.terracotta.dynamic_config.system_tests.ClusterDefinition;
import org.terracotta.dynamic_config.system_tests.DynamicConfigIT;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.stringContainsInOrder;
import static org.junit.Assert.assertThat;
import static org.terracotta.dynamic_config.system_tests.util.AngelaMatchers.successful;

@ClusterDefinition(nodesPerStripe = 2)
public class Ipv6CliActivationIT extends DynamicConfigIT {

  @Rule public final SystemOutRule out = new SystemOutRule().enableLog();

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

    out.clearLog();
    assertThat(configToolInvocation("activate", "-s", "[::1]:" + getNodePort(), "-n", "tc-cluster"), is(successful()));
    waitUntil(out::getLog, containsString("Moved to State[ ACTIVE-COORDINATOR ]"));
  }

  @Test
  public void testMultiNodeStartupFromCliParamsAndActivateCommand() {
    waitUntil(out::getLog, stringContainsInOrder(asList("Started the server in diagnostic mode", "Started the server in diagnostic mode")));

    assertThat(configToolInvocation("attach", "-d", "[::1]:" + getNodePort(), "-s", "[::1]:" + getNodePort(1, 2)), is(successful()));

    out.clearLog();
    assertThat(configToolInvocation("activate", "-s", "[::1]:" + getNodePort(), "-n", "tc-cluster"), is(successful()));
    waitUntil(out::getLog, containsString("Moved to State[ ACTIVE-COORDINATOR ]"));
    waitUntil(out::getLog, containsString("Moved to State[ PASSIVE-STANDBY ]"));
  }
}
