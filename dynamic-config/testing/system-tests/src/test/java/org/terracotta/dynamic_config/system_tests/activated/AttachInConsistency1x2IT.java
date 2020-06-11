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
import org.terracotta.angela.client.support.junit.NodeOutputRule;
import org.terracotta.dynamic_config.api.model.FailoverPriority;
import org.terracotta.dynamic_config.test_support.ClusterDefinition;
import org.terracotta.dynamic_config.test_support.DynamicConfigIT;

import java.time.Duration;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.containsLog;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.containsOutput;

@ClusterDefinition(nodesPerStripe = 2, autoActivateNodes = {1})
public class AttachInConsistency1x2IT extends DynamicConfigIT {
  @Rule
  public final NodeOutputRule out = new NodeOutputRule();

  public AttachInConsistency1x2IT() {
    super(Duration.ofSeconds(180));
  }

  @Override
  protected FailoverPriority getFailoverPriority() {
    return FailoverPriority.consistency();
  }

  @Test
  public void test_attach_to_activated_cluster() throws Exception {
    // start a second node
    startNode(1, 2);
    waitForDiagnostic(1, 2);
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 2)).getNodeCount(), is(equalTo(1)));

    // attach
    invokeConfigTool("attach", "-d", "localhost:" + getNodePort(1, 1), "-s", "localhost:" + getNodePort(1, 2));
    waitForPassive(1, 2);

    assertThat(getUpcomingCluster("localhost", getNodePort(1, 1)).getNodeCount(), is(equalTo(2)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, 1)).getNodeCount(), is(equalTo(2)));

    assertThat(getUpcomingCluster("localhost", getNodePort(1, 2)).getNodeCount(), is(equalTo(2)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, 2)).getNodeCount(), is(equalTo(2)));

    withTopologyService(1, 1, topologyService -> assertTrue(topologyService.isActivated()));
    withTopologyService(1, 2, topologyService -> assertTrue(topologyService.isActivated()));

    out.clearLog(1, 2);
    stopNode(1, 1);
    waitUntil(out.getLog(1, 2), containsLog("Not enough registered voters.  Require override intervention or 1 members of the stripe to be connected for action MOVE_TO_ACTIVE"));
  }

  @Test
  public void test_attach_to_activated_cluster_requiring_restart() throws Exception {
    String destination = "localhost:" + getNodePort();

    // do a change requiring a restart
    assertThat(
        invokeConfigTool("set", "-s", destination, "-c", "stripe.1.node.1.tc-properties.foo=bar"),
        containsOutput("IMPORTANT: A restart of the cluster is required to apply the changes"));

    // start a second node
    startNode(1, 2);
    waitForDiagnostic(1, 2);

    // try forcing the attach
    invokeConfigTool("attach", "-f", "-d", destination, "-s", "localhost:" + getNodePort(1, 2));
    waitForPassive(1, 2);

    assertThat(getUpcomingCluster("localhost", getNodePort(1, 1)).getNodeCount(), is(equalTo(2)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, 1)).getNodeCount(), is(equalTo(2)));

    assertThat(getUpcomingCluster("localhost", getNodePort(1, 2)).getNodeCount(), is(equalTo(2)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, 2)).getNodeCount(), is(equalTo(2)));

    withTopologyService(1, 1, topologyService -> assertTrue(topologyService.isActivated()));
    withTopologyService(1, 2, topologyService -> assertTrue(topologyService.isActivated()));
  }
}
