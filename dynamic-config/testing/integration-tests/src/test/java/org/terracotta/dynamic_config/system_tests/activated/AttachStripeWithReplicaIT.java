/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2026
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

@ClusterDefinition(stripes = 2, autoStart = false)
public class AttachStripeWithReplicaIT extends DynamicConfigIT {
  @Test
  public void test_attach_stripe_to_activated_cluster_with_replica() throws Exception {
    startNode(1, 1);
    assertThat(configTool("set", "-s", "localhost:" + getNodePort(1, 1), "-c", "replica=true", "-c", "stripe.1.node.1.relay-hostname=" + "localhost", "-c", "stripe.1.node.1.relay-port=" + "9411", "-c", "stripe.1.node.1.relay-group-port=" + "9511"), is(successful()));
    activateCluster();
    waitForPassiveReplicaStart(1, 1);
    startNode(2, 1);
    assertThat(configTool("set", "-s", "localhost:" + getNodePort(2, 1), "-c", "replica=true", "-c", "stripe.1.node.1.relay-hostname=" + "localhost", "-c", "stripe.1.node.1.relay-port=" + "9412", "-c", "stripe.1.node.1.relay-group-port=" + "9512"), is(successful()));
    assertThat(configTool("attach", "-t", "stripe", "-d", "localhost:" + getNodePort(1, 1), "-s", "localhost:" + getNodePort(2, 1)), is(successful()));
    // verify the #stripes in the new topology of the cluster
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 1)).getStripeCount(), is(equalTo(2)));
    assertThat(getUpcomingCluster("localhost", getNodePort(2, 1)).getStripeCount(), is(equalTo(2)));
    waitUntil(() -> configTool("log", "-s", "localhost:" + getNodePort(1, 1)), allOf(is(successful()), containsOutput("Attaching stripe:")));
    waitUntil(() -> configTool("log", "-s", "localhost:" + getNodePort(2, 1)), allOf(is(successful()), containsOutput("Attaching stripe:")));
  }
}
