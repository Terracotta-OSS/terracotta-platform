/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.system_tests;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.not;
import static org.terracotta.dynamic_config.system_tests.util.AngelaMatchers.containsOutput;
import static org.terracotta.dynamic_config.system_tests.util.AngelaMatchers.hasExitStatus;

/**
 * @author Mathieu Carbou
 */
@ClusterDefinition(autoActivate = true)
public class SimulationHandlerIT extends DynamicConfigIT {
  @Test
  public void test_missing_value() {
    assertThat(
        configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.tc-properties.org.terracotta.dynamic-config.simulate="),
        allOf(
            not(hasExitStatus(0)),
            containsOutput("Invalid input: 'stripe.1.node.1.tc-properties.org.terracotta.dynamic-config.simulate='. Reason: Operation set requires a value")));
  }

  @Test
  public void test_prepare_fails() {
    assertThat(
        configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.tc-properties.org.terracotta.dynamic-config.simulate=prepare-failure"),
        allOf(
            not(hasExitStatus(0)),
            containsOutput("Prepare rejected for node localhost:" + getNodePort() + ". Reason: Error when trying to apply setting change 'set tc-properties.org.terracotta.dynamic-config.simulate=prepare-failure (stripe ID: 1, node: node1-1)': Simulate prepare failure")));
  }

  @Test
  public void test_commit_fails() {
    assertThat(
        configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.tc-properties.org.terracotta.dynamic-config.simulate=commit-failure"),
        allOf(
            not(hasExitStatus(0)),
            containsOutput("Commit failed for node localhost:" + getNodePort() + ". Reason: org.terracotta.nomad.server.NomadException: Error when applying setting change 'set tc-properties.org.terracotta.dynamic-config.simulate=commit-failure (stripe ID: 1, node: node1-1)': Simulate commit failure")));
  }

  @Test
  public void test_requires_restart() {
    assertThat(
        configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.tc-properties.org.terracotta.dynamic-config.simulate=restart-required"),
        allOf(
            hasExitStatus(0),
            containsOutput("IMPORTANT: A restart of the cluster is required to apply the changes")));
  }
}
