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
import org.terracotta.dynamic_config.test_support.ClusterDefinition;
import org.terracotta.dynamic_config.test_support.DynamicConfigIT;

import java.time.Duration;

import static org.hamcrest.Matchers.allOf;
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

  public DetachCommand1x2IT() {
    super(Duration.ofSeconds(180));
  }

  @Test
  public void test_force_detach_active_node() throws Exception {
    final int activeId = findActive(1).getAsInt();
    final int passiveId = findPassives(1)[0];

    assertThat(configToolInvocation("detach", "-f", "-d", "localhost:" + getNodePort(1, passiveId), "-s", "localhost:" + getNodePort(1, activeId)), is(successful()));

    // failover - existing passive becomes active
    waitForActive(1, passiveId);
    withTopologyService(1, passiveId, topologyService -> assertTrue(topologyService.isActivated()));

    waitUntil(() -> angela.tsa().getStopped().size(), is(1));
    assertTopologyChanged(passiveId);
  }

  @Test
  public void test_force_detach_passive_from_activated_cluster() throws Exception {
    final int activeId = findActive(1).getAsInt();
    final int passiveId = findPassives(1)[0];

    assertThat(configToolInvocation("detach", "-f", "-d", "localhost:" + getNodePort(1, activeId), "-s", "localhost:" + getNodePort(1, passiveId)), is(successful()));
    waitUntil(() -> angela.tsa().getStopped().size(), is(1));
    assertTopologyChanged(activeId);
  }

  @Test
  public void test_detach_passive_from_activated_cluster_requiring_restart() throws Exception {
    final int activeId = findActive(1).getAsInt();
    final int passiveId = findPassives(1)[0];

    // do a change requiring a restart
    assertThat(configToolInvocation("set", "-s", "localhost:" + getNodePort(1, activeId), "-c", "stripe.1.node.1.tc-properties.foo=bar"),
        allOf(is(successful()), containsOutput("IMPORTANT: A restart of the cluster is required to apply the changes")));

    stopNode(1, passiveId);

    // try to detach this node
    assertThat(
        configToolInvocation("detach", "-d", "localhost:" + getNodePort(1, activeId), "-s", "localhost:" + getNodePort(1, passiveId)),
        containsOutput("Impossible to do any topology change. Cluster at address: " + "localhost:" + getNodePort(1, activeId) +
            " is waiting to be restarted to apply some pending changes. " +
            "You can run the command with -f option to force the comment but at the risk of breaking this cluster configuration consistency. " +
            "The newly added node will be restarted, but not the existing ones."));

    // try forcing the detach
    assertThat(configToolInvocation("detach", "-f", "-d", "localhost:" + getNodePort(1, activeId), "-s", "localhost:" + getNodePort(1, passiveId)), is(successful()));

    assertTopologyChanged(activeId);
  }

  @Test
  public void test_detach_online_node_in_availability_mode() throws Exception {
    final int activeId = findActive(1).getAsInt();
    final int passiveId = findPassives(1)[0];

    // detaching an online node needs to be forced
    assertThat(
        configToolInvocation("detach", "-d", "localhost:" + getNodePort(1, activeId), "-s", "localhost:" + getNodePort(1, passiveId)),
        containsOutput("Safely shutdown the nodes first"));

    configToolInvocation("detach", "-d", "localhost:" + getNodePort(1, activeId), "-s", "localhost:" + getNodePort(1, passiveId), "-f");

    waitUntil(() -> angela.tsa().getStopped().size(), is(1));
    assertTopologyChanged(activeId);
  }

  @Test
  public void detachNodeFailInActiveAtPrepare() throws Exception {
    final int activeId = findActive(1).getAsInt();
    final int passiveId = findPassives(1)[0];

    String propertySettingString = "stripe.1.node." + activeId + ".tc-properties.detachStatus=prepareDeletion-failure";

    //create prepare failure on active
    assertThat(configToolInvocation("set", "-s", "localhost:" + getNodePort(1, 1), "-c", propertySettingString), is(successful()));

    // detach failure (forcing detach otherwise we have to restart cluster)
    assertThat(
        configToolInvocation("detach", "-f", "-d", "localhost:" + getNodePort(1, activeId),
            "-s", "localhost:" + getNodePort(1, passiveId)),
        containsOutput("Two-Phase commit failed"));

    waitUntil(() -> angela.tsa().getStopped().size(), is(1));

    // Nomad rollback happened
    // we end up with a cluster of 2 nodes with 1 of them removed
    withTopologyService(1, activeId, topologyService -> assertTrue(topologyService.isActivated()));
    assertThat(getUpcomingCluster("localhost", getNodePort(1, activeId)).getNodeCount(), is(equalTo(2)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, activeId)).getNodeCount(), is(equalTo(2)));
  }

  @Test
  public void testFailoverDuringNomadCommitForPassiveRemoval() throws Exception {
    final int activeId = findActive(1).getAsInt();
    final int passiveId = findPassives(1)[0];

    String propertySettingString = "stripe.1.node." + activeId + ".tc-properties.failoverDeletion=killDeletion-commit";

    //setup for failover in commit phase on active
    assertThat(configToolInvocation("set", "-s", "localhost:" + getNodePort(1, 1), "-c", propertySettingString), is(successful()));

    // Stripe is lost no active
    assertThat(
        configToolInvocation("-er", "40s", "detach", "-f", "-d", "localhost:" + getNodePort(1, activeId),
            "-s", "localhost:" + getNodePort(1, passiveId)),
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
    final int activeId = findActive(1).getAsInt();
    final int passiveId = findPassives(1)[0];

    //create prepare failure on active
    String propertySettingString = "stripe.1.node." + activeId + ".tc-properties.detachStatus=prepareDeletion-failure";
    assertThat(configToolInvocation("set", "-s", "localhost:" + getNodePort(1, 1), "-c", propertySettingString), is(successful()));

    // detach failure (forcing detach otherwise we have to restart cluster)
    assertThat(
        configToolInvocation("detach", "-f", "-d", "localhost:" + getNodePort(1, activeId),
            "-s", "localhost:" + getNodePort(1, passiveId)),
        containsOutput("Two-Phase commit failed"));

    withTopologyService(1, activeId, topologyService -> assertTrue(topologyService.isActivated()));
    withTopologyService(1, activeId, topologyService -> assertFalse(topologyService.hasIncompleteChange()));
    assertThat(getUpcomingCluster("localhost", getNodePort(1, activeId)).getNodeCount(), is(equalTo(2)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, activeId)).getNodeCount(), is(equalTo(2)));
  }

  @Test
  public void test_detach_passive_commit_fail_at_active() throws Exception {
    final int activeId = findActive(1).getAsInt();
    final int passiveId = findPassives(1)[0];

    //create failover while committing
    String propertySettingString = "stripe.1.node." + activeId + ".tc-properties.failoverDeletion=killDeletion-commit";
    assertThat(configToolInvocation("set", "-s", "localhost:" + getNodePort(1, 1), "-c", propertySettingString), is(successful()));

    //Both active and passive are down.
    assertThat(
        configToolInvocation("-er", "40s", "detach", "-f", "-d", "localhost:" + getNodePort(1, activeId),
            "-s", "localhost:" + getNodePort(1, passiveId)),
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
    assertThat(getRuntimeCluster("localhost", getNodePort(1, nodeId)).getSingleNode().get().getNodePort(), is(equalTo(getNodePort(1, nodeId))));
  }
}
