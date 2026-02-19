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
package org.terracotta.dynamic_config.system_tests.activation;

import org.junit.Test;
import org.terracotta.angela.common.ToolExecutionResult;
import org.terracotta.dynamic_config.test_support.ClusterDefinition;
import org.terracotta.dynamic_config.test_support.DynamicConfigIT;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.containsOutput;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.successful;

@ClusterDefinition(stripes = 2, autoStart = false)
public class ReplicaActivate2x1IT extends DynamicConfigIT {
  @Test
  public void test_replica_activation_without_relay_link() {
    startNode(1, 1, getNewOptions(getNode(1, 1),
      "-replica", "true", "-relay-hostname", "localhost", "-relay-port", "9410", "-relay-group-port", "9430"));
    waitForPassiveReplicaStart(1, 1);

    startNode(2, 1, getNewOptions(getNode(2, 1),
      "-replica", "true", "-relay-hostname", "localhost", "-relay-port", "9410", "-relay-group-port", "9430"));
    waitForPassiveReplicaStart(2, 1);

    String config = copyConfigProperty("/config-property-files/import2x1.properties").toString();
    assertThat(configTool("activate", "-cluster-name", "my-cluster", "-config-file", config), is(successful()));
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 1)).getNodeCount(), is(equalTo(2)));

    waitForActive(1);
    waitForActive(2);
  }

  @Test
  public void test_successful_restricted_activation_both_at_same_time() {
    // both are replicas
    startNode(1, 1, getNewOptions(getNode(1, 1),
      "-replica", "true", "-relay-hostname", "localhost", "-relay-port", "9410", "-relay-group-port", "9430"));
    waitForPassiveReplicaStart(1, 1);
    startNode(2, 1, getNewOptions(getNode(2, 1),
      "-replica", "true", "-relay-hostname", "localhost", "-relay-port", "9410", "-relay-group-port", "9430"));
    waitForPassiveReplicaStart(2, 1);

    String config = copyConfigProperty("/config-property-files/import2x1.properties").toString();

    // activate both replicas with restrict flag
    ToolExecutionResult activate = configTool("activate", "-connect-to", getNodeHostPort(1, 1) + "," + getNodeHostPort(2, 1), "-restrict", "-cluster-name", "my-cluster", "-config-file", config);

    assertThat(activate, allOf(successful()));
  }

  @Test
  public void test_failed_activation_missing_replica_in_one_stripe() {
    //stripe 2 has a replica node, stripe 1 has none
    startNode(1, 1);
    startNode(2, 1, getNewOptions(getNode(2, 1),
      "-replica", "true", "-relay-hostname", "localhost", "-relay-port", "9410", "-relay-group-port", "9430"));
    waitForPassiveReplicaStart(2, 1);

    String config = copyConfigProperty("/config-property-files/import2x1.properties").toString();
    ToolExecutionResult activate = configTool("activate", "-cluster-name", "my-cluster", "-config-file", config);

    assertThat(activate, allOf(
      is(not(successful())),
      containsOutput("Cluster activation failed, each stripe must have at least one replica node"),
      containsOutput("Provided cluster topology:"),
      containsOutput("Replica distribution:"),
      containsOutput("( node-1@" + getNodeHostPort(1, 1) + " ): " + "No replica nodes found"),
      containsOutput("Replicas(node-2@" + getNodeHostPort(2, 1) + ")")
    ));
  }

  @Test
  public void test_failed_restricted_activation_activate_non_replica_first() {
    // activate non-replica node first with restrict flag, activating restricted replica in 2nd stripe should fail
    startNode(1, 1);
    startNode(2, 1, getNewOptions(getNode(2, 1),
      "-replica", "true", "-relay-hostname", "localhost", "-relay-port", "9410", "-relay-group-port", "9430"));
    waitForPassiveReplicaStart(2, 1);

    String config = copyConfigProperty("/config-property-files/import2x1.properties").toString();
    ToolExecutionResult activate = configTool("activate", "-connect-to", getNodeHostPort(1, 1).toString(), "-restrict", "-cluster-name", "my-cluster", "-config-file", config);

    assertThat(activate, allOf(successful()));

    activate = configTool("activate", "-connect-to", getNodeHostPort(2, 1).toString(), "-restrict", "-cluster-name", "my-cluster", "-config-file", config);
    assertThat(activate, allOf(
      is(not(successful())),
      containsOutput("( node-1@" + getNodeHostPort(1, 1) + " ): " + "No replica nodes found"),
      containsOutput("Replicas(node-2@" + getNodeHostPort(2, 1) + ")")
    ));
  }

  @Test
  public void test_failed_restricted_activation_activate_replicas_at_different_times() {
    // both are replicas
    startNode(1, 1, getNewOptions(getNode(1, 1),
      "-replica", "true", "-relay-hostname", "localhost", "-relay-port", "9410", "-relay-group-port", "9430"));
    waitForPassiveReplicaStart(1, 1);
    startNode(2, 1, getNewOptions(getNode(2, 1),
      "-replica", "true", "-relay-hostname", "localhost", "-relay-port", "9410", "-relay-group-port", "9430"));
    waitForPassiveReplicaStart(2, 1);

    String config = copyConfigProperty("/config-property-files/import2x1.properties").toString();

    // activate one replica with restrict flag
    ToolExecutionResult activate = configTool("activate", "-connect-to", getNodeHostPort(1, 1).toString(), "-restrict", "-cluster-name", "my-cluster", "-config-file", config);

    assertThat(activate, allOf(
      is(not(successful())),
      containsOutput("Replicas(node-1@" + getNodeHostPort(1, 1) + ")"),
      containsOutput("( node-2@" + getNodeHostPort(2, 1) + " ): " + "No replica nodes found")
    ));
  }
}
