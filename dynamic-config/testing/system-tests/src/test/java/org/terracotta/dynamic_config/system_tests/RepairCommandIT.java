/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.system_tests;

import org.junit.Before;
import org.junit.Test;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.cli.config_tool.ConfigTool;

import java.util.Arrays;

import static java.util.Collections.emptyMap;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.stringContainsInOrder;
import static org.junit.Assert.assertThat;
import static org.terracotta.testing.ExceptionMatcher.throwing;

/**
 * @author Mathieu Carbou
 */
@ClusterDefinition
public class RepairCommandIT extends DynamicConfigIT {

  @Before
  @Override
  public void before() throws Exception {
    super.before();
    activateCluster(() -> waitUntil(out::getLog, containsString("Moved to State[ ACTIVE-COORDINATOR ]")));
  }

  @SuppressWarnings("OptionalGetWithoutIsPresent")
  @Test
  public void test_automatic_commit_after_commit_failure() throws Exception {
    assertThat(
        () -> ConfigTool.start("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.tc-properties.org.terracotta.dynamic-config.simulate=recover-needed"),
        is(throwing(instanceOf(IllegalStateException.class))
            .andMessage(both(
                containsString("Commit failed for node localhost:" + getNodePort() + ". Reason: org.terracotta.nomad.server.NomadException: Error when applying setting change 'set tc-properties.org.terracotta.dynamic-config.simulate=recover-needed (stripe ID: 1, node: node-1)': Simulate commit failure"))
                .and(containsString("Please run the 'diagnostic' command to diagnose the configuration state and try to run the 'repair' command.")))));

    assertThat(getRuntimeCluster("localhost", getNodePort()).getSingleNode().get().getTcProperties(), is(equalTo(emptyMap())));
    assertThat(getUpcomingCluster("localhost", getNodePort()).getSingleNode().get().getTcProperties(), hasEntry("org.terracotta.dynamic-config.simulate", "recover-needed"));

    assertThat(
        () -> ConfigTool.start("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.tc-properties.org.terracotta.dynamic-config.simulate=recover-needed"),
        is(throwing(instanceOf(IllegalStateException.class))
            .andMessage(stringContainsInOrder(Arrays.asList("Another change (with UUID ", " is already underway on ", ". It was started by ", " on ")))));

    out.clearLog();
    ConfigTool.start("repair", "-s", "localhost:" + getNodePort());
    waitUntil(out::getLog, containsString("Attempting an automatic repair of the configuration..."));
    waitUntil(out::getLog, containsString("Configuration is repaired"));

    assertThat(getRuntimeCluster("localhost", getNodePort()).getSingleNode().get().getTcProperties(), hasEntry("org.terracotta.dynamic-config.simulate", "recover-needed"));
  }

  @SuppressWarnings("OptionalGetWithoutIsPresent")
  @Test
  public void test_node_starts_with_previous_config_when_not_committed_or_rollback() throws Exception {
    Cluster initialCluster = getRuntimeCluster("localhost", getNodePort());
    assertThat(initialCluster, is(equalTo(getUpcomingCluster("localhost", getNodePort()))));
    assertThat(getRuntimeCluster("localhost", getNodePort()).getSingleNode().get().getTcProperties(), is(equalTo(emptyMap())));

    assertThat(
        () -> ConfigTool.start("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.tc-properties.org.terracotta.dynamic-config.simulate=recover-needed"),
        is(throwing(instanceOf(IllegalStateException.class))
            .andMessage(both(
                containsString("Commit failed for node localhost:" + getNodePort() + ". Reason: org.terracotta.nomad.server.NomadException: Error when applying setting change 'set tc-properties.org.terracotta.dynamic-config.simulate=recover-needed (stripe ID: 1, node: node-1)': Simulate commit failure"))
                .and(containsString("Please run the 'diagnostic' command to diagnose the configuration state and try to run the 'repair' command.")))));

    assertThat(getRuntimeCluster("localhost", getNodePort()), is(equalTo(initialCluster)));
    assertThat(getUpcomingCluster("localhost", getNodePort()).getSingleNode().get().getTcProperties(), hasEntry("org.terracotta.dynamic-config.simulate", "recover-needed"));

    // close the server when the last change is not committed or rolled back
    getNodeProcess().close();

    // ensure the server can still start if the configuration is not committed
    out.clearLog();
    startNode(1, 1);
    waitUntil(out::getLog, containsString("INFO - Moved to State[ ACTIVE-COORDINATOR ]"));
    waitUntil(out::getLog, containsString("The configuration of this node has not been committed or rolled back. Please run the 'diagnostic' command to diagnose the configuration state."));

    // ensure that the server has started with the last committed config
    assertThat(getRuntimeCluster("localhost", getNodePort()), is(equalTo(initialCluster)));
    assertThat(getUpcomingCluster("localhost", getNodePort()), is(equalTo(initialCluster)));

    // repair the newly started server once (the simulated handler needs to repair after a restart - first one will fail)
    out.clearLog();
    assertThat(
        () -> ConfigTool.start("repair", "-s", "localhost:" + getNodePort()),
        is(throwing(instanceOf(IllegalStateException.class))
            .andMessage(both(
                containsString("Reason: org.terracotta.nomad.server.NomadException: Error when applying setting change 'set tc-properties.org.terracotta.dynamic-config.simulate=recover-needed (stripe ID: 1, node: node-1)': Simulate commit failure"))
                .and(containsString("Please run the 'diagnostic' command to diagnose the configuration state and try to run the 'repair' command.")))));
    waitUntil(out::getLog, containsString("Attempting an automatic repair of the configuration..."));
    waitUntil(out::getLog, not(containsString("Configuration is repaired.")));

    out.clearLog();
    ConfigTool.start("repair", "-s", "localhost:" + getNodePort());
    waitUntil(out::getLog, containsString("Attempting an automatic repair of the configuration..."));
    waitUntil(out::getLog, containsString("Configuration is repaired"));

    // ensure that the server has started with the last committed config
    assertThat(getRuntimeCluster("localhost", getNodePort()), is(not(equalTo(initialCluster))));
    assertThat(getRuntimeCluster("localhost", getNodePort()).getSingleNode().get().getTcProperties(), hasEntry("org.terracotta.dynamic-config.simulate", "recover-needed"));
  }
}
