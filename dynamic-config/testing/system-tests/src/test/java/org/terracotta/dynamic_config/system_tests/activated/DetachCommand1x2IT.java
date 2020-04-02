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
@ClusterDefinition(nodesPerStripe = 2, autoActivate = true)
public class DetachCommand1x2IT extends DynamicConfigIT {

  @Rule
  public final NodeOutputRule out = new NodeOutputRule();

  public DetachCommand1x2IT() {
    super(Duration.ofSeconds(180));
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

  @Test
  public void detachNodeFailInActiveAtPrepare() throws Exception {
    final int activeId = findActive(1).getAsInt();
    final int passiveId = findPassives(1)[0];

    String propertySettingString = "stripe.1.node." + activeId + ".tc-properties.detachStatus=prepareDeletion-failure";

    //create prepare failure on active
    assertThat(configToolInvocation("set", "-s", "localhost:" + getNodePort(1, 1), "-c", propertySettingString), is(successful()));

    // detach failure (forcing detach otherwise we have to restart cluster)
    assertThat(
        configToolInvocation("detach", "-f", "-d", "localhost:" + getNodePort(1, activeId),
            "-s", "localhost:" + getNodePort(1, passiveId)),
        containsOutput("Two-Phase commit failed"));

    assertThat(getUpcomingCluster("localhost", getNodePort(1, activeId)).getNodeCount(), is(equalTo(2)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, activeId)).getNodeCount(), is(equalTo(2)));
    assertThat(getUpcomingCluster("localhost", getNodePort(1, passiveId)).getNodeCount(), is(equalTo(2)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, passiveId)).getNodeCount(), is(equalTo(2)));

    withTopologyService(1, activeId, topologyService -> assertTrue(topologyService.isActivated()));
    withTopologyService(1, passiveId, topologyService -> assertTrue(topologyService.isActivated()));
  }

  @Test
  public void detachNodeFailInPassiveAtPrepare() throws Exception {
    final int activeId = findActive(1).getAsInt();
    final int passiveId = findPassives(1)[0];

    String propertySettingString = "stripe.1.node." + passiveId + ".tc-properties.detachStatus=prepareDeletion-failure";

    //create prepare failure on passive
    assertThat(configToolInvocation("set", "-s", "localhost:" + getNodePort(1, 1), "-c", propertySettingString), is(successful()));

    // detach failure (forcing detach otherwise we have to restart cluster)
    assertThat(
        configToolInvocation("detach", "-f", "-d", "localhost:" + getNodePort(1, activeId),
            "-s", "localhost:" + getNodePort(1, passiveId)),
        containsOutput("Two-Phase commit failed"));

    assertThat(getUpcomingCluster("localhost", getNodePort(1, activeId)).getNodeCount(), is(equalTo(2)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, activeId)).getNodeCount(), is(equalTo(2)));
    assertThat(getUpcomingCluster("localhost", getNodePort(1, passiveId)).getNodeCount(), is(equalTo(2)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, passiveId)).getNodeCount(), is(equalTo(2)));

    withTopologyService(1, activeId, topologyService -> assertTrue(topologyService.isActivated()));
    withTopologyService(1, passiveId, topologyService -> assertTrue(topologyService.isActivated()));
  }

  @Test
  public void testFailoverDuringNomadCommitForPassiveRemoval() throws Exception {
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
        configToolInvocation("-r", "5s", "repair", "-s", "localhost:" + getNodePort(1, activeId)),
        allOf(
            containsOutput("Attempting an automatic repair of the configuration on nodes"),
            containsOutput("Configuration is repaired")));

    withTopologyService(1, activeId, topologyService -> assertFalse(topologyService.hasIncompleteChange()));
    assertThat(getUpcomingCluster("localhost", getNodePort(1, activeId)).getNodeCount(), is(equalTo(1)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, activeId)).getNodeCount(), is(equalTo(1)));
  }

  @Test
  public void test_detach_passive_offline_prepare_fail_at_active() throws Exception {
    final int activeId = findActive(1).getAsInt();
    final int passiveId = findPassives(1)[0];

    //create prepare failure on active
    String propertySettingString = "stripe.1.node." + activeId + ".tc-properties.detachStatus=prepareDeletion-failure";
    assertThat(configToolInvocation("set", "-s", "localhost:" + getNodePort(1, 1), "-c", propertySettingString), is(successful()));

    stopNode(1, passiveId);

    // detach failure (forcing detach otherwise we have to restart cluster)
    assertThat(
        configToolInvocation("-t", "5s", "detach", "-f", "-d", "localhost:" + getNodePort(1, activeId),
            "-s", "localhost:" + getNodePort(1, passiveId)),
        containsOutput("Two-Phase commit failed"));

    withTopologyService(1, activeId, topologyService -> assertTrue(topologyService.isActivated()));
    assertThat(getUpcomingCluster("localhost", getNodePort(1, activeId)).getNodeCount(), is(equalTo(2)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, activeId)).getNodeCount(), is(equalTo(2)));

    out.clearLog(1, passiveId);
    startNode(1, passiveId);

    // in availability mode, the server will restart ACTIVE with the topology it knows
    waitUntil(out.getLog(1, passiveId), containsLog("Moved to State[ PASSIVE-STANDBY ]"));
    withTopologyService(1, passiveId, topologyService -> assertTrue(topologyService.isActivated()));
    assertThat(getUpcomingCluster("localhost", getNodePort(1, passiveId)).getNodeCount(), is(equalTo(2)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, passiveId)).getNodeCount(), is(equalTo(2)));
  }

  @Test
  public void test_detach_passive_offline_commit_fail_at_active() throws Exception {
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

    assertThat(
        configToolInvocation("-r", "5s", "repair", "-f", "commit", "-s", "localhost:" + getNodePort(1, activeId)),
        allOf(
            containsOutput("Attempting an automatic repair of the configuration on nodes"),
            containsOutput("Forcing a commit"),
            containsOutput("Configuration is repaired")));

    withTopologyService(1, activeId, topologyService -> assertTrue(topologyService.isActivated()));
    assertThat(getUpcomingCluster("localhost", getNodePort(1, activeId)).getNodeCount(), is(equalTo(1)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, activeId)).getNodeCount(), is(equalTo(1)));

    out.clearLog(1, passiveId);
    startNode(1, passiveId, "-r", getNode(1, passiveId).getConfigRepo());
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
