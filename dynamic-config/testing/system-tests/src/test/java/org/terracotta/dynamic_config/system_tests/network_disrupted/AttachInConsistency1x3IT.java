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
package org.terracotta.dynamic_config.system_tests.network_disrupted;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.terracotta.angela.client.net.ClientToServerDisruptor;
import org.terracotta.angela.client.net.ServerToServerDisruptor;
import org.terracotta.angela.client.net.SplitCluster;
import org.terracotta.angela.client.support.junit.NodeOutputRule;
import org.terracotta.angela.common.tcconfig.ServerSymbolicName;
import org.terracotta.angela.common.tcconfig.TerracottaServer;
import org.terracotta.dynamic_config.api.model.FailoverPriority;
import org.terracotta.dynamic_config.test_support.ClusterDefinition;
import org.terracotta.dynamic_config.test_support.DynamicConfigIT;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.containsOutput;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.successful;

@ClusterDefinition(nodesPerStripe = 3, autoStart = false, netDisruptionEnabled = true)
public class AttachInConsistency1x3IT extends DynamicConfigIT {

  @Rule
  public final NodeOutputRule out = new NodeOutputRule();

  public AttachInConsistency1x3IT() {
    super(Duration.ofSeconds(300));
  }

  @Override
  protected FailoverPriority getFailoverPriority() {
    return FailoverPriority.consistency();
  }

  @Before
  public void setup() throws Exception {
    startNode(1, 1);
    waitForDiagnostic(1, 1);
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 1)).getNodeCount(), is(equalTo(1)));

    // start the second node
    startNode(1, 2);
    waitForDiagnostic(1, 2);
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 2)).getNodeCount(), is(equalTo(1)));

    setClientServerDisruptionLinks(Collections.singletonMap(1, 2));

    //attach the second node
    assertThat(configTool("attach", "-d", "localhost:" + getNodePort(1, 1), "-s", "localhost:" + getNodePort(1, 2)), is(successful()));

    setServerDisruptionLinks(Collections.singletonMap(1, 2));

    //Activate cluster
    activateCluster();
    waitForNPassives(1, 1);
  }

  @Test
  public void test_attach_when_active_passive_disrupted() throws Exception {
    startNode(1, 3);
    waitForDiagnostic(1, 3);
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 3)).getNodeCount(), is(equalTo(1)));

    TerracottaServer active = angela.tsa().getActive();
    TerracottaServer passive = angela.tsa().getPassive();
    SplitCluster split1 = new SplitCluster(active);
    SplitCluster split2 = new SplitCluster(passive);
    int activeId = findActive(1).getAsInt();
    int passiveId = findPassives(1)[0];
    //server to server disruption with active at one end and passives at other end.
    try (ServerToServerDisruptor disruptor = angela.tsa().disruptionController().newServerToServerDisruptor(split1, split2)) {

      //start partition
      disruptor.disrupt();

      //verify passive gets blocked
      waitForServerBlocked(passive);

      assertThat(
          configTool("attach", "-d", "localhost:" + getNodePort(1, activeId), "-s", "localhost:" + getNodePort(1, 3)),
          containsOutput("Please ensure all online nodes are either ACTIVE or PASSIVE before sending any update."));

      //stop partition
      disruptor.undisrupt();
      stopNode(1, passiveId);
      startNode(1, passiveId);
      waitForPassive(1, passiveId);
    }
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 1)).getNodeCount(), is(equalTo(2)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, 1)).getNodeCount(), is(equalTo(2)));

    assertThat(getUpcomingCluster("localhost", getNodePort(1, 2)).getNodeCount(), is(equalTo(2)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, 2)).getNodeCount(), is(equalTo(2)));

    assertThat(getUpcomingCluster("localhost", getNodePort(1, 3)).getNodeCount(), is(equalTo(1)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, 3)).getNodeCount(), is(equalTo(1)));

    withTopologyService(1, 1, topologyService -> assertTrue(topologyService.isActivated()));
    withTopologyService(1, 2, topologyService -> assertTrue(topologyService.isActivated()));
    withTopologyService(1, 3, topologyService -> assertFalse(topologyService.isActivated()));
  }

  @Test
  public void test_attach_when_active_passive_disrupted_client_can_see_active() throws Exception {
    startNode(1, 3);
    waitForDiagnostic(1, 3);
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 3)).getNodeCount(), is(equalTo(1)));

    TerracottaServer active = angela.tsa().getActive();
    TerracottaServer passive = angela.tsa().getPassive();
    SplitCluster split1 = new SplitCluster(active);
    SplitCluster split2 = new SplitCluster(passive);
    int activeId = findActive(1).getAsInt();
    int passiveId = findPassives(1)[0];
    Map<ServerSymbolicName, Integer> map = angela.tsa().updateToProxiedPorts();
    //server to server disruption with active at one end and passives at other end.
    try (ServerToServerDisruptor disruptor = angela.tsa().disruptionController().newServerToServerDisruptor(split1, split2)) {

      //start partition
      disruptor.disrupt();

      //verify passive gets blocked
      waitForServerBlocked(passive);

      try (ClientToServerDisruptor clientToServerDisruptor = angela.tsa().disruptionController().newClientToServerDisruptor()) {
        clientToServerDisruptor.disrupt(Collections.singletonList(passive.getServerSymbolicName()));
        String publicHostName = "stripe.1.node.1.public-hostname=localhost";
        String publicPort = "stripe.1.node.1.public-port=" + getNodePort(1, 3);
        assertThat(configTool("set", "-s", "localhost:" + getNodePort(1, 3), "-c",
            publicHostName, "-c", publicPort), is(successful()));
        int publicPortForActive = map.get(active.getServerSymbolicName());
        assertThat(configTool("attach", "-d", "localhost:" + publicPortForActive, "-s", "localhost:" + getNodePort(1, 3)),
            is(successful()));
        clientToServerDisruptor.undisrupt(Collections.singletonList(passive.getServerSymbolicName()));
      }
      //stop partition
      disruptor.undisrupt();
      stopNode(1, passiveId);
      startNode(1, passiveId);
      waitForPassive(1, passiveId);
    }
    assertThat(getUpcomingCluster("localhost", getNodePort(1, activeId)).getNodeCount(), is(equalTo(3)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, activeId)).getNodeCount(), is(equalTo(3)));

    assertThat(getUpcomingCluster("localhost", getNodePort(1, passiveId)).getNodeCount(), is(equalTo(3)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, passiveId)).getNodeCount(), is(equalTo(3)));

    assertThat(getUpcomingCluster("localhost", getNodePort(1, 3)).getNodeCount(), is(equalTo(3)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, 3)).getNodeCount(), is(equalTo(3)));

    withTopologyService(1, 1, topologyService -> assertTrue(topologyService.isActivated()));
    withTopologyService(1, 2, topologyService -> assertTrue(topologyService.isActivated()));
    withTopologyService(1, 3, topologyService -> assertTrue(topologyService.isActivated()));
  }
}

