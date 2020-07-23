/*
 * Copyright Terracotta, Inc.
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

import java.nio.file.Path;

import static org.hamcrest.Matchers.hasKey;
import static org.junit.Assert.assertThat;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.containsOutput;

@ClusterDefinition(nodesPerStripe = 2, autoActivate = true)
public class SetCommand1x2IT extends DynamicConfigIT {
  @Test
  public void testCluster_setClientReconnectWindow() {
    invokeConfigTool("set", "-s", "localhost:" + getNodePort(), "-c", "client-reconnect-window=10s");

    assertThat(
        invokeConfigTool("get", "-s", "localhost:" + getNodePort(), "-c", "client-reconnect-window"),
        containsOutput("client-reconnect-window=10s"));
  }

  @Test
  public void testNode_setDataDirs() throws Exception {
    invokeConfigTool("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.data-dirs=foo:data-dir", "-c", "stripe.1.node.2.data-dirs=foo:data-dir");

    assertThat(getRuntimeCluster(1, 1).getNode(1, 1).get().getDataDirs(), hasKey("foo"));
    assertThat(getRuntimeCluster(1, 1).getNode(1, 2).get().getDataDirs(), hasKey("foo"));
  }

  @Test
  public void testStripe_setDataDirs() throws Exception {
    invokeConfigTool("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.data-dirs=foo:data-dir");

    assertThat(getRuntimeCluster(1, 1).getNode(1, 1).get().getDataDirs(), hasKey("foo"));
    assertThat(getRuntimeCluster(1, 1).getNode(1, 2).get().getDataDirs(), hasKey("foo"));
  }

  @Test
  public void testCluster_setDataDirs() throws Exception {
    invokeConfigTool("set", "-s", "localhost:" + getNodePort(), "-c", "data-dirs=foo:data-dir");

    assertThat(getRuntimeCluster(1, 1).getNode(1, 1).get().getDataDirs(), hasKey("foo"));
    assertThat(getRuntimeCluster(1, 1).getNode(1, 2).get().getDataDirs(), hasKey("foo"));
  }

  @Test
  public void testNode_setDataDirsFails() {
    assertThat(
        () -> invokeConfigTool("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.data-dirs=foo:data-dir"),
        exceptionMatcher("Data directory names need to match across the cluster"));
  }

  @Test
  public void testFailedConfigChangedDoesntFailPassiveSync() throws Exception {
    int passiveId = findPassives(1)[0];
    Path metadataDir = usingTopologyService(1, passiveId, topologyService -> topologyService.getUpcomingNodeContext().getNode().getMetadataDir());
    assertThat(
        () -> invokeConfigTool("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node." + passiveId + ".metadata-dir=foo"),
        exceptionMatcher("Setting 'metadata-dir' cannot be set when node is activated"));

    // kill active and wait for passive to become active
    stopNode(1, passiveId == 1 ? 2 : 1);
    waitForActive(1);

    // Verify that old passive can successfully start as passive
    startNode(1, passiveId == 1 ? 2 : 1);
    waitForPassives(1);

    // Finally ensure that metadata-dir has remain unchanged
    assertThat(
        invokeConfigTool("get", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node." + passiveId + ".metadata-dir"),
        containsOutput(metadataDir.toString()));
  }

  @Test
  public void testTargetOfflineNode() {
    int activeId = findActive(1).getAsInt();
    int passiveId = findPassives(1)[0];
    stopNode(1, passiveId);

    assertThat(
        () -> invokeConfigTool("set", "-s", "localhost:" + getNodePort(1, activeId), "-c", "stripe.1.node." + passiveId + ".log-dir=foo"),
        exceptionMatcher("Error: Some nodes that are targeted by the change are not reachable and thus cannot be validated"));

    assertThat(
        () -> invokeConfigTool("set", "-s", "localhost:" + getNodePort(1, activeId), "-c", "stripe.1.log-dir=foo"),
        exceptionMatcher("Error: Some nodes that are targeted by the change are not reachable and thus cannot be validated"));

    assertThat(
        () -> invokeConfigTool("set", "-s", "localhost:" + getNodePort(1, activeId), "-c", "log-dir=foo"),
        exceptionMatcher("Error: Some nodes that are targeted by the change are not reachable and thus cannot be validated"));
  }
}
