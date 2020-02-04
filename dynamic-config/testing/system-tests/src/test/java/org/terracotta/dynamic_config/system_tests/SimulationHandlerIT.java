/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.system_tests;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.terracotta.dynamic_config.cli.config_tool.ConfigTool;

import static org.hamcrest.Matchers.containsString;

/**
 * @author Mathieu Carbou
 */
@ClusterDefinition(autoActivate = true)
public class SimulationHandlerIT extends DynamicConfigIT {

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Test
  public void test_missing_value() {
    exception.expect(IllegalArgumentException.class);
    exception.expectMessage(containsString("Invalid input: 'stripe.1.node.1.tc-properties.org.terracotta.dynamic-config.simulate='. Reason: Operation set requires a value"));
    ConfigTool.start("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.tc-properties.org.terracotta.dynamic-config.simulate=");
  }

  @Test
  public void test_prepare_fails() {
    exception.expect(IllegalStateException.class);
    exception.expectMessage(containsString("Prepare rejected for node localhost:" + getNodePort() + ". Reason: Error when trying to apply setting change 'set tc-properties.org.terracotta.dynamic-config.simulate=prepare-failure (stripe ID: 1, node: node-1)': Simulate prepare failure"));
    ConfigTool.start("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.tc-properties.org.terracotta.dynamic-config.simulate=prepare-failure");
  }

  @Test
  public void test_commit_fails() {
    exception.expect(IllegalStateException.class);
    exception.expectMessage(containsString("Commit failed for node localhost:" + getNodePort() + ". Reason: org.terracotta.nomad.server.NomadException: Error when applying setting change 'set tc-properties.org.terracotta.dynamic-config.simulate=commit-failure (stripe ID: 1, node: node-1)': Simulate commit failure"));
    ConfigTool.start("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.tc-properties.org.terracotta.dynamic-config.simulate=commit-failure");
  }

  @Test
  public void test_requires_restart() {
    ConfigTool.start("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.tc-properties.org.terracotta.dynamic-config.simulate=restart-required");
    waitUntil(out::getLog, containsString("IMPORTANT: A restart of the cluster is required to apply the changes"));
  }
}
