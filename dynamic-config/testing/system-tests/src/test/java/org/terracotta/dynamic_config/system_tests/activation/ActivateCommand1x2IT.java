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
package org.terracotta.dynamic_config.system_tests.activation;

import org.junit.Test;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.test_support.ClusterDefinition;
import org.terracotta.dynamic_config.test_support.DynamicConfigIT;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.containsOutput;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.successful;

@ClusterDefinition(nodesPerStripe = 2)
public class ActivateCommand1x2IT extends DynamicConfigIT {

  @Test
  public void testSingleNodeActivation() {
    assertThat(activateCluster(),
        allOf(is(successful()), containsOutput("No license installed"), containsOutput("came back up")));
    waitForActive(1, 1);
  }

  @Test
  public void testMultiNodeSingleStripeActivation() {
    assertThat(configTool("attach", "-d", "localhost:" + getNodePort(), "-s", "localhost:" + getNodePort(1, 2)), is(successful()));

    assertThat(activateCluster(), allOf(is(successful()), containsOutput("No license installed"), containsOutput("came back up")));
    waitForActive(1);
    waitForPassives(1);
  }

  @Test
  public void changeNodeNameAndActivate() throws Exception {
    assertThat(usingTopologyService(1, 1, topologyService -> topologyService.getUpcomingNodeContext().getNode().getName()), is(equalTo("node-1-1")));
    assertThat(configTool("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.name=foo"), is(successful()));
    assertThat(usingTopologyService(1, 1, topologyService -> topologyService.getUpcomingNodeContext().getNode().getName()), is(equalTo("foo")));
    activateCluster();
    assertThat(usingTopologyService(1, 1, topologyService -> topologyService.getUpcomingNodeContext().getNode().getName()), is(equalTo("foo")));
  }

  @Test
  public void testSingleNodeActivationWithConfigFile() throws Exception {
    assertThat(
        configTool("activate", "-f", copyConfigProperty("/config-property-files/single-stripe.properties").toString(), "-n", "my-cluster"),
        allOf(containsOutput("No license installed"), containsOutput("came back up")));

    waitForActive(1, 1);

    withTopologyService("localhost", getNodePort(), topologyService -> {
      NodeContext runtimeNodeContext = topologyService.getRuntimeNodeContext();
      assertThat(runtimeNodeContext.getCluster().getName(), is(equalTo("my-cluster")));
    });
  }

  @Test
  public void testMultiNodeSingleStripeActivationWithConfigFile() {
    assertThat(
        configTool("activate", "-f", copyConfigProperty("/config-property-files/single-stripe_multi-node.properties").toString()),
        allOf(containsOutput("No license installed"), containsOutput("came back up")));

    waitForActive(1);
    waitForPassives(1);
  }

  @Test
  public void testRestrictedActivationToActivateNodesAtDifferentTime() throws Exception {
    assertThat(
        configTool("activate", "-R", "-s", "localhost:" + getNodePort(1, 1), "-f", copyConfigProperty("/config-property-files/single-stripe_multi-node.properties").toString()),
        allOf(containsOutput("No license installed"), containsOutput("came back up")));
    waitForActive(1, 1);

    withTopologyService(1, 1, topologyService -> assertTrue(topologyService.isActivated()));
    withTopologyService(1, 2, topologyService -> assertFalse(topologyService.isActivated()));

    assertThat(getRuntimeCluster("localhost", getNodePort(1, 1)).getNodeCount(), is(equalTo(2)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, 2)).getNodeCount(), is(equalTo(1)));

    String exportPath = tmpDir.getRoot().resolve("export.properties").toAbsolutePath().toString();
    assertThat(configTool("export", "-s", "localhost:" + getNodePort(1, 1), "-f", exportPath, "-t", "properties"), is(successful()));

    assertThat(
        configTool("activate", "-R", "-s", "localhost:" + getNodePort(1, 2), "-f", exportPath),
        allOf(containsOutput("No license installed"), containsOutput("came back up")));
    waitForPassive(1, 2);

    withTopologyService(1, 1, topologyService -> assertTrue(topologyService.isActivated()));
    withTopologyService(1, 2, topologyService -> assertTrue(topologyService.isActivated()));

    assertThat(getRuntimeCluster("localhost", getNodePort(1, 1)).getNodeCount(), is(equalTo(2)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, 2)).getNodeCount(), is(equalTo(2)));
  }

  @Test
  public void testRestrictedActivationToAttachANewActivatedNode() throws Exception {
    String config = copyConfigProperty("/config-property-files/single-stripe_multi-node.properties").toString();

    // import the cluster config to node 1 and node 2
    assertThat(configTool("import", "-f", config), is(successful()));

    // restrict activation to only node 1
    assertThat(
        configTool("activate", "-R", "-n", "my-cluster", "-s", "localhost:" + getNodePort(1, 1)),
        allOf(containsOutput("No license installed"), containsOutput("came back up")));

    waitForActive(1, 1);
    waitForDiagnostic(1, 2);

    withTopologyService(1, 1, topologyService -> assertTrue(topologyService.isActivated()));
    withTopologyService(1, 2, topologyService -> assertFalse(topologyService.isActivated()));

    // restrict activation to only node 2, which will become passive as node 1
    assertThat(
        configTool("activate", "-R", "-n", "my-cluster", "-s", "localhost:" + getNodePort(1, 2)),
        allOf(containsOutput("No license installed"), containsOutput("came back up")));

    waitForPassive(1, 2);

    withTopologyService(1, 1, topologyService -> assertTrue(topologyService.isActivated()));
    withTopologyService(1, 2, topologyService -> assertTrue(topologyService.isActivated()));

    assertThat(getRuntimeCluster(1, 1), is(equalTo(getRuntimeCluster(1, 2))));
  }
}
