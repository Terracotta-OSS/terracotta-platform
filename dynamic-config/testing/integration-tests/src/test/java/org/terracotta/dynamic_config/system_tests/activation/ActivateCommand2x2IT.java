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
package org.terracotta.dynamic_config.system_tests.activation;

import org.junit.Test;
import org.terracotta.angela.common.ToolExecutionResult;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.test_support.ClusterDefinition;
import org.terracotta.dynamic_config.test_support.DynamicConfigIT;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertTrue;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.containsOutput;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.successful;

@ClusterDefinition(stripes = 2, nodesPerStripe = 2)
public class ActivateCommand2x2IT extends DynamicConfigIT {

  @Test
  public void test_fast_activation_2x2() {
    assertThat(
        configTool("activate", "-cluster-name", "my-cluster",
            "-stripe-shape", getNodeHostPort(1, 1) + "|" + getNodeHostPort(1, 2),
            "-stripe-shape", getNodeHostPort(2, 1) + "|" + getNodeHostPort(2, 2)),
        allOf(successful(), containsOutput("No license specified for activation"), containsOutput("came back up")));

    waitForActive(1);
    waitForActive(2);
    waitForPassives(1);
    waitForPassives(2);

    withTopologyService("localhost", getNodePort(), topologyService -> {
      Cluster cluster = topologyService.getRuntimeNodeContext().getCluster();
      assertThat(cluster.getName(), is(equalTo("my-cluster")));
      assertThat(cluster.getStripeCount(), is(equalTo(2)));
      assertThat(cluster.getNodeCount(), is(equalTo(4)));
    });
  }

  @Test
  public void test_fast_activation_2x2_comma() {
    assertThat(
        configTool("activate", "-cluster-name", "my-cluster", "-stripe-shape", getNodeHostPort(1, 1) + "|" + getNodeHostPort(1, 2) + "," + getNodeHostPort(2, 1) + "|" + getNodeHostPort(2, 2)),
        allOf(successful(), containsOutput("No license specified for activation"), containsOutput("came back up")));

    waitForActive(1);
    waitForActive(2);
    waitForPassives(1);
    waitForPassives(2);

    withTopologyService("localhost", getNodePort(), topologyService -> {
      Cluster cluster = topologyService.getRuntimeNodeContext().getCluster();
      assertThat(cluster.getName(), is(equalTo("my-cluster")));
      assertThat(cluster.getStripeCount(), is(equalTo(2)));
      assertThat(cluster.getNodeCount(), is(equalTo(4)));
    });
  }

  @Test
  public void test_fast_activation_2x2_with_stripe_name() {
    assertThat(
        configTool("activate", "-cluster-name", "my-cluster",
            "-stripe-shape", "foo/" + getNodeHostPort(1, 1) + "|" + getNodeHostPort(1, 2),
            "-stripe-shape", "bar/" + getNodeHostPort(2, 1) + "|" + getNodeHostPort(2, 2)),
        allOf(successful(), containsOutput("No license specified for activation"), containsOutput("came back up")));

    waitForActive(1);
    waitForActive(2);
    waitForPassives(1);
    waitForPassives(2);

    withTopologyService("localhost", getNodePort(), topologyService -> {
      Cluster cluster = topologyService.getRuntimeNodeContext().getCluster();
      assertThat(cluster.getName(), is(equalTo("my-cluster")));
      assertThat(cluster.getStripeCount(), is(equalTo(2)));
      assertThat(cluster.getNodeCount(), is(equalTo(4)));
      assertTrue(cluster.getStripeByName("foo").isPresent());
      assertTrue(cluster.getStripeByName("bar").isPresent());
    });
  }

  @Test
  public void test_fast_activation_2x2_with_duplicate_stripe_name() {
    assertThat(
        configTool("activate", "-cluster-name", "my-cluster",
            "-stripe-shape", "foo/" + getNodeHostPort(1, 1) + "|" + getNodeHostPort(1, 2),
            "-stripe-shape", "foo/" + getNodeHostPort(2, 1) + "|" + getNodeHostPort(2, 2)),
        allOf(not(successful()), containsOutput("Found duplicate stripe name: foo")));
  }

  @Test
  public void test_replica_activation_without_relay_link() {
    stopNode(1, 2);
    waitForStopped(1, 2);
    startNode(1, 2, getNewOptions(getNode(1, 2),
      "-replica", "true", "-relay-hostname", "localhost", "-relay-port", "9410", "-relay-group-port", "9430"));
    waitForPassiveReplicaStart(1, 2);

    stopNode(2, 2);
    waitForStopped(2, 2);
    startNode(2, 2, getNewOptions(getNode(2, 2),
      "-replica", "true", "-relay-hostname", "localhost", "-relay-port", "9410", "-relay-group-port", "9430"));
    waitForPassiveReplicaStart(2, 2);

    String config = copyConfigProperty("/config-property-files/multi-stripe_multi-node.properties").toString();
    assertThat(configTool("activate", "-cluster-name", "my-cluster", "-config-file", config), is(successful()));
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 2)).getNodeCount(), is(equalTo(4)));

    waitForActive(1);
    waitForPassives(1);
    waitForActive(2);
    waitForPassives(2);
  }

  @Test
  public void test_failed_activation_missing_replica_in_one_stripe() {
    // stripe 2 has a replica node, stripe 1 has none
    stopNode(2, 2);
    waitForStopped(2, 2);
    startNode(2, 2, getNewOptions(getNode(2, 2),
      "-replica", "true", "-relay-hostname", "localhost", "-relay-port", "9410", "-relay-group-port", "9430"));
    waitForPassiveReplicaStart(2, 2);

    String config = copyConfigProperty("/config-property-files/multi-stripe_multi-node.properties").toString();

    assertThat(configTool("activate", "-cluster-name", "my-cluster", "-config-file", config), allOf(
      is(not(successful())),
      containsOutput("Cluster activation failed, each stripe must have at least one replica node"),
      containsOutput("Provided cluster topology:"),
      containsOutput("Replica distribution:"),
      containsOutput("node-1-2@" + getNodeHostPort(1, 2) + " ): " + "No replica nodes found"),
      containsOutput("Replicas(node-2-2@" + getNodeHostPort(2, 2) + ")")
    ));
  }

  @Test
  public void test_activation_with_multiple_replicas_per_stripe() {
    stopNode(1, 1);
    waitForStopped(1, 1);
    startNode(1, 1, getNewOptions(getNode(1, 1),
      "-replica", "true", "-relay-hostname", "localhost", "-relay-port", "9410", "-relay-group-port", "9430"));
    waitForPassiveReplicaStart(1, 1);

    stopNode(1, 2);
    waitForStopped(1, 2);
    startNode(1, 2, getNewOptions(getNode(1, 2),
      "-replica", "true", "-relay-hostname", "localhost", "-relay-port", "9410", "-relay-group-port", "9430"));
    waitForPassiveReplicaStart(1, 2);

    stopNode(2, 1);
    waitForStopped(2, 1);
    startNode(2, 1, getNewOptions(getNode(2, 1),
      "-replica", "true", "-relay-hostname", "localhost", "-relay-port", "9410", "-relay-group-port", "9430"));
    waitForPassiveReplicaStart(2, 1);

    stopNode(2, 2);
    waitForStopped(2, 2);
    startNode(2, 2, getNewOptions(getNode(2, 2),
      "-replica", "true", "-relay-hostname", "localhost", "-relay-port", "9410", "-relay-group-port", "9430"));
    waitForPassiveReplicaStart(2, 2);

    String config = copyConfigProperty("/config-property-files/multi-stripe_multi-node.properties").toString();

    assertThat(configTool("activate", "-cluster-name", "my-cluster", "-config-file", config), is(successful()));
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 1)).getNodeCount(), is(equalTo(4)));

    waitForActive(1);
    waitForPassives(1);
    waitForActive(2);
    waitForPassives(2);
  }

  @Test
  public void test_restricted_activation_with_replicas() {
    stopNode(1, 2);
    waitForStopped(1, 2);
    startNode(1, 2, getNewOptions(getNode(1, 2),
      "-replica", "true", "-relay-hostname", "localhost", "-relay-port", "9410", "-relay-group-port", "9430"));
    waitForPassiveReplicaStart(1, 2);

    stopNode(2, 2);
    waitForStopped(2, 2);
    startNode(2, 2, getNewOptions(getNode(2, 2),
      "-replica", "true", "-relay-hostname", "localhost", "-relay-port", "9410", "-relay-group-port", "9430"));
    waitForPassiveReplicaStart(2, 2);

    // activate only replica nodes
    String config = copyConfigProperty("/config-property-files/multi-stripe_multi-node.properties").toString();
    ToolExecutionResult activate = configTool("activate", "-cluster-name", "my-cluster", "-config-file", config,
      "-connect-to", getNodeHostPort(1, 2) + "", "-connect-to", getNodeHostPort(2, 2) + "", "-restrict");

    assertThat(activate, is(successful()));

    // replica nodes should move to active
    waitForActive(1, 2);
    waitForActive(2, 2);

    String exportPath = tmpDir.getRoot().resolve("export.properties").toAbsolutePath().toString();
    assertThat(configTool("export", "-connect-to", "localhost:" + getNodePort(1, 2), "-output-file", exportPath, "-output-format", "properties"), is(successful()));

    // activate rest of the nodes
    activate = configTool("activate", "-cluster-name", "my-cluster", "-config-file",
      exportPath, "-connect-to", getNodeHostPort(1, 1) + "", "-connect-to", getNodeHostPort(2, 1) + "", "-restrict");

    assertThat(activate, is(successful()));

    // normal nodes should transition to passive
    waitForPassive(1, 1);
    waitForPassive(2, 1);
  }

  @Test
  public void test_failed_restricted_activation_missing_replica_in_one_stripe() {
    // Only stripe 2 has a replica node
    stopNode(2, 2);
    waitForStopped(2, 2);
    startNode(2, 2, getNewOptions(getNode(2, 2),
      "-replica", "true", "-relay-hostname", "localhost", "-relay-port", "9410", "-relay-group-port", "9430"));
    waitForPassiveReplicaStart(2, 2);

    String config = copyConfigProperty("/config-property-files/multi-stripe_multi-node.properties").toString();
    ToolExecutionResult activate = configTool("activate", "-cluster-name", "my-cluster",
      "-config-file", config, "-connect-to", getNodeHostPort(2, 2) + "", "-restrict");

    assertThat(activate, allOf(
      is(not(successful())),
      containsOutput("Cluster activation failed, each stripe must have at least one replica node"),
      containsOutput("No replica nodes found")
    ));
  }
}
