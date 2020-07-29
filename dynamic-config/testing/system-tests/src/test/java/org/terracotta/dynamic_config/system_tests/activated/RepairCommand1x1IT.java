/*
 * Copyright Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.dynamic_config.system_tests.activated;

import org.junit.Test;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.test_support.ClusterDefinition;
import org.terracotta.dynamic_config.test_support.DynamicConfigIT;

import java.time.Duration;
import java.util.Arrays;

import static java.util.Collections.emptyMap;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.stringContainsInOrder;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.containsOutput;
import static org.terracotta.testing.ExceptionMatcher.throwing;

/**
 * @author Mathieu Carbou
 */
@ClusterDefinition(autoActivate = true)
public class RepairCommand1x1IT extends DynamicConfigIT {

  public RepairCommand1x1IT() {
    super(Duration.ofSeconds(180));
  }

  @SuppressWarnings("OptionalGetWithoutIsPresent")
  @Test
  public void test_auto_repair_commit_failure() throws Exception {
    assertThat(
        () -> invokeConfigTool("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.logger-overrides.org.terracotta.dynamic-config.simulate=DEBUG"),
        is(throwing(instanceOf(RuntimeException.class)).andMessage(allOf(
            containsString("Commit failed for node localhost:" + getNodePort() + ". Reason: Error when applying setting change: 'set logger-overrides.org.terracotta.dynamic-config.simulate=DEBUG (stripe ID: 1, node: node-1-1)': Simulate temporary commit failure"),
            containsString("Please run the 'diagnostic' command to diagnose the configuration state and try to run the 'repair' command.")))));

    assertThat(getRuntimeCluster("localhost", getNodePort()).getSingleNode().get().getLoggerOverrides().orDefault(), is(equalTo(emptyMap())));
    assertThat(getUpcomingCluster("localhost", getNodePort()).getSingleNode().get().getLoggerOverrides().orDefault(), is(equalTo(emptyMap())));

    assertThat(
        () -> invokeConfigTool("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.logger-overrides.org.terracotta.dynamic-config.simulate=DEBUG"),
        is(throwing(instanceOf(RuntimeException.class)).andMessage(stringContainsInOrder(Arrays.asList("Another change (with UUID ", " is already underway on ", ". It was started by ", " on ")))));

    assertThat(
        invokeConfigTool("repair", "-s", "localhost:" + getNodePort()),
        allOf(
            containsOutput("Attempting an automatic repair of the configuration"),
            containsOutput("Configuration is repaired")));

    assertThat(getRuntimeCluster("localhost", getNodePort()).getSingleNode().get().getLoggerOverrides().orDefault(), hasEntry("org.terracotta.dynamic-config.simulate", "DEBUG"));
    assertThat(getUpcomingCluster("localhost", getNodePort()).getSingleNode().get().getLoggerOverrides().orDefault(), hasEntry("org.terracotta.dynamic-config.simulate", "DEBUG"));
  }

  @SuppressWarnings("OptionalGetWithoutIsPresent")
  @Test
  public void test_auto_repair_uncommitted_node() throws Exception {
    Cluster initialCluster = getRuntimeCluster("localhost", getNodePort());
    assertThat(initialCluster, is(equalTo(getUpcomingCluster("localhost", getNodePort()))));
    assertThat(getRuntimeCluster("localhost", getNodePort()).getSingleNode().get().getTcProperties().orDefault(), is(equalTo(emptyMap())));

    assertThat(
        () -> invokeConfigTool("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.logger-overrides.org.terracotta.dynamic-config.simulate=DEBUG"),
        is(throwing(instanceOf(RuntimeException.class)).andMessage(allOf(
            containsString("Commit failed for node localhost:" + getNodePort() + ". Reason: Error when applying setting change: 'set logger-overrides.org.terracotta.dynamic-config.simulate=DEBUG (stripe ID: 1, node: node-1-1)': Simulate temporary commit failure"),
            containsString("Please run the 'diagnostic' command to diagnose the configuration state and try to run the 'repair' command.")))));

    assertThat(getRuntimeCluster("localhost", getNodePort()), is(equalTo(initialCluster)));
    assertThat(getUpcomingCluster("localhost", getNodePort()), is(equalTo(initialCluster)));

    // close the server when the last change is not committed or rolled back
    stopNode(1, 1);
    assertThat(angela.tsa().getStopped().size(), is(1));

    // ensure the server can still start if the configuration is not committed
    startNode(1, 1);
    waitForActive(1, 1);
    withTopologyService("localhost", getNodePort(), topologyService -> assertTrue(topologyService.hasIncompleteChange()));

    // ensure that the server has started with the last committed config
    assertThat(getRuntimeCluster("localhost", getNodePort()), is(equalTo(initialCluster)));
    assertThat(getUpcomingCluster("localhost", getNodePort()), is(equalTo(initialCluster)));

    // intermediary call just to set a state in the SimulationHandler so that it can recover
    assertThat(
        () -> invokeConfigTool("repair", "-s", "localhost:" + getNodePort()),
        is(throwing(instanceOf(RuntimeException.class)).andMessage(allOf(
            containsString("Reason: org.terracotta.nomad.server.NomadException: Error when applying setting change: 'set logger-overrides.org.terracotta.dynamic-config.simulate=DEBUG (stripe ID: 1, node: node-1-1)': Simulate temporary commit failure"),
            containsString("Please run the 'diagnostic' command to diagnose the configuration state and try to run the 'repair' command."),
            containsString("Attempting an automatic repair of the configuration"),
            not(containsString("Configuration is repaired."))))));

    // repair the newly started server
    assertThat(
        invokeConfigTool("repair", "-s", "localhost:" + getNodePort()),
        allOf(
            containsOutput("Attempting an automatic repair of the configuration"),
            containsOutput("Configuration is repaired")));

    // ensure that the server has started with the last committed config
    assertThat(getRuntimeCluster("localhost", getNodePort()), is(not(equalTo(initialCluster))));
    assertThat(getRuntimeCluster("localhost", getNodePort()).getSingleNode().get().getLoggerOverrides().orDefault(), hasEntry("org.terracotta.dynamic-config.simulate", "DEBUG"));
    assertThat(getUpcomingCluster("localhost", getNodePort()).getSingleNode().get().getLoggerOverrides().orDefault(), hasEntry("org.terracotta.dynamic-config.simulate", "DEBUG"));
  }
}
