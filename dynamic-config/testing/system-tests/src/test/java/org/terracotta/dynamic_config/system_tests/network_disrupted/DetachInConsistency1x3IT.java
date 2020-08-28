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
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.successful;

@ClusterDefinition(nodesPerStripe = 3, autoStart = false, netDisruptionEnabled = true)
public class DetachInConsistency1x3IT extends DynamicConfigIT {
  @Rule
  public final NodeOutputRule out = new NodeOutputRule();

  public DetachInConsistency1x3IT() {
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

    assertThat(invokeConfigTool("attach", "-d", "localhost:" + getNodePort(1, 1), "-s", "localhost:" + getNodePort(1, 2)), is(successful()));
    assertThat(invokeConfigTool("attach", "-d", "localhost:" + getNodePort(1, 1), "-s", "localhost:" + getNodePort(1, 3)), is(successful()));

    setServerDisruptionLinks(Collections.singletonMap(1, 3));
    activateCluster();
    waitForNPassives(1, 2);
  }

  @Test
  public void test_detach_when_active_passive_disrupted() throws Exception {
    TerracottaServer active = angela.tsa().getActive();
    Collection<TerracottaServer> passives = angela.tsa().getPassives();
    Iterator<TerracottaServer> iterator = passives.iterator();
    TerracottaServer passive1 = iterator.next();
    TerracottaServer passive2 = iterator.next();
    SplitCluster split1 = new SplitCluster(active);
    SplitCluster split2 = new SplitCluster(passives);
    int oldActiveId = findActive(1).getAsInt();
    int passiveId1 = findPassives(1)[0];
    int passiveId2 = findPassives(1)[1];
    int newActiveId;
    try (ServerToServerDisruptor disruptor = angela.tsa().disruptionController().newServerToServerDisruptor(split1, split2)) {

      //start partition
      disruptor.disrupt();

      //verify active gets blocked
      waitForServerBlocked(active);

      TerracottaServer newActive = isActive(passive1, passive2);
      assertThat(newActive, is(notNullValue()));
      if (newActive == passive1) {
        newActiveId = passiveId1;
      } else {
        newActiveId = passiveId2;
      }

      assertThat(
          () -> invokeConfigTool("detach", "-f", "-d", "localhost:" + getNodePort(1, oldActiveId), "-s", "localhost:" + getNodePort(1, newActiveId)),
          exceptionMatcher("Please ensure all online nodes are either ACTIVE or PASSIVE before sending any update."));

      //stop partition
      disruptor.undisrupt();
      stopNode(1, oldActiveId);
      startNode(1, oldActiveId);
      waitForPassive(1, oldActiveId);
    }
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 1)).getNodeCount(), is(equalTo(3)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, 1)).getNodeCount(), is(equalTo(3)));

    assertThat(getUpcomingCluster("localhost", getNodePort(1, 2)).getNodeCount(), is(equalTo(3)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, 2)).getNodeCount(), is(equalTo(3)));

    assertThat(getUpcomingCluster("localhost", getNodePort(1, 3)).getNodeCount(), is(equalTo(3)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, 3)).getNodeCount(), is(equalTo(3)));

    withTopologyService(1, 1, topologyService -> assertTrue(topologyService.isActivated()));
    withTopologyService(1, 2, topologyService -> assertTrue(topologyService.isActivated()));
    withTopologyService(1, 3, topologyService -> assertTrue(topologyService.isActivated()));
  }

  @Test
  public void test_detach_when_active_client_and_passive_disrupted() throws Exception {
    TerracottaServer active = angela.tsa().getActive();
    Collection<TerracottaServer> passives = angela.tsa().getPassives();
    Iterator<TerracottaServer> iterator = passives.iterator();
    TerracottaServer passive1 = iterator.next();
    TerracottaServer passive2 = iterator.next();
    SplitCluster split1 = new SplitCluster(active);
    SplitCluster split2 = new SplitCluster(passives);
    Map<ServerSymbolicName, Integer> map = angela.tsa().updateToProxiedPorts();
    int oldActiveId = findActive(1).getAsInt();
    int passiveId1 = findPassives(1)[0];
    int passiveId2 = findPassives(1)[1];
    int newActiveId;
    try (ServerToServerDisruptor disruptor = angela.tsa().disruptionController().newServerToServerDisruptor(split1, split2)) {

      //start partition
      disruptor.disrupt();

      //verify active gets blocked
      waitForServerBlocked(active);

      TerracottaServer newActive = isActive(passive1, passive2);
      TerracottaServer newPassive;
      assertThat(newActive, is(notNullValue()));
      if (newActive == passive1) {
        newActiveId = passiveId1;
        newPassive = passive2;
      } else {
        newActiveId = passiveId2;
        newPassive = passive1;
      }
      try (ClientToServerDisruptor clientToServerDisruptor = angela.tsa().disruptionController().newClientToServerDisruptor()) {
        clientToServerDisruptor.disrupt(Collections.singletonList(active.getServerSymbolicName()));
        int newActivePublicPort = map.get(newActive.getServerSymbolicName());
        int newPassivePublicPort = map.get(newPassive.getServerSymbolicName());
        assertThat(() -> invokeConfigTool("detach", "-f", "-d", "localhost:" + newActivePublicPort, "-s", "localhost:" + newPassivePublicPort),
            exceptionMatcher("Nomad system is currently not accessible."));
        clientToServerDisruptor.undisrupt(Collections.singletonList(active.getServerSymbolicName()));
      }
      //stop partition
      disruptor.undisrupt();
      stopNode(1, oldActiveId);
      startNode(1, oldActiveId);
      waitForPassive(1, oldActiveId);
    }
    assertThat(getUpcomingCluster("localhost", getNodePort(1, oldActiveId)).getNodeCount(), is(equalTo(3)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, oldActiveId)).getNodeCount(), is(equalTo(3)));

    assertThat(getUpcomingCluster("localhost", getNodePort(1, newActiveId)).getNodeCount(), is(equalTo(3)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, newActiveId)).getNodeCount(), is(equalTo(3)));

    withTopologyService(1, oldActiveId, topologyService -> assertTrue(topologyService.isActivated()));
    withTopologyService(1, newActiveId, topologyService -> assertTrue(topologyService.isActivated()));
  }
}

