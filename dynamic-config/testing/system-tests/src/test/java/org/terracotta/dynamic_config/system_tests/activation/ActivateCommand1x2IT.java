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

import org.junit.Rule;
import org.junit.Test;
import org.terracotta.diagnostic.client.DiagnosticService;
import org.terracotta.diagnostic.client.DiagnosticServiceFactory;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.service.TopologyService;
import org.terracotta.dynamic_config.test_support.ClusterDefinition;
import org.terracotta.dynamic_config.test_support.DynamicConfigIT;
import org.terracotta.dynamic_config.test_support.util.NodeOutputRule;

import java.net.InetSocketAddress;

import static java.time.Duration.ofSeconds;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.terracotta.dynamic_config.test_support.util.AngelaMatchers.containsOutput;
import static org.terracotta.dynamic_config.test_support.util.AngelaMatchers.successful;

@ClusterDefinition(nodesPerStripe = 2)
public class ActivateCommand1x2IT extends DynamicConfigIT {

  @Rule public final NodeOutputRule out = new NodeOutputRule();

  @Test
  public void testSingleNodeActivation() {
    assertThat(activateCluster(),
        allOf(is(successful()), containsOutput("No license installed"), containsOutput("came back up")));
    waitForActive(1, 1);
  }

  @Test
  public void testMultiNodeSingleStripeActivation() {
    assertThat(
        configToolInvocation("attach", "-d", "localhost:" + getNodePort(), "-s", "localhost:" + getNodePort(1, 2)),
        is(successful()));

    assertThat(activateCluster(), allOf(is(successful()), containsOutput("No license installed"), containsOutput("came back up")));
    waitForActive(1);
    waitForPassives(1);
  }

  @Test
  public void testSingleNodeActivationWithConfigFile() throws Exception {
    assertThat(
        configToolInvocation("activate", "-f", copyConfigProperty("/config-property-files/single-stripe.properties").toString(), "-n", "my-cluster"),
        allOf(
            containsOutput("No license installed"),
            containsOutput("came back up"),
            is(successful())));

    waitForActive(1, 1);

    // TDB-4726
    try (DiagnosticService diagnosticService = DiagnosticServiceFactory.fetch(InetSocketAddress.createUnresolved("localhost", getNodePort()), "diag", ofSeconds(10), ofSeconds(10), null)) {
      NodeContext runtimeNodeContext = diagnosticService.getProxy(TopologyService.class).getRuntimeNodeContext();
      assertThat(runtimeNodeContext.getCluster().getName(), is(equalTo("my-cluster")));
    }
  }

  @Test
  public void testMultiNodeSingleStripeActivationWithConfigFile() {
    assertThat(
        configToolInvocation(
            "-r", timeout + "s",
            "activate",
            "-f", copyConfigProperty("/config-property-files/single-stripe_multi-node.properties").toString()),
        allOf(is(successful()), containsOutput("No license installed"), containsOutput("came back up")));

    waitForActive(1);
    waitForPassives(1);
  }

  @Test
  public void testRestrictedActivationToAttachANewActivatedNode() throws Exception {
    String config = copyConfigProperty("/config-property-files/single-stripe_multi-node.properties").toString();

    // import the cluster config to node 1 and node 2
    assertThat(configToolInvocation("import", "-f", config), is(successful()));

    // restrict activation to only node 1
    assertThat(configToolInvocation("activate", "-R", "-n", "my-cluster", "-s", "localhost:" + getNodePort(1, 1)),
        allOf(is(successful()), containsOutput("No license installed"), containsOutput("came back up")));

    waitForActive(1, 1);

    withTopologyService(1, 1, topologyService -> assertTrue(topologyService.isActivated()));
    withTopologyService(1, 2, topologyService -> assertFalse(topologyService.isActivated()));

    // restrict activation to only node 2, which will become passive as node 1
    assertThat(configToolInvocation("activate", "-R", "-n", "my-cluster", "-s", "localhost:" + getNodePort(1, 2)),
        allOf(is(successful()), containsOutput("No license installed"), containsOutput("came back up")));

    waitForPassive(1, 2);

    withTopologyService(1, 1, topologyService -> assertTrue(topologyService.isActivated()));
    withTopologyService(1, 2, topologyService -> assertTrue(topologyService.isActivated()));

    assertThat(getRuntimeCluster(1, 1), is(equalTo(getRuntimeCluster(1, 2))));
  }
}
