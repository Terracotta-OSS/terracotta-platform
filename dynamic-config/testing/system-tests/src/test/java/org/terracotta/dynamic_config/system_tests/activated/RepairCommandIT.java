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

import org.junit.Rule;
import org.junit.Test;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.test_support.ClusterDefinition;
import org.terracotta.dynamic_config.test_support.DynamicConfigIT;
import org.terracotta.dynamic_config.test_support.util.NodeOutputRule;

import java.util.Arrays;

import static java.util.Collections.emptyMap;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.stringContainsInOrder;
import static org.junit.Assert.assertThat;
import static org.slf4j.event.Level.DEBUG;
import static org.terracotta.dynamic_config.test_support.util.AngelaMatchers.containsLog;
import static org.terracotta.dynamic_config.test_support.util.AngelaMatchers.containsOutput;

/**
 * @author Mathieu Carbou
 */
@ClusterDefinition(autoActivate = true)
public class RepairCommandIT extends DynamicConfigIT {

  @Rule public final NodeOutputRule out = new NodeOutputRule();

  @SuppressWarnings("OptionalGetWithoutIsPresent")
  @Test
  public void test_automatic_commit_after_commit_failure() throws Exception {
    assertThat(
        configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.node-logger-overrides.org.terracotta.dynamic-config.simulate=DEBUG"),
        allOf(
            containsOutput("Commit failed for node localhost:" + getNodePort() + ". Reason: org.terracotta.nomad.server.NomadException: Error when applying setting change 'set node-logger-overrides.org.terracotta.dynamic-config.simulate=DEBUG (stripe ID: 1, node: node-1-1)': Simulate temporary commit failure"),
            containsOutput("Please run the 'diagnostic' command to diagnose the configuration state and try to run the 'repair' command.")));

    assertThat(getRuntimeCluster("localhost", getNodePort()).getSingleNode().get().getNodeLoggerOverrides(), is(equalTo(emptyMap())));
    assertThat(getUpcomingCluster("localhost", getNodePort()).getSingleNode().get().getNodeLoggerOverrides(), is(equalTo(emptyMap())));

    assertThat(
        configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.node-logger-overrides.org.terracotta.dynamic-config.simulate=DEBUG").toString(),
        stringContainsInOrder(Arrays.asList("Another change (with UUID ", " is already underway on ", ". It was started by ", " on ")));

    assertThat(
        configToolInvocation("repair", "-s", "localhost:" + getNodePort()),
        allOf(
            containsOutput("Attempting an automatic repair of the configuration..."),
            containsOutput("Configuration is repaired")));

    assertThat(getRuntimeCluster("localhost", getNodePort()).getSingleNode().get().getNodeLoggerOverrides(), hasEntry("org.terracotta.dynamic-config.simulate", DEBUG));
    assertThat(getUpcomingCluster("localhost", getNodePort()).getSingleNode().get().getNodeLoggerOverrides(), hasEntry("org.terracotta.dynamic-config.simulate", DEBUG));
  }

  @SuppressWarnings("OptionalGetWithoutIsPresent")
  @Test
  public void test_node_starts_with_previous_config_when_not_committed_or_rollback() throws Exception {
    Cluster initialCluster = getRuntimeCluster("localhost", getNodePort());
    assertThat(initialCluster, is(equalTo(getUpcomingCluster("localhost", getNodePort()))));
    assertThat(getRuntimeCluster("localhost", getNodePort()).getSingleNode().get().getTcProperties(), is(equalTo(emptyMap())));

    assertThat(
        configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.node-logger-overrides.org.terracotta.dynamic-config.simulate=DEBUG"),
        allOf(
            containsOutput("Commit failed for node localhost:" + getNodePort() + ". Reason: org.terracotta.nomad.server.NomadException: Error when applying setting change 'set node-logger-overrides.org.terracotta.dynamic-config.simulate=DEBUG (stripe ID: 1, node: node-1-1)': Simulate temporary commit failure"),
            containsOutput("Please run the 'diagnostic' command to diagnose the configuration state and try to run the 'repair' command.")));

    assertThat(getRuntimeCluster("localhost", getNodePort()), is(equalTo(initialCluster)));
    assertThat(getUpcomingCluster("localhost", getNodePort()), is(equalTo(initialCluster)));

    // close the server when the last change is not committed or rolled back
    stopNode(1, 1);
    assertThat(tsa.getStopped().size(), is(1));

    // ensure the server can still start if the configuration is not committedø
    startNode(1, 1);
    waitUntil(out.getLog(1, 1), containsLog("INFO - Moved to State[ ACTIVE-COORDINATOR ]"));
    waitUntil(out.getLog(1, 1), containsLog("The configuration of this node has not been committed or rolled back. Please run the 'diagnostic' command to diagnose the configuration state."));

    // ensure that the server has started with the last committed config
    assertThat(getRuntimeCluster("localhost", getNodePort()), is(equalTo(initialCluster)));
    assertThat(getUpcomingCluster("localhost", getNodePort()), is(equalTo(initialCluster)));

    // repair the newly started server once (the simulated handler needs to repair after a restart - first one will fail)
    assertThat(
        configToolInvocation("repair", "-s", "localhost:" + getNodePort()),
        allOf(
            containsOutput("Reason: org.terracotta.nomad.server.NomadException: Error when applying setting change 'set node-logger-overrides.org.terracotta.dynamic-config.simulate=DEBUG (stripe ID: 1, node: node-1-1)': Simulate temporary commit failure"),
            containsOutput("Please run the 'diagnostic' command to diagnose the configuration state and try to run the 'repair' command."),
            containsOutput("Attempting an automatic repair of the configuration..."),
            not(containsOutput("Configuration is repaired."))));

    assertThat(
        configToolInvocation("repair", "-s", "localhost:" + getNodePort()),
        allOf(
            containsOutput("Attempting an automatic repair of the configuration..."),
            containsOutput("Configuration is repaired")));

    // ensure that the server has started with the last committed config
    assertThat(getRuntimeCluster("localhost", getNodePort()), is(not(equalTo(initialCluster))));
    assertThat(getRuntimeCluster("localhost", getNodePort()).getSingleNode().get().getNodeLoggerOverrides(), hasEntry("org.terracotta.dynamic-config.simulate", DEBUG));
    assertThat(getUpcomingCluster("localhost", getNodePort()).getSingleNode().get().getNodeLoggerOverrides(), hasEntry("org.terracotta.dynamic-config.simulate", DEBUG));
  }
}
