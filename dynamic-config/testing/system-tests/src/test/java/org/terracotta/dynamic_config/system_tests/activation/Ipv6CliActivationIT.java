/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.system_tests.activation;

import org.junit.Rule;
import org.junit.Test;
import org.terracotta.dynamic_config.system_tests.ClusterDefinition;
import org.terracotta.dynamic_config.system_tests.DynamicConfigIT;
import org.terracotta.dynamic_config.system_tests.util.NodeOutputRule;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.terracotta.dynamic_config.system_tests.util.AngelaMatchers.containsLog;
import static org.terracotta.dynamic_config.system_tests.util.AngelaMatchers.successful;

@ClusterDefinition(nodesPerStripe = 2)
public class Ipv6CliActivationIT extends DynamicConfigIT {

  @Rule public final NodeOutputRule out = new NodeOutputRule();

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
    waitUntil(out.getLog(1, 1), containsLog("Started the server in diagnostic mode"));

    out.clearLog(1, 1);
    assertThat(configToolInvocation("activate", "-s", "[::1]:" + getNodePort(), "-n", "tc-cluster"), is(successful()));
    waitUntil(out.getLog(1, findActive(1).getAsInt()), containsLog("Moved to State[ ACTIVE-COORDINATOR ]"));
  }

  @Test
  public void testMultiNodeStartupFromCliParamsAndActivateCommand() {
    waitUntil(out.getLog(1, 1), containsLog("Started the server in diagnostic mode"));
    waitUntil(out.getLog(1, 2), containsLog("Started the server in diagnostic mode"));

    assertThat(configToolInvocation("attach", "-d", "[::1]:" + getNodePort(), "-s", "[::1]:" + getNodePort(1, 2)), is(successful()));

    out.clearLog();
    assertThat(configToolInvocation("activate", "-s", "[::1]:" + getNodePort(), "-n", "tc-cluster"), is(successful()));
    waitUntil(out.getLog(1, findActive(1).getAsInt()), containsLog("Moved to State[ ACTIVE-COORDINATOR ]"));
    waitUntil(out.getLog(1, findPassives(1)[0]), containsLog("Moved to State[ PASSIVE-STANDBY ]"));
  }
}
