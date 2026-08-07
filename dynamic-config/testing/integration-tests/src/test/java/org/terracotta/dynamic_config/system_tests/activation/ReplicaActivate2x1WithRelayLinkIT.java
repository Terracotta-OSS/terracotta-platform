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
import org.terracotta.dynamic_config.test_support.ClusterDefinition;
import org.terracotta.dynamic_config.test_support.DynamicConfigIT;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.stream.IntStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.containsOutput;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.successful;

@ClusterDefinition(stripes = 2, nodesPerStripe = 4, autoStart = false)
public class ReplicaActivate2x1WithRelayLinkIT extends DynamicConfigIT {
  @Test
  public void test_replica_activation_with_relay_link() throws Exception {
    setupPrimaryCluster();
    configureAndActivateRelayNodes();
    startAndLinkReplicaNodes();
    failoverToReplicaCluster();
  }

  private void setupPrimaryCluster() {
    // start nodes 2-4 in both stripes as part of primary cluster
    IntStream.rangeClosed(1, 2).forEach(stripeId ->
      IntStream.rangeClosed(2, 4).forEach(nodeId ->
        startNode(stripeId, nodeId)
      )
    );

    assertThat(configTool("attach", "-to-stripe", "localhost:" + getNodePort(1, 2), "-node", "localhost:" + getNodePort(1, 3)), is(successful()));
    assertThat(configTool("attach", "-to-stripe", "localhost:" + getNodePort(1, 2), "-node", "localhost:" + getNodePort(1, 4)), is(successful()));
    assertThat(configTool("attach", "-to-stripe", "localhost:" + getNodePort(2, 2), "-node", "localhost:" + getNodePort(2, 3)), is(successful()));
    assertThat(configTool("attach", "-to-stripe", "localhost:" + getNodePort(2, 2), "-node", "localhost:" + getNodePort(2, 4)), is(successful()));
    assertThat(configTool("attach", "-to-cluster", "localhost:" + getNodePort(1, 2), "-stripe", "localhost:" + getNodePort(2, 2)), is(successful()));
  }

  private void configureAndActivateRelayNodes() {
    // set node 4 in each stripe as relay nodes
    assertThat(configTool("set", "-connect-to", "localhost:" + getNodePort(1, 2), "-setting", "node-1-4:relay=true",
      "-setting", "node-1-4:replica-hostname=" + "localhost", "-setting", "node-1-4:replica-port=" + 1234,
      "-setting", "node-2-4:relay=true", "-setting", "node-2-4:replica-hostname=" + "localhost", "-setting", "node-2-4:replica-port=" + 1234), is(successful()));

    // activate the primary cluster
    assertThat(configTool("activate", "-s", "localhost:" + getNodePort(1, 2), "-n", "relay-cluster"), is(successful()));

    // wait for cluster to be active
    waitForActive(1);
    waitForActive(2);
    waitForNPassives(1, 1);
    waitForNPassives(2, 1);
    waitForPassiveRelay(1, 4);
    waitForPassiveRelay(2, 4);

    assertThat(getUpcomingCluster("localhost", getNodePort(1, 2)).getNodeCount(), is(equalTo(6)));
  }

  private void startAndLinkReplicaNodes() {
    // relay ports
    int relay1Port = getNode(1, 4).getTsaPort();
    int relay1GroupPort = getNode(1, 4).getTsaGroupPort();
    int relay2Port = getNode(2, 4).getTsaPort();
    int relay2GroupPort = getNode(2, 4).getTsaGroupPort();

    // start replica nodes with corresponding relay nodes, replicas should transition to PASSIVE-REPLICA-START
    startNode(1, 1, getNewOptions(getNode(1, 1),
      "-replica", "true", "-relay-hostname", "localhost", "-relay-port", relay1Port + "", "-relay-group-port", relay1GroupPort + ""));
    startNode(2, 1, getNewOptions(getNode(2, 1),
      "-replica", "true", "-relay-hostname", "localhost", "-relay-port", relay2Port + "", "-relay-group-port", relay2GroupPort + ""));

    // attach and activate replica cluster
    assertThat(configTool("attach", "-to-cluster", "localhost:" + getNodePort(1, 1), "-stripe", "localhost:" + getNodePort(2, 1)), is(successful()));
    assertThat(configTool("activate", "-s", "localhost:" + getNodePort(1, 1), "-n", "replica-cluster"), is(successful()));

    waitForPassiveReplicaStart(1, 1);
    waitForPassiveReplicaStart(2, 1);

    // replica node ports
    int replica1Port = getNode(1, 1).getTsaPort();
    int replica2Port = getNode(2, 1).getTsaPort();

    // set replica port in relay nodes to establish link
    assertThat(configTool("set", "-connect-to", "localhost:" + getNodePort(1, 2),
      "-setting", "node-1-4:replica-port=" + replica1Port, "-setting", "node-2-4:replica-port=" + replica2Port), is(successful()));

    waitForRelayChangeToSync();
    waitForPassiveRelay(1, 4);
    waitForPassiveRelay(2, 4);

    // link should've been established between relay and replica nodes, replicas should transition to PASSIVE-REPLICA state
    waitUntilServerLogs(getNode(1, 1), "joined the cluster");
    waitUntilServerLogs(getNode(2, 1), "joined the cluster");
    waitForPassiveReplica(1, 1);
    waitForPassiveReplica(2, 1);
  }

  private void failoverToReplicaCluster() throws IOException, URISyntaxException {
    // Stop all primary cluster nodes
    IntStream.rangeClosed(1, 2).forEach(stripeId ->
      IntStream.rangeClosed(2, 4).forEach(nodeId -> {
        stopNode(stripeId, nodeId);
        waitForStopped(stripeId, nodeId);
      })
    );

    // activate DR cluster
    assertThat(configTool("replica-failover", "-connect-to", "localhost:" + getNodePort(1, 1)), is(successful()));

    // replica flag is unset
    waitUntil(() -> configTool("log", "-s", "localhost:" + getNodePort(1, 1)), allOf(is(successful()), containsOutput("unset replica")));
    waitUntil(() -> configTool("log", "-s", "localhost:" + getNodePort(2, 1)), allOf(is(successful()), containsOutput("unset replica")));

    // replicas should transition to active state
    waitUntilServerLogs(getNode(1, 1), "Successfully transitioned Replica to Active state");
    waitUntilServerLogs(getNode(2, 1), "Successfully transitioned Replica to Active state");

    waitForActive(1, 1);
    waitForActive(2, 1);
  }

  private void waitForRelayChangeToSync() {
    waitUntilServerLogs(getNode(1, 4), "No configuration change left to sync");
    waitUntilServerLogs(getNode(2, 4), "No configuration change left to sync");
  }
}
