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
public class DetachStripeWithRelayIT extends DynamicConfigIT {

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

    // stripe-2
    startNode(2, 1);
    assertThat(getUpcomingCluster("localhost", getNodePort(2, 1)).getNodeCount(), is(equalTo(1)));
    startNode(2, 2);
    assertThat(configTool("set", "-s", "localhost:" + getNodePort(2, 2), "-c", "stripe.1.node.1.relay=true", "-c", "stripe.1.node.1.replica-hostname=" + "localhost", "-c", "stripe.1.node.1.replica-port=" + "9411"), is(successful()));
    assertThat(getUpcomingCluster("localhost", getNodePort(2, 2)).getNodeCount(), is(equalTo(1)));
    assertThat(configTool("attach", "-d", "localhost:" + getNodePort(2, 1), "-s", "localhost:" + getNodePort(2, 2)), is(successful()));

    // attach the two stripes to form a 2x2 cluster
    assertThat(configTool("attach", "-t", "stripe", "-d", "localhost:" + getNodePort(1, 1), "-s", "localhost:" + getNodePort(2, 1)), is(successful()));

    //Activate cluster
    activateCluster();
    waitForPassiveRelay(1, 2);
    waitForPassiveRelay(2, 2);
  }

  @Test
  public void test_detach_stripe_from_activated_cluster_with_relay() throws Exception {
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 1)).getNodeCount(), is(equalTo(4)));
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 1)).getStripeCount(), is(equalTo(2)));

    // detach second stripe from the activated 2x2 cluster to form a 1x2 cluster
    assertThat(configTool("detach", "-t", "stripe", "-d", "localhost:" + getNodePort(1, 1), "-s", "localhost:" + getNodePort(2, 1)), is(successful()));

    // verify the #nodes in the new topology of the cluster
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 1)).getNodeCount(), is(equalTo(2)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, 2)).getNodeCount(), is(equalTo(2)));

    // verify the #stripes in the new topology of the cluster
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 1)).getStripeCount(), is(equalTo(1)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, 2)).getStripeCount(), is(equalTo(1)));

    assertThat(configTool("log", "-s", "localhost:" + getNodePort(1, 2)), allOf(is(successful()), containsOutput("Detaching stripe:")));

  }
}
