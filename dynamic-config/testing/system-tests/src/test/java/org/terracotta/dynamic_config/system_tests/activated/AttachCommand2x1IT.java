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
import static org.junit.Assert.assertThat;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.containsOutput;

/**
 * @author Mathieu Carbou
 */
@ClusterDefinition(stripes = 2, nodesPerStripe = 1, autoStart = false)
public class AttachCommand2x1IT extends DynamicConfigIT {

  @Override
  protected String getNodeName(int stripeId, int nodeId) {
    return "foo";
  }

  @Test
  public void test_prevent_duplicate_name_during_attach() throws Exception {
    // activate a 1x1 cluster
    getNode(1, 1).configRepo("config-1-1").logs("logs-1-1");
    getNode(2, 1).configRepo("config-2-1").logs("logs-2-1");
    startNode(1, 1);
    waitForDiagnostic(1, 1);
    activateCluster();
    withTopologyService(1, 1, topologyService -> {
      assertThat(topologyService.getUpcomingNodeContext().getCluster().getNodeCount(), is(equalTo(1)));
      assertThat(topologyService.getUpcomingNodeContext().getCluster().getSingleNode().get().getName(), is(equalTo("foo")));
    });

    // start a second node with same name
    startNode(2, 1);
    waitForDiagnostic(2, 1);
    withTopologyService(2, 1, topologyService -> {
      assertThat(topologyService.getUpcomingNodeContext().getCluster().getNodeCount(), is(equalTo(1)));
      assertThat(topologyService.getUpcomingNodeContext().getCluster().getSingleNode().get().getName(), is(equalTo("foo")));
    });

    // attach
    assertThat(
        configTool("attach", "-f", "-d", "localhost:" + getNodePort(1, 1), "-s", "localhost:" + getNodePort(2, 1)),
        containsOutput("Found duplicate node name: foo"));
  }
}
