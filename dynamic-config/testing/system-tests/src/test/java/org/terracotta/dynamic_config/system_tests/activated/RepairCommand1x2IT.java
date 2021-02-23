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
import org.terracotta.angela.common.tcconfig.TerracottaServer;
import org.terracotta.dynamic_config.api.service.TopologyService;
import org.terracotta.dynamic_config.test_support.ClusterDefinition;
import org.terracotta.dynamic_config.test_support.DynamicConfigIT;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.containsLinesStartingWith;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.containsOutput;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.successful;

/**
 * @author Mathieu Carbou
 */
@ClusterDefinition(nodesPerStripe = 2, autoStart = false)
public class RepairCommand1x2IT extends DynamicConfigIT {

  public RepairCommand1x2IT() {
    super(Duration.ofSeconds(300));
  }

  @SuppressWarnings("OptionalGetWithoutIsPresent")
  @Test
  public void test_repair_partial_commit_when_passive_down() throws Exception {
    startNode(1, 1);
    startNode(1, 2);
    activate1x2Cluster();

    final int activeId = findActive(1).getAsInt();
    final int passiveId = findPassives(1)[0];

    // triggers a failure during Nomad commit phase on all servers
    // active entity will return the failure to the nomad client (commit phase done through entity)
    // passive entity will fail and restart
    assertThat(
        configTool("set", "-s", "localhost:" + getNodePort(1, activeId), "-c", "logger-overrides.org.terracotta.dynamic-config.simulate=DEBUG"),
        containsOutput("Commit failed for node"));

    waitForPassive(1, passiveId);

    // wait until we can really connect to the passive and see it has an incomplete change
    // this is because he passive will restart after sync
    waitUntil(() -> usingTopologyService(1, activeId, TopologyService::hasIncompleteChange), is(true));
    waitUntil(() -> usingTopologyService(1, passiveId, TopologyService::hasIncompleteChange), is(true));

    // stop passive
    stopNode(1, passiveId);

    // cannot automatic repair since 1 node is down
    assertThat(
        configTool("repair", "-s", "localhost:" + getNodePort(1, activeId)),
        containsOutput("Some nodes are offline. Unable to determine what kind of repair to run. Please refer to the Troubleshooting Guide."));

    // forces a repair
    assertThat(
        configTool("repair", "-f", "commit", "-s", "localhost:" + getNodePort(1, activeId)),
        containsOutput("Configuration is repaired."));
    withTopologyService(1, activeId, topologyService -> assertFalse(topologyService.hasIncompleteChange()));

    // restart passive - it should sync
    startNode(1, passiveId);
    waitForPassive(1, passiveId);
    withTopologyService(1, passiveId, topologyService -> assertFalse(topologyService.hasIncompleteChange()));
  }

  @Test
  public void test_repair_detach_node_partially_committed() throws Exception {
    startNode(1, 1);
    startNode(1, 2);
    activate1x2Cluster();

    final int activeId = findActive(1).getAsInt();
    final int passiveId = findPassives(1)[0];

    //create failover while committing
    String propertySettingString = "stripe.1.node." + activeId + ".tc-properties.failoverDeletion=killDeletion-commit";
    assertThat(configTool("set", "-s", "localhost:" + getNodePort(1, 1), "-c", propertySettingString), is(successful()));

    // passive down
    stopNode(1, passiveId);

    // detach command will kill the active, so the stripe will go down
    assertThat(
        configTool("-er", "40s", "detach", "-f", "-d", "localhost:" + getNodePort(1, activeId), "-s", "localhost:" + getNodePort(1, passiveId)),
        containsOutput("Two-Phase commit failed"));

    // we restart the active
    startNode(1, activeId, "-r", getNode(1, activeId).getConfigRepo());
    waitForActive(1, activeId);

    // active has a prepared change
    withTopologyService(1, activeId, topologyService -> assertTrue(topologyService.isActivated()));
    withTopologyService(1, activeId, topologyService -> assertTrue(topologyService.hasIncompleteChange()));
    assertThat(getUpcomingCluster("localhost", getNodePort(1, activeId)).getNodeCount(), is(equalTo(2)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, activeId)).getNodeCount(), is(equalTo(2)));

    // we force a commit on the active
    assertThat(
        configTool("repair", "-f", "commit", "-s", "localhost:" + getNodePort(1, activeId)),
        allOf(
            containsOutput("Repairing configuration by running a commit..."),
            containsOutput("Configuration is repaired")));

    // the topology is down 1 node
    withTopologyService(1, activeId, topologyService -> assertTrue(topologyService.isActivated()));
    withTopologyService(1, activeId, topologyService -> assertFalse(topologyService.hasIncompleteChange()));
    assertThat(getUpcomingCluster("localhost", getNodePort(1, activeId)).getNodeCount(), is(equalTo(1)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, activeId)).getNodeCount(), is(equalTo(1)));

    // if we restart the detached node, it will restart in active mode because it has been stopped before the detach
    startNode(1, passiveId, "-r", getNode(1, passiveId).getConfigRepo());
    waitForActive(1, passiveId);
    withTopologyService(1, passiveId, topologyService -> assertTrue(topologyService.isActivated()));
  }

  @Test
  public void test_repair_stripe_down_during_detach_node() throws Exception {
    startNode(1, 1);
    startNode(1, 2);
    activate1x2Cluster();

    final int activeId = findActive(1).getAsInt();
    final int passiveId = findPassives(1)[0];

    String propertySettingString = "stripe.1.node." + activeId + ".tc-properties.failoverDeletion=killDeletion-commit";

    //setup for failover in commit phase on active
    assertThat(configTool("set", "-s", "localhost:" + getNodePort(1, 1), "-c", propertySettingString), is(successful()));

    assertThat(
        configTool("-er", "40s", "detach", "-f", "-d", "localhost:" + getNodePort(1, activeId), "-s", "localhost:" + getNodePort(1, passiveId)),
        containsOutput("Commit failed for node localhost:" + getNodePort(1, activeId) + ". Reason: java.util.concurrent.TimeoutException"));

    // Stripe is lost no active
    startNode(1, activeId, "-r", getNode(1, activeId).getConfigRepo());
    waitForActive(1, activeId);

    // Active has prepare changes for node removal
    withTopologyService(1, activeId, topologyService -> assertTrue(topologyService.isActivated()));
    withTopologyService(1, activeId, topologyService -> assertTrue(topologyService.hasIncompleteChange()));
    assertThat(getUpcomingCluster(1, activeId).getNodeCount(), is(equalTo(2)));
    assertThat(getRuntimeCluster(1, activeId).getNodeCount(), is(equalTo(2)));

    assertThat(
        configTool("diagnostic", "-s", "localhost:" + getNodePort(1, activeId)),
        containsLinesStartingWith(Files.lines(Paths.get(getClass().getResource("/diagnostic-output/diagnostic5.txt").toURI())).collect(toList())));

    assertThat(
        configTool("repair", "-f", "commit", "-s", "localhost:" + getNodePort(1, activeId)),
        allOf(
            containsOutput("Repairing configuration by running a commit..."),
            containsOutput("Configuration is repaired")));

    withTopologyService(1, activeId, topologyService -> assertFalse(topologyService.hasIncompleteChange()));
    assertThat(getUpcomingCluster("localhost", getNodePort(1, activeId)).getNodeCount(), is(equalTo(1)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, activeId)).getNodeCount(), is(equalTo(1)));
  }

  @Test
  public void test_repair_detached_node_restarting_as_active() throws Exception {
    startNode(1, 1);
    startNode(1, 2);
    activate1x2Cluster();

    final int activeId = findActive(1).getAsInt();
    final int passiveId = findPassives(1)[0];

    //create failover while committing
    String propertySettingString = "stripe.1.node." + activeId + ".tc-properties.failoverDeletion=killDeletion-commit";
    assertThat(configTool("set", "-s", "localhost:" + getNodePort(1, 1), "-c", propertySettingString), is(successful()));

    stopNode(1, passiveId);

    //Both active and passive is down.
    assertThat(
        configTool("-er", "40s", "detach", "-f", "-d", "localhost:" + getNodePort(1, activeId), "-s", "localhost:" + getNodePort(1, passiveId)),
        containsOutput("Two-Phase commit failed"));

    startNode(1, activeId, "-r", getNode(1, activeId).getConfigRepo());
    waitForActive(1, activeId);

    withTopologyService(1, activeId, topologyService -> assertTrue(topologyService.isActivated()));
    withTopologyService(1, activeId, topologyService -> assertTrue(topologyService.hasIncompleteChange()));
    assertThat(getUpcomingCluster("localhost", getNodePort(1, activeId)).getNodeCount(), is(equalTo(2)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, activeId)).getNodeCount(), is(equalTo(2)));

    assertThat(
        configTool("repair", "-f", "commit", "-s", "localhost:" + getNodePort(1, activeId)),
        allOf(
            containsOutput("Repairing configuration by running a commit..."),
            containsOutput("Configuration is repaired")));

    withTopologyService(1, activeId, topologyService -> assertTrue(topologyService.isActivated()));
    assertThat(getUpcomingCluster("localhost", getNodePort(1, activeId)).getNodeCount(), is(equalTo(1)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, activeId)).getNodeCount(), is(equalTo(1)));

    // if we restart the detached node, that was offline  it will restart as active, which is expected in availability mode
    TerracottaServer toStart = getNode(1, passiveId);
    startNode(1, passiveId, "-r", toStart.getConfigRepo());
    waitForActive(1, passiveId);
    withTopologyService(1, passiveId, topologyService -> assertTrue(topologyService.isActivated()));

    // we can reset this node
    assertThat(configTool("repair", "-f", "reset", "-s", "localhost:" + getNodePort(1, passiveId)), is(successful()));
    waitForStopped(1, passiveId);

    // TODO - simplify to startNode(1, passiveId) once TDB-5113 is fixed
    startNode(1, passiveId, "-y", "availability", "-s", toStart.getHostName(),
        "-p", String.valueOf(toStart.getTsaPort()), "-g", String.valueOf(toStart.getTsaGroupPort()),
        "-L", toStart.getLogs(), "-r", tmpDir.getRoot().resolve("config").toString());
    waitForDiagnostic(1, passiveId);
  }

  @Test
  public void test_reset_node() {
    // reset diagnostic node
    startNode(1, 1);
    waitForDiagnostic(1, 1);
    assertThat(configTool("repair", "-f", "reset", "-s", "localhost:" + getNodePort()), is(successful()));
    waitForStopped(1, 1);
    startNode(1, 1);
    waitForDiagnostic(1, 1);

    // reset activated node
    assertThat(configTool("activate", "-n", "my-cluster", "-s", "localhost:" + getNodePort()), is(successful()));
    waitForActive(1, 1);
    assertThat(configTool("repair", "-f", "reset", "-s", "localhost:" + getNodePort()), is(successful()));
    waitForStopped(1, 1);
    startNode(1, 1);
    waitForDiagnostic(1, 1);

    // reset node restarted in repair mode
    assertThat(configTool("activate", "-n", "my-cluster", "-s", "localhost:" + getNodePort()), is(successful()));
    waitForActive(1, 1);
    stopNode(1, 1);
    startNode(1, 1, "-r", getNode(1, 1).getConfigRepo(), "--repair-mode");
    waitForDiagnostic(1, 1);
    assertThat(configTool("repair", "-f", "reset", "-s", "localhost:" + getNodePort()), is(successful()));
    waitForStopped(1, 1);
    startNode(1, 1);
    waitForDiagnostic(1, 1);

    // ensure we still can re-activate the node
    stopNode(1, 1);
    // restart but not in repair mode (angela keeps last params used)
    startNode(1, 1);
    waitForDiagnostic(1, 1);
    assertThat(configTool("activate", "-n", "my-cluster", "-s", "localhost:" + getNodePort()), is(successful()));
    waitForActive(1, 1);
  }

  private void activate1x2Cluster() {
    assertThat(configTool("attach", "-d", "localhost:" + getNodePort(), "-s", "localhost:" + getNodePort(1, 2)), is(successful()));
    assertThat(activateCluster(), allOf(is(successful()), containsOutput("No license installed"), containsOutput("came back up")));

    waitForActive(1);
    waitForPassives(1);
  }
}
