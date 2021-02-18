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

import org.junit.Before;
import org.junit.Test;
import org.terracotta.dynamic_config.api.model.FailoverPriority;
import org.terracotta.dynamic_config.test_support.ClusterDefinition;
import org.terracotta.dynamic_config.test_support.DynamicConfigIT;
import org.terracotta.voter.ActiveVoter;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.successful;

@ClusterDefinition(nodesPerStripe = 3)
public class AttachCommandWithVoter1x3IT extends DynamicConfigIT {

  public AttachCommandWithVoter1x3IT() {
    super(Duration.ofSeconds(180));
  }

  @Override
  protected FailoverPriority getFailoverPriority() {
    return FailoverPriority.consistency(1);
  }

  @Before
  public void setUp() throws Exception {
    startNode(1, 1);
    waitForDiagnostic(1, 1);
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 1)).getNodeCount(), is(equalTo(1)));

    // start the second node
    startNode(1, 2);
    waitForDiagnostic(1, 2);
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 2)).getNodeCount(), is(equalTo(1)));

    //attach the second node
    assertThat(configTool("attach", "-d", "localhost:" + getNodePort(1, 1), "-s", "localhost:" + getNodePort(1, 2)), is(successful()));

    //Activate cluster
    activateCluster();
    waitForActive(1);
    waitForNPassives(1, 1);
  }

  @Test
  public void testAttachAndVerifyWithVoter() throws Exception {
    int activeId = findActive(1).getAsInt();
    int passiveId = findPassives(1)[0];

    try (ActiveVoter activeVoter = new ActiveVoter("mvoter", new CompletableFuture<>(), Optional.empty(), getNode(1, activeId).getHostPort(), getNode(1, passiveId).getHostPort())) {
      activeVoter.start();

      startNode(1, 3);
      waitForDiagnostic(1, 3);
      assertThat(getUpcomingCluster("localhost", getNodePort(1, 3)).getNodeCount(), is(equalTo(1)));

      assertThat(configTool("attach", "-d", "localhost:" + getNodePort(1, activeId), "-s", "localhost:" + getNodePort(1, 3)), is(successful()));

      String[] nodes = new String[]{
          getNode(1, activeId).getHostPort(),
          getNode(1, passiveId).getHostPort(),
          getNode(1, 3).getHostPort()};

      waitUntil(activeVoter::getExistingTopology, containsInAnyOrder(nodes));
      waitUntil(() -> activeVoter.getHeartbeatFutures().size(), is(3));

      // kill the old passive and detach it from cluster
      stopNode(1, passiveId);
      assertThat(configTool("detach", "-d", "localhost:" + getNodePort(1, activeId), "-s", "localhost:" + getNodePort(1, passiveId)), is(successful()));

      nodes = new String[]{
          getNode(1, activeId).getHostPort(),
          getNode(1, 3).getHostPort()};

      waitUntil(activeVoter::getExistingTopology, containsInAnyOrder(nodes));
      waitUntil(() -> activeVoter.getHeartbeatFutures().size(), is(2));

      withTopologyService(1, activeId, topologyService -> assertTrue(topologyService.isActivated()));
      withTopologyService(1, 3, topologyService -> assertTrue(topologyService.isActivated()));
    }
  }

  @Test
  public void testAttachAfterKillingActive() throws Exception {
    int activeId = findActive(1).getAsInt();
    int passiveId = findPassives(1)[0];

    try (ActiveVoter activeVoter = new ActiveVoter("mvoter", new CompletableFuture<>(), Optional.empty(), getNode(1, activeId).getHostPort(), getNode(1, passiveId).getHostPort())) {
      activeVoter.start();

      startNode(1, 3);
      waitForDiagnostic(1, 3);
      assertThat(getUpcomingCluster("localhost", getNodePort(1, 3)).getNodeCount(), is(equalTo(1)));

      //Kill active so other passive becomes active by the vote of voter
      stopNode(1, activeId);
      waitForActive(1, passiveId);

      assertThat(configTool("attach", "-d", "localhost:" + getNodePort(1, passiveId), "-s", "localhost:" + getNodePort(1, 3)), is(successful()));

      String[] nodes = new String[]{
          getNode(1, activeId).getHostPort(),
          getNode(1, passiveId).getHostPort(),
          getNode(1, 3).getHostPort()};

      waitUntil(activeVoter::getExistingTopology, containsInAnyOrder(nodes));
      waitUntil(() -> activeVoter.getHeartbeatFutures().size(), is(3));
    }
  }

  @Test
  public void testStalePassivePortsRemovedFromVoterTopology() throws Exception {
    int activeId = findActive(1).getAsInt();
    int passiveId = findPassives(1)[0];

    try (ActiveVoter activeVoter = new ActiveVoter("mvoter", new CompletableFuture<>(), Optional.empty(), getNode(1, activeId).getHostPort(), "localhost:123", "locahost:235")) {
      // Addding some dummy passive hostPorts to simulate as stale passive hostPorts
      activeVoter.start();

      startNode(1, 3);
      waitForDiagnostic(1, 3);
      assertThat(getUpcomingCluster("localhost", getNodePort(1, 3)).getNodeCount(), is(equalTo(1)));

      assertThat(configTool("attach", "-d", "localhost:" + getNodePort(1, 1), "-s", "localhost:" + getNodePort(1, 3)), is(successful()));

      String[] nodes = new String[]{
          getNode(1, activeId).getHostPort(),
          getNode(1, passiveId).getHostPort(),
          getNode(1, 3).getHostPort()};

      waitUntil(activeVoter::getExistingTopology, containsInAnyOrder(nodes));
      waitUntil(() -> activeVoter.getHeartbeatFutures().size(), is(3));

      withTopologyService(1, activeId, topologyService -> assertTrue(topologyService.isActivated()));
      withTopologyService(1, passiveId, topologyService -> assertTrue(topologyService.isActivated()));
      withTopologyService(1, 3, topologyService -> assertTrue(topologyService.isActivated()));
    }
  }
}
