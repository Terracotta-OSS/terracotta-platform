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
import org.terracotta.dynamic_config.test_support.ClusterDefinition;
import org.terracotta.dynamic_config.test_support.DynamicConfigIT;

import java.time.Duration;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author Mathieu Carbou
 */
@ClusterDefinition(nodesPerStripe = 3, autoActivateNodes = {2})
public class AttachCommand1x3IT extends DynamicConfigIT {

  public AttachCommand1x3IT() {
    super(Duration.ofSeconds(180));
  }

  @Test
  public void test_attach_to_activated_cluster_with_offline_node() throws Exception {
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 1)).getNodeCount(), is(equalTo(2)));
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 2)).getNodeCount(), is(equalTo(2)));

    int activeId = findActive(1).getAsInt(); // either 1 or 2
    int passiveId = findPassives(1)[0]; // either 1 or 2

    stopNode(1, passiveId);

    // start a third node
    startNode(1, 3);
    waitForDiagnostic(1, 3);
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 3)).getNodeCount(), is(equalTo(1)));

    // attach
    invokeConfigTool("attach", "-d", "localhost:" + getNodePort(1, activeId), "-s", "localhost:" + getNodePort(1, 3));
    waitForPassive(1, 3);

    // verify that the active node topology has 3 nodes
    assertThat(getUpcomingCluster("localhost", getNodePort(1, activeId)).getNodeCount(), is(equalTo(3)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, activeId)).getNodeCount(), is(equalTo(3)));

    // verify that the added node topology has 3 nodes
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 3)).getNodeCount(), is(equalTo(3)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, 3)).getNodeCount(), is(equalTo(3)));

    // then try to start the passive that was down
    startNode(1, passiveId);
    waitForPassive(1, passiveId);

    // verify that the restarted passive topology has 3 nodes
    assertThat(getUpcomingCluster("localhost", getNodePort(1, passiveId)).getNodeCount(), is(equalTo(3)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, passiveId)).getNodeCount(), is(equalTo(3)));
  }

  @Test
  public void attachNodeFailAtPrepare() throws Exception {
    //create prepare failure 
    invokeConfigTool("set", "-s", "localhost:" + getNodePort(1, 1), "-c", "stripe.1.node.1.tc-properties.attachStatus=prepareAddition-failure");

    startNode(1, 3);
    waitForDiagnostic(1, 3);
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 3)).getNodeCount(), is(equalTo(1)));

    // attach failure (forcing attach otherwise we have to restart cluster)
    assertThat(
        () -> invokeConfigTool("attach", "-f", "-d", "localhost:" + getNodePort(1, 1), "-s", "localhost:" + getNodePort(1, 3)),
        exceptionMatcher("Two-Phase commit failed"));

    assertThat(getUpcomingCluster("localhost", getNodePort(1, 1)).getNodeCount(), is(equalTo(2)));
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 2)).getNodeCount(), is(equalTo(2)));
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 3)).getNodeCount(), is(equalTo(1)));

    withTopologyService(1, 1, topologyService -> assertTrue(topologyService.isActivated()));
    withTopologyService(1, 2, topologyService -> assertTrue(topologyService.isActivated()));
    withTopologyService(1, 3, topologyService -> assertFalse(topologyService.isActivated()));
  }

  @Test
  public void attachNodeFailingBecauseOfNodeGoingDownInPreparePhase() throws Exception {
    int activeId = findActive(1).getAsInt();
    int passiveId = findPassives(1)[0];

    startNode(1, 3);
    waitForDiagnostic(1, 3);
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 3)).getNodeCount(), is(equalTo(1)));

    //create failover in prepare phase for active
    String propertySettingString = "stripe.1.node." + activeId + ".tc-properties.failoverAddition=killAddition-prepare";
    invokeConfigTool("set", "-s", "localhost:" + getNodePort(1, 1), "-c", propertySettingString);

    assertThat(
        () -> invokeConfigTool("attach", "-f", "-d", "localhost:" + getNodePort(1, activeId), "-s", "localhost:" + getNodePort(1, 3)),
        exceptionMatcher("Two-Phase commit failed"));

    assertThat(getUpcomingCluster("localhost", getNodePort(1, passiveId)).getNodeCount(), is(equalTo(2)));
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 3)).getNodeCount(), is(equalTo(1)));

    withTopologyService(1, passiveId, topologyService -> assertTrue(topologyService.isActivated()));
    withTopologyService(1, 3, topologyService -> assertFalse(topologyService.isActivated()));
  }

  @Test
  public void testFailoverDuringNomadCommitForPassiveAddition() throws Exception {
    int activeId = findActive(1).getAsInt();
    int passiveId = findPassives(1)[0];
    String propertySettingString = "stripe.1.node." + activeId + ".tc-properties.failoverAddition=killAddition-commit";

    startNode(1, 3);
    waitForDiagnostic(1, 3);
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 3)).getNodeCount(), is(equalTo(1)));

    //setup for failover in commit phase on active
    invokeConfigTool("set", "-s", "localhost:" + getNodePort(1, 1), "-c", propertySettingString);
    invokeConfigTool("attach", "-f", "-d", "localhost:" + getNodePort(1, activeId), "-s", "localhost:" + getNodePort(1, 3));

    waitForPassive(1, 3);
    assertThat(getUpcomingCluster("localhost", getNodePort(1, passiveId)).getNodeCount(), is(equalTo(3)));
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 3)).getNodeCount(), is(equalTo(3)));

    withTopologyService(1, passiveId, topologyService -> assertTrue(topologyService.isActivated()));
    withTopologyService(1, 3, topologyService -> assertTrue(topologyService.isActivated()));
  }

}
