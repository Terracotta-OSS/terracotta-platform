/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
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
import org.terracotta.angela.common.tcconfig.TerracottaServer;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.service.TopologyService;
import org.terracotta.dynamic_config.test_support.ClusterDefinition;
import org.terracotta.dynamic_config.test_support.DynamicConfigIT;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.containsOutput;

/**
 * @author Mathieu Carbou
 */
@ClusterDefinition(nodesPerStripe = 2, autoActivate = true)
public class DiagnosticMode1x2IT extends DynamicConfigIT {

  @Test
  public void test_restart_active_in_diagnostic_mode() {
    int activeNodeId = waitForActive(1);
    TerracottaServer active = getNode(1, activeNodeId);
    angela.tsa().stop(active);
    assertThat(angela.tsa().getStopped().size(), is(1));

    startNode(active, "--repair-mode", "--name", active.getServerSymbolicName().getSymbolicName(), "-r", active.getConfigRepo());
    waitUntil(() -> usingTopologyService(1, activeNodeId, TopologyService::isActivated), is(false));
  }

  @Test
  public void test_restart_passive_in_diagnostic_mode() {
    int passiveNodeId = waitForNPassives(1, 1)[0];
    TerracottaServer passive = getNode(1, passiveNodeId);
    angela.tsa().stop(passive);
    assertThat(angela.tsa().getStopped().size(), is(1));

    startNode(passive, "--repair-mode", "--name", passive.getServerSymbolicName().getSymbolicName(), "-r", passive.getConfigRepo());
    waitUntil(() -> usingTopologyService(1, passiveNodeId, TopologyService::isActivated), is(false));
  }

  @Test
  public void test_diagnostic_port_accessible_but_nomad_change_impossible() throws Exception {
    int activeNodeId = waitForActive(1);
    int passiveId = waitForNPassives(1, 1)[0];
    TerracottaServer active = getNode(1, activeNodeId);
    angela.tsa().stop(active);
    assertThat(angela.tsa().getStopped().size(), is(1));

    startNode(active, "--repair-mode", "-n", active.getServerSymbolicName().getSymbolicName(), "-r", active.getConfigRepo());

    // diag port available
    Cluster cluster = getUpcomingCluster("localhost", getNodePort(1, activeNodeId));
    assertThat(cluster.getStripeCount(), is(equalTo(1)));

    // log command works, both when targeting node to repair and a normal node in the cluster
    assertThat(
        configTool("log", "-s", "localhost:" + getNodePort(1, activeNodeId)),
        containsOutput("Activating cluster"));
    assertThat(
        configTool("log", "-s", "localhost:" + getNodePort(1, passiveId)),
        containsOutput("Activating cluster"));

    // unable to trigger a change on the cluster from the node in diagnostic mode
    assertThat(
        configTool("set", "-s", "localhost:" + getNodePort(1, activeNodeId), "-c", "stripe.1.node." + activeNodeId + ".tc-properties.something=value"),
        containsOutput("Detected a mix of activated and unconfigured nodes (or being repaired)."));

    // unable to trigger a change on the cluster from any other node
    assertThat(
        configTool("set", "-s", "localhost:" + getNodePort(1, passiveId), "-c", "stripe.1.node.1.tc-properties.something=value"),
        containsOutput("Detected a mix of activated and unconfigured nodes (or being repaired)."));
  }
}
