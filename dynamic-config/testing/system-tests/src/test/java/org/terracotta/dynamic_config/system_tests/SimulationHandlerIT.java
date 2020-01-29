/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.system_tests;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.terracotta.dynamic_config.cli.config_tool.ConfigTool;

import static org.hamcrest.Matchers.containsString;

/**
 * @author Mathieu Carbou
 */
public class SimulationHandlerIT extends BaseStartupIT {

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Before
  public void setUp() throws Exception {
    forEachNode((stripeId, nodeId, port) -> startNode(
        "--node-name", "node-" + nodeId,
        "--node-hostname", "localhost",
        "--node-port", String.valueOf(port),
        "--node-group-port", String.valueOf(port + 10),
        "--node-log-dir", "logs/stripe" + stripeId + "/node-" + nodeId,
        "--node-backup-dir", "backup/stripe" + stripeId,
        "--node-metadata-dir", "metadata/stripe" + stripeId,
        "--node-repository-dir", "repository/stripe" + stripeId + "/node-" + nodeId,
        "--data-dirs", "main:user-data/main/stripe" + stripeId));

    waitedAssert(out::getLog, containsString("Started the server in diagnostic mode"));

    ConfigTool.start("activate", "-s", "localhost:" + ports.getPort(), "-n", "tc-cluster", "-l", licensePath().toString());
    out.clearLog();
  }

  @Test
  public void test_missing_value() {
    exception.expect(IllegalArgumentException.class);
    exception.expectMessage(containsString("Invalid input: 'stripe.1.node.1.tc-properties.org.terracotta.dynamic-config.simulate='. Reason: Operation set requires a value"));
    ConfigTool.start("set", "-s", "localhost:" + ports.getPort(), "-c", "stripe.1.node.1.tc-properties.org.terracotta.dynamic-config.simulate=");
  }

  @Test
  public void test_prepare_fails() {
    exception.expect(IllegalStateException.class);
    exception.expectMessage(containsString("Prepare rejected for node localhost:" + ports.getPort() + ". Reason: Error when trying to apply setting change 'set tc-properties.org.terracotta.dynamic-config.simulate=prepare-failure (stripe ID: 1, node: node-1)': Simulate prepare failure"));
    ConfigTool.start("set", "-s", "localhost:" + ports.getPort(), "-c", "stripe.1.node.1.tc-properties.org.terracotta.dynamic-config.simulate=prepare-failure");
  }

  @Test
  public void test_commit_fails() {
    exception.expect(IllegalStateException.class);
    exception.expectMessage(containsString("Commit failed for node localhost:" + ports.getPort() + ". Reason: org.terracotta.nomad.server.NomadException: Error when applying setting change 'set tc-properties.org.terracotta.dynamic-config.simulate=commit-failure (stripe ID: 1, node: node-1)': Simulate commit failure"));
    ConfigTool.start("set", "-s", "localhost:" + ports.getPort(), "-c", "stripe.1.node.1.tc-properties.org.terracotta.dynamic-config.simulate=commit-failure");
  }

  @Test
  public void test_requires_restart() {
    ConfigTool.start("set", "-s", "localhost:" + ports.getPort(), "-c", "stripe.1.node.1.tc-properties.org.terracotta.dynamic-config.simulate=restart-required");
    waitedAssert(out::getLog, containsString("IMPORTANT: A restart of the cluster is required to apply the changes"));
  }
}
