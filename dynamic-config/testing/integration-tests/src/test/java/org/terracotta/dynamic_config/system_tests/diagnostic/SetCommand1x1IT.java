/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
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
  public void setRelaySource() {
    assertThat(configTool("set", "-s", "localhost:" + getNodePort(), "-c", "relay-source=" + "193.123.5.4:" + 7878), is(successful()));

    configTool("get", "-s", "localhost:" + getNodePort(), "-c", "relay-source", "-t", "index");
    assertThat(
      configTool("get", "-s", "localhost:" + getNodePort(), "-c", "relay-source", "-t", "index"),
      containsOutput("relay-source=193.123.5.4:" + 7878));
  }

  @Test
  public void setRelayDestination() {
    assertThat(configTool("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.relay-destination=" + "localhost:" + 8787), is(successful()));

    configTool("get", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.relay-destination", "-t", "index");
    assertThat(
      configTool("get", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.relay-destination", "-t", "index"),
      containsOutput("stripe.1.node.1.relay-destination=localhost:" + 8787));
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
}
