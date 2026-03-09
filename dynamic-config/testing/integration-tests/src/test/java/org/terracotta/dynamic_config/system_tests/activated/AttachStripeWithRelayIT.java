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

import org.junit.Before;
import org.junit.Test;
import org.terracotta.dynamic_config.test_support.ClusterDefinition;
import org.terracotta.dynamic_config.test_support.DynamicConfigIT;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.containsOutput;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.successful;

@ClusterDefinition(stripes = 2, nodesPerStripe = 2, autoStart = false)
public class AttachStripeWithRelayIT extends DynamicConfigIT {
  @Before
  public void setup() throws Exception {
    startNode(1, 1);
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 1)).getNodeCount(), is(equalTo(1)));

    // start the second node
    startNode(1, 2);
    assertThat(configTool("set", "-s", "localhost:" + getNodePort(1, 2), "-c", "stripe.1.node.1.relay=true", "-c", "stripe.1.node.1.replica-hostname=" + "localhost", "-c", "stripe.1.node.1.replica-port=" + "9410"), is(successful()));
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 2)).getNodeCount(), is(equalTo(1)));

    //attach the second node
    assertThat(configTool("attach", "-d", "localhost:" + getNodePort(1, 1), "-s", "localhost:" + getNodePort(1, 2)), is(successful()));

    //Activate cluster
    activateCluster();
    waitForActive(1);
    waitForPassiveRelay(1, 2);
  }

  @Test
  public void test_attach_stripe_to_activated_cluster_with_relay() throws Exception {
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 1)).getNodeCount(), is(equalTo(2)));
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 2)).getNodeCount(), is(equalTo(2)));
    // start a 2 node stripe
    startNode(2, 1);
    assertThat(getUpcomingCluster("localhost", getNodePort(2, 1)).getNodeCount(), is(equalTo(1)));
    startNode(2, 2);
    assertThat(configTool("set", "-s", "localhost:" + getNodePort(2, 2), "-c", "stripe.1.node.1.relay=true", "-c", "stripe.1.node.1.replica-hostname=" + "localhost", "-c", "stripe.1.node.1.replica-port=" + "9411"), is(successful()));
    assertThat(getUpcomingCluster("localhost", getNodePort(2, 2)).getNodeCount(), is(equalTo(1)));
    assertThat(configTool("attach", "-d", "localhost:" + getNodePort(2, 1), "-s", "localhost:" + getNodePort(2, 2)), is(successful()));

    // attach the new stripe to the activated 1x2 cluster to form a 2x2 cluster
    assertThat(configTool("attach", "-t", "stripe", "-d", "localhost:" + getNodePort(1, 1), "-s", "localhost:" + getNodePort(2, 1)), is(successful()));
    waitForPassiveRelay(2, 2);

    waitUntil(() -> configTool("log", "-s", "localhost:" + getNodePort(1, 2)), allOf(is(successful()), containsOutput("Attaching stripe:")));
    waitUntil(() -> configTool("log", "-s", "localhost:" + getNodePort(2, 2)), allOf(is(successful()), containsOutput("Attaching stripe:")));
  }
}
