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
package org.terracotta.dynamic_config.system_tests.diagnostic;

import org.junit.Test;
import org.terracotta.dynamic_config.test_support.ClusterDefinition;
import org.terracotta.dynamic_config.test_support.DynamicConfigIT;

import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.containsOutput;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.successful;

/**
 * @author Mathieu Carbou
 */
@ClusterDefinition(stripes = 2, nodesPerStripe = 2, failoverPriority = "")
public class AttachCommand2x2IT extends DynamicConfigIT {

  @Test
  public void test_attach_stripe() throws Exception {
    // create a 1x2
    assertThat(configTool("attach", "-d", "localhost:" + getNodePort(1, 1), "-s", "localhost:" + getNodePort(1, 2)), is(successful()));

    assertThat(getUpcomingCluster("localhost", getNodePort(1, 1)).getNodeCount(), is(equalTo(2)));
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 2)).getNodeCount(), is(equalTo(2)));

    // create a 1x2
    assertThat(configTool("attach", "-d", "localhost:" + getNodePort(2, 1), "-s", "localhost:" + getNodePort(2, 2)), is(successful()));

    assertThat(getUpcomingCluster("localhost", getNodePort(2, 1)).getNodeCount(), is(equalTo(2)));
    assertThat(getUpcomingCluster("localhost", getNodePort(2, 2)).getNodeCount(), is(equalTo(2)));

    assertThat(configTool("attach", "-t", "stripe", "-d", "localhost:" + getNodePort(1, 1), "-s", "localhost:" + getNodePort(2, 1)), is(successful()));

    assertThat(getUpcomingCluster("localhost", getNodePort(1, 1)).getNodeCount(), is(equalTo(4)));
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 2)).getNodeCount(), is(equalTo(4)));
    assertThat(getUpcomingCluster("localhost", getNodePort(2, 1)).getNodeCount(), is(equalTo(4)));
    assertThat(getUpcomingCluster("localhost", getNodePort(2, 2)).getNodeCount(), is(equalTo(4)));
  }

  @Test
  public void test_attach_node() throws Exception {
    // create a 2x1
    assertThat(configTool("attach", "-t", "stripe", "-d", "localhost:" + getNodePort(1, 1), "-s", "localhost:" + getNodePort(1, 2)), is(successful()));

    assertThat(getUpcomingCluster("localhost", getNodePort(1, 1)).getNodeCount(), is(equalTo(2)));
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 1)).getStripeCount(), is(equalTo(2)));

    assertThat(getUpcomingCluster("localhost", getNodePort(1, 2)).getNodeCount(), is(equalTo(2)));
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 2)).getStripeCount(), is(equalTo(2)));

    // attach node to stripe 1
    assertThat(configTool("attach", "-d", "localhost:" + getNodePort(1, 1), "-s", "localhost:" + getNodePort(2, 1)), is(successful()));

    assertThat(getUpcomingCluster("localhost", getNodePort(1, 1)).getNodeCount(), is(equalTo(3)));
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 1)).getStripeCount(), is(equalTo(2)));

    assertThat(getUpcomingCluster("localhost", getNodePort(1, 2)).getNodeCount(), is(equalTo(3)));
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 2)).getStripeCount(), is(equalTo(2)));

    assertThat(getUpcomingCluster("localhost", getNodePort(2, 1)).getNodeCount(), is(equalTo(3)));
    assertThat(getUpcomingCluster("localhost", getNodePort(2, 1)).getStripeCount(), is(equalTo(2)));
  }

  @Test
  public void test_attach_stripe_shape() {
    assertThat(configTool("attach", "-to-cluster", getNodeHostPort(1, 1).toString(), "-stripe-shape", getNodeHostPort(2, 1) + "|" + getNodeHostPort(2, 2)), is(successful()));

    Stream.of(getNodePort(1, 1), getNodePort(2, 1), getNodePort(2, 2)).forEach(port -> {
      assertThat(getUpcomingCluster("localhost", port).getNodeCount(), is(equalTo(3)));
      assertThat(getUpcomingCluster("localhost", port).getStripeCount(), is(equalTo(2)));
    });
  }

  @Test
  public void test_attach_stripe_shape_named() {
    assertThat(configTool("attach", "-to-cluster", getNodeHostPort(1, 1).toString(), "-stripe-shape", "stripe2/" + getNodeHostPort(2, 1) + "|" + getNodeHostPort(2, 2)), is(successful()));

    Stream.of(getNodePort(1, 1), getNodePort(2, 1), getNodePort(2, 2)).forEach(port -> {
      assertThat(getUpcomingCluster("localhost", port).getNodeCount(), is(equalTo(3)));
      assertThat(getUpcomingCluster("localhost", port).getStripeCount(), is(equalTo(2)));
      assertThat(getUpcomingCluster("localhost", port).getStripes().get(1).getName(), is(equalTo("stripe2")));
    });
  }

  @Test
  public void test_attach_invalid_replica_with_replica() {
    stopNode(1, 1);
    waitForStopped(1, 1);
    startNode(1, 1, getNewOptions(getNode(1, 1),
      "-replica-mode", "true", "-relay-hostname", "localhost", "-relay-port", "9410", "-relay-group-port", "9430"));

    stopNode(2, 1);
    waitForStopped(2, 1);
    startNode(2, 1, getNewOptions(getNode(2, 1),
      "-replica-mode", "true", "-relay-hostname", "localhost", "-relay-port", "9410", "-relay-group-port", "9430"));

    assertThat(configTool("attach", "-f", "-d", "localhost:" + getNodePort(1, 1), "-s", "localhost:" + getNodePort(2, 1)),
      allOf(not(successful()), containsOutput("Only a single node can have replica-mode enabled. Nodes with replica-mode: [node-1-1, node-2-1]")));
  }

  @Test
  public void test_attach_invalid_replica_with_relay() {
    startNode(1, 1);
    // start in replica-mode
    stopNode(2, 1);
    waitForStopped(2, 1);
    startNode(2, 1, getNewOptions(getNode(2, 1),
      "-replica-mode", "true", "-relay-hostname", "localhost", "-relay-port", "9410", "-relay-group-port", "9430"));

    assertThat(configTool("set", "-s", "localhost:" + getNodePort(1, 1),
      "-c", "stripe.1.node.1.relay-mode=true",
      "-c", "stripe.1.node.1.replica-hostname=localhost",
      "-c", "stripe.1.node.1.replica-port=9410"), is(successful()));

    assertThat(configTool("attach", "-f", "-d", "localhost:" + getNodePort(1, 1), "-s", "localhost:" + getNodePort(2, 1)),
      allOf(not(successful()), containsOutput("Node with name: node-2-1 has replica-mode enabled and cannot coexist with other nodes with names: [node-1-1]")));
  }

  @Test
  public void test_attach_invalid_replica_with_normal_node() {
    startNode(1, 1);
    // start in replica-mode
    stopNode(2, 1);
    waitForStopped(2, 1);
    startNode(2, 1, getNewOptions(getNode(2, 1),
      "-replica-mode", "true", "-relay-hostname", "localhost", "-relay-port", "9410", "-relay-group-port", "9430"));

    assertThat(configTool("attach", "-f", "-d", "localhost:" + getNodePort(1, 1), "-s", "localhost:" + getNodePort(2, 1)),
      allOf(not(successful()), containsOutput("Node with name: node-2-1 has replica-mode enabled and cannot coexist with other nodes with names: [node-1-1]")));
  }

  @Test
  public void test_attach_multiple_relay_nodes() {
    stopNode(1, 2);
    waitForStopped(1, 2);
    stopNode(2, 2);
    waitForStopped(2, 2);

    startNode(1, 2, getNewOptions(getNode(1, 2), "-relay-mode", "true", "-replica-hostname", "localhost", "-replica-port", "9410"));
    startNode(2, 2, getNewOptions(getNode(2, 2), "-relay-mode", "true", "-replica-hostname", "localhost", "-replica-port", "9410"));

    // create a 1x2 with relay-mode
    assertThat(configTool("attach", "-d", "localhost:" + getNodePort(1, 1), "-s", "localhost:" + getNodePort(1, 2)), is(successful()));
    // create a 1x2 with relay-mode
    assertThat(configTool("attach", "-d", "localhost:" + getNodePort(2, 1), "-s", "localhost:" + getNodePort(2, 2)), is(successful()));
    assertThat(configTool("attach", "-t", "stripe", "-d", "localhost:" + getNodePort(1, 1), "-s", "localhost:" + getNodePort(2, 1)), is(successful()));

    assertThat(getUpcomingCluster("localhost", getNodePort(1, 2)).getNodeCount(), is(equalTo(4)));
    assertThat(getUpcomingCluster("localhost", getNodePort(2, 2)).getNodeCount(), is(equalTo(4)));

    assertThat(configTool("export", "-s", "localhost:" + getNodePort(1, 1), "-t", "properties"),
      allOf(
        successful(),
        containsOutput("stripe.1.node.2.relay-mode=true"), containsOutput("stripe.2.node.2.relay-mode=true")));
  }
}
