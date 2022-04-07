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
import org.terracotta.dynamic_config.test_support.ClusterDefinition;
import org.terracotta.dynamic_config.test_support.DynamicConfigIT;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.containsOutput;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.successful;

@ClusterDefinition(nodesPerStripe = 2, autoStart = false)
public class AttachCommandFail1x1IT extends DynamicConfigIT {

  @Test
  public void attachFailAtPrepareOnlyActiveInStripe() throws Exception {
    // activate a 1x1 cluster
    startNode(1, 1);
    activateCluster();
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 1)).getNodeCount(), is(equalTo(1)));

    // start a second node
    startNode(1, 2);
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 2)).getNodeCount(), is(equalTo(1)));

    //create prepare failure
    assertThat(configTool("set", "-s", "localhost:" + getNodePort(1, 1), "-c", "stripe.1.node.1.tc-properties.attachStatus=prepareAddition-failure"), is(successful()));

    // attach failure (forcing attach otherwise we have to restart cluster)
    assertThat(
        configTool("attach", "-f", "-d", "localhost:" + getNodePort(1, 1), "-s", "localhost:" + getNodePort(1, 2)),
        containsOutput("Two-Phase commit failed"));
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 1)).getNodeCount(), is(equalTo(1)));
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 2)).getNodeCount(), is(equalTo(1)));

    withTopologyService(1, 1, topologyService -> assertTrue(topologyService.isActivated()));
    withTopologyService(1, 2, topologyService -> assertFalse(topologyService.isActivated()));
  }

}
