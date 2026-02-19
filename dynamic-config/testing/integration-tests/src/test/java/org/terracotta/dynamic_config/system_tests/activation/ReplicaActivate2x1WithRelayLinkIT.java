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

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
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
    String config = copyConfigProperty("/config-property-files/replica2x1.properties", List.of(new int[]{1, 1}, new int[]{2, 1})).toString();
    ToolExecutionResult activateReplica = configTool("activate", "-cluster-name", "replica-cluster1", "-config-file", config);
    assertThat(activateReplica, is(successful()));

    // replicas should transition to active state
    waitForActive(1, 1);
    waitForActive(2, 1);
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 1)).getNodeCount(), is(equalTo(2)));
    assertThat(getUpcomingCluster("localhost", getNodePort(2, 1)).getNodeCount(), is(equalTo(2)));
  }

  private Path copyConfigProperty(String configFile, List<int[]> stripeNodePairs) throws IOException, URISyntaxException {
      Path src = Paths.get(getClass().getResource(configFile).toURI());
      Path dest = getBaseDir().resolve(src.getFileName());
      Files.createDirectories(getBaseDir());

      Map<String, String> replacements = stripeNodePairs.stream()
        .flatMap(pair -> {
          int stripeId = pair[0];
          int nodeId = pair[1];
          return Map.of(
            "${PORT-" + stripeId + "-" + nodeId + "}", String.valueOf(angela.getNodePort(stripeId, nodeId)),
            "${GROUP-PORT-" + stripeId + "-" + nodeId + "}", String.valueOf(angela.getNodeGroupPort(stripeId, nodeId))
          ).entrySet().stream();
        })
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

      String updated = Files.readString(src, StandardCharsets.UTF_8);
      for (Map.Entry<String, String> entry : replacements.entrySet()) {
        updated = updated.replace(entry.getKey(), entry.getValue());
      }
      Files.writeString(dest, updated, StandardCharsets.UTF_8);
      return dest;
  }

  private void waitForRelayChangeToSync() {
    waitUntilServerLogs(getNode(1, 4), "No configuration change left to sync");
    waitUntilServerLogs(getNode(2, 4), "No configuration change left to sync");
  }
}
