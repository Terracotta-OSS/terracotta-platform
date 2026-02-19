/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2026
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

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.terracotta.angela.common.ToolExecutionResult;
import org.terracotta.dynamic_config.test_support.ClusterDefinition;
import org.terracotta.dynamic_config.test_support.DynamicConfigIT;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.IntStream.rangeClosed;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.containsOutput;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.successful;

@ClusterDefinition(stripes = 2, nodesPerStripe = 3)
public class RelaySetUnsetCommand2x3IT extends DynamicConfigIT {
  private static final Duration WAIT_UNTIL = Duration.ofMinutes(5);

  @Before
  public void setup() throws Exception {
    assertThat(configTool("attach", "-d", "localhost:" + getNodePort(1, 1), "-s", "localhost:" + getNodePort(1, 2)), is(successful()));
    assertThat(configTool("attach", "-d", "localhost:" + getNodePort(1, 1), "-s", "localhost:" + getNodePort(1, 3)), is(successful()));

    assertThat(configTool("attach", "-d", "localhost:" + getNodePort(2, 1), "-s", "localhost:" + getNodePort(2, 2)), is(successful()));
    assertThat(configTool("attach", "-d", "localhost:" + getNodePort(2, 1), "-s", "localhost:" + getNodePort(2, 3)), is(successful()));

    assertThat(configTool("attach", "-to-cluster", "localhost:" + getNodePort(1, 1), "-stripe", "localhost:" + getNodePort(2, 1)), is(successful()));

    assertThat(configTool("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.3.relay=true", "-c", "stripe.1.node.3.replica-hostname=" + "localhost", "-c", "stripe.1.node.3.replica-port=" + "9410",
      "-c", "stripe.2.node.3.relay=true", "-c", "stripe.2.node.3.replica-hostname=" + "localhost", "-c", "stripe.2.node.3.replica-port=" + "9410"), is(successful()));

    activateCluster();
    waitForActive(1);
    waitForActive(2);
    waitForNPassives(1, 1);
    waitForNPassives(2, 1);
    waitForPassiveRelay(1, 3);
    waitForPassiveRelay(2, 3);
  }

  @Test
  public void testSetUnsetRelaySettingRequiringManualRestart() {
    // set change targeting all nodes
    assertThat(configTool("set", "-s", "localhost:" + getNodePort(), "-c", "tc-properties=foo:foo"), allOf(is(successful()),
      containsOutput("Restart required for nodes"),
      containsOutput("Performing automatic restart of relay nodes")));
    waitForChangeToSync();

    List<Matcher<? super ToolExecutionResult>> change = getAllNodesConfigMutation(".tc-properties=foo:foo", false);
    waitUntil(() -> configTool("get", "-s", "localhost:" + getNodePort(1, 3), "-c", "tc-properties", "-t", "index"), allOf(change), WAIT_UNTIL);
    waitUntil(() -> configTool("get", "-s", "localhost:" + getNodePort(2, 3), "-c", "tc-properties", "-t", "index"), allOf(change), WAIT_UNTIL);

    // unset change targeting all nodes
    assertThat(configTool("unset", "-s", "localhost:" + getNodePort(), "-c", "tc-properties"), allOf(is(successful()),
      containsOutput("Restart required for nodes")));
    waitForChangeToSync();

    List<Matcher<? super ToolExecutionResult>> negatedChange = getAllNodesConfigMutation(".tc-properties=foo:foo", true);
    waitUntil(() -> configTool("get", "-s", "localhost:" + getNodePort(1, 3), "-c", "tc-properties", "-t", "index"), allOf(negatedChange), WAIT_UNTIL);
    waitUntil(() -> configTool("get", "-s", "localhost:" + getNodePort(2, 3), "-c", "tc-properties", "-t", "index"), allOf(negatedChange), WAIT_UNTIL);

    // set change targeting non-relay node
    assertThat(configTool("set", "-s", "localhost:" + getNodePort(), "-c", getNodeName(1, 1) + ":tc-properties=foo:bar"), allOf(is(successful())));
    waitForChangeToSync();
    waitUntil(() -> configTool("get", "-s", "localhost:" + getNodePort(1, 3), "-c", "tc-properties"), allOf(successful(), containsOutput(getNodeName(1, 1) + ":tc-properties=foo:bar")), WAIT_UNTIL);
    waitUntil(() -> configTool("get", "-s", "localhost:" + getNodePort(2, 3), "-c", "tc-properties"), allOf(successful(), containsOutput(getNodeName(1, 1) + ":tc-properties=foo:bar")), WAIT_UNTIL);

    // unset change targeting non-relay node
    assertThat(configTool("unset", "-s", "localhost:" + getNodePort(), "-c", getNodeName(1, 1) + ":tc-properties"), allOf(is(successful())));
    waitForChangeToSync();
    waitUntil(() -> configTool("get", "-s", "localhost:" + getNodePort(1, 3), "-c", "tc-properties"), allOf(successful(), not(containsOutput(getNodeName(1, 1) + ":tc-properties=foo:bar"))), WAIT_UNTIL);
    waitUntil(() -> configTool("get", "-s", "localhost:" + getNodePort(2, 3), "-c", "tc-properties"), allOf(successful(), not(containsOutput(getNodeName(1, 1) + ":tc-properties=foo:bar"))), WAIT_UNTIL);

    // set change targeting relay nodes and connect-to=relay address
    waitUntil(() -> configTool("set", "-s", "localhost:" + getNodePort(1, 3), "-c", getNodeName(1, 3) + ":tc-properties=foo:baz", "-c", getNodeName(2, 3) + ":tc-properties=foo:baz"), is(successful()), WAIT_UNTIL);
    waitForChangeToSync();
    waitUntil(() -> configTool("get", "-s", "localhost:" + getNodePort(1, 3), "-c", "tc-properties"),
      allOf(successful(), containsOutput(getNodeName(1, 3) + ":tc-properties=foo:baz"), containsOutput(getNodeName(2, 3) + ":tc-properties=foo:baz")), WAIT_UNTIL);
    waitUntil(() -> configTool("get", "-s", "localhost:" + getNodePort(2, 3), "-c", "tc-properties"),
      allOf(successful(), containsOutput(getNodeName(1, 3) + ":tc-properties=foo:baz"), containsOutput(getNodeName(2, 3) + ":tc-properties=foo:baz")), WAIT_UNTIL);

    // unset change targeting relay node and connect-to=relay address
    waitUntil(() -> configTool("unset", "-s", "localhost:" + getNodePort(1, 3), "-c", getNodeName(1, 3) + ":tc-properties", "-c", getNodeName(2, 3) + ":tc-properties"), is(successful()), WAIT_UNTIL);
    waitForChangeToSync();
    waitUntil(() -> configTool("get", "-s", "localhost:" + getNodePort(1, 3), "-c", "tc-properties"),
      allOf(successful(), not(containsOutput(getNodeName(1, 3) + ":tc-properties=foo:baz")), not(containsOutput(getNodeName(2, 3) + ":tc-properties=foo:baz"))), WAIT_UNTIL);
    waitUntil(() -> configTool("get", "-s", "localhost:" + getNodePort(2, 3), "-c", "tc-properties"),
      allOf(successful(), not(containsOutput(getNodeName(1, 3) + ":tc-properties=foo:baz")), not(containsOutput(getNodeName(2, 3) + ":tc-properties=foo:baz"))), WAIT_UNTIL);
  }

  @Test
  public void testSetUnsetRelaySettingNotRequiringRestart() {
    // set change targeting all nodes
    assertThat(configTool("set", "-s", "localhost:" + getNodePort(), "-c", "logger-overrides=org.terracotta.foo:TRACE"), allOf(is(successful()),
      containsOutput("Performing automatic restart of relay nodes"),
      not(containsOutput("Restart required for nodes"))));
    waitForChangeToSync();

    List<Matcher<? super ToolExecutionResult>> change = getAllNodesConfigMutation(".logger-overrides=org.terracotta.foo:TRACE", false);
    waitUntil(() -> configTool("get", "-s", "localhost:" + getNodePort(1, 3), "-c", "logger-overrides", "-t", "index"), allOf(change), WAIT_UNTIL);
    waitUntil(() -> configTool("get", "-s", "localhost:" + getNodePort(2, 3), "-c", "logger-overrides", "-t", "index"), allOf(change), WAIT_UNTIL);

    // unset change targeting all nodes
    assertThat(configTool("unset", "-s", "localhost:" + getNodePort(), "-c", "logger-overrides"), allOf(is(successful()),
      not(containsOutput("Restart required for nodes"))));
    waitForChangeToSync();

    List<Matcher<? super ToolExecutionResult>> negatedChange = getAllNodesConfigMutation(".logger-overrides=org.terracotta.foo:TRACE", true);
    waitUntil(() -> configTool("get", "-s", "localhost:" + getNodePort(1, 3), "-c", "logger-overrides", "-t", "index"), allOf(negatedChange), WAIT_UNTIL);
    waitUntil(() -> configTool("get", "-s", "localhost:" + getNodePort(2, 3), "-c", "logger-overrides", "-t", "index"), allOf(negatedChange), WAIT_UNTIL);

    // set change targeting non-relay node
    assertThat(configTool("set", "-s", "localhost:" + getNodePort(), "-c", getNodeName(1, 1) + ":logger-overrides=org.terracotta.bar:TRACE"), allOf(is(successful())));
    waitForChangeToSync();
    waitUntil(() -> configTool("get", "-s", "localhost:" + getNodePort(1, 3), "-c", "logger-overrides"), allOf(successful(), containsOutput(getNodeName(1, 1) + ":logger-overrides=org.terracotta.bar:TRACE")), WAIT_UNTIL);
    waitUntil(() -> configTool("get", "-s", "localhost:" + getNodePort(2, 3), "-c", "logger-overrides"), allOf(successful(), containsOutput(getNodeName(1, 1) + ":logger-overrides=org.terracotta.bar:TRACE")), WAIT_UNTIL);

    // unset change targeting non-relay node
    assertThat(configTool("unset", "-s", "localhost:" + getNodePort(), "-c", getNodeName(1, 1) + ":logger-overrides"), allOf(is(successful())));
    waitForChangeToSync();
    waitUntil(() -> configTool("get", "-s", "localhost:" + getNodePort(1, 3), "-c", "logger-overrides"), allOf(successful(), not(containsOutput(getNodeName(1, 1) + ":logger-overrides=org.terracotta.bar:TRACE"))), WAIT_UNTIL);
    waitUntil(() -> configTool("get", "-s", "localhost:" + getNodePort(2, 3), "-c", "logger-overrides"), allOf(successful(), not(containsOutput(getNodeName(1, 1) + ":logger-overrides=org.terracotta.bar:TRACE"))), WAIT_UNTIL);

    // set change targeting relay node and connect-to=relay address
    waitUntil(() -> configTool("set", "-s", "localhost:" + getNodePort(1, 3), "-c", getNodeName(1, 3) + ":logger-overrides=org.terracotta.baz:TRACE", "-c", getNodeName(2, 3) + ":logger-overrides=org.terracotta.baz:TRACE"),
      is(successful()), WAIT_UNTIL);
    waitForChangeToSync();
    waitUntil(() -> configTool("get", "-s", "localhost:" + getNodePort(1, 3), "-c", "logger-overrides"),
      allOf(successful(), containsOutput(getNodeName(1, 3) + ":logger-overrides=org.terracotta.baz:TRACE"), containsOutput(getNodeName(2, 3) + ":logger-overrides=org.terracotta.baz:TRACE")), WAIT_UNTIL);
    waitUntil(() -> configTool("get", "-s", "localhost:" + getNodePort(2, 3), "-c", "logger-overrides"),
      allOf(successful(), containsOutput(getNodeName(1, 3) + ":logger-overrides=org.terracotta.baz:TRACE"), containsOutput(getNodeName(2, 3) + ":logger-overrides=org.terracotta.baz:TRACE")), WAIT_UNTIL);

    // unset change targeting relay node and connect-to=relay address
    waitUntil(() -> configTool("unset", "-s", "localhost:" + getNodePort(1, 3), "-c", getNodeName(1, 3) + ":logger-overrides", "-c", getNodeName(2, 3) + ":logger-overrides"), is(successful()), WAIT_UNTIL);
    waitForChangeToSync();
    waitUntil(() -> configTool("get", "-s", "localhost:" + getNodePort(1, 3), "-c", "logger-overrides"),
      allOf(successful(), not(containsOutput(getNodeName(1, 3) + ":logger-overrides=org.terracotta.baz:TRACE")), not(containsOutput(getNodeName(2, 3) + ":logger-overrides=org.terracotta.baz:TRACE"))), WAIT_UNTIL);
    waitUntil(() -> configTool("get", "-s", "localhost:" + getNodePort(2, 3), "-c", "logger-overrides"),
      allOf(successful(), not(containsOutput(getNodeName(1, 3) + ":logger-overrides=org.terracotta.baz:TRACE")), not(containsOutput(getNodeName(2, 3) + ":logger-overrides=org.terracotta.baz:TRACE"))), WAIT_UNTIL);
  }

  @Test
  public void testSetAllRelayUnavailable() {
    stopNode(1, 3);
    waitForStopped(1, 3);
    stopNode(2, 3);
    waitForStopped(2, 3);

    // set change targeting all nodes
    assertThat(configTool("set", "-s", "localhost:" + getNodePort(), "-c", "logger-overrides=org.terracotta.foo:TRACE"), allOf(is(successful()),
      containsOutput("Relay nodes: " + getNodeName(1, 3) + ", " + getNodeName(2, 3) + " are unreachable"),
      not(containsOutput("Restart required for nodes"))));
    waitForChangeToSync();

    startNode(1, 3);
    startNode(2, 3);
    waitForPassiveRelay(1, 3);
    waitForPassiveRelay(2, 3);

    List<Matcher<? super ToolExecutionResult>> change = getAllNodesConfigMutation(".logger-overrides=org.terracotta.foo:TRACE", false);
    waitUntil(() -> configTool("get", "-s", "localhost:" + getNodePort(1, 3), "-c", "logger-overrides", "-t", "index"), allOf(change), WAIT_UNTIL);
    waitUntil(() -> configTool("get", "-s", "localhost:" + getNodePort(2, 3), "-c", "logger-overrides", "-t", "index"), allOf(change), WAIT_UNTIL);
  }

  @Test
  public void testSetPartialRelayUnavailable() throws Exception {
    stopNode(2, 3);
    waitForStopped(2, 3);

    // set change targeting all nodes
    assertThat(configTool("set", "-s", "localhost:" + getNodePort(), "-c", "logger-overrides=org.terracotta.foo:TRACE"), allOf(is(successful()),
      containsOutput("Relay nodes: " + getNodeName(2, 3) + " are unreachable"),
      not(containsOutput("Restart required for nodes"))));
    waitForChangeToSync();

    startNode(2, 3);
    waitForPassiveRelay(2, 3);

    List<Matcher<? super ToolExecutionResult>> change = getAllNodesConfigMutation(".logger-overrides=org.terracotta.foo:TRACE", false);
    waitUntil(() -> configTool("get", "-s", "localhost:" + getNodePort(1, 3), "-c", "logger-overrides", "-t", "index"), allOf(change), WAIT_UNTIL);
    waitUntil(() -> configTool("get", "-s", "localhost:" + getNodePort(2, 3), "-c", "logger-overrides", "-t", "index"), allOf(change), WAIT_UNTIL);
  }

  @Test
  public void testUnsetPartialRelayUnavailable() throws Exception {
    // set change targeting all nodes
    assertThat(configTool("set", "-s", "localhost:" + getNodePort(), "-c", "logger-overrides=org.terracotta.foo:TRACE"), allOf(is(successful()),
      containsOutput("Performing automatic restart of relay nodes"),
      not(containsOutput("Restart required for nodes"))));
    waitForChangeToSync();

    List<Matcher<? super ToolExecutionResult>> change = getAllNodesConfigMutation(".logger-overrides=org.terracotta.foo:TRACE", false);
    waitUntil(() -> configTool("get", "-s", "localhost:" + getNodePort(1, 3), "-c", "logger-overrides", "-t", "index"), allOf(change), WAIT_UNTIL);
    waitUntil(() -> configTool("get", "-s", "localhost:" + getNodePort(2, 3), "-c", "logger-overrides", "-t", "index"), allOf(change), WAIT_UNTIL);

    stopNode(2, 3);
    waitForStopped(2, 3);

    assertThat(configTool("unset", "-s", "localhost:" + getNodePort(), "-c", "logger-overrides"), allOf(is(successful())));
    waitForChangeToSync();
    List<Matcher<? super ToolExecutionResult>> negatedChange = getAllNodesConfigMutation(".logger-overrides=org.terracotta.foo:TRACE", true);

    startNode(2, 3);
    waitForPassiveRelay(2, 3);

    waitUntil(() -> configTool("get", "-s", "localhost:" + getNodePort(1, 3), "-c", "logger-overrides", "-t", "index"), allOf(negatedChange), WAIT_UNTIL);
    waitUntil(() -> configTool("get", "-s", "localhost:" + getNodePort(2, 3), "-c", "logger-overrides", "-t", "index"), allOf(negatedChange), WAIT_UNTIL);
  }

  private List<Matcher<? super ToolExecutionResult>> getAllNodesConfigMutation(String property, boolean negate) {
    Stream<Matcher<? super ToolExecutionResult>> change = rangeClosed(1, 2).boxed()
      .flatMap(stripeId -> rangeClosed(1, 3)
        .mapToObj(nodeId -> containsOutput("stripe." + stripeId + ".node." + nodeId + property)));

    if (negate) {
      change = change.map(Matchers::not);
    }

    return Stream.concat(change, Stream.of(successful())).collect(Collectors.toList());
  }

  /**
   * Waits for configuration changes to sync to relay nodes. Relay nodes are automatically restarted
   * by the config tool, then restarted again during passive sync, making them temporarily unavailable.
   * This ensures they complete the second restart
   */
  private void waitForChangeToSync() {
    waitUntilServerLogs(getNode(1, 3), "No configuration change left to sync");
    waitUntilServerLogs(getNode(2, 3), "No configuration change left to sync");
  }
}
