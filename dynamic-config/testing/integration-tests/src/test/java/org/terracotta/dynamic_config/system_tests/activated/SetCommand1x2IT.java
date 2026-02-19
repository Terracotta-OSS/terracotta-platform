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
import org.terracotta.dynamic_config.api.model.RawPath;
import org.terracotta.dynamic_config.api.service.TopologyService;
import org.terracotta.dynamic_config.test_support.ClusterDefinition;
import org.terracotta.dynamic_config.test_support.DynamicConfigIT;

import java.util.stream.Stream;

import static java.util.function.Predicate.isEqual;
import static java.util.stream.IntStream.rangeClosed;
import static java.util.stream.Stream.concat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.containsOutput;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.successful;

@ClusterDefinition(nodesPerStripe = 2, autoActivate = true)
public class SetCommand1x2IT extends DynamicConfigIT {
  @Test
  public void testCluster_setClientReconnectWindow() {
    assertThat(configTool("set", "-s", "localhost:" + getNodePort(), "-c", "client-reconnect-window=10s"), is(successful()));

    assertThat(
        configTool("get", "-s", "localhost:" + getNodePort(), "-c", "client-reconnect-window"),
        containsOutput("client-reconnect-window=10s"));
  }

  @Test
  public void testFailedConfigChangedDoesntFailPassiveSync() throws Exception {
    int passiveId = waitForNPassives(1, 1)[0];
    RawPath metadataDir = usingTopologyService(1, passiveId, topologyService -> topologyService.getUpcomingNodeContext().getNode().getMetadataDir().orDefault());
    assertThat(
        configTool("unset", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node." + passiveId + ".metadata-dir"),
        containsOutput("Setting 'metadata-dir' cannot be unset when node is activated"));

    // kill active and wait for passive to become active
    stopNode(1, passiveId == 1 ? 2 : 1);
    waitForActive(1);

    // Verify that old passive can successfully start as passive
    startNode(1, passiveId == 1 ? 2 : 1);
    waitForPassives(1);

    // Finally ensure that metadata-dir has remained unchanged
    assertThat(
        configTool("get", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node." + passiveId + ".metadata-dir"),
        containsOutput(metadataDir.toString()));
  }

  @Test
  public void testTargetOfflineNode() {
    int activeId = waitForActive(1);
    int passiveId = waitForNPassives(1, 1)[0];
    stopNode(1, passiveId);

    assertThat(
        configTool("set", "-s", "localhost:" + getNodePort(1, activeId), "-c", "stripe.1.node." + passiveId + ".log-dir=foo"),
        containsOutput("Error: Some nodes that are targeted by the change are not reachable and thus cannot be validated"));

    assertThat(
        configTool("set", "-s", "localhost:" + getNodePort(1, activeId), "-c", "stripe.1.log-dir=foo"),
        containsOutput("Error: Some nodes that are targeted by the change are not reachable and thus cannot be validated"));

    assertThat(
        configTool("set", "-s", "localhost:" + getNodePort(1, activeId), "-c", "log-dir=foo"),
        containsOutput("Error: Some nodes that are targeted by the change are not reachable and thus cannot be validated"));
  }

  @Test
  public void testTargetOfflineNode_setSecurityLogDir() {
    int activeId = waitForActive(1);
    int passiveId = waitForNPassives(1, 1)[0];
    stopNode(1, passiveId);

    assertThat(
        configTool("set", "-s", "localhost:" + getNodePort(1, activeId), "-c", "stripe.1.node." + passiveId + ".security-log-dir=foo"),
        containsOutput("Error: Some nodes that are targeted by the change are not reachable and thus cannot be validated"));

    assertThat(
        configTool("set", "-s", "localhost:" + getNodePort(1, activeId), "-c", "stripe.1.security-log-dir=foo"),
        containsOutput("Error: Some nodes that are targeted by the change are not reachable and thus cannot be validated"));

    assertThat(
        configTool("set", "-s", "localhost:" + getNodePort(1, activeId), "-c", "security-log-dir=foo"),
        containsOutput("Error: Some nodes that are targeted by the change are not reachable and thus cannot be validated"));
  }

  @Test
  public void testSetRelay_passiveServerMovesToRelay() {
    waitForActive(1);
    int passiveId = waitForNPassives(1, 1)[0];
    String passive = getNodeName(1, passiveId);
    assertThat(configTool("set", "-s", "localhost:" + getNodePort(),
      "-c", passive + ":relay=" + "true",
      "-c", passive + ":replica-hostname=" + "localhost",
      "-c", passive + ":replica-port=" + "9410"), is(successful()));

    stopNode(1, passiveId);
    startNode(1, passiveId);

    waitForPassiveRelay(1, passiveId);
  }
}
