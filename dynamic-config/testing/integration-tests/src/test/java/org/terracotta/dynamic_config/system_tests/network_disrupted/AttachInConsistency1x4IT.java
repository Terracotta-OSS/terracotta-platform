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
package org.terracotta.dynamic_config.system_tests.network_disrupted;

import org.junit.Before;
import org.junit.Test;
import org.terracotta.angela.client.net.ClientToServerDisruptor;
import org.terracotta.angela.client.net.ServerToServerDisruptor;
import org.terracotta.angela.client.net.SplitCluster;
import org.terracotta.angela.common.tcconfig.ServerSymbolicName;
import org.terracotta.angela.common.tcconfig.TerracottaServer;
import org.terracotta.diagnostic.client.DiagnosticService;
import org.terracotta.diagnostic.model.LogicalServerState;
import org.terracotta.dynamic_config.api.model.FailoverPriority;
import org.terracotta.dynamic_config.test_support.ClusterDefinition;
import org.terracotta.dynamic_config.test_support.DynamicConfigIT;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.containsOutput;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.successful;

@ClusterDefinition(nodesPerStripe = 4, autoStart = false, netDisruptionEnabled = true)
public class AttachInConsistency1x4IT extends DynamicConfigIT {

  @Override
  protected FailoverPriority getFailoverPriority() {
    return FailoverPriority.consistency();
  }

  @Before
  public void setup() throws Exception {
    startNode(1, 1);
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 1)).getNodeCount(), is(equalTo(1)));

    // start the second node
    startNode(1, 2);
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 2)).getNodeCount(), is(equalTo(1)));

    // start the third node
    startNode(1, 3);
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 3)).getNodeCount(), is(equalTo(1)));

    setClientServerDisruptionLinks(Collections.singletonMap(1, 3));

    //attach the second node
    assertThat(configTool("attach", "-d", "localhost:" + getNodePort(1, 1), "-s", "localhost:" + getNodePort(1, 2)), is(successful()));
    //attach the third node
    assertThat(configTool("attach", "-d", "localhost:" + getNodePort(1, 1), "-s", "localhost:" + getNodePort(1, 3)), is(successful()));

    setServerDisruptionLinks(Collections.singletonMap(1, 3));
    //Activate cluster
    activateCluster();
    waitForNPassives(1, 2);
  }

  @Test
  public void test_attach_when_active_passives_disrupted() {
    startNode(1, 4);
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 4)).getNodeCount(), is(equalTo(1)));

    TerracottaServer active = angela.tsa().getActive();
    Collection<TerracottaServer> passives = angela.tsa().getPassives();
    Iterator<TerracottaServer> iterator = passives.iterator();
    TerracottaServer passive1 = iterator.next();
    TerracottaServer passive2 = iterator.next();
    SplitCluster split1 = new SplitCluster(active);
    SplitCluster split2 = new SplitCluster(passives);
    int activeId = waitForActive(1);
    int passiveId = waitForNPassives(1, 1)[0];
    //server to server disruption with active at one end and passives at other end.
    try (ServerToServerDisruptor disruptor = angela.tsa().disruptionController().newServerToServerDisruptor(split1, split2)) {

      //start partition
      disruptor.disrupt();

      waitForServerBlocked(active);
      waitForNewActive(passive1, passive2);

      assertThat(
          configTool("attach", "-d", "localhost:" + getNodePort(1, passiveId), "-s", "localhost:" + getNodePort(1, 4)),
          containsOutput("Please ensure all online nodes are either ACTIVE or PASSIVE before sending any update."));

      //stop partition
      disruptor.undisrupt();

      // waitForPassive is not working correctly
      // we can see in the logs that the active has shutdown because its weight is lower than the other active
      // it restarts and become passive
      // but the angela assertion still detects it as an active server
      //waitForPassive(1, activeId);
      waitUntil(() -> usingDiagnosticService(1, activeId, DiagnosticService::getLogicalServerState), is(LogicalServerState.PASSIVE));
    }
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 1)).getNodeCount(), is(equalTo(3)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, 1)).getNodeCount(), is(equalTo(3)));

    assertThat(getUpcomingCluster("localhost", getNodePort(1, 2)).getNodeCount(), is(equalTo(3)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, 2)).getNodeCount(), is(equalTo(3)));

    assertThat(getUpcomingCluster("localhost", getNodePort(1, 3)).getNodeCount(), is(equalTo(3)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, 3)).getNodeCount(), is(equalTo(3)));

    assertThat(getUpcomingCluster("localhost", getNodePort(1, 4)).getNodeCount(), is(equalTo(1)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, 4)).getNodeCount(), is(equalTo(1)));

    withTopologyService(1, 1, topologyService -> assertTrue(topologyService.isActivated()));
    withTopologyService(1, 2, topologyService -> assertTrue(topologyService.isActivated()));
    withTopologyService(1, 3, topologyService -> assertTrue(topologyService.isActivated()));
    withTopologyService(1, 4, topologyService -> assertFalse(topologyService.isActivated()));
  }

  @Test
  public void test_attach_when_active_client_and_passives_disrupted() throws Exception {
    startNode(1, 4);
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 4)).getNodeCount(), is(equalTo(1)));

    TerracottaServer active = angela.tsa().getActive();
    Collection<TerracottaServer> passives = angela.tsa().getPassives();
    SplitCluster split1 = new SplitCluster(active);
    SplitCluster split2 = new SplitCluster(passives);
    int activeId = waitForActive(1);
    final int[] pp = waitForNPassives(1, 2);
    final int passiveId1 = pp[0];
    final int passiveId2 = pp[1];
    Map<ServerSymbolicName, Integer> map = angela.tsa().updateToProxiedPorts();
    TerracottaServer passive = passives.iterator().next();
    //server to server disruption with active at one end and passives at other end.
    try (ServerToServerDisruptor disruptor = angela.tsa().disruptionController().newServerToServerDisruptor(split1, split2)) {

      //start partition
      disruptor.disrupt();

      waitForServerBlocked(active);
      // attach command expects to see an active so wait for it
      waitForActive(1);

      try (ClientToServerDisruptor clientToServerDisruptor = angela.tsa().disruptionController().newClientToServerDisruptor()) {
        clientToServerDisruptor.disrupt(Collections.singletonList(active.getServerSymbolicName()));
        String publicHostName = "stripe.1.node.1.public-hostname=localhost";
        String publicPort = "stripe.1.node.1.public-port=" + getNodePort(1, 4);
        assertThat(configTool("set", "-s", "localhost:" + getNodePort(1, 4), "-c", publicHostName, "-c", publicPort), is(successful()));
        int publicPassivePort = map.get(passive.getServerSymbolicName());
        assertThat(configTool("attach", "-d", "localhost:" + publicPassivePort, "-s", "localhost:" + getNodePort(1, 4)), is(successful()));
        clientToServerDisruptor.undisrupt(Collections.singletonList(active.getServerSymbolicName()));
      }
      disruptor.undisrupt();
      waitForPassive(1, activeId);
    }
    assertThat(getUpcomingCluster("localhost", getNodePort(1, activeId)).getNodeCount(), is(equalTo(4)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, activeId)).getNodeCount(), is(equalTo(4)));

    assertThat(getUpcomingCluster("localhost", getNodePort(1, passiveId1)).getNodeCount(), is(equalTo(4)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, passiveId1)).getNodeCount(), is(equalTo(4)));

    assertThat(getUpcomingCluster("localhost", getNodePort(1, passiveId2)).getNodeCount(), is(equalTo(4)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, passiveId2)).getNodeCount(), is(equalTo(4)));

    assertThat(getUpcomingCluster("localhost", getNodePort(1, 4)).getNodeCount(), is(equalTo(4)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, 4)).getNodeCount(), is(equalTo(4)));

    withTopologyService(1, 1, topologyService -> assertTrue(topologyService.isActivated()));
    withTopologyService(1, 2, topologyService -> assertTrue(topologyService.isActivated()));
    withTopologyService(1, 3, topologyService -> assertTrue(topologyService.isActivated()));
    withTopologyService(1, 4, topologyService -> assertTrue(topologyService.isActivated()));
  }
}

