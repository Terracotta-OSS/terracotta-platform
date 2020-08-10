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
package org.terracotta.dynamic_config.system_tests.diagnostic;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.terracotta.dynamic_config.test_support.ClusterDefinition;
import org.terracotta.dynamic_config.test_support.DynamicConfigIT;

import static java.io.File.separator;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.containsOutput;

@ClusterDefinition
public class SetCommand1x1IT extends DynamicConfigIT {

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Test
  public void setOffheapResource() {
    invokeConfigTool("set", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.main=512MB");

    assertThat(
        invokeConfigTool("get", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.main"),
        containsOutput("offheap-resources.main=512MB"));
  }

  @Test
  public void setTcProperties() {
    invokeConfigTool("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.tc-properties.something=value");

    assertThat(
        invokeConfigTool("get", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.tc-properties.something"),
        containsOutput("stripe.1.node.1.tc-properties.something=value"));
  }

  @Test
  public void setClientReconnectWindow() {
    invokeConfigTool("set", "-s", "localhost:" + getNodePort(), "-c", "client-reconnect-window=10s");

    assertThat(
        invokeConfigTool("get", "-s", "localhost:" + getNodePort(), "-c", "client-reconnect-window"),
        containsOutput("client-reconnect-window=10s"));
  }

  @Test
  public void setNodeGroupPort() {
    invokeConfigTool("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.group-port=9630");

    assertThat(
        invokeConfigTool("get", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.group-port"),
        containsOutput("stripe.1.node.1.group-port=9630"));
  }

  @Test
  public void setDataDir() {
    invokeConfigTool("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.data-dirs.main=user-data/main/stripe1-node1-data-dir");

    assertThat(
        invokeConfigTool("get", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.data-dirs.main"),
        containsOutput("stripe.1.node.1.data-dirs.main=user-data" + separator + "main" + separator + "stripe1-node1-data-dir"));
  }

  @Test
  public void setNodeBackupDir() {
    invokeConfigTool("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.backup-dir=backup/stripe1-node1-backup");

    assertThat(
        invokeConfigTool("get", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.backup-dir"),
        containsOutput("stripe.1.node.1.backup-dir=backup" + separator + "stripe1-node1-backup"));
  }

  @Test
  public void setTwoProperties() {
    invokeConfigTool("set", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.main=1GB", "-c", "stripe.1.node.1.data-dirs.main=stripe1-node1-data-dir");

    assertThat(
        invokeConfigTool("get", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.main", "-c", "stripe.1.node.1.data-dirs.main"),
        allOf(containsOutput("offheap-resources.main=1GB"), containsOutput("stripe.1.node.1.data-dirs.main=stripe1-node1-data-dir")));
  }

  @Test
  public void setNodeName() throws Exception {
    assertThat(usingTopologyService(1, 1, topologyService -> topologyService.getUpcomingNodeContext().getNode().getName()), is(equalTo("node-1-1")));
    invokeConfigTool("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.name=foo");
    assertThat(usingTopologyService(1, 1, topologyService -> topologyService.getUpcomingNodeContext().getNode().getName()), is(equalTo("foo")));
  }

  @Test
  public void setStripeName() throws Exception {
    assertThat(usingTopologyService(1, 1, topologyService -> topologyService.getUpcomingNodeContext().getStripe().getName()), startsWith("stripe-"));
    invokeConfigTool("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.stripe-name=foo");
    assertThat(usingTopologyService(1, 1, topologyService -> topologyService.getUpcomingNodeContext().getStripe().getName()), is(equalTo("foo")));
  }
}
