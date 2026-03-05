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

@ClusterDefinition(stripes = 2, nodesPerStripe = 3, autoStart = false)
public class DetachCommand2x3WithRelayIT extends DynamicConfigIT {

  @Test
  public void test_detach_passive_with_2x3_activated_relay_cluster() throws Exception {
    startNode(1, 1);
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 1)).getNodeCount(), is(equalTo(1)));

    startNode(1, 2);
    assertThat(configTool("set", "-s", "localhost:" + getNodePort(1, 2), "-c", "stripe.1.node.1.relay-mode=true", "-c", "stripe.1.node.1.replica-hostname=" + "localhost", "-c", "stripe.1.node.1.replica-port=" + "9410"), is(successful()));

    assertThat(configTool("attach", "-d", "localhost:" + getNodePort(1, 1), "-s", "localhost:" + getNodePort(1, 2)), is(successful()));
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 2)).getNodeCount(), is(equalTo(2)));

    startNode(1, 3);
    assertThat(configTool("attach", "-d", "localhost:" + getNodePort(1, 1), "-s", "localhost:" + getNodePort(1, 3)), is(successful()));
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 3)).getNodeCount(), is(equalTo(3)));

    startNode(2, 1);
    assertThat(getUpcomingCluster("localhost", getNodePort(2, 1)).getNodeCount(), is(equalTo(1)));

    startNode(2, 2);
    assertThat(configTool("set", "-s", "localhost:" + getNodePort(2, 2), "-c", "stripe.1.node.1.relay-mode=true", "-c", "stripe.1.node.1.replica-hostname=" + "localhost", "-c", "stripe.1.node.1.replica-port=" + "9410"), is(successful()));

    assertThat(configTool("attach", "-d", "localhost:" + getNodePort(2, 1), "-s", "localhost:" + getNodePort(2, 2)), is(successful()));
    assertThat(getUpcomingCluster("localhost", getNodePort(2, 2)).getNodeCount(), is(equalTo(2)));

    startNode(2, 3);
    assertThat(configTool("attach", "-d", "localhost:" + getNodePort(2, 1), "-s", "localhost:" + getNodePort(2, 3)), is(successful()));
    assertThat(getUpcomingCluster("localhost", getNodePort(2, 3)).getNodeCount(), is(equalTo(3)));

    // attach the two stripes to form a 2x3 cluster
    assertThat(configTool("attach", "-t", "stripe", "-d", "localhost:" + getNodePort(1, 1), "-s", "localhost:" + getNodePort(2, 1)), is(successful()));
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 1)).getNodeCount(), is(equalTo(6)));

    //Activate cluster
    activateCluster();
    final int firstStripePassiveId = waitForNPassives(1, 1)[0];
    waitForPassiveRelay(1, 2);
    final int secondStripePassiveId = waitForNPassives(2, 1)[0];
    waitForPassiveRelay(2, 2);
    stopNode(1, firstStripePassiveId);
    waitUntil(() -> angela.tsa().getStopped().size(), is(1));
    assertThat(configTool("detach", "-d", "localhost:" + getNodePort(1, 2), "-s", "localhost:" + getNodePort(1, firstStripePassiveId)), is(successful()));

    String expectedNomadChangeFirstStripe = "Detaching node: node-1-" + firstStripePassiveId +" from stripe";
    assertThat(configTool("log", "-s", "localhost:" + getNodePort(1, 2)), allOf(is(successful()), containsOutput(expectedNomadChangeFirstStripe)));
    assertThat(configTool("log", "-s", "localhost:" + getNodePort(2, 2)), allOf(is(successful()), containsOutput(expectedNomadChangeFirstStripe)));

    stopNode(2, secondStripePassiveId);
    waitUntil(() -> angela.tsa().getStopped().size(), is(2));
    assertThat(configTool("detach", "-d", "localhost:" + getNodePort(2, 2), "-s", "localhost:" + getNodePort(2, secondStripePassiveId)), is(successful()));

    String expectedNomadChangeSecondStripe = "Detaching node: node-2-" + secondStripePassiveId +" from stripe";
    assertThat(configTool("log", "-s", "localhost:" + getNodePort(1, 2)), allOf(is(successful()), containsOutput(expectedNomadChangeSecondStripe)));
    assertThat(configTool("log", "-s", "localhost:" + getNodePort(2, 2)), allOf(is(successful()), containsOutput(expectedNomadChangeSecondStripe)));

  }
}
