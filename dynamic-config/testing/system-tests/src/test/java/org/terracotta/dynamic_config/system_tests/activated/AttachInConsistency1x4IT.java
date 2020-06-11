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

import org.junit.Before;
import org.junit.Test;
import org.terracotta.dynamic_config.api.model.FailoverPriority;
import org.terracotta.dynamic_config.test_support.ClusterDefinition;
import org.terracotta.dynamic_config.test_support.DynamicConfigIT;

import java.time.Duration;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.containsOutput;

@ClusterDefinition(nodesPerStripe = 4, autoActivateNodes = {3})
public class AttachInConsistency1x4IT extends DynamicConfigIT {

  public AttachInConsistency1x4IT() {
    super(Duration.ofSeconds(300));
  }

  @Override
  protected FailoverPriority getFailoverPriority() {
    return FailoverPriority.consistency();
  }

  @Test
  public void testAttachNodeFailAtPrepare() throws Exception {
    //create prepare failure on active
    invokeConfigTool("set", "-s", "localhost:" + getNodePort(1, 1), "-c", "stripe.1.node.1.tc-properties.attachStatus=prepareAddition-failure");

    startNode(1, 4);
    waitForDiagnostic(1, 4);
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 4)).getNodeCount(), is(equalTo(1)));

    // attach failure (forcing attach otherwise we have to restart cluster)
    assertThat(
        () -> invokeConfigTool("attach", "-f", "-d", "localhost:" + getNodePort(1, 1), "-s", "localhost:" + getNodePort(1, 4)),
        exceptionMatcher("Two-Phase commit failed"));

    assertThat(getUpcomingCluster("localhost", getNodePort(1, 1)).getNodeCount(), is(equalTo(3)));
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 2)).getNodeCount(), is(equalTo(3)));
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 3)).getNodeCount(), is(equalTo(3)));
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 4)).getNodeCount(), is(equalTo(1)));

    withTopologyService(1, 1, topologyService -> assertTrue(topologyService.isActivated()));
    withTopologyService(1, 2, topologyService -> assertTrue(topologyService.isActivated()));
    withTopologyService(1, 3, topologyService -> assertTrue(topologyService.isActivated()));
    withTopologyService(1, 4, topologyService -> assertFalse(topologyService.isActivated()));
  }

  @Test
  public void attachNodeFailingBecauseOfNodeGoingDownInPreparePhase() throws Exception {
    int activeId = findActive(1).getAsInt();
    int passiveId1 = findPassives(1)[0];
    int passiveId2 = findPassives(1)[1];

    startNode(1, 4);
    waitForDiagnostic(1, 4);
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 4)).getNodeCount(), is(equalTo(1)));

    //create failover in prepare phase for active
    String propertySettingString = "stripe.1.node." + activeId + ".tc-properties.failoverAddition=killAddition-prepare";
    invokeConfigTool("set", "-s", "localhost:" + getNodePort(1, 1), "-c", propertySettingString);

    assertThat(
        () -> invokeConfigTool("attach", "-f", "-d", "localhost:" + getNodePort(1, activeId), "-s", "localhost:" + getNodePort(1, 4)),
        exceptionMatcher("Two-Phase commit failed"));

    assertThat(getUpcomingCluster("localhost", getNodePort(1, passiveId1)).getNodeCount(), is(equalTo(3)));
    assertThat(getUpcomingCluster("localhost", getNodePort(1, passiveId2)).getNodeCount(), is(equalTo(3)));
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 4)).getNodeCount(), is(equalTo(1)));

    withTopologyService(1, passiveId1, topologyService -> assertTrue(topologyService.isActivated()));
    withTopologyService(1, passiveId2, topologyService -> assertTrue(topologyService.isActivated()));
    withTopologyService(1, 4, topologyService -> assertFalse(topologyService.isActivated()));

    // Ensure that earlier stopped active now restarts as passive and sync the config from current active
    startNode(1, activeId, "-r", getNode(1, activeId).getConfigRepo());
    waitForPassive(1, activeId);
    withTopologyService(1, activeId, topologyService -> assertTrue(topologyService.isActivated()));
  }

  @Test
  public void testFailoverDuringNomadCommitForPassiveAddition() throws Exception {
    int activeId = findActive(1).getAsInt();
    int passiveId1 = findPassives(1)[0];
    int passiveId2 = findPassives(1)[1];

    startNode(1, 4);
    waitForDiagnostic(1, 4);
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 4)).getNodeCount(), is(equalTo(1)));

    //setup for failover in commit phase on active
    String propertySettingString = "stripe.1.node." + activeId + ".tc-properties.failoverAddition=killAddition-commit";
    invokeConfigTool("set", "-s", "localhost:" + getNodePort(1, 1), "-c", propertySettingString);

    // attach command, and failover triggered during commit
    // this will bring down the active

    // 1. but the 2 other passives cannot decide which one will become active because they are only 2 nodes so no majority
    //    so the command will block... Until the thread has time to restart the old active (which will become passive and vote)!
    // 2. or it might be possible that one of the passive has time ot become active
    assertThat(
        () -> invokeConfigTool("-er", "40s", "attach", "-f", "-d", "localhost:" + getNodePort(1, activeId), "-s", "localhost:" + getNodePort(1, 4)),
        exceptionMatcher("Two-Phase commit failed"));

    //start the old active and verify it becomes passive
    startNode(1, activeId, "-r", getNode(1, activeId).getConfigRepo());
    waitForPassive(1, activeId);

    // in any case, we must have an activate elected now
    waitForActive(1);

    // change was not committed on the active that has crashed, but if the passive replication was done,
    // it is possible that one of the passive that became active got the change and committed.
    // repair command will be able to replay the commit if necessary
    invokeConfigTool("repair", "-f", "commit", "-s", "localhost:" + getNodePort(1, activeId));

    // all nodes of the destination cluster now have the updated topology
    assertThat(getUpcomingCluster("localhost", getNodePort(1, activeId)).getNodeCount(), is(equalTo(4)));
    assertThat(getUpcomingCluster("localhost", getNodePort(1, passiveId1)).getNodeCount(), is(equalTo(4)));
    assertThat(getUpcomingCluster("localhost", getNodePort(1, passiveId2)).getNodeCount(), is(equalTo(4)));

    // node 4 was not added
    withTopologyService(1, 4, topologyService -> assertFalse(topologyService.isActivated()));
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 4)).getNodeCount(), is(equalTo(1)));

    // we will be able to add it through a restrictive activation
    invokeConfigTool("export", "-s", "localhost:" + getNodePort(1, activeId), "-f", tmpDir.getRoot().resolve("cluster.properties").toAbsolutePath().toString());
    assertThat(
        invokeConfigTool("activate", "-R", "-s", "localhost:" + getNodePort(1, 4), "-f", tmpDir.getRoot().resolve("cluster.properties").toAbsolutePath().toString()),
        allOf(containsOutput("No license installed"), containsOutput("came back up")));

    // we finally verify that the added node became passive, activated with the right topology
    waitForPassive(1, 4);
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 4)).getNodeCount(), is(equalTo(4)));
    withTopologyService(1, 4, topologyService -> assertTrue(topologyService.isActivated()));
  }
}
