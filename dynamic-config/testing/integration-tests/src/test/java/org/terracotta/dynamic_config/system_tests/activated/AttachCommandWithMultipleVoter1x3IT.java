/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.terracotta.dynamic_config.api.model.FailoverPriority;
import org.terracotta.dynamic_config.test_support.ClusterDefinition;
import org.terracotta.dynamic_config.test_support.DynamicConfigIT;
import org.terracotta.dynamic_config.test_support.InlineServers;
import org.terracotta.voter.VotingGroup;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertTrue;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.successful;

@ClusterDefinition(nodesPerStripe = 3)
public class AttachCommandWithMultipleVoter1x3IT extends DynamicConfigIT {

  private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(AttachCommandWithMultipleVoter1x3IT.class);

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
  public void testFailoverWhileAttachingAndVerifyWithVoter() throws InterruptedException {
    int activeId = waitForActive(1);
    int passiveId = waitForNPassives(1, 1)[0];

    try (VotingGroup activeVoter = new VotingGroup("fvoter", getNode(1, activeId).getHostPort(), getNode(1, passiveId).getHostPort());
         VotingGroup secondVoter = new VotingGroup("svoter", getNode(1, activeId).getHostPort(), getNode(1, passiveId).getHostPort())) {
      activeVoter.start().awaitRegistrationWithAll();
      secondVoter.start().awaitRegistrationWithAll();

      Assert.assertEquals(2, activeVoter.countConnectedServers());
      Assert.assertEquals(2, secondVoter.countConnectedServers());

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
      activeVoter.forceTopologyUpdate().join();
      secondVoter.forceTopologyUpdate().join();

      waitUntil(activeVoter::countConnectedServers, is(2));
      waitUntil(secondVoter::countConnectedServers, is(2));

      assertThat(getUpcomingCluster("localhost", getNodePort(1, passiveId)).getNodeCount(), is(equalTo(3)));
      assertThat(getUpcomingCluster("localhost", getNodePort(1, 3)).getNodeCount(), is(equalTo(3)));

      withTopologyService(1, passiveId, topologyService -> assertTrue(topologyService.isActivated()));
      withTopologyService(1, 3, topologyService -> assertTrue(topologyService.isActivated()));
    } catch (InterruptedException ie) {
      LOGGER.warn("test failed by interrupt", ie);
      throw ie;
    }
  }
}
