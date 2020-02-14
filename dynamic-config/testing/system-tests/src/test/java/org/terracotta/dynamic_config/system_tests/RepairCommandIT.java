/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.system_tests;

import org.junit.Test;
import org.terracotta.dynamic_config.api.model.Cluster;

import java.util.Arrays;

import static java.util.Collections.emptyMap;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.stringContainsInOrder;
import static org.junit.Assert.assertThat;
import static org.terracotta.dynamic_config.system_tests.util.AngelaMatchers.containsOutput;

/**
 * @author Mathieu Carbou
 */
@ClusterDefinition(autoActivate = true)
public class RepairCommandIT extends DynamicConfigIT {

  @SuppressWarnings("OptionalGetWithoutIsPresent")
  @Test
  public void test_automatic_commit_after_commit_failure() throws Exception {
    assertThat(
        configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.tc-properties.org.terracotta.dynamic-config.simulate=recover-needed"),
        allOf(
            containsOutput("Commit failed for node localhost:" + getNodePort() + ". Reason: org.terracotta.nomad.server.NomadException: Error when applying setting change 'set tc-properties.org.terracotta.dynamic-config.simulate=recover-needed (stripe ID: 1, node: node1-1)': Simulate commit failure"),
            containsOutput("Please run the 'diagnostic' command to diagnose the configuration state and try to run the 'repair' command.")));

    assertThat(getRuntimeCluster("localhost", getNodePort()).getSingleNode().get().getTcProperties(), is(equalTo(emptyMap())));
    assertThat(getUpcomingCluster("localhost", getNodePort()).getSingleNode().get().getTcProperties(), hasEntry("org.terracotta.dynamic-config.simulate", "recover-needed"));

    assertThat(
        configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.tc-properties.org.terracotta.dynamic-config.simulate=recover-needed").toString(),
        stringContainsInOrder(Arrays.asList("Another change (with UUID ", " is already underway on ", ". It was started by ", " on ")));

    out.clearLog();
    assertThat(
        configToolInvocation("repair", "-s", "localhost:" + getNodePort()),
        allOf(
            containsOutput("Attempting an automatic repair of the configuration..."),
            containsOutput("Configuration is repaired")));

    assertThat(getRuntimeCluster("localhost", getNodePort()).getSingleNode().get().getTcProperties(), hasEntry("org.terracotta.dynamic-config.simulate", "recover-needed"));
  }

  @SuppressWarnings("OptionalGetWithoutIsPresent")
  @Test
  public void test_node_starts_with_previous_config_when_not_committed_or_rollback() throws Exception {
    Cluster initialCluster = getRuntimeCluster("localhost", getNodePort());
    assertThat(initialCluster, is(equalTo(getUpcomingCluster("localhost", getNodePort()))));
    assertThat(getRuntimeCluster("localhost", getNodePort()).getSingleNode().get().getTcProperties(), is(equalTo(emptyMap())));

    assertThat(
        configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.tc-properties.org.terracotta.dynamic-config.simulate=recover-needed"),
        allOf(
            containsOutput("Commit failed for node localhost:" + getNodePort() + ". Reason: org.terracotta.nomad.server.NomadException: Error when applying setting change 'set tc-properties.org.terracotta.dynamic-config.simulate=recover-needed (stripe ID: 1, node: node1-1)': Simulate commit failure"),
            containsOutput("Please run the 'diagnostic' command to diagnose the configuration state and try to run the 'repair' command.")));

    assertThat(getRuntimeCluster("localhost", getNodePort()), is(equalTo(initialCluster)));
    assertThat(getUpcomingCluster("localhost", getNodePort()).getSingleNode().get().getTcProperties(), hasEntry("org.terracotta.dynamic-config.simulate", "recover-needed"));

    // close the server when the last change is not committed or rolled back
    tsa.stop(getNode(1, 1));
    assertThat(tsa.getStopped().size(), is(1));

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
        configToolInvocation("repair", "-s", "localhost:" + getNodePort()),
        allOf(
            containsOutput("Reason: org.terracotta.nomad.server.NomadException: Error when applying setting change 'set tc-properties.org.terracotta.dynamic-config.simulate=recover-needed (stripe ID: 1, node: node1-1)': Simulate commit failure"),
            containsOutput("Please run the 'diagnostic' command to diagnose the configuration state and try to run the 'repair' command."),
            containsOutput("Attempting an automatic repair of the configuration..."),
            not(containsOutput("Configuration is repaired."))));

    out.clearLog();
    assertThat(
        configToolInvocation("repair", "-s", "localhost:" + getNodePort()),
        allOf(
            containsOutput("Attempting an automatic repair of the configuration..."),
            containsOutput("Configuration is repaired")));

    // ensure that the server has started with the last committed config
    assertThat(getRuntimeCluster("localhost", getNodePort()), is(not(equalTo(initialCluster))));
    assertThat(getRuntimeCluster("localhost", getNodePort()).getSingleNode().get().getTcProperties(), hasEntry("org.terracotta.dynamic-config.simulate", "recover-needed"));
  }
}
