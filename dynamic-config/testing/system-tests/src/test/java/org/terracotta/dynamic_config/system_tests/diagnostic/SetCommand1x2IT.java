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

import java.io.File;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.containsOutput;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.hasExitStatus;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.successful;

@ClusterDefinition(nodesPerStripe = 2)
public class SetCommand1x2IT extends DynamicConfigIT {

  @Before
  public void before() throws Exception {
    assertThat(configToolInvocation("attach", "-d", "localhost:" + getNodePort(), "-s", "localhost:" + getNodePort(1, 2)), is(successful()));
  }

  /*<--Stripe-wide Tests-->*/
  @Test
  public void testStripe_level_setDataDirectory() {
    assertThat(configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.data-dirs.main=stripe1-node1-data-dir"), is(successful()));

    assertThat(configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "data-dirs"),
        allOf(hasExitStatus(0), containsOutput("stripe.1.node.1.data-dirs=main:stripe1-node1-data-dir"), containsOutput("stripe.1.node.2.data-dirs=main:stripe1-node1-data-dir")));
  }

  @Test
  public void testStripe_level_setBackupDirectory() {
    assertThat(configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.backup-dir=backup" + File.separator + "stripe-1"), is(successful()));

    assertThat(configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "backup-dir"),
        allOf(hasExitStatus(0), containsOutput("stripe.1.node.1.backup-dir=backup" + File.separator + "stripe-1"), containsOutput("stripe.1.node.2.backup-dir=backup" + File.separator + "stripe-1")));
  }


  /*<--Cluster-wide Tests-->*/
  @Test
  public void testCluster_setOffheap() {
    assertThat(configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.main=1GB"), is(successful()));

    assertThat(configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.main"),
        allOf(hasExitStatus(0), containsOutput("offheap-resources.main=1GB")));
  }

  @Test
  public void testCluster_setBackupDirectory() {
    assertThat(configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "backup-dir=backup" + File.separator + "data"), is(successful()));

    assertThat(configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "backup-dir"),
        allOf(hasExitStatus(0), containsOutput("backup-dir=backup" + File.separator + "data")));
  }

  @Test
  public void testCluster_setClientLeaseTime() {
    assertThat(configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "client-lease-duration=10s"), is(successful()));

    assertThat(configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "client-lease-duration"),
        allOf(hasExitStatus(0), containsOutput("client-lease-duration=10s")));
  }

  @Test
  public void testCluster_setFailoverPriorityAvailability() {
    assertThat(configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "failover-priority=availability"), is(successful()));

    assertThat(configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "failover-priority"),
        allOf(hasExitStatus(0), containsOutput("failover-priority=availability")));
  }

  @Test
  public void testCluster_setFailoverPriorityConsistency() {
    assertThat(configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "failover-priority=consistency:2"), is(successful()));

    assertThat(configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "failover-priority"),
        allOf(hasExitStatus(0), containsOutput("failover-priority=consistency:2")));
  }

  @Test
  public void testCluster_setClientReconnectWindow() {
    assertThat(configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "client-reconnect-window=10s"), is(successful()));

    assertThat(configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "client-reconnect-window"),
        allOf(hasExitStatus(0), containsOutput("client-reconnect-window=10s")));
  }
}
