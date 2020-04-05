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
import static org.terracotta.dynamic_config.test_support.util.AngelaMatchers.containsLinesStartingWith;
import static org.terracotta.dynamic_config.test_support.util.AngelaMatchers.containsLog;
import static org.terracotta.dynamic_config.test_support.util.AngelaMatchers.containsOutput;
import static org.terracotta.dynamic_config.test_support.util.AngelaMatchers.successful;

/**
 * @author Mathieu Carbou
 */
@ClusterDefinition(nodesPerStripe = 2, autoStart = false)
public class RepairCommand1x2IT extends DynamicConfigIT {

  public RepairCommand1x2IT() {
    super(Duration.ofSeconds(180));
  }

  @Rule public final NodeOutputRule out = new NodeOutputRule();

  @SuppressWarnings("OptionalGetWithoutIsPresent")
  @Test
  public void test_repair_partial_commit_when_passive_down() throws Exception {
    startNode(1, 1);
    startNode(1, 2);
    activate1x2Cluster();

    final int activeId = findActive(1).getAsInt();
    final int passiveId = findPassives(1)[0];

    assertThat(
        configToolInvocation("set", "-s", "localhost:" + getNodePort(1, activeId), "-c", "node-logger-overrides.org.terracotta.dynamic-config.simulate=DEBUG"),
        containsOutput("Commit failed for node"));

    withTopologyService(1, activeId, topologyService -> assertTrue(topologyService.hasIncompleteChange()));
    withTopologyService(1, passiveId, topologyService -> assertTrue(topologyService.hasIncompleteChange()));

    // stop passive
    stopNode(1, passiveId);

    // cannot automatic repair since 1 node is down
    assertThat(
        configToolInvocation("-t", "5s", "repair", "-s", "localhost:" + getNodePort(1, activeId)),
        containsOutput("Please run the 'diagnostic' command to diagnose the configuration state and try to run the 'repair' command and force either a commit or rollback."));

    // forces a repair
    assertThat(configToolInvocation("-t", "5s", "repair", "-f", "commit", "-s", "localhost:" + getNodePort(1, activeId)), containsOutput("Configuration is repaired."));
    withTopologyService(1, activeId, topologyService -> assertFalse(topologyService.hasIncompleteChange()));

    // restart passive - it should sync
    startNode(1, passiveId);
    waitUntil(out.getLog(1, passiveId), containsLog("Moved to State[ PASSIVE-STANDBY ]"));
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
    assertThat(configToolInvocation("set", "-s", "localhost:" + getNodePort(1, 1), "-c", propertySettingString), is(successful()));

    stopNode(1, passiveId);

    //Both active and passive is down.
    assertThat(
        configToolInvocation("-e", "40s", "-r", "5s", "-t", "5s", "detach", "-f", "-d", "localhost:" + getNodePort(1, activeId),
            "-s", "localhost:" + getNodePort(1, passiveId)),
        containsOutput("Two-Phase commit failed"));

    out.clearLog(1, activeId);
    startNode(1, activeId, "-r", getNode(1, activeId).getConfigRepo());
    waitUntil(out.getLog(1, activeId), containsLog("Moved to State[ ACTIVE-COORDINATOR ]"));

    withTopologyService(1, activeId, topologyService -> assertTrue(topologyService.isActivated()));
    withTopologyService(1, activeId, topologyService -> assertTrue(topologyService.hasIncompleteChange()));
    assertThat(getUpcomingCluster("localhost", getNodePort(1, activeId)).getNodeCount(), is(equalTo(2)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, activeId)).getNodeCount(), is(equalTo(2)));

    assertThat(
        configToolInvocation("-r", "5s", "repair", "-f", "commit", "-s", "localhost:" + getNodePort(1, activeId)),
        allOf(
            containsOutput("Attempting an automatic repair of the configuration on nodes"),
            containsOutput("Forcing a commit"),
            containsOutput("Configuration is repaired")));

    withTopologyService(1, activeId, topologyService -> assertTrue(topologyService.isActivated()));
    assertThat(getUpcomingCluster("localhost", getNodePort(1, activeId)).getNodeCount(), is(equalTo(1)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, activeId)).getNodeCount(), is(equalTo(1)));

    // if we restart the detached node, it will restart as active, which is expected in availability mode
    out.clearLog(1, passiveId);
    startNode(1, passiveId, "-r", getNode(1, passiveId).getConfigRepo());
    waitUntil(out.getLog(1, passiveId), containsLog("Moved to State[ ACTIVE-COORDINATOR ]"));
    withTopologyService(1, passiveId, topologyService -> assertTrue(topologyService.isActivated()));
    // on passive, the set command was committed
    withTopologyService(1, passiveId, topologyService -> assertFalse(topologyService.hasIncompleteChange()));
    assertThat(getUpcomingCluster("localhost", getNodePort(1, passiveId)).getNodeCount(), is(equalTo(2)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, passiveId)).getNodeCount(), is(equalTo(2)));
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
    assertThat(configToolInvocation("set", "-s", "localhost:" + getNodePort(1, 1), "-c", propertySettingString), is(successful()));

    out.clearLog(1, activeId);
    out.clearLog(1, passiveId);

    assertThat(
        configToolInvocation("detach", "-f", "-d", "localhost:" + getNodePort(1, activeId),
            "-s", "localhost:" + getNodePort(1, passiveId)),
        is(successful()));

    waitUntil(out.getLog(1, passiveId), containsLog("Started the server in diagnostic mode"));
    withTopologyService(1, passiveId, topologyService -> assertFalse(topologyService.isActivated()));

    // Stripe is lost no active
    assertThat(getUpcomingCluster("localhost", getNodePort(1, passiveId)).getNodeCount(), is(equalTo(1)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, passiveId)).getNodeCount(), is(equalTo(1)));

    out.clearLog(1, activeId);
    startNode(1, activeId, "-r", getNode(1, activeId).getConfigRepo());
    waitUntil(out.getLog(1, activeId), containsLog("Moved to State[ ACTIVE-COORDINATOR ]"));

    // Active has prepare changes for node removal
    withTopologyService(1, activeId, topologyService -> assertTrue(topologyService.isActivated()));
    withTopologyService(1, activeId, topologyService -> assertTrue(topologyService.hasIncompleteChange()));
    assertThat(getUpcomingCluster(1, activeId).getNodeCount(), is(equalTo(2)));
    assertThat(getRuntimeCluster(1, activeId).getNodeCount(), is(equalTo(2)));

    assertThat(configToolInvocation("-r", "5s", "diagnostic", "-s", "localhost:" + getNodePort(1, activeId)),
        containsLinesStartingWith(Files.lines(Paths.get(getClass().getResource("/diagnostic5.txt").toURI())).collect(toList())));

    assertThat(
        configToolInvocation("-r", "5s", "repair", "-f", "commit", "-s", "localhost:" + getNodePort(1, activeId)),
        allOf(
            containsOutput("Attempting an automatic repair of the configuration on nodes"),
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
    assertThat(configToolInvocation("set", "-s", "localhost:" + getNodePort(1, 1), "-c", propertySettingString), is(successful()));

    stopNode(1, passiveId);

    //Both active and passive is down.
    assertThat(
        configToolInvocation("-e", "40s", "-r", "5s", "-t", "5s", "detach", "-f", "-d", "localhost:" + getNodePort(1, activeId),
            "-s", "localhost:" + getNodePort(1, passiveId)),
        containsOutput("Two-Phase commit failed"));

    out.clearLog(1, activeId);
    startNode(1, activeId, "-r", getNode(1, activeId).getConfigRepo());
    waitUntil(out.getLog(1, activeId), containsLog("Moved to State[ ACTIVE-COORDINATOR ]"));

    withTopologyService(1, activeId, topologyService -> assertTrue(topologyService.isActivated()));
    withTopologyService(1, activeId, topologyService -> assertTrue(topologyService.hasIncompleteChange()));
    assertThat(getUpcomingCluster("localhost", getNodePort(1, activeId)).getNodeCount(), is(equalTo(2)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, activeId)).getNodeCount(), is(equalTo(2)));

    assertThat(
        configToolInvocation("-r", "5s", "repair", "-f", "commit", "-s", "localhost:" + getNodePort(1, activeId)),
        allOf(
            containsOutput("Attempting an automatic repair of the configuration on nodes"),
            containsOutput("Forcing a commit"),
            containsOutput("Configuration is repaired")));

    withTopologyService(1, activeId, topologyService -> assertTrue(topologyService.isActivated()));
    assertThat(getUpcomingCluster("localhost", getNodePort(1, activeId)).getNodeCount(), is(equalTo(1)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, activeId)).getNodeCount(), is(equalTo(1)));

    // if we restart the detached node, it will restart as active, which is expected in availability mode
    out.clearLog(1, passiveId);
    startNode(1, passiveId, "-r", getNode(1, passiveId).getConfigRepo());
    waitUntil(out.getLog(1, passiveId), containsLog("Moved to State[ ACTIVE-COORDINATOR ]"));
    withTopologyService(1, passiveId, topologyService -> assertTrue(topologyService.isActivated()));
    // on passive, the set command was committed
    withTopologyService(1, passiveId, topologyService -> assertFalse(topologyService.hasIncompleteChange()));
    assertThat(getUpcomingCluster("localhost", getNodePort(1, passiveId)).getNodeCount(), is(equalTo(2)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, passiveId)).getNodeCount(), is(equalTo(2)));

    configToolInvocation("-r", "5s", "repair", "-f", "reset", "-s", "localhost:" + getNodePort(1, passiveId));
    waitUntil(out.getLog(1, passiveId), containsLog("Started the server in diagnostic mode"));
  }

  @Test
  public void test_reset_node() {
    // reset diagnostic node
    startNode(1, 1);
    waitUntil(out.getLog(1, 1), containsLog("Started the server in diagnostic mode"));
    out.clearLog(1, 1);
    assertThat(configToolInvocation("-r", "5s", "repair", "-f", "reset", "-s", "localhost:" + getNodePort()), is(successful()));
    waitUntil(out.getLog(1, 1), containsLog("Started the server in diagnostic mode"));

    // reset activated node
    out.clearLog(1, 1);
    assertThat(configToolInvocation("activate", "-n", "my-cluster", "-s", "localhost:" + getNodePort()), is(successful()));
    waitUntil(out.getLog(1, 1), containsLog("Moved to State[ ACTIVE-COORDINATOR ]"));
    out.clearLog(1, 1);
    assertThat(configToolInvocation("-r", "5s", "repair", "-f", "reset", "-s", "localhost:" + getNodePort()), is(successful()));
    waitUntil(out.getLog(1, 1), containsLog("Started the server in diagnostic mode"));

    // reset node restarted in repair mode
    out.clearLog(1, 1);
    assertThat(configToolInvocation("activate", "-n", "my-cluster", "-s", "localhost:" + getNodePort()), is(successful()));
    waitUntil(out.getLog(1, 1), containsLog("Moved to State[ ACTIVE-COORDINATOR ]"));
    stopNode(1, 1);
    out.clearLog(1, 1);
    startNode(1, 1, "-r", getNode(1, 1).getConfigRepo(), "--repair-mode");
    waitUntil(out.getLog(1, 1), containsLog("Started the server in diagnostic mode"));
    out.clearLog(1, 1);
    assertThat(configToolInvocation("-r", "5s", "repair", "-f", "reset", "-s", "localhost:" + getNodePort()), is(successful()));
    waitUntil(out.getLog(1, 1), containsLog("Started the server in diagnostic mode"));

    // ensure we still can re-activate the node
    stopNode(1, 1);
    out.clearLog(1, 1);
    // restart but not in repair mode (angela keeps last params used)
    startNode(1, 1);
    waitUntil(out.getLog(1, 1), containsLog("Started the server in diagnostic mode"));
    out.clearLog(1, 1);
    assertThat(configToolInvocation("activate", "-n", "my-cluster", "-s", "localhost:" + getNodePort()), is(successful()));
    waitUntil(out.getLog(1, 1), containsLog("Moved to State[ ACTIVE-COORDINATOR ]"));
  }

  private void activate1x2Cluster() {
    assertThat(
        configToolInvocation("attach", "-d", "localhost:" + getNodePort(), "-s", "localhost:" + getNodePort(1, 2)),
        is(successful()));
    assertThat(activateCluster(), allOf(is(successful()), containsOutput("No license installed"), containsOutput("came back up")));
    waitUntil(out.getLog(1, findActive(1).getAsInt()), containsLog("Moved to State[ ACTIVE-COORDINATOR ]"));
    waitUntil(out.getLog(1, findPassives(1)[0]), containsLog("Moved to State[ PASSIVE-STANDBY ]"));
  }
}
