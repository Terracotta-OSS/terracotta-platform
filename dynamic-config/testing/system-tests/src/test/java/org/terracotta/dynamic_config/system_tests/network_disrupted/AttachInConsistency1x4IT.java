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
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.containsOutput;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.successful;

@ClusterDefinition(nodesPerStripe = 4, autoStart = false, netDisruptionEnabled = true)
public class AttachInConsistency1x4IT extends DynamicConfigIT {
  @Rule
  public final NodeOutputRule out = new NodeOutputRule();

  public AttachInConsistency1x4IT() {
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

    // start the third node
    startNode(1, 3);
    waitForDiagnostic(1, 3);
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
  public void test_attach_when_active_passives_disrupted() throws Exception {
    startNode(1, 4);
    waitForDiagnostic(1, 4);
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 4)).getNodeCount(), is(equalTo(1)));

    TerracottaServer active = angela.tsa().getActive();
    Collection<TerracottaServer> passives = angela.tsa().getPassives();
    Iterator<TerracottaServer> iterator = passives.iterator();
    TerracottaServer passive1 = iterator.next();
    TerracottaServer passive2 = iterator.next();
    SplitCluster split1 = new SplitCluster(active);
    SplitCluster split2 = new SplitCluster(passives);
    int activeId = findActive(1).getAsInt();
    int passiveId = findPassives(1)[0];
    //server to server disruption with active at one end and passives at other end.
    try (ServerToServerDisruptor disruptor = angela.tsa().disruptionController().newServerToServerDisruptor(split1, split2)) {

      //start partition
      disruptor.disrupt();

      waitForServerBlocked(active);
      TerracottaServer newActive = isActive(passive1, passive2);
      assertThat(newActive, is(notNullValue()));

      assertThat(
          configTool("attach", "-d", "localhost:" + getNodePort(1, passiveId), "-s", "localhost:" + getNodePort(1, 4)),
          containsOutput("Please ensure all online nodes are either ACTIVE or PASSIVE before sending any update."));

      //stop partition
      disruptor.undisrupt();
      waitForPassive(1, activeId);
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
    waitForDiagnostic(1, 4);
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 4)).getNodeCount(), is(equalTo(1)));

    TerracottaServer active = angela.tsa().getActive();
    Collection<TerracottaServer> passives = angela.tsa().getPassives();
    SplitCluster split1 = new SplitCluster(active);
    SplitCluster split2 = new SplitCluster(passives);
    int activeId = findActive(1).getAsInt();
    int passiveId = findPassives(1)[0];
    int passiveId2 = findPassives(1)[1];
    Map<ServerSymbolicName, Integer> map = angela.tsa().updateToProxiedPorts();
    TerracottaServer passive = passives.iterator().next();
    //server to server disruption with active at one end and passives at other end.
    try (ServerToServerDisruptor disruptor = angela.tsa().disruptionController().newServerToServerDisruptor(split1, split2)) {

      //start partition
      disruptor.disrupt();

      waitForServerBlocked(active);
      Thread.sleep(5000);
      try (ClientToServerDisruptor clientToServerDisruptor = angela.tsa().disruptionController().newClientToServerDisruptor()) {
        clientToServerDisruptor.disrupt(Collections.singletonList(active.getServerSymbolicName()));
        String publicHostName = "stripe.1.node.1.public-hostname=localhost";
        String publicPort = "stripe.1.node.1.public-port=" + getNodePort(1, 4);
        assertThat(configTool("set", "-s", "localhost:" + getNodePort(1, 4), "-c",
            publicHostName, "-c", publicPort), is(successful()));
        int publicPassivePort = map.get(passive.getServerSymbolicName());
        assertThat(configTool("attach", "-d", "localhost:" + publicPassivePort, "-s", "localhost:" + getNodePort(1, 4)),
            is(successful()));
        clientToServerDisruptor.undisrupt(Collections.singletonList(active.getServerSymbolicName()));
      }
      disruptor.undisrupt();
      waitForPassive(1, activeId);
    }
    assertThat(getUpcomingCluster("localhost", getNodePort(1, activeId)).getNodeCount(), is(equalTo(4)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, activeId)).getNodeCount(), is(equalTo(4)));

    assertThat(getUpcomingCluster("localhost", getNodePort(1, passiveId)).getNodeCount(), is(equalTo(4)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, passiveId)).getNodeCount(), is(equalTo(4)));

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

