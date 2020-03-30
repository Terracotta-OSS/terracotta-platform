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

import org.junit.Rule;
import org.junit.Test;
import org.terracotta.dynamic_config.test_support.ClusterDefinition;
import org.terracotta.dynamic_config.test_support.DynamicConfigIT;
import org.terracotta.dynamic_config.test_support.util.NodeOutputRule;

import java.time.Duration;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.terracotta.dynamic_config.test_support.util.AngelaMatchers.containsLog;
import static org.terracotta.dynamic_config.test_support.util.AngelaMatchers.containsOutput;
import static org.terracotta.dynamic_config.test_support.util.AngelaMatchers.successful;

/**
 * @author Mathieu Carbou
 */
@ClusterDefinition(nodesPerStripe = 2, autoActivate = true)
public class DetachCommand1x2IT extends DynamicConfigIT {

  @Rule public final NodeOutputRule out = new NodeOutputRule();

  public DetachCommand1x2IT() {
    super(Duration.ofSeconds(120));
  }

  @Test
  public void test_detach_active_node() throws Exception {
    final int activeId = findActive(1).getAsInt();
    final int passiveId = findPassives(1)[0];

    out.clearLog(1, activeId);
    assertThat(configToolInvocation("detach", "-d", "localhost:" + getNodePort(1, passiveId), "-s", "localhost:" + getNodePort(1, activeId)), is(successful()));

    // the detached node is cleared and restarts in diagnostic mode
    waitUntil(out.getLog(1, activeId), containsLog("Started the server in diagnostic mode"));
    withTopologyService(1, activeId, topologyService -> assertFalse(topologyService.isActivated()));

    // failover - existing passive becomes active
    waitUntil(out.getLog(1, passiveId), containsLog("Moved to State[ ACTIVE-COORDINATOR ]"));
    withTopologyService(1, passiveId, topologyService -> assertTrue(topologyService.isActivated()));

    assertTopologyChanged(activeId, passiveId);
  }

  @Test
  public void test_detach_passive_from_activated_cluster() throws Exception {
    final int activeId = findActive(1).getAsInt();
    final int passiveId = findPassives(1)[0];

    out.clearLog(1, passiveId);
    assertThat(configToolInvocation("detach", "-d", "localhost:" + getNodePort(1, activeId), "-s", "localhost:" + getNodePort(1, passiveId)), is(successful()));
    waitUntil(out.getLog(1, passiveId), containsLog("Started the server in diagnostic mode"));
    withTopologyService(1, passiveId, topologyService -> assertFalse(topologyService.isActivated()));

    assertTopologyChanged(activeId, passiveId);
  }

  @Test
  public void test_detach_passive_from_activated_cluster_requiring_restart() throws Exception {
    final int activeId = findActive(1).getAsInt();
    final int passiveId = findPassives(1)[0];

    // do a change requiring a restart
    assertThat(configToolInvocation("set", "-s", "localhost:" + getNodePort(1, activeId), "-c", "stripe.1.node.1.tc-properties.foo=bar"),
        allOf(is(successful()), containsOutput("IMPORTANT: A restart of the cluster is required to apply the changes")));

    // try to detach this node
    assertThat(
        configToolInvocation("detach", "-d", "localhost:" + getNodePort(1, activeId), "-s", "localhost:" + getNodePort(1, passiveId)),
        containsOutput("Impossible to do any topology change. Cluster at address: " + "localhost:" + getNodePort(1, activeId) +
            " is waiting to be restarted to apply some pending changes. " +
            "You can run the command with -f option to force the comment but at the risk of breaking this cluster configuration consistency. " +
            "The newly added node will be restarted, but not the existing ones."));

    // try forcing the detach
    out.clearLog(1, passiveId);
    assertThat(configToolInvocation("detach", "-f", "-d", "localhost:" + getNodePort(1, activeId), "-s", "localhost:" + getNodePort(1, passiveId)), is(successful()));
    waitUntil(out.getLog(1, passiveId), containsLog("Started the server in diagnostic mode"));
    withTopologyService(1, passiveId, topologyService -> assertFalse(topologyService.isActivated()));

    assertTopologyChanged(activeId, passiveId);
  }

  @Test
  public void test_detach_offline_node_in_availability_mode() throws Exception {
    final int activeId = findActive(1).getAsInt();
    final int passiveId = findPassives(1)[0];

    stopNode(1, passiveId);

    // detaching an offline node needs to be forced
    assertThat(
        configToolInvocation("-t", "5s", "detach", "-d", "localhost:" + getNodePort(1, activeId), "-s", "localhost:" + getNodePort(1, passiveId)),
        containsOutput("Node to detach: localhost:" + getNodePort(1, passiveId) + " is not reachable."));

    configToolInvocation("-t", "5s", "detach", "-d", "localhost:" + getNodePort(1, activeId), "-s", "localhost:" + getNodePort(1, passiveId), "-f");

    withTopologyService(1, activeId, topologyService -> assertTrue(topologyService.isActivated()));
    assertThat(getUpcomingCluster("localhost", getNodePort(1, activeId)).getNodeCount(), is(equalTo(1)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, activeId)).getNodeCount(), is(equalTo(1)));

    // restart the detached node : it should be removed
    out.clearLog(1, passiveId);
    startNode(1, passiveId);

    // in availability mode, the server will restart ACTIVE with the topology it knows
    waitUntil(out.getLog(1, passiveId), containsLog("Moved to State[ ACTIVE-COORDINATOR ]"));
    withTopologyService(1, passiveId, topologyService -> assertTrue(topologyService.isActivated()));
    assertThat(getUpcomingCluster("localhost", getNodePort(1, passiveId)).getNodeCount(), is(equalTo(2)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, passiveId)).getNodeCount(), is(equalTo(2)));
  }

  private void assertTopologyChanged(int activeId, int passiveId) throws Exception {
    assertThat(getUpcomingCluster("localhost", getNodePort(1, activeId)).getNodeCount(), is(equalTo(1)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, activeId)).getNodeCount(), is(equalTo(1)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, activeId)).getSingleNode().get().getNodePort(), is(equalTo(getNodePort(1, activeId))));

    assertThat(getUpcomingCluster("localhost", getNodePort(1, passiveId)).getNodeCount(), is(equalTo(1)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, passiveId)).getNodeCount(), is(equalTo(1)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, passiveId)).getSingleNode().get().getNodePort(), is(equalTo(getNodePort(1, passiveId))));
  }
}
