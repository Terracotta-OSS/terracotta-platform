/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2026
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.containsOutput;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.successful;

@ClusterDefinition(nodesPerStripe = 3, autoStart = false)
public class AttachCommand1x3WithRelayIT extends DynamicConfigIT {
  @Test
  public void test_passive_attach_with_activated_relay_cluster() throws Exception {
    // activate a 1x2 cluster which contains relay node
    startNode(1, 1);
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 1)).getNodeCount(), is(equalTo(1)));

    // start a second node
    startNode(1, 2);
    assertThat(configTool("set", "-s", "localhost:" + getNodePort(1, 2), "-c", "stripe.1.node.1.relay=true", "-c", "stripe.1.node.1.replica-hostname=" + "localhost", "-c", "stripe.1.node.1.replica-port=" + "9410"), is(successful()));

    // attach
    assertThat(configTool("attach", "-d", "localhost:" + getNodePort(1, 1), "-s", "localhost:" + getNodePort(1, 2)), is(successful()));
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 2)).getNodeCount(), is(equalTo(2)));

    activateCluster();
    waitForActive(1, 1);
    waitForPassiveRelay(1, 2);
    // start node
    startNode(1, 3);
    assertThat(configTool("attach", "-d", "localhost:" + getNodePort(1, 1), "-s", "localhost:" + getNodePort(1, 3)), is(successful()));
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 2)).getNodeCount(), is(equalTo(3)));
    String expectedNomadChange = "Attaching node: node-1-3@" + getDefaultHostname(1, 3) + ":" + getNodePort(1, 3) + " to stripe";
    waitUntil(() -> configTool("log", "-s", "localhost:" + getNodePort(1, 2)), allOf(is(successful()), containsOutput(expectedNomadChange)));
  }
}

