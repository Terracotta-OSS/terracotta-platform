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
import org.junit.Ignore;
import org.junit.Test;
import org.terracotta.dynamic_config.api.model.FailoverPriority;
import org.terracotta.dynamic_config.test_support.ClusterDefinition;
import org.terracotta.dynamic_config.test_support.DynamicConfigIT;
import org.terracotta.voter.ActiveVoter;

import java.time.Duration;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.terracotta.dynamic_config.test_support.angela.AngelaMatchers.successful;

@ClusterDefinition(nodesPerStripe = 3, autoActivate = false)
public class AttachCommandWithVoter1x3IT extends DynamicConfigIT {
  private static final long TOPOLOGY_FETCH_INTERVAL = 11000L;

  public AttachCommandWithVoter1x3IT() {
    super(Duration.ofSeconds(180));
    this.failoverPriority = FailoverPriority.consistency(1);
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
    assertThat(configToolInvocation("attach", "-d", "localhost:" + getNodePort(1, 1), "-s", "localhost:" + getNodePort(1, 2)), is(successful()));

    //Activate cluster
    activateCluster();
    waitForActive(1);
    waitForNPassives(1, 1);
  }

  @Test
  public void testAttachAndVerifyWithVoter() throws Exception {
    ActiveVoter activeVoter = null;
    try {
      int activeId = findActive(1).getAsInt();
      int passiveId = findPassives(1)[0];

      activeVoter = new ActiveVoter("mvoter", new CompletableFuture<>(), Optional.empty(), getNode(1, activeId).getHostPort(), getNode(1, passiveId).getHostPort());
      activeVoter.start();

      startNode(1, 3);
      waitForDiagnostic(1, 3);
      assertThat(getUpcomingCluster("localhost", getNodePort(1, 3)).getNodeCount(), is(equalTo(1)));

      assertThat(configToolInvocation("attach", "-d", "localhost:" + getNodePort(1, activeId), "-s", "localhost:" + getNodePort(1, 3)), is(successful()));

      Set<String> expectedRes = new HashSet<>();
      expectedRes.add(getNode(1, activeId).getHostPort());
      expectedRes.add(getNode(1, passiveId).getHostPort());
      expectedRes.add(getNode(1, 3).getHostPort());

      Thread.sleep(TOPOLOGY_FETCH_INTERVAL);
      assertThat(activeVoter.getExistingTopology(), is(expectedRes));
      assertThat(activeVoter.getHeartbeatFutures().size(), is(3));

      // kill the old passive and detach it from cluster
      stopNode(1, passiveId);
      assertThat(configToolInvocation("-t", "5s", "detach", "-d", "localhost:" + getNodePort(1, activeId), "-s", "localhost:" + getNodePort(1, passiveId)), is(successful()));
      expectedRes.remove(getNode(1, passiveId).getHostPort());

      Thread.sleep(TOPOLOGY_FETCH_INTERVAL);
      assertThat(activeVoter.getExistingTopology(), is(expectedRes));
      assertThat(activeVoter.getHeartbeatFutures().size(), is(2));

      withTopologyService(1, activeId, topologyService -> assertTrue(topologyService.isActivated()));
      withTopologyService(1, 3, topologyService -> assertTrue(topologyService.isActivated()));
    } finally {
      activeVoter.stop();
    }
  }

  @Ignore("TDB-4949")
  @Test
  public void testAttachAfterKillingActive() throws Exception {
    ActiveVoter activeVoter = null;
    try {
      int activeId = findActive(1).getAsInt();
      int passiveId = findPassives(1)[0];

      activeVoter = new ActiveVoter("mvoter", new CompletableFuture<>(), Optional.empty(), getNode(1, activeId).getHostPort(), getNode(1, passiveId).getHostPort());
      activeVoter.start();

      startNode(1, 3);
      waitForDiagnostic(1, 3);
      assertThat(getUpcomingCluster("localhost", getNodePort(1, 3)).getNodeCount(), is(equalTo(1)));

      //Kill active so other passive becomes active by the vote of voter
      stopNode(1, activeId);
      waitForActive(1, passiveId);

      // Not able to connect to the new active 
      // Perhaps because of two concurrent diagnostic connection one from voter for heartbeating and other from config tool
      // Investigate ?
      assertThat(configToolInvocation("attach", "-d", "localhost:" + getNodePort(1, passiveId), "-s", "localhost:" + getNodePort(1, 3)), is(successful()));

      Set<String> expectedRes = new HashSet<>();
      expectedRes.add(getNode(1, activeId).getHostPort());
      expectedRes.add(getNode(1, passiveId).getHostPort());
      expectedRes.add(getNode(1, 3).getHostPort());

      Thread.sleep(TOPOLOGY_FETCH_INTERVAL);
      assertThat(activeVoter.getExistingTopology(), is(expectedRes));
      assertThat(activeVoter.getHeartbeatFutures().size(), is(3));
    } finally {
      activeVoter.stop();
    }
  }

  @Test
  public void testStalePassivePortsRemovedFromVoterTopology() throws Exception {
    ActiveVoter activeVoter = null;
    try {
      int activeId = findActive(1).getAsInt();
      int passiveId = findPassives(1)[0];

      // Addding some dummy passive hostPorts to simulate as stale passive hostPorts
      activeVoter = new ActiveVoter("mvoter", new CompletableFuture<>(), Optional.empty(), getNode(1, activeId).getHostPort(), "localhost:123", "locahost:235");
      activeVoter.start();

      startNode(1, 3);
      waitForDiagnostic(1, 3);
      assertThat(getUpcomingCluster("localhost", getNodePort(1, 3)).getNodeCount(), is(equalTo(1)));

      assertThat(configToolInvocation("attach", "-d", "localhost:" + getNodePort(1, 1), "-s", "localhost:" + getNodePort(1, 3)), is(successful()));

      Set<String> expectedRes = new HashSet<>();
      expectedRes.add(getNode(1, activeId).getHostPort());
      expectedRes.add(getNode(1, passiveId).getHostPort());
      expectedRes.add(getNode(1, 3).getHostPort());

      Thread.sleep(TOPOLOGY_FETCH_INTERVAL);
      assertThat(activeVoter.getExistingTopology(), is(expectedRes));
      assertThat(activeVoter.getHeartbeatFutures().size(), is(3));

      withTopologyService(1, activeId, topologyService -> assertTrue(topologyService.isActivated()));
      withTopologyService(1, passiveId, topologyService -> assertTrue(topologyService.isActivated()));
      withTopologyService(1, 3, topologyService -> assertTrue(topologyService.isActivated()));
    } finally {
      activeVoter.stop();
    }
  }
}
