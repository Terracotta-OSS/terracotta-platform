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
public class AttachCommand2x3WithRelayIT extends DynamicConfigIT {
  @Test
  public void test_passive_attach_with_2x2_activated_relay_cluster() throws Exception {
    startNode(1, 1);
    // start the second node
    startNode(1, 2);
    assertThat(configTool("set", "-s", "localhost:" + getNodePort(1, 2), "-c", "stripe.1.node.1.relay=true", "-c", "stripe.1.node.1.replica-hostname=" + "localhost", "-c", "stripe.1.node.1.replica-port=" + "9410"), is(successful()));
    //attach the second node
    assertThat(configTool("attach", "-d", "localhost:" + getNodePort(1, 1), "-s", "localhost:" + getNodePort(1, 2)), is(successful()));

    startNode(2, 1);
    assertThat(getUpcomingCluster("localhost", getNodePort(2, 1)).getNodeCount(), is(equalTo(1)));
    startNode(2, 2);
    assertThat(configTool("set", "-s", "localhost:" + getNodePort(2, 2), "-c", "stripe.1.node.1.relay=true", "-c", "stripe.1.node.1.replica-hostname=" + "localhost", "-c", "stripe.1.node.1.replica-port=" + "9411"), is(successful()));

    assertThat(configTool("attach", "-d", "localhost:" + getNodePort(2, 1), "-s", "localhost:" + getNodePort(2, 2)), is(successful()));

    // attach the new stripe to form a 2x2 cluster
    assertThat(configTool("attach", "-t", "stripe", "-d", "localhost:" + getNodePort(1, 1), "-s", "localhost:" + getNodePort(2, 1)), is(successful()));
    //Activate cluster
    activateCluster();
    waitForActive(1);
    waitForActive(2);
    waitForPassiveRelay(1, 2);
    waitForPassiveRelay(2, 2);

    // now attach 1 node to the first stripe
    startNode(1, 3);
    assertThat(configTool("attach", "-d", "localhost:" + getNodePort(1, 1), "-s", "localhost:" + getNodePort(1, 3)), is(successful()));

    String expectedNomadChange_1_3 = "Attaching node: node-1-3@" + getDefaultHostname(1, 3) + ":" + getNodePort(1, 3) + " to stripe";
    assertThat(configTool("log", "-s", "localhost:" + getNodePort(1, 2)), allOf(is(successful()), containsOutput(expectedNomadChange_1_3)));
    assertThat(configTool("log", "-s", "localhost:" + getNodePort(2, 2)), allOf(is(successful()), containsOutput(expectedNomadChange_1_3)));

    // now attach 1 node to the second stripe
    startNode(2, 3);
    assertThat(configTool("attach", "-d", "localhost:" + getNodePort(2, 1), "-s", "localhost:" + getNodePort(2, 3)), is(successful()));

    String expectedNomadChangeFor2_3 = "Attaching node: node-2-3@" + getDefaultHostname(2, 3) + ":" + getNodePort(2, 3) + " to stripe";
    assertThat(configTool("log", "-s", "localhost:" + getNodePort(1, 2)), allOf(is(successful()), containsOutput(expectedNomadChangeFor2_3)));
    assertThat(configTool("log", "-s", "localhost:" + getNodePort(2, 2)), allOf(is(successful()), containsOutput(expectedNomadChangeFor2_3)));
  }
}
