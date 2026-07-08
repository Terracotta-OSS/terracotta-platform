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
import org.terracotta.dynamic_config.api.model.NodeContext;
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

@ClusterDefinition(stripes = 2, autoStart = false)
public class ReplicaActivate2x1IT extends DynamicConfigIT {
  @Test
  public void test_replica_cli_activation_without_relay_link() {
    startNode(1, 1, getNewOptions(getNode(1, 1),
      "-replica", "true", "-relay-hostname", "localhost", "-relay-port", "9410", "-relay-group-port", "9430"));
    startNode(2, 1, getNewOptions(getNode(2, 1),
      "-replica", "true", "-relay-hostname", "localhost", "-relay-port", "9410", "-relay-group-port", "9430"));
    assertThat(configTool("attach", "-to-cluster", "localhost:" + getNodePort(1, 1), "-stripe", "localhost:" + getNodePort(2, 1)), is(successful()));

    waitForDiagnostic(1, 1);
    waitForDiagnostic(2, 1);

    activateCluster();

    waitForPassiveReplicaStart(1, 1);
    waitForPassiveReplicaStart(2, 1);
    assertThat(getRuntimeCluster("localhost", getNodePort(1, 1)).getStripeCount(), is(equalTo(2)));
    assertThat(getRuntimeCluster("localhost", getNodePort(2, 1)).getStripeCount(), is(equalTo(2)));
  }

  @Test
  public void test_2x1_replica_activation_with_config_file_without_relay_link() {
    startNode(1, 1);
    startNode(2, 1);

    assertThat(
      configTool("activate", "-f", copyConfigProperty("/config-property-files/2x1-replica.properties").toString(), "-n", "my-cluster"),
      allOf(containsOutput("No license specified for activation"), containsOutput("came back up")));

    waitForPassiveReplicaStart(1, 1);
    waitForPassiveReplicaStart(2, 1);

    withTopologyService("localhost", getNodePort(), topologyService -> {
      NodeContext runtimeNodeContext = topologyService.getRuntimeNodeContext();
      assertThat(runtimeNodeContext.getCluster().getName(), is(equalTo("my-cluster")));
    });

    assertThat(getUpcomingCluster(1, 1).getStripeCount(), is(equalTo(2)));
    assertThat(getUpcomingCluster(1, 1).getNodeCount(), is(equalTo(2)));
    assertThat(getUpcomingCluster(2, 1).getNodeCount(), is(equalTo(2)));
    assertThat(getUpcomingCluster(2, 1).getNodeCount(), is(equalTo(2)));
  }

  @Test
  public void test_2x1_failed_replica_activation_with_config_file_without_relay_link() {
    startNode(1, 1);
    startNode(2, 1);

    assertThat(
      configTool("activate", "-f", copyConfigProperty("/config-property-files/2x1-replica-invalid1.properties").toString(), "-n", "my-cluster"),
      allOf(is(not(successful())), containsOutput("The replica setting is enabled for node with name: node-2-1, " +
        "replica properties: {relay-hostname=null, relay-port=null, relay-group-port=null} aren't well-formed, " +
        "[relay-hostname, relay-port, relay-group-port] need to be set together")));
  }

  @Test
  public void test_restricted_activation_with_replica() {
    startNode(1, 1);
    startNode(2, 1);

    String config = copyConfigProperty("/config-property-files/2x1-replica.properties").toString();
    assertThat(configTool("activate", "-cluster-name", "my-cluster", "-config-file", config,
      "-connect-to", getNodeHostPort(1, 1).toString(), "-restrict"), is(successful()));

    waitForPassiveReplicaStart(1, 1);
    waitForDiagnostic(2, 1);

    String exportPath = tmpDir.getRoot().resolve("export.properties").toAbsolutePath().toString();
    assertThat(configTool("export", "-connect-to", "localhost:" + getNodePort(1, 1), "-output-file", exportPath, "-output-format", "properties"), is(successful()));

    // activate 2nd replica
    assertThat(configTool("activate", "-cluster-name", "my-cluster", "-config-file", exportPath,
      "-connect-to", getNodeHostPort(2, 1).toString(), "-restrict"), is(successful()));

    waitForPassiveReplicaStart(2, 1);

    withTopologyService(1, 1, topologyService -> assertTrue(topologyService.isActivated()));
    withTopologyService(2, 1, topologyService -> assertTrue(topologyService.isActivated()));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, 1)).getStripeCount(), is(equalTo(2)));
    assertThat(getRuntimeCluster("localhost", getNodePort(2, 1)).getStripeCount(), is(equalTo(2)));
  }
}
