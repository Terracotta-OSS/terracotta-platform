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
import org.terracotta.dynamic_config.test_support.DcActiveVoter;
import org.terracotta.dynamic_config.test_support.DynamicConfigIT;
import org.terracotta.dynamic_config.test_support.InlineServers;

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

    try (DcActiveVoter activeVoter = new DcActiveVoter("fvoter", getNode(1, activeId).getHostPort(), getNode(1, passiveId).getHostPort());
         DcActiveVoter secondVoter = new DcActiveVoter("svoter", getNode(1, activeId).getHostPort(), getNode(1, passiveId).getHostPort())) {
      activeVoter.startAndAwaitRegistration();
      secondVoter.startAndAwaitRegistration();

      String propertySettingString = "stripe.1.node." + activeId + ".tc-properties.failoverAddition=killAddition-commit";
      startNode(1, 3);
      assertThat(getUpcomingCluster("localhost", getNodePort(1, 3)).getNodeCount(), is(equalTo(1)));

      //setup for failover in commit phase on active
      assertThat(configTool("set", "-s", "localhost:" + getNodePort(1, 1), "-c", propertySettingString), is(successful()));

      assertThat(configTool("attach", "-f", "-d", "localhost:" + getNodePort(1, activeId), "-s", "localhost:" + getNodePort(1, 3)), is(successful()));
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
