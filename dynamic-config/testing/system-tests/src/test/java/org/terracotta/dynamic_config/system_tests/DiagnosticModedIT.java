/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.system_tests;

import org.junit.Test;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.cli.config_tool.ConfigTool;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.terracotta.testing.ExceptionMatcher.throwing;

/**
 * @author Mathieu Carbou
 */
@ClusterDefinition(stripes = 2, nodesPerStripe = 2, autoActivate = true)
public class DiagnosticModedIT extends DynamicConfigIT {

  @SuppressWarnings("OptionalGetWithoutIsPresent")
  @Test
  public void test_restart_active_in_diagnostic_mode() {
    int activeNodeId = findActive(1).getAsInt();
    getNodeProcess(activeNodeId, activeNodeId).close();
    startNode(1, activeNodeId, "--diagnostic-mode", "--node-name", "node-" + activeNodeId, "--node-repository-dir", "repository/stripe1/node-" + activeNodeId);
    waitUntil(out::getLog, containsString("Node is starting in diagnostic mode. This mode is used to manually repair a broken configuration on a node."));
    waitUntil(out::getLog, containsString("Started the server in diagnostic mode"));
  }

  @Test
  public void test_restart_passive_in_diagnostic_mode() {
    int passiveNodeId = findPassives(1)[0];
    getNodeProcess(passiveNodeId, passiveNodeId).close();
    startNode(1, passiveNodeId, "--diagnostic-mode", "--node-name", "node-" + passiveNodeId, "--node-repository-dir", "repository/stripe1/node-" + passiveNodeId);
    waitUntil(out::getLog, containsString("Node is starting in diagnostic mode. This mode is used to manually repair a broken configuration on a node."));
    waitUntil(out::getLog, containsString("Started the server in diagnostic mode"));
  }

  @SuppressWarnings("OptionalGetWithoutIsPresent")
  @Test
  public void test_diagnostic_port_accessible_but_nomad_change_impossible() throws Exception {
    int activeNodeId = findActive(1).getAsInt();
    getNodeProcess(activeNodeId, activeNodeId).close();

    out.clearLog();
    startNode(1, activeNodeId, "--diagnostic-mode", "--node-name", "node-" + activeNodeId, "--node-repository-dir", "repository/stripe1/node-" + activeNodeId);
    waitUntil(out::getLog, containsString("Node is starting in diagnostic mode. This mode is used to manually repair a broken configuration on a node."));
    waitUntil(out::getLog, containsString("Started the server in diagnostic mode"));

    // diag port available
    Cluster cluster = getUpcomingCluster("localhost", getNodePort(1, activeNodeId));
    assertThat(cluster.getStripeCount(), is(equalTo(2)));

    // log command works, both when targeting node to repair and a normal node in the cluster
    out.clearLog();
    ConfigTool.start("log", "-s", "localhost:" + getNodePort(1, activeNodeId));
    waitUntil(out::getLog, containsString("Activating cluster"));
    out.clearLog();
    ConfigTool.start("log", "-s", "localhost:" + getNodePort(2, 1));
    waitUntil(out::getLog, containsString("Activating cluster"));

    // diag command works, both when targeting node to repair and a normal node in the cluster
    out.clearLog();
    ConfigTool.start("diagnostic", "-s", "localhost:" + getNodePort(1, activeNodeId));
    waitUntil(out::getLog, containsString("Node started in diagnostic mode for initial configuration or repair: YES"));
    out.clearLog();
    ConfigTool.start("diagnostic", "-s", "localhost:" + getNodePort(2, 1));
    waitUntil(out::getLog, containsString("Node started in diagnostic mode for initial configuration or repair: YES"));


    // unable to trigger a change on the cluster from the node in diagnostic mode
    assertThat(
        () -> ConfigTool.start("set", "-s", "localhost:" + getNodePort(1, activeNodeId), "-c", "stripe.1.node." + activeNodeId + ".tc-properties.something=value"),
        is(throwing(instanceOf(IllegalStateException.class))
            .andMessage(containsString("Detected a mix of activated and unconfigured nodes (or being repaired)."))));

    // unable to trigger a change on the cluster from any other node
    assertThat(
        () -> ConfigTool.start("set", "-s", "localhost:" + getNodePort(2, 1), "-c", "stripe.2.node.1.tc-properties.something=value"),
        is(throwing(instanceOf(IllegalStateException.class))
            .andMessage(containsString("Detected a mix of activated and unconfigured nodes (or being repaired)."))));
  }
}
