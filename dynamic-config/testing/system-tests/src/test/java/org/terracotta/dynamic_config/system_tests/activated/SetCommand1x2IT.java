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

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.containsOutput;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.hasExitStatus;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.successful;

@ClusterDefinition(nodesPerStripe = 2, autoActivate = true)
public class SetCommand1x2IT extends DynamicConfigIT {
  @Test
  public void testCluster_setClientReconnectWindow_postActivation() throws Exception {
    assertThat(configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "client-reconnect-window=10s"), is(successful()));

    assertThat(configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "client-reconnect-window"),
        allOf(hasExitStatus(0), containsOutput("client-reconnect-window=10s")));
  }

  @Test
  public void testCluster_setDataDirs_postActivation() throws Exception {
    assertThat(configToolInvocation("set", "-s", "localhost:" + getNodePort(),
        "-c", "stripe.1.node.1.data-dirs.foo=foo/node-1-1",
        "-c", "stripe.1.node.2.data-dirs.foo=foo/node-1-2"
    ), is(successful()));

    assertThat(getRuntimeCluster(1, 1).getNode(1, 1).get().getDataDirs(), hasKey("foo"));
    assertThat(getRuntimeCluster(1, 1).getNode(1, 2).get().getDataDirs(), hasKey("foo"));
  }

  @Test
  public void testNode_setDataDirs_postActivationFails() throws Exception {
    assertThat(configToolInvocation("set", "-s", "localhost:" + getNodePort(),
        "-c", "stripe.1.node.1.data-dirs.foo=foo/node-1-1"),
        containsOutput("Data directory names need to match across the cluster, but found the following mismatches: [[main], [main, foo]] " +
            "If using -c option to add data dirs, use it multiple times to do for every node in cluster"));
  }

  @Test
  public void testFailedConfigChangedDoesntFailPassiveSync() throws Exception {
    int passiveId = findPassives(1)[0];
    Path metadataDir = usingTopologyService(1, passiveId, topologyService -> topologyService.getUpcomingNodeContext().getNode().getNodeMetadataDir());
    assertThat(
        configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node." + passiveId + ".metadata-dir=foo"),
        containsOutput("Setting 'metadata-dir' cannot be changed once a node is activated"));

    // kill active and wait for passive to become active
    stopNode(1, passiveId == 1 ? 2 : 1);
    waitForActive(1);

    // Verify that old passive can successfully start as passive
    startNode(1, passiveId == 1 ? 2 : 1);
    waitForPassives(1);

    // Finally ensure that metadata-dir has remain unchanged
    assertThat(
        configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node." + passiveId + ".metadata-dir"),
        containsOutput(metadataDir.toString()));

  }
}
