/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2026
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
package org.terracotta.dynamic_config.system_tests.diagnostic;

import org.junit.Test;
import org.terracotta.dynamic_config.test_support.ClusterDefinition;
import org.terracotta.dynamic_config.test_support.DynamicConfigIT;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.containsOutput;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.successful;

@ClusterDefinition(failoverPriority = "")
public class SetCommand1x1IT extends DynamicConfigIT {

  @Test
  public void setOffheapResource() {
    assertThat(configTool("set", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.main=512MB"), is(successful()));

    assertThat(
        configTool("get", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.main"),
        containsOutput("offheap-resources.main=512MB"));
  }

  @Test
  public void setTcProperties() {
    assertThat(configTool("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.tc-properties.something=value"), is(successful()));

    assertThat(
        configTool("get", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.tc-properties.something", "-t", "index"),
        containsOutput("stripe.1.node.1.tc-properties.something=value"));
  }

  @Test
  public void setClientReconnectWindow() {
    assertThat(configTool("set", "-s", "localhost:" + getNodePort(), "-c", "client-reconnect-window=10s"), is(successful()));

    assertThat(
        configTool("get", "-s", "localhost:" + getNodePort(), "-c", "client-reconnect-window"),
        containsOutput("client-reconnect-window=10s"));
  }

  @Test
  public void setNodeGroupPort() {
    assertThat(configTool("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.group-port=9630"), is(successful()));

    assertThat(
        configTool("get", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.group-port", "-t", "index"),
        containsOutput("stripe.1.node.1.group-port=9630"));
  }

  @Test
  public void setDataDir() {
    assertThat(configTool("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.data-dirs.main=user-data/main/stripe1-node1-data-dir"), is(successful()));

    assertThat(
        configTool("get", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.data-dirs.main", "-t", "index"),
        containsOutput("stripe.1.node.1.data-dirs.main=user-data/main/stripe1-node1-data-dir"));
  }

  @Test
  public void setNodeBackupDir() {
    assertThat(configTool("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.backup-dir=backup/stripe1-node1-backup"), is(successful()));

    assertThat(
        configTool("get", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.backup-dir", "-t", "index"),
        containsOutput("stripe.1.node.1.backup-dir=backup/stripe1-node1-backup"));
  }

  @Test
  public void setTwoProperties() {
    assertThat(configTool("set", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.main=1GB", "-c", "stripe.1.node.1.data-dirs.main=stripe1-node1-data-dir"), is(successful()));

    assertThat(
        configTool("get", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.main", "-c", "stripe.1.node.1.data-dirs.main", "-t", "index"),
        allOf(containsOutput("offheap-resources.main=1GB"), containsOutput("stripe.1.node.1.data-dirs.main=stripe1-node1-data-dir")));
  }

  @Test
  public void setNodeName() throws Exception {
    assertThat(usingTopologyService(1, 1, topologyService -> topologyService.getUpcomingNodeContext().getNode().getName()), is(equalTo("node-1-1")));
    assertThat(configTool("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.name=foo"), is(successful()));
    assertThat(usingTopologyService(1, 1, topologyService -> topologyService.getUpcomingNodeContext().getNode().getName()), is(equalTo("foo")));
  }

  @Test
  public void setStripeName() throws Exception {
    assertThat(usingTopologyService(1, 1, topologyService -> topologyService.getUpcomingNodeContext().getStripe().getName()), startsWith("stripe-"));
    assertThat(configTool("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.stripe-name=foo"), is(successful()));
    assertThat(usingTopologyService(1, 1, topologyService -> topologyService.getUpcomingNodeContext().getStripe().getName()), is(equalTo("foo")));
  }

  @Test
  public void setRelaySource() {
    // not allowed at cluster level
    assertThat(configTool("set", "-s", "localhost:" + getNodePort(),
        "-c", "relay-source-hostname=" + "localhost",
        "-c", "relay-source-port=" + "9410"),
      allOf(
        not(successful()),
        containsOutput("Setting 'relay-source-hostname' cannot be set at cluster level")));

    // not allowed at stripe level
    assertThat(configTool("set", "-s", "localhost:" + getNodePort(),
        "-c", "stripe.1.relay-source-hostname=" + "localhost",
        "-c", "stripe.1.relay-source-port=" + "9410"),
      allOf(
        not(successful()),
        containsOutput("Setting 'relay-source-hostname' cannot be set at stripe level")));

    // test empty value
    assertThat(configTool("set", "-s", "localhost:" + getNodePort(),
        "-c", "stripe.1.node.1.relay-source-hostname=" + "  ",
        "-c", "stripe.1.node.1.relay-source-port=" + "   "),
      is(containsOutput("Invalid input: 'stripe.1.node.1.relay-source-hostname='. Reason: Operation set requires a value")));

    // all relay properties
    assertThat(configTool("set", "-s", "localhost:" + getNodePort(),
      "-c", "stripe.1.node.1.relay-source-hostname=" + "localhost",
      "-c", "stripe.1.node.1.relay-source-port=" + "9410"), is(successful()));

    assertThat(
      configTool("get", "-s", "localhost:" + getNodePort(), "-c", "relay-source-hostname", "-c", "relay-source-port", "-t", "index"),
      allOf(
        containsOutput("relay-source-hostname=localhost"),
        containsOutput("relay-source-port=" + 9410)));

    // all relay properties without index
    String nodeName = getNodeName(1, 1);
    assertThat(configTool("set", "-connect-to", "localhost:" + getNodePort(),
      "-setting", nodeName + ":relay-source-hostname=" + "localhost1",
      "-setting", nodeName + ":relay-source-port=" + "9411"), is(successful()));

    assertThat(
      configTool("get", "-connect-to", "localhost:" + getNodePort(), "-setting", "relay-source-hostname", "-setting", "relay-source-port"),
      allOf(
        containsOutput(nodeName + ":relay-source-hostname=localhost1"),
        containsOutput(nodeName + ":relay-source-port=" + 9411)));
  }

  @Test
  public void setRelayDestination() {
    // not allowed at cluster level
    assertThat(configTool("set", "-s", "localhost:" + getNodePort(),
        "-c", "relay-destination-hostname=" + "localhost",
        "-c", "relay-destination-port=" + "9410",
        "-c", "relay-destination-group-port=" + "9430"),
      allOf(
        not(successful()),
        containsOutput("Setting 'relay-destination-hostname' cannot be set at cluster level")));

    // not allowed at stripe level
    assertThat(configTool("set", "-s", "localhost:" + getNodePort(),
        "-c", "stripe.1.relay-destination-hostname=" + "localhost"),
      allOf(
        not(successful()),
        containsOutput("Setting 'relay-destination-hostname' cannot be set at stripe level")));

    // all relay properties
    assertThat(configTool("set", "-s", "localhost:" + getNodePort(),
        "-c", "stripe.1.node.1.relay-destination-hostname=" + "localhost",
        "-c", "stripe.1.node.1.relay-destination-port=" + "9410",
        "-c", "stripe.1.node.1.relay-destination-group-port=" + "9430")
      , is(successful()));

    assertThat(
      configTool("get", "-s", "localhost:" + getNodePort(),
        "-c", "relay-destination-hostname", "-c", "relay-destination-port", "-c", "relay-destination-group-port", "-t", "index"),
      allOf(
        containsOutput("relay-destination-hostname=localhost"),
        containsOutput("relay-destination-port=" + 9410),
        containsOutput("relay-destination-group-port=" + 9430)));

    // all relay properties without index
    String nodeName = getNodeName(1, 1);
    assertThat(configTool("set", "-connect-to", "localhost:" + getNodePort(),
      "-setting", nodeName + ":relay-destination-hostname=" + "localhost1",
      "-setting", nodeName + ":relay-destination-port=" + "9411",
      "-setting", nodeName + ":relay-destination-group-port=" + "9431"), is(successful()));

    assertThat(
      configTool("get", "-connect-to", "localhost:" + getNodePort(), "-setting", "relay-destination-hostname", "-setting", "relay-destination-port", "-setting", "relay-destination-group-port"),
      allOf(
        containsOutput(nodeName + ":relay-destination-hostname=localhost1"),
        containsOutput(nodeName + ":relay-destination-port=9411"),
        containsOutput(nodeName + ":relay-destination-group-port=9431")));
  }

  @Test
  public void setIncompleteRelayProperties() {
    assertThat(configTool("set", "-s", "localhost:" + getNodePort(),
        "-c", "stripe.1.node.1.relay-source-hostname=" + "localhost"),
      allOf(
        not(successful()),
        containsOutput("Relay source properties: {relay-source-hostname=localhost, relay-source-port=null} " +
          "of node with name: node-1-1 aren't well-formed. All relay source properties must be modified together")));

    assertThat(configTool("set", "-s", "localhost:" + getNodePort(),
        "-c", "stripe.1.node.1.relay-destination-hostname=" + "localhost",
        "-c", "stripe.1.node.1.relay-destination-group-port=" + "9430"),
      allOf(
        not(successful()),
        containsOutput("Relay destination properties: {relay-destination-group-port=9430, relay-destination-hostname=localhost, relay-destination-port=null} " +
          "of node with name: node-1-1 aren't well-formed. All relay destination properties must be modified together")));
  }

  @Test
  public void setBothRelaySourceAndDestination() {
    assertThat(configTool("set", "-s", "localhost:" + getNodePort(),
        "-c", "stripe.1.node.1.relay-destination-hostname=" + "localhost",
        "-c", "stripe.1.node.1.relay-source-port=" + "9410"),
      containsOutput("Node with name: node-1-1 has both relay source and relay destination properties configured: " +
        "[relay-source-port, relay-destination-hostname]. A node cannot be both relay source and relay destination"));

    assertThat(configTool("set", "-s", "localhost:" + getNodePort(),
        "-c", "stripe.1.node.1.relay-destination-hostname=" + "localhost",
        "-c", "stripe.1.node.1.relay-destination-port=" + "9410",
        "-c", "stripe.1.node.1.relay-destination-group-port=" + "9430",
        "-c", "stripe.1.node.1.relay-source-hostname=" + "localhost")
      , containsOutput("Node with name: node-1-1 has both relay source and relay destination properties configured: " +
        "[relay-source-hostname, relay-destination-hostname, relay-destination-port, relay-destination-group-port]. A node cannot be both relay source and relay destination"));
  }
}
