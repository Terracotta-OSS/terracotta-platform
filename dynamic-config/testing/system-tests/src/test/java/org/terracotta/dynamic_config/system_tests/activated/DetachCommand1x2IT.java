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

import com.terracotta.connection.api.TerracottaConnectionService;
import org.junit.Test;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.UID;
import org.terracotta.dynamic_config.entity.topology.client.DynamicTopologyEntity;
import org.terracotta.dynamic_config.entity.topology.client.DynamicTopologyEntityFactory;
import org.terracotta.dynamic_config.test_support.ClusterDefinition;
import org.terracotta.dynamic_config.test_support.DynamicConfigIT;
import org.terracotta.dynamic_config.test_support.InlineServers;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.containsOutput;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.successful;

/**
 * @author Mathieu Carbou
 */
@ClusterDefinition(nodesPerStripe = 2, autoActivate = true)
public class DetachCommand1x2IT extends DynamicConfigIT {

  @Test
  public void test_force_detach_active_node() throws Exception {
    final int activeId = waitForActive(1);
    final int passiveId = waitForNPassives(1, 1)[0];

    assertThat(configTool("detach", "-f", "-d", "localhost:" + getNodePort(1, passiveId), "-s", "localhost:" + getNodePort(1, activeId)), is(successful()));

    // failover - existing passive becomes active
    waitForActive(1, passiveId);
    withTopologyService(1, passiveId, topologyService -> assertTrue(topologyService.isActivated()));

    waitUntil(() -> angela.tsa().getStopped().size(), is(1));
    assertTopologyChanged(passiveId);
  }

  @Test
  public void test_force_detach_passive_from_activated_cluster() throws Exception {
    final int activeId = waitForActive(1);
    final int passiveId = waitForNPassives(1, 1)[0];

    assertThat(configTool("detach", "-f", "-d", "localhost:" + getNodePort(1, activeId), "-s", "localhost:" + getNodePort(1, passiveId)), is(successful()));
    waitUntil(() -> angela.tsa().getStopped().size(), is(1));
    assertTopologyChanged(activeId);
  }

  @Test
  public void test_topology_entity_callback_onNodeRemoval() throws Exception {
    final int activeId = waitForActive(1);
    final int passiveId = waitForNPassives(1, 1)[0];

    try (DynamicTopologyEntity dynamicTopologyEntity = DynamicTopologyEntityFactory.fetch(
        new TerracottaConnectionService(),
        Collections.singletonList(InetSocketAddress.createUnresolved("localhost", getNodePort(1, activeId))),
        "dynamic-config-topology-entity",
        getConnectionTimeout(),
        new DynamicTopologyEntity.Settings().setRequestTimeout(getDiagnosticOperationTimeout()),
        null)) {

      CountDownLatch called = new CountDownLatch(1);

      dynamicTopologyEntity.setListener(new DynamicTopologyEntity.Listener() {
        @Override
        public void onNodeRemoval(Cluster cluster, UID stripeUID, Node removedNode) {
          called.countDown();
        }
      });

      assertThat(configTool("detach", "-f", "-d", "localhost:" + getNodePort(1, activeId), "-s", "localhost:" + getNodePort(1, passiveId)), is(successful()));

      called.await();
    }
  }

  @Test
  public void test_detach_passive_from_activated_cluster_requiring_restart() throws Exception {
    final int activeId = waitForActive(1);
    final int passiveId = waitForNPassives(1, 1)[0];

    // do a change requiring a restart on the remaining nodes
    assertThat(
        configTool("set", "-s", "localhost:" + getNodePort(1, activeId), "-c", "stripe.1.node." + activeId + ".tc-properties.foo=bar"),
        containsOutput("Restart required for nodes:"));

    stopNode(1, passiveId);

    // try to detach the passive node
    assertThat(
        configTool("detach", "-d", "localhost:" + getNodePort(1, activeId), "-s", "localhost:" + getNodePort(1, passiveId)),
        both(not(successful())).and(containsOutput("Impossible to do any topology change")));

    // try forcing the detach
    assertThat(configTool("detach", "-f", "-d", "localhost:" + getNodePort(1, activeId), "-s", "localhost:" + getNodePort(1, passiveId)), is(successful()));

    assertTopologyChanged(activeId);
  }

  @Test
  public void test_detach_passive_requiring_restart_from_activated_cluster() throws Exception {
    final int activeId = waitForActive(1);
    final int passiveId = waitForNPassives(1, 1)[0];

    // do a change requiring a restart on the remaining nodes
    assertThat(
        configTool("set", "-s", "localhost:" + getNodePort(1, passiveId), "-c", "stripe.1.node." + passiveId + ".tc-properties.foo=bar"),
        containsOutput("Restart required for nodes:"));

    stopNode(1, passiveId);

    // try to detach the passive node
    assertThat(
        configTool("detach", "-d", "localhost:" + getNodePort(1, activeId), "-s", "localhost:" + getNodePort(1, passiveId)),
        is(successful()));

    assertTopologyChanged(activeId);
  }

  @Test
  public void test_detach_online_node_in_availability_mode() throws Exception {
    final int activeId = waitForActive(1);
    final int passiveId = waitForNPassives(1, 1)[0];

    // detaching an online node needs to be forced
    assertThat(
        configTool("detach", "-d", "localhost:" + getNodePort(1, activeId), "-s", "localhost:" + getNodePort(1, passiveId)),
        containsOutput("Nodes must be safely shutdown first. Please refer to the Troubleshooting Guide for more help."));

    assertThat(configTool("detach", "-d", "localhost:" + getNodePort(1, activeId), "-s", "localhost:" + getNodePort(1, passiveId), "-f"), is(successful()));

    waitUntil(() -> angela.tsa().getStopped().size(), is(1));
    assertTopologyChanged(activeId);
  }

  @Test
  public void detachNodeFailInActiveAtPrepare() throws Exception {
    final int activeId = waitForActive(1);
    final int passiveId = waitForNPassives(1, 1)[0];

    String propertySettingString = "stripe.1.node." + activeId + ".tc-properties.detachStatus=prepareDeletion-failure";

    //create prepare failure on active
    waitUntil(()->configTool("set", "-s", "localhost:" + getNodePort(1, 1), "-c", propertySettingString), is(successful()));

    // detach failure (forcing detach otherwise we have to restart cluster)
    assertThat(
        configTool("detach", "-f", "-d", "localhost:" + getNodePort(1, activeId), "-s", "localhost:" + getNodePort(1, passiveId)),
        containsOutput("Two-Phase commit failed"));

    waitUntil(() -> angela.tsa().getStopped().size(), is(1));

    // Nomad rollback happened
    // we end up with a cluster of 2 nodes with 1 of them removed
    withTopologyService(1, activeId, topologyService -> assertTrue(topologyService.isActivated()));
    assertThat(getUpcomingCluster("localhost", getNodePort(1, activeId)).getNodeCount(), is(equalTo(2)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, activeId)).getNodeCount(), is(equalTo(2)));
  }

  @Test
  @InlineServers(false)
  public void testFailoverDuringNomadCommitForPassiveRemoval() throws Exception {
    final int activeId = waitForActive(1);
    final int passiveId = waitForNPassives(1, 1)[0];

    String propertySettingString = "stripe.1.node." + activeId + ".tc-properties.failoverDeletion=killDeletion-commit";

    //setup for failover in commit phase on active
    assertThat(configTool("set", "-s", "localhost:" + getNodePort(1, 1), "-c", propertySettingString), is(successful()));

    // Stripe is lost no active
    entityOperationTimeout = Duration.ofSeconds(5); // to not be stuck in failover
    assertThat(
        configTool("detach", "-f", "-d", "localhost:" + getNodePort(1, activeId), "-s", "localhost:" + getNodePort(1, passiveId)),
        containsOutput("Commit failed for node localhost:" + getNodePort(1, activeId) + ". Reason: java.util.concurrent.TimeoutException"));

    waitUntil(() -> angela.tsa().getStopped().size(), is(2));

    startNode(1, activeId, "-r", getNode(1, activeId).getConfigRepo());
    waitForActive(1, activeId);

    // Active has prepare changes for node removal
    withTopologyService(1, activeId, topologyService -> assertTrue(topologyService.isActivated()));
    withTopologyService(1, activeId, topologyService -> assertTrue(topologyService.hasIncompleteChange()));
    assertThat(getUpcomingCluster(1, activeId).getNodeCount(), is(equalTo(2)));
    assertThat(getRuntimeCluster(1, activeId).getNodeCount(), is(equalTo(2)));
  }

  @Test
  public void test_detach_passive_prepare_fail_at_active() throws Exception {
    final int activeId = waitForActive(1);
    final int passiveId = waitForNPassives(1, 1)[0];

    //create prepare failure on active
    String propertySettingString = "stripe.1.node." + activeId + ".tc-properties.detachStatus=prepareDeletion-failure";
    assertThat(configTool("set", "-s", "localhost:" + getNodePort(1, 1), "-c", propertySettingString), is(successful()));

    // detach failure (forcing detach otherwise we have to restart cluster)
    assertThat(
        configTool("detach", "-f", "-d", "localhost:" + getNodePort(1, activeId), "-s", "localhost:" + getNodePort(1, passiveId)),
        containsOutput("Two-Phase commit failed"));

    withTopologyService(1, activeId, topologyService -> assertTrue(topologyService.isActivated()));
    withTopologyService(1, activeId, topologyService -> assertFalse(topologyService.hasIncompleteChange()));
    assertThat(getUpcomingCluster("localhost", getNodePort(1, activeId)).getNodeCount(), is(equalTo(2)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, activeId)).getNodeCount(), is(equalTo(2)));
  }

  @Test
  @InlineServers(false)
  public void test_detach_passive_commit_fail_at_active() throws Exception {
    final int activeId = waitForActive(1);
    final int passiveId = waitForNPassives(1, 1)[0];

    //create failover while committing
    String propertySettingString = "stripe.1.node." + activeId + ".tc-properties.failoverDeletion=killDeletion-commit";
    assertThat(configTool("set", "-s", "localhost:" + getNodePort(1, 1), "-c", propertySettingString), is(successful()));

    //Both active and passive are down.
    entityOperationTimeout = Duration.ofSeconds(5); // to not be stuck in failover
    assertThat(
        configTool("detach", "-f", "-d", "localhost:" + getNodePort(1, activeId), "-s", "localhost:" + getNodePort(1, passiveId)),
        containsOutput("Two-Phase commit failed"));

    waitUntil(() -> angela.tsa().getStopped().size(), is(2));

    startNode(1, activeId, "-r", getNode(1, activeId).getConfigRepo());
    waitForActive(1, activeId);

    withTopologyService(1, activeId, topologyService -> assertTrue(topologyService.isActivated()));
    withTopologyService(1, activeId, topologyService -> assertTrue(topologyService.hasIncompleteChange()));
    assertThat(getUpcomingCluster("localhost", getNodePort(1, activeId)).getNodeCount(), is(equalTo(2)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, activeId)).getNodeCount(), is(equalTo(2)));
  }

  private void assertTopologyChanged(int nodeId) throws Exception {
    withTopologyService(1, nodeId, topologyService -> assertTrue(topologyService.isActivated()));
    assertThat(getUpcomingCluster("localhost", getNodePort(1, nodeId)).getNodeCount(), is(equalTo(1)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, nodeId)).getNodeCount(), is(equalTo(1)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, nodeId)).getSingleNode().get().getPort().orDefault(), is(equalTo(getNodePort(1, nodeId))));
  }
}
