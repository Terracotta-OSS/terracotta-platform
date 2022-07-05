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

import org.junit.Before;
import org.junit.Test;
import org.terracotta.dynamic_config.test_support.ClusterDefinition;
import org.terracotta.dynamic_config.test_support.DynamicConfigIT;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.containsOutput;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.successful;

@ClusterDefinition(nodesPerStripe = 2, failoverPriority = "")
public class SetCommand1x2IT extends DynamicConfigIT {

  @Before
  public void before() {
    assertThat(configTool("attach", "-d", "localhost:" + getNodePort(), "-s", "localhost:" + getNodePort(1, 2)), is(successful()));
  }

  @Test
  public void testStripe_level_setDataDirectory() {
    assertThat(configTool("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.data-dirs.main=stripe1-node1-data-dir"), is(successful()));

    assertThat(
        configTool("get", "-s", "localhost:" + getNodePort(), "-c", "data-dirs", "-t", "index"),
        allOf(containsOutput("stripe.1.node.1.data-dirs=main:stripe1-node1-data-dir"), containsOutput("stripe.1.node.2.data-dirs=main:stripe1-node1-data-dir")));
  }

  @Test
  public void testStripe_level_setBackupDirectory() {
    assertThat(configTool("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.backup-dir=backup/stripe-1"), is(successful()));

    assertThat(
        configTool("get", "-s", "localhost:" + getNodePort(), "-c", "backup-dir", "-t", "index"),
        allOf(containsOutput("stripe.1.node.1.backup-dir=backup/stripe-1"), containsOutput("stripe.1.node.2.backup-dir=backup/stripe-1")));
  }

  @Test
  public void testCluster_setOffheap() {
    assertThat(configTool("set", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.main=1GB"), is(successful()));

    assertThat(
        configTool("get", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.main"),
        containsOutput("offheap-resources.main=1GB"));
  }

  @Test
  public void testCluster_setBackupDirectory() {
    assertThat(configTool("set", "-s", "localhost:" + getNodePort(), "-c", "backup-dir=backup/data"), is(successful()));

    assertThat(
        configTool("get", "-s", "localhost:" + getNodePort(), "-c", "backup-dir"),
        containsOutput("backup-dir=backup/data"));
  }

  @Test
  public void testCluster_setClientLeaseTime() {
    assertThat(configTool("set", "-s", "localhost:" + getNodePort(), "-c", "client-lease-duration=10s"), is(successful()));

    assertThat(
        configTool("get", "-s", "localhost:" + getNodePort(), "-c", "client-lease-duration"),
        containsOutput("client-lease-duration=10s"));
  }

  @Test
  public void testCluster_setFailoverPriorityAvailability() {
    assertThat(configTool("set", "-s", "localhost:" + getNodePort(), "-c", "failover-priority=availability"), is(successful()));

    assertThat(
        configTool("get", "-s", "localhost:" + getNodePort(), "-c", "failover-priority"),
        containsOutput("failover-priority=availability"));
  }

  @Test
  public void testCluster_setFailoverPriorityConsistency() {
    assertThat(configTool("set", "-s", "localhost:" + getNodePort(), "-c", "failover-priority=consistency:2"), is(successful()));

    assertThat(
        configTool("get", "-s", "localhost:" + getNodePort(), "-c", "failover-priority"),
        containsOutput("failover-priority=consistency:2"));
  }

  @Test
  public void testCluster_setClientReconnectWindow() {
    assertThat(configTool("set", "-s", "localhost:" + getNodePort(), "-c", "client-reconnect-window=10s"), is(successful()));

    assertThat(
        configTool("get", "-s", "localhost:" + getNodePort(), "-c", "client-reconnect-window"),
        containsOutput("client-reconnect-window=10s"));
  }

  @Test
  public void setDuplicateNodeName() {
    assertThat(
        configTool("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.name=node-1-2"),
        containsOutput("Error: Found duplicate node name: node-1-2"));
  }

  @Test
  public void test_set_new_config() {
    assertThat(configTool("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.stripe-name=stripeA"), is(successful()));

    assertThat(configTool("set", "-s", "localhost:" + getNodePort(), "-c", "stripeA:data-dirs.main=stripe1-node1-data-dir"), is(successful()));
    assertThat(
        configTool("get", "-s", "localhost:" + getNodePort(), "-c", "data-dirs"),
        allOf(containsOutput("node-1-1:data-dirs=main:stripe1-node1-data-dir"), containsOutput("node-1-2:data-dirs=main:stripe1-node1-data-dir")));

    assertThat(configTool("set", "-s", "localhost:" + getNodePort(), "-c", "stripe:stripeA:backup-dir=backup/stripe-1"), is(successful()));
    assertThat(
        configTool("get", "-s", "localhost:" + getNodePort(), "-c", "backup-dir"),
        allOf(containsOutput("node:node-1-1:backup-dir=backup/stripe-1"), containsOutput("node-1-2:backup-dir=backup/stripe-1")));

    assertThat(
        configTool("set", "-s", "localhost:" + getNodePort(), "-c", "node-1-1:name=node-1-2"),
        containsOutput("Error: Found duplicate node name: node-1-2"));
  }
}
