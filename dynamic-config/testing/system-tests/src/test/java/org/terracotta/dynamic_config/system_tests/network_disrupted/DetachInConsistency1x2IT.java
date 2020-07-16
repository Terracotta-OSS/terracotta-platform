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
import org.terracotta.angela.client.net.ServerToServerDisruptor;
import org.terracotta.angela.client.net.SplitCluster;
import org.terracotta.angela.client.support.junit.NodeOutputRule;
import org.terracotta.angela.common.tcconfig.TerracottaServer;
import org.terracotta.dynamic_config.api.model.FailoverPriority;
import org.terracotta.dynamic_config.test_support.ClusterDefinition;
import org.terracotta.dynamic_config.test_support.DynamicConfigIT;

import java.time.Duration;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.successful;

@ClusterDefinition(nodesPerStripe = 2, autoStart = false, netDisruptionEnabled = true)
public class DetachInConsistency1x2IT extends DynamicConfigIT {
  @Rule
  public final NodeOutputRule out = new NodeOutputRule();

  public DetachInConsistency1x2IT() {
    super(Duration.ofSeconds(180));
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

    //attach the second node
    assertThat(invokeConfigTool("attach", "-d", "localhost:" + getNodePort(1, 1), "-s", "localhost:" + getNodePort(1, 2)), is(successful()));

    setServerDisruptionLinks(1, 2);
    activateCluster();
    waitForNPassives(1, 1);
  }

  @Test
  public void test_detach_when_active_passive_disrupted() throws Exception {
    TerracottaServer active = angela.tsa().getActive();
    TerracottaServer passive = angela.tsa().getPassive();
    SplitCluster split1 = new SplitCluster(active);
    SplitCluster split2 = new SplitCluster(passive);
    int activeId = findActive(1).getAsInt();
    int passiveId = findPassives(1)[0];
    try (ServerToServerDisruptor disruptor = angela.tsa().disruptionController().newServerToServerDisruptor(split1, split2)) {

      //start partition
      disruptor.disrupt();

      //verify passive gets blocked
      waitForServerBlocked(passive);

      assertThat(
          () -> invokeConfigTool("detach", "-f", "-d", "localhost:" + getNodePort(1, activeId), "-s", "localhost:" + getNodePort(1, passiveId)),
          exceptionMatcher("Please ensure all online nodes are either ACTIVE or PASSIVE before sending any update."));

      //stop partition
      disruptor.undisrupt();
    }
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 1)).getNodeCount(), is(equalTo(2)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, 1)).getNodeCount(), is(equalTo(2)));

    assertThat(getUpcomingCluster("localhost", getNodePort(1, 2)).getNodeCount(), is(equalTo(2)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, 2)).getNodeCount(), is(equalTo(2)));

    withTopologyService(1, 1, topologyService -> assertTrue(topologyService.isActivated()));
    withTopologyService(1, 2, topologyService -> assertTrue(topologyService.isActivated()));
  }
}
