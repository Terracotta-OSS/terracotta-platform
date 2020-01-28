/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.test;

import com.terracottatech.dynamic_config.cli.ConfigTool;
import com.terracottatech.dynamic_config.model.Cluster;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static com.terracottatech.dynamic_config.test.util.ExceptionMatcher.throwing;
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

/**
 * @author Mathieu Carbou
 */
@SuppressFBWarnings("RV_RETURN_VALUE_IGNORED")
public class RepairCommandIT extends BaseStartupIT {

  @Before
  public void setUp() throws Exception {
    forEachNode(this::start);

    waitedAssert(out::getLog, containsString("Started the server in diagnostic mode"));

    ConfigTool.start("activate", "-s", "localhost:" + ports.getPort(), "-n", "tc-cluster", "-l", licensePath().toString());
    out.clearLog();
  }

  @Test
  public void test_automatic_commit_after_commit_failure() throws Exception {
    assertThat(
        () -> ConfigTool.start("set", "-s", "localhost:" + ports.getPort(), "-c", "stripe.1.node.1.tc-properties.com.terracottatech.dynamic-config.simulate=recover-needed"),
        is(throwing(instanceOf(IllegalStateException.class))
            .andMessage(both(
                containsString("Commit failed for node localhost:" + ports.getPort() + ". Reason: com.terracottatech.nomad.server.NomadException: Error when applying setting change 'set tc-properties.com.terracottatech.dynamic-config.simulate=recover-needed (stripe ID: 1, node: node-1)': Simulate commit failure"))
                .and(containsString("Please run the 'diagnostic' command to diagnose the configuration state and try to run the 'repair' command.")))));

    assertThat(getRuntimeCluster("localhost", ports.getPort()).getSingleNode().get().getTcProperties(), is(equalTo(emptyMap())));
    assertThat(getUpcomingCluster("localhost", ports.getPort()).getSingleNode().get().getTcProperties(), hasEntry("com.terracottatech.dynamic-config.simulate", "recover-needed"));

    assertThat(
        () -> ConfigTool.start("set", "-s", "localhost:" + ports.getPort(), "-c", "stripe.1.node.1.tc-properties.com.terracottatech.dynamic-config.simulate=recover-needed"),
        is(throwing(instanceOf(IllegalStateException.class))
            .andMessage(stringContainsInOrder(Arrays.asList("Another change (with UUID ", " is already underway on ", ". It was started by ", " on ")))));

    out.clearLog();
    ConfigTool.start("repair", "-s", "localhost:" + ports.getPort());
    waitedAssert(out::getLog, containsString("Attempting an automatic repair of the configuration..."));
    waitedAssert(out::getLog, containsString("Configuration is repaired"));

    assertThat(getRuntimeCluster("localhost", ports.getPort()).getSingleNode().get().getTcProperties(), hasEntry("com.terracottatech.dynamic-config.simulate", "recover-needed"));
  }

  @Test
  public void test_node_starts_with_previous_config_when_not_committed_or_rollback() throws Exception {
    Cluster initialCluster = getRuntimeCluster("localhost", ports.getPort());
    assertThat(initialCluster, is(equalTo(getUpcomingCluster("localhost", ports.getPort()))));
    assertThat(getRuntimeCluster("localhost", ports.getPort()).getSingleNode().get().getTcProperties(), is(equalTo(emptyMap())));

    assertThat(
        () -> ConfigTool.start("set", "-s", "localhost:" + ports.getPort(), "-c", "stripe.1.node.1.tc-properties.com.terracottatech.dynamic-config.simulate=recover-needed"),
        is(throwing(instanceOf(IllegalStateException.class))
            .andMessage(both(
                containsString("Commit failed for node localhost:" + ports.getPort() + ". Reason: com.terracottatech.nomad.server.NomadException: Error when applying setting change 'set tc-properties.com.terracottatech.dynamic-config.simulate=recover-needed (stripe ID: 1, node: node-1)': Simulate commit failure"))
                .and(containsString("Please run the 'diagnostic' command to diagnose the configuration state and try to run the 'repair' command.")))));

    assertThat(getRuntimeCluster("localhost", ports.getPort()), is(equalTo(initialCluster)));
    assertThat(getUpcomingCluster("localhost", ports.getPort()).getSingleNode().get().getTcProperties(), hasEntry("com.terracottatech.dynamic-config.simulate", "recover-needed"));

    // close the server when the last change is not committed or rolled back
    getNodeProcess().close();

    // ensure the server can still start if the configuration is not committed
    out.clearLog();
    start(1, 1, ports.getPort());
    waitedAssert(out::getLog, containsString("INFO - Moved to State[ ACTIVE-COORDINATOR ]"));
    waitedAssert(out::getLog, containsString("The configuration of this node has not been committed or rolled back. Please run the 'diagnostic' command to diagnose the configuration state."));

    // ensure that the server has started with the last committed config
    assertThat(getRuntimeCluster("localhost", ports.getPort()), is(equalTo(initialCluster)));
    assertThat(getUpcomingCluster("localhost", ports.getPort()), is(equalTo(initialCluster)));

    // repair the newly started server once (the simulated handler needs to repair after a restart - first one will fail)
    out.clearLog();
    assertThat(
        () -> ConfigTool.start("repair", "-s", "localhost:" + ports.getPort()),
        is(throwing(instanceOf(IllegalStateException.class))
            .andMessage(both(
                containsString("Reason: com.terracottatech.nomad.server.NomadException: Error when applying setting change 'set tc-properties.com.terracottatech.dynamic-config.simulate=recover-needed (stripe ID: 1, node: node-1)': Simulate commit failure"))
                .and(containsString("Please run the 'diagnostic' command to diagnose the configuration state and try to run the 'repair' command.")))));
    waitedAssert(out::getLog, containsString("Attempting an automatic repair of the configuration..."));
    waitedAssert(out::getLog, not(containsString("Configuration is repaired.")));

    out.clearLog();
    ConfigTool.start("repair", "-s", "localhost:" + ports.getPort());
    waitedAssert(out::getLog, containsString("Attempting an automatic repair of the configuration..."));
    waitedAssert(out::getLog, containsString("Configuration is repaired"));

    // ensure that the server has started with the last committed config
    assertThat(getRuntimeCluster("localhost", ports.getPort()), is(not(equalTo(initialCluster))));
    assertThat(getRuntimeCluster("localhost", ports.getPort()).getSingleNode().get().getTcProperties(), hasEntry("com.terracottatech.dynamic-config.simulate", "recover-needed"));
  }

  private void start(int stripeId, int nodeId, int port) {
    startNode(
        "--node-name", "node-" + nodeId,
        "--node-hostname", "localhost",
        "--node-port", String.valueOf(port),
        "--node-group-port", String.valueOf(port + 10),
        "--node-log-dir", "logs/stripe" + stripeId + "/node-" + nodeId,
        "--node-backup-dir", "backup/stripe" + stripeId,
        "--node-metadata-dir", "metadata/stripe" + stripeId,
        "--node-repository-dir", "repository/stripe" + stripeId + "/node-" + nodeId,
        "--data-dirs", "main:user-data/main/stripe" + stripeId);
  }
}
