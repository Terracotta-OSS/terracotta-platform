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

import org.junit.Before;
import org.junit.Test;
import org.terracotta.dynamic_config.api.model.FailoverPriority;
import org.terracotta.dynamic_config.api.service.TopologyService;
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

@ClusterDefinition(nodesPerStripe = 3)
public class DetachInConsistency1x3IT extends DynamicConfigIT {

  @Override
  protected FailoverPriority getFailoverPriority() {
    return FailoverPriority.consistency();
  }

  @Before
  public void setUp() {
    // Angela limitation causing to attach explicitly and create cluster
    assertThat(configTool("attach", "-d", "localhost:" + getNodePort(1, 1), "-s", "localhost:" + getNodePort(1, 2)), is(successful()));
    assertThat(configTool("attach", "-d", "localhost:" + getNodePort(1, 1), "-s", "localhost:" + getNodePort(1, 3)), is(successful()));
    assertThat(configTool("activate", "-n", "mycluster", "-s", "localhost:" + getNodePort(1, 1)), is(successful()));
    waitForActive(1);
    waitForNPassives(1, 2);
  }

  @Test
  public void test_detach_active_node() {
    final int activeId = waitForActive(1);
    final int[] passives = waitForNPassives(1, 2);
    final int passiveId1 = passives[0];
    final int passiveId2 = passives[1];

    assertThat(configTool("detach", "-f", "-d", "localhost:" + getNodePort(1, passiveId1), "-s", "localhost:" + getNodePort(1, activeId)), is(successful()));

    // failover - one of the passive becomes active
    waitForActive(1);
    withTopologyService(1, passiveId1, topologyService -> assertTrue(topologyService.isActivated()));
    withTopologyService(1, passiveId2, topologyService -> assertTrue(topologyService.isActivated()));

    waitUntil(() -> angela.tsa().getStopped().size(), is(1));
    assertThat(getUpcomingCluster("localhost", getNodePort(1, passiveId1)).getNodeCount(), is(equalTo(2)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, passiveId1)).getNodeCount(), is(equalTo(2)));

    assertThat(getUpcomingCluster("localhost", getNodePort(1, passiveId2)).getNodeCount(), is(equalTo(2)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, passiveId2)).getNodeCount(), is(equalTo(2)));
  }

  @Test
  public void test_detach_passive_from_activated_cluster() {
    final int activeId = waitForActive(1);
    final int[] passives = waitForNPassives(1, 2);
    final int passiveId1 = passives[0];
    final int passiveId2 = passives[1];

    assertThat(configTool("detach", "-f", "-d", "localhost:" + getNodePort(1, activeId), "-s", "localhost:" + getNodePort(1, passiveId1)), is(successful()));
    waitUntil(() -> angela.tsa().getStopped().size(), is(1));

    withTopologyService(1, activeId, topologyService -> assertTrue(topologyService.isActivated()));
    withTopologyService(1, passiveId2, topologyService -> assertTrue(topologyService.isActivated()));

    assertThat(getUpcomingCluster("localhost", getNodePort(1, activeId)).getNodeCount(), is(equalTo(2)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, activeId)).getNodeCount(), is(equalTo(2)));

    assertThat(getUpcomingCluster("localhost", getNodePort(1, passiveId2)).getNodeCount(), is(equalTo(2)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, passiveId2)).getNodeCount(), is(equalTo(2)));
  }

  @Test
  public void test_detach_offline_node_in_consistency_mode() {
    final int activeId = waitForActive(1);
    final int[] passives = waitForNPassives(1, 2);
    final int passiveId1 = passives[0];
    final int passiveId2 = passives[1];

    stopNode(1, passiveId1);

    assertThat(configTool("detach", "-d", "localhost:" + getNodePort(1, activeId), "-s", "localhost:" + getNodePort(1, passiveId1)), is(successful()));

    withTopologyService(1, activeId, topologyService -> assertTrue(topologyService.isActivated()));
    assertThat(getUpcomingCluster("localhost", getNodePort(1, activeId)).getNodeCount(), is(equalTo(2)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, activeId)).getNodeCount(), is(equalTo(2)));

    withTopologyService(1, passiveId2, topologyService -> assertTrue(topologyService.isActivated()));
    assertThat(getUpcomingCluster("localhost", getNodePort(1, passiveId2)).getNodeCount(), is(equalTo(2)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, passiveId2)).getNodeCount(), is(equalTo(2)));
  }

  @Test
  public void detachNodeFailInActiveAtPrepare() {
    final int activeId = waitForActive(1);
    final int[] passives = waitForNPassives(1, 2);
    final int passiveId1 = passives[0];
    final int passiveId2 = passives[1];

    String propertySettingString = "stripe.1.node." + activeId + ".tc-properties.detachStatus=prepareDeletion-failure";

    //create prepare failure on active
    assertThat(configTool("set", "-s", "localhost:" + getNodePort(1, 1), "-c", propertySettingString), is(successful()));

    // detach failure (forcing detach otherwise we have to restart cluster)
    assertThat(
        configTool("detach", "-f", "-d", "localhost:" + getNodePort(1, activeId), "-s", "localhost:" + getNodePort(1, passiveId1)),
        containsOutput("Two-Phase commit failed"));

    waitUntil(() -> angela.tsa().getStopped().size(), is(1));

    assertThat(getUpcomingCluster("localhost", getNodePort(1, activeId)).getNodeCount(), is(equalTo(3)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, activeId)).getNodeCount(), is(equalTo(3)));

    assertThat(getUpcomingCluster("localhost", getNodePort(1, passiveId2)).getNodeCount(), is(equalTo(3)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, passiveId2)).getNodeCount(), is(equalTo(3)));

    withTopologyService(1, activeId, topologyService -> assertTrue(topologyService.isActivated()));
    withTopologyService(1, passiveId2, topologyService -> assertTrue(topologyService.isActivated()));
  }

  @Test
  @InlineServers(false)
  public void testFailoverDuringNomadCommitForPassiveRemoval() {
    final int activeId = waitForActive(1);
    final int[] passives = waitForNPassives(1, 2);
    final int passiveId1 = passives[0];
    final int passiveId2 = passives[1];

    String propertySettingString = "stripe.1.node." + activeId + ".tc-properties.failoverDeletion=killDeletion-commit";

    //setup for failover in commit phase on active
    assertThat(configTool("set", "-s", "localhost:" + getNodePort(1, 1), "-c", propertySettingString), is(successful()));

    // To ensure that passiveId don't become active during failover since that is what we will remove
    stopNode(1, passiveId1);

    entityOperationTimeout = Duration.ofSeconds(5); // to not be stuck in failover
    assertThat(
        configTool("detach", "-f", "-d", "localhost:" + getNodePort(1, activeId), "-s", "localhost:" + getNodePort(1, passiveId1)),
        containsOutput("Two-Phase commit failed"));
    waitForStopped(1, activeId);

    startNode(1, activeId, "-r", getNode(1, activeId).getConfigRepo());

    waitForActive(1, passiveId2);
    waitForPassive(1, activeId);

    // the passive server will restart once after startup: start => partial commit => sync => repair partial commit => restart
    // waitForPassive(1, activeId); will see the first startup but the problem is that the test will continue while the passive is
    // restarting after the sync
    waitUntil(() -> usingTopologyService(1, passiveId2, TopologyService::hasIncompleteChange), is(false));

    assertThat(getUpcomingCluster("localhost", getNodePort(1, passiveId2)).getNodeCount(), is(equalTo(2)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, passiveId2)).getNodeCount(), is(equalTo(2)));

    withTopologyService(1, activeId, topologyService -> assertTrue(topologyService.isActivated()));
    withTopologyService(1, activeId, topologyService -> assertFalse(topologyService.hasIncompleteChange()));
    assertThat(getUpcomingCluster(1, activeId).getNodeCount(), is(equalTo(2)));
    assertThat(getRuntimeCluster(1, activeId).getNodeCount(), is(equalTo(2)));
  }

  @Test
  public void test_detach_passive_offline_prepare_fail_at_active() {
    final int activeId = waitForActive(1);
    final int[] passives = waitForNPassives(1, 2);
    final int passiveId1 = passives[0];
    final int passiveId2 = passives[1];

    //create prepare failure on active
    String propertySettingString = "stripe.1.node." + activeId + ".tc-properties.detachStatus=prepareDeletion-failure";
    assertThat(configTool("set", "-s", "localhost:" + getNodePort(1, 1), "-c", propertySettingString), is(successful()));

    stopNode(1, passiveId1);

    // detach failure (forcing detach otherwise we have to restart cluster)
    assertThat(
        configTool("detach", "-f", "-d", "localhost:" + getNodePort(1, activeId), "-s", "localhost:" + getNodePort(1, passiveId1)),
        containsOutput("Two-Phase commit failed"));

    // How will update the existing nodes ? 
    withTopologyService(1, activeId, topologyService -> assertTrue(topologyService.isActivated()));
    assertThat(getUpcomingCluster("localhost", getNodePort(1, activeId)).getNodeCount(), is(equalTo(3)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, activeId)).getNodeCount(), is(equalTo(3)));

    withTopologyService(1, passiveId2, topologyService -> assertTrue(topologyService.isActivated()));
    assertThat(getUpcomingCluster("localhost", getNodePort(1, passiveId2)).getNodeCount(), is(equalTo(3)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, passiveId2)).getNodeCount(), is(equalTo(3)));
  }

  @Test
  @InlineServers(false)
  public void test_detach_passive_offline_commit_fail_at_active() {
    final int activeId = waitForActive(1);
    final int[] passives = waitForNPassives(1, 2);
    final int passiveId1 = passives[0];
    final int passiveId2 = passives[1];

    //create failover while committing
    String propertySettingString = "stripe.1.node." + activeId + ".tc-properties.failoverDeletion=killDeletion-commit";
    assertThat(configTool("set", "-s", "localhost:" + getNodePort(1, 1), "-c", propertySettingString), is(successful()));

    stopNode(1, passiveId1);

    //Both active and one passive is down.
    entityOperationTimeout = Duration.ofSeconds(5); // to not be stuck in failover
    assertThat(
        configTool("detach", "-f", "-d", "localhost:" + getNodePort(1, activeId), "-s", "localhost:" + getNodePort(1, passiveId1)),
        containsOutput("Two-Phase commit failed"));

    startNode(1, activeId, "-r", getNode(1, activeId).getConfigRepo());
    waitForActive(1, passiveId2);
    waitForPassive(1, activeId);

    withTopologyService(1, activeId, topologyService -> assertTrue(topologyService.isActivated()));
    withTopologyService(1, activeId, topologyService -> assertFalse(topologyService.hasIncompleteChange()));
    assertThat(getUpcomingCluster("localhost", getNodePort(1, activeId)).getNodeCount(), is(equalTo(2)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, activeId)).getNodeCount(), is(equalTo(2)));

    withTopologyService(1, passiveId2, topologyService -> assertTrue(topologyService.isActivated()));
    assertThat(getUpcomingCluster("localhost", getNodePort(1, passiveId2)).getNodeCount(), is(equalTo(2)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, passiveId2)).getNodeCount(), is(equalTo(2)));
  }
}
