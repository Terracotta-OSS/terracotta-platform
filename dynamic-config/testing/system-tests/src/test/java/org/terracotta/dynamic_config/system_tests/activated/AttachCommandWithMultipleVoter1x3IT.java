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
import org.terracotta.dynamic_config.test_support.InlineServers;
import org.terracotta.voter.ActiveVoter;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.successful;

@ClusterDefinition(nodesPerStripe = 3)
public class AttachCommandWithMultipleVoter1x3IT extends DynamicConfigIT {

  @Override
  protected FailoverPriority getFailoverPriority() {
    return FailoverPriority.consistency(2);
  }

  @Before
  public void setUp() throws Exception {
    startNode(1, 1);
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 1)).getNodeCount(), is(equalTo(1)));

    // start the second node
    startNode(1, 2);
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 2)).getNodeCount(), is(equalTo(1)));

    //attach the second node
    assertThat(configTool("attach", "-d", "localhost:" + getNodePort(1, 1), "-s", "localhost:" + getNodePort(1, 2)), is(successful()));

    //Activate cluster
    activateCluster();
  }

  @Test
  @InlineServers(false)
  public void testFailoverWhileAttachingAndVerifyWithVoter() {
    int activeId = waitForActive(1);
    int passiveId = waitForNPassives(1, 1)[0];

    try (ActiveVoter activeVoter = new ActiveVoter("fvoter", getNode(1, activeId).getHostPort(), getNode(1, passiveId).getHostPort());
         ActiveVoter secondVoter = new ActiveVoter("svoter", getNode(1, activeId).getHostPort(), getNode(1, passiveId).getHostPort())) {
      activeVoter.startAndAwaitRegistrationWithAll();
      secondVoter.startAndAwaitRegistrationWithAll();

      String propertySettingString = "stripe.1.node." + activeId + ".tc-properties.failoverAddition=killAddition-commit";
      startNode(1, 3);
      assertThat(getUpcomingCluster("localhost", getNodePort(1, 3)).getNodeCount(), is(equalTo(1)));

      //setup for failover in commit phase on active
      assertThat(configTool("set", "-s", "localhost:" + getNodePort(1, 1), "-c", propertySettingString), is(successful()));

      // the shutdown of the active might break the entity connection during a commit and cause this error:
      // Commit failed for node localhost:3908. Reason: Entity: org.terracotta.nomad.entity.client.NomadEntity:nomad-entity Connection closed under in-flight message
      // Once the failover is done and voter voted, we should be able to retry the command
      waitUntil(() -> configTool("attach", "-f", "-d", "localhost:" + getNodePort(1, passiveId), "-s", "localhost:" + getNodePort(1, 3)), is(successful()));
      waitForPassive(1, 3);

      waitUntil(activeVoter::getKnownHosts, is(3));
      waitUntil(secondVoter::getKnownHosts, is(3));

      assertThat(getUpcomingCluster("localhost", getNodePort(1, passiveId)).getNodeCount(), is(equalTo(3)));
      assertThat(getUpcomingCluster("localhost", getNodePort(1, 3)).getNodeCount(), is(equalTo(3)));

      withTopologyService(1, passiveId, topologyService -> assertTrue(topologyService.isActivated()));
      withTopologyService(1, 3, topologyService -> assertTrue(topologyService.isActivated()));
    }
  }
}
