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
package org.terracotta.dynamic_config.system_tests.activated;

import org.junit.Test;
import org.terracotta.dynamic_config.api.model.FailoverPriority;
import org.terracotta.dynamic_config.test_support.ClusterDefinition;
import org.terracotta.dynamic_config.test_support.DynamicConfigIT;
import org.terracotta.dynamic_config.test_support.InlineServers;

import java.time.Duration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.containsOutput;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.successful;

@ClusterDefinition(nodesPerStripe = 2, autoActivate = true)
public class DetachInConsistency1x2IT extends DynamicConfigIT {

  @Override
  protected FailoverPriority getFailoverPriority() {
    return FailoverPriority.consistency();
  }

  @Test
  public void test_detach_active_node() {
    final int activeId = waitForActive(1);
    final int passiveId = waitForNPassives(1, 1)[0];

    // Passive goes in passive_suspended state
    connectionTimeout = Duration.ofSeconds(5); // to not be stuck when we connect to fetch the nomad entity
    assertThat(
        configTool("detach", "-f", "-d", "localhost:" + getNodePort(1, passiveId), "-s", "localhost:" + getNodePort(1, activeId)),
        containsOutput("Unable to connect to stripe UID"));

    waitUntil(() -> angela.tsa().getStopped().size(), is(1));
    withTopologyService(1, passiveId, topologyService -> assertTrue(topologyService.isActivated()));
    assertThat(getUpcomingCluster("localhost", getNodePort(1, passiveId)).getNodeCount(), is(equalTo(2)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, passiveId)).getNodeCount(), is(equalTo(2)));
  }

  @Test
  public void test_detach_passive_from_activated_cluster() throws Exception {
    final int activeId = waitForActive(1);
    final int passiveId = waitForNPassives(1, 1)[0];

    assertThat(configTool("detach", "-f", "-d", "localhost:" + getNodePort(1, activeId), "-s", "localhost:" + getNodePort(1, passiveId)), is(successful()));

    waitUntil(() -> angela.tsa().getStopped().size(), is(1));
    withTopologyService(1, activeId, topologyService -> assertTrue(topologyService.isActivated()));
    assertTopologyChanged(activeId);
  }

  @Test
  public void test_detach_offline_node_in_consistency_mode() {
    final int activeId = waitForActive(1);
    final int passiveId = waitForNPassives(1, 1)[0];

    stopNode(1, passiveId);

    assertThat(configTool("detach", "-d", "localhost:" + getNodePort(1, activeId), "-s", "localhost:" + getNodePort(1, passiveId)), is(successful()));

    withTopologyService(1, activeId, topologyService -> assertTrue(topologyService.isActivated()));
    assertThat(getUpcomingCluster("localhost", getNodePort(1, activeId)).getNodeCount(), is(equalTo(1)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, activeId)).getNodeCount(), is(equalTo(1)));
  }

  @Test
  public void detachNodeFailInActiveAtPrepare() {
    final int activeId = waitForActive(1);
    final int passiveId = waitForNPassives(1, 1)[0];

    String propertySettingString = "stripe.1.node." + activeId + ".tc-properties.detachStatus=prepareDeletion-failure";

    //create prepare failure on active
    assertThat(configTool("set", "-s", "localhost:" + getNodePort(1, 1), "-c", propertySettingString), is(successful()));

    // detach failure (forcing detach otherwise we have to restart cluster)
    assertThat(
        configTool("detach", "-f", "-d", "localhost:" + getNodePort(1, activeId), "-s", "localhost:" + getNodePort(1, passiveId)),
        containsOutput("Two-Phase commit failed"));

    waitUntil(() -> angela.tsa().getStopped().size(), is(1));

    assertThat(getUpcomingCluster("localhost", getNodePort(1, activeId)).getNodeCount(), is(equalTo(2)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, activeId)).getNodeCount(), is(equalTo(2)));
    withTopologyService(1, activeId, topologyService -> assertTrue(topologyService.isActivated()));
  }

  @Test
  @InlineServers(false)
  public void testFailoverDuringNomadCommitForPassiveRemoval() {
    final int activeId = waitForActive(1);
    final int passiveId = waitForNPassives(1, 1)[0];

    String propertySettingString = "stripe.1.node." + activeId + ".tc-properties.failoverDeletion=killDeletion-commit";

    //setup for failover in commit phase on active
    assertThat(configTool("set", "-s", "localhost:" + getNodePort(1, 1), "-c", propertySettingString), is(successful()));

    entityOperationTimeout = Duration.ofSeconds(5); // to not be stuck in failover
    assertThat(
        configTool("detach", "-f", "-d", "localhost:" + getNodePort(1, activeId),
            "-s", "localhost:" + getNodePort(1, passiveId)),
        containsOutput("Two-Phase commit failed"));

    // Stripe is lost no active
    waitUntil(() -> angela.tsa().getStopped().size(), is(2));

    // Can't become active
    startNode(1, activeId, "-r", getNode(1, activeId).getConfigRepo());

    // Force the commit
    assertThat(configTool("repair", "-f", "commit", "-s", "localhost:" + getNodePort(1, activeId)), is(successful()));

    withTopologyService(1, activeId, topologyService -> assertTrue(topologyService.isActivated()));
    withTopologyService(1, activeId, topologyService -> assertFalse(topologyService.hasIncompleteChange()));
    assertThat(getUpcomingCluster("localhost", getNodePort(1, activeId)).getNodeCount(), is(equalTo(1)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, activeId)).getNodeCount(), is(equalTo(1)));
  }

  @Test
  public void test_detach_passive_offline_prepare_fail_at_active() {
    final int activeId = waitForActive(1);
    final int passiveId = waitForNPassives(1, 1)[0];

    //create prepare failure on active
    String propertySettingString = "stripe.1.node." + activeId + ".tc-properties.detachStatus=prepareDeletion-failure";
    assertThat(configTool("set", "-s", "localhost:" + getNodePort(1, 1), "-c", propertySettingString), is(successful()));

    stopNode(1, passiveId);

    // detach failure (forcing detach otherwise we have to restart cluster)
    assertThat(
        configTool("detach", "-f", "-d", "localhost:" + getNodePort(1, activeId), "-s", "localhost:" + getNodePort(1, passiveId)),
        containsOutput("Two-Phase commit failed"));

    withTopologyService(1, activeId, topologyService -> assertTrue(topologyService.isActivated()));
    assertThat(getUpcomingCluster("localhost", getNodePort(1, activeId)).getNodeCount(), is(equalTo(2)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, activeId)).getNodeCount(), is(equalTo(2)));
  }

  @Test
  @InlineServers(false)
  public void test_detach_passive_offline_commit_fail_at_active() {
    final int activeId = waitForActive(1);
    final int passiveId = waitForNPassives(1, 1)[0];

    //create failover while committing
    String propertySettingString = "stripe.1.node." + activeId + ".tc-properties.failoverDeletion=killDeletion-commit";
    assertThat(configTool("set", "-s", "localhost:" + getNodePort(1, 1), "-c", propertySettingString), is(successful()));

    stopNode(1, passiveId);

    //Both active and passive is down.
    entityOperationTimeout = Duration.ofSeconds(5); // to not be stuck in failover
    assertThat(
        configTool("detach", "-f", "-d", "localhost:" + getNodePort(1, activeId), "-s", "localhost:" + getNodePort(1, passiveId)),
        containsOutput("Two-Phase commit failed"));

    // can't become active
    startNode(1, activeId, "-r", getNode(1, activeId).getConfigRepo());

    // Force the commit
    assertThat(configTool("repair", "-f", "commit", "-s", "localhost:" + getNodePort(1, activeId)), is(successful()));

    withTopologyService(1, activeId, topologyService -> assertTrue(topologyService.isActivated()));
    withTopologyService(1, activeId, topologyService -> assertFalse(topologyService.hasIncompleteChange()));
    assertThat(getUpcomingCluster("localhost", getNodePort(1, activeId)).getNodeCount(), is(equalTo(1)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, activeId)).getNodeCount(), is(equalTo(1)));
  }

  private void assertTopologyChanged(int nodeId) {
    withTopologyService(1, nodeId, topologyService -> assertTrue(topologyService.isActivated()));
    assertThat(getUpcomingCluster("localhost", getNodePort(1, nodeId)).getNodeCount(), is(equalTo(1)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, nodeId)).getNodeCount(), is(equalTo(1)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, nodeId)).getSingleNode().get().getPort().orDefault(), is(equalTo(getNodePort(1, nodeId))));
  }
}
