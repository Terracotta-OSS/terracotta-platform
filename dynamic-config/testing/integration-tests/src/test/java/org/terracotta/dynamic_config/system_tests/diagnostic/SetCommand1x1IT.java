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
  public void setRelay() {
    // not allowed at cluster level
    assertThat(configTool("set", "-s", "localhost:" + getNodePort(),
        "-c", "relay=" + "localhost"),
      allOf(
        not(successful()),
        containsOutput("Setting 'relay' cannot be set at cluster level")));

    // not allowed at stripe level
    assertThat(configTool("set", "-s", "localhost:" + getNodePort(),
        "-c", "stripe.1.relay=" + "localhost"),
      allOf(
        not(successful()),
        containsOutput("Setting 'relay' cannot be set at stripe level")));

    // test empty value
    assertThat(configTool("set", "-s", "localhost:" + getNodePort(),
        "-c", "stripe.1.node.1.relay=" + "  "),
      is(containsOutput("Invalid input: 'stripe.1.node.1.relay='. Reason: Setting 'relay' requires a value")));

    // all relay properties
    assertThat(configTool("set", "-s", "localhost:" + getNodePort(),
      "-c", "stripe.1.node.1.relay=" + "true",
      "-c", "stripe.1.node.1.replica-hostname=" + "localhost",
      "-c", "stripe.1.node.1.replica-port=" + "9410"), is(successful()));

    assertThat(
      configTool("get", "-s", "localhost:" + getNodePort(), "-c", "relay", "-c", "replica-hostname", "-c", "replica-port", "-t", "index"),
      allOf(
        containsOutput("relay=true"),
        containsOutput("replica-hostname=localhost"),
        containsOutput("replica-port=" + 9410)));

    // all relay properties without index
    String nodeName = getNodeName(1, 1);
    assertThat(configTool("set", "-connect-to", "localhost:" + getNodePort(),
      "-setting", nodeName + ":relay=" + "true",
      "-setting", nodeName + ":replica-hostname=" + "localhost1",
      "-setting", nodeName + ":replica-port=" + "9411"), is(successful()));

    assertThat(
      configTool("get", "-connect-to", "localhost:" + getNodePort(), "-setting", "relay", "-setting", "replica-hostname", "-setting", "replica-port"),
      allOf(
        containsOutput(nodeName + ":relay=true"),
        containsOutput(nodeName + ":replica-hostname=localhost1"),
        containsOutput(nodeName + ":replica-port=" + 9411)));
  }

  @Test
  public void setReplica() {
    // set not allowed
    assertThat(configTool("set", "-s", "localhost:" + getNodePort(),
        "-c", "stripe.1.node.1.replica=" + "true"),
      allOf(
        not(successful()),
        containsOutput("Reason: Setting 'replica' cannot be set")));
  }

  @Test
  public void setIncompleteRelayProperties() {
    assertThat(configTool("set", "-s", "localhost:" + getNodePort(),
        "-c", "stripe.1.node.1.relay=" + "true"),
      allOf(
        not(successful()),
        containsOutput("The relay setting is enabled for node with name: node-1-1, relay properties: {replica-hostname=null, replica-port=null} aren't well-formed")));

    // since relay is set to false, setting partial config will throw (all or none allowed)
    assertThat(configTool("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.replica-hostname=" + "localhost"),
      allOf(
        is(not(successful())),
        containsOutput("The relay setting is disabled for node with name: node-1-1, " +
          "properties: {replica-hostname=localhost, replica-port=null} are partially configured. " +
          "Either remove all properties or set all required properties")));
  }
}
