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

import org.junit.Test;
import org.terracotta.dynamic_config.api.model.FailoverPriority;
import org.terracotta.dynamic_config.test_support.ClusterDefinition;
import org.terracotta.dynamic_config.test_support.DynamicConfigIT;

import java.time.Duration;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.successful;

@ClusterDefinition(nodesPerStripe = 2, autoActivate = true)
public class DetachInConsistency1x2IT extends DynamicConfigIT {

  public DetachInConsistency1x2IT() {
    super(Duration.ofSeconds(180));
  }

  @Override
  protected FailoverPriority getFailoverPriority() {
    return FailoverPriority.consistency();
  }

  @Test
  public void test_detach_active_node() throws Exception {
    final int activeId = findActive(1).getAsInt();
    final int passiveId = findPassives(1)[0];

    // Passive goes in passive_suspended state
    assertThat(
        () -> invokeConfigTool("detach", "-f", "-d", "localhost:" + getNodePort(1, passiveId), "-s", "localhost:" + getNodePort(1, activeId)),
        exceptionMatcher("Nomad system is currently not accessible."));

    waitUntil(() -> angela.tsa().getStopped().size(), is(1));
    withTopologyService(1, passiveId, topologyService -> assertTrue(topologyService.isActivated()));
    assertThat(getUpcomingCluster("localhost", getNodePort(1, passiveId)).getNodeCount(), is(equalTo(2)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, passiveId)).getNodeCount(), is(equalTo(2)));
  }

  @Test
  public void test_detach_passive_from_activated_cluster() throws Exception {
    final int activeId = findActive(1).getAsInt();
    final int passiveId = findPassives(1)[0];

    invokeConfigTool("detach", "-f", "-d", "localhost:" + getNodePort(1, activeId), "-s", "localhost:" + getNodePort(1, passiveId));

    waitUntil(() -> angela.tsa().getStopped().size(), is(1));
    withTopologyService(1, activeId, topologyService -> assertTrue(topologyService.isActivated()));
    assertTopologyChanged(activeId);
  }

  @Test
  public void test_detach_offline_node_in_consistency_mode() throws Exception {
    final int activeId = findActive(1).getAsInt();
    final int passiveId = findPassives(1)[0];

    stopNode(1, passiveId);

    invokeConfigTool("detach", "-d", "localhost:" + getNodePort(1, activeId), "-s", "localhost:" + getNodePort(1, passiveId));

    withTopologyService(1, activeId, topologyService -> assertTrue(topologyService.isActivated()));
    assertThat(getUpcomingCluster("localhost", getNodePort(1, activeId)).getNodeCount(), is(equalTo(1)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, activeId)).getNodeCount(), is(equalTo(1)));
  }

  @Test
  public void detachNodeFailInActiveAtPrepare() throws Exception {
    final int activeId = findActive(1).getAsInt();
    final int passiveId = findPassives(1)[0];

    String propertySettingString = "stripe.1.node." + activeId + ".tc-properties.detachStatus=prepareDeletion-failure";

    //create prepare failure on active
    assertThat(invokeConfigTool("set", "-s", "localhost:" + getNodePort(1, 1), "-c", propertySettingString), is(successful()));

    // detach failure (forcing detach otherwise we have to restart cluster)
    assertThat(
        () -> invokeConfigTool("detach", "-f", "-d", "localhost:" + getNodePort(1, activeId), "-s", "localhost:" + getNodePort(1, passiveId)),
        exceptionMatcher("Two-Phase commit failed"));

    waitUntil(() -> angela.tsa().getStopped().size(), is(1));

    assertThat(getUpcomingCluster("localhost", getNodePort(1, activeId)).getNodeCount(), is(equalTo(2)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, activeId)).getNodeCount(), is(equalTo(2)));
    withTopologyService(1, activeId, topologyService -> assertTrue(topologyService.isActivated()));
  }

  @Test
  public void testFailoverDuringNomadCommitForPassiveRemoval() throws Exception {
    final int activeId = findActive(1).getAsInt();
    final int passiveId = findPassives(1)[0];

    String propertySettingString = "stripe.1.node." + activeId + ".tc-properties.failoverDeletion=killDeletion-commit";

    //setup for failover in commit phase on active
    invokeConfigTool("set", "-s", "localhost:" + getNodePort(1, 1), "-c", propertySettingString);

    assertThat(
        () -> invokeConfigTool("-er", "40s", "detach", "-f", "-d", "localhost:" + getNodePort(1, activeId),
            "-s", "localhost:" + getNodePort(1, passiveId)),
        exceptionMatcher("Two-Phase commit failed"));

    // Stripe is lost no active
    waitUntil(() -> angela.tsa().getStopped().size(), is(2));

    // Can't become active 
    startNode(1, activeId, "-r", getNode(1, activeId).getConfigRepo());

    // Force the commit 
    invokeConfigTool("repair", "-f", "commit", "-s", "localhost:" + getNodePort(1, activeId));

    withTopologyService(1, activeId, topologyService -> assertTrue(topologyService.isActivated()));
    withTopologyService(1, activeId, topologyService -> assertFalse(topologyService.hasIncompleteChange()));
    assertThat(getUpcomingCluster("localhost", getNodePort(1, activeId)).getNodeCount(), is(equalTo(1)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, activeId)).getNodeCount(), is(equalTo(1)));
  }

  @Test
  public void test_detach_passive_offline_prepare_fail_at_active() throws Exception {
    final int activeId = findActive(1).getAsInt();
    final int passiveId = findPassives(1)[0];

    //create prepare failure on active
    String propertySettingString = "stripe.1.node." + activeId + ".tc-properties.detachStatus=prepareDeletion-failure";
    invokeConfigTool("set", "-s", "localhost:" + getNodePort(1, 1), "-c", propertySettingString);

    stopNode(1, passiveId);

    // detach failure (forcing detach otherwise we have to restart cluster)
    assertThat(
        () -> invokeConfigTool("detach", "-f", "-d", "localhost:" + getNodePort(1, activeId), "-s", "localhost:" + getNodePort(1, passiveId)),
        exceptionMatcher("Two-Phase commit failed"));

    withTopologyService(1, activeId, topologyService -> assertTrue(topologyService.isActivated()));
    assertThat(getUpcomingCluster("localhost", getNodePort(1, activeId)).getNodeCount(), is(equalTo(2)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, activeId)).getNodeCount(), is(equalTo(2)));
  }

  @Test
  public void test_detach_passive_offline_commit_fail_at_active() throws Exception {
    final int activeId = findActive(1).getAsInt();
    final int passiveId = findPassives(1)[0];

    //create failover while committing
    String propertySettingString = "stripe.1.node." + activeId + ".tc-properties.failoverDeletion=killDeletion-commit";
    invokeConfigTool("set", "-s", "localhost:" + getNodePort(1, 1), "-c", propertySettingString);

    stopNode(1, passiveId);

    //Both active and passive is down.
    assertThat(
        () -> invokeConfigTool("-er", "40s", "detach", "-f", "-d", "localhost:" + getNodePort(1, activeId), "-s", "localhost:" + getNodePort(1, passiveId)),
        exceptionMatcher("Two-Phase commit failed"));

    // can't become active
    startNode(1, activeId, "-r", getNode(1, activeId).getConfigRepo());

    // Force the commit 
    invokeConfigTool("repair", "-f", "commit", "-s", "localhost:" + getNodePort(1, activeId));

    withTopologyService(1, activeId, topologyService -> assertTrue(topologyService.isActivated()));
    withTopologyService(1, activeId, topologyService -> assertFalse(topologyService.hasIncompleteChange()));
    assertThat(getUpcomingCluster("localhost", getNodePort(1, activeId)).getNodeCount(), is(equalTo(1)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, activeId)).getNodeCount(), is(equalTo(1)));
  }

  private void assertTopologyChanged(int nodeId) throws Exception {
    withTopologyService(1, nodeId, topologyService -> assertTrue(topologyService.isActivated()));
    assertThat(getUpcomingCluster("localhost", getNodePort(1, nodeId)).getNodeCount(), is(equalTo(1)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, nodeId)).getNodeCount(), is(equalTo(1)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, nodeId)).getSingleNode().get().getPort().orDefault(), is(equalTo(getNodePort(1, nodeId))));
  }
}
