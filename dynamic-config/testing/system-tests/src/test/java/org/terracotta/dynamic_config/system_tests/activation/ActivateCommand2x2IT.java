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
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.test_support.ClusterDefinition;
import org.terracotta.dynamic_config.test_support.DynamicConfigIT;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.containsOutput;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.successful;

@ClusterDefinition(stripes = 2, nodesPerStripe = 2)
public class ActivateCommand2x2IT extends DynamicConfigIT {

  @Test
  public void test_fast_activation_2x2() {
    assertThat(
        configTool("activate", "-cluster-name", "my-cluster",
            "-stripe", getNodeHostPort(1, 1) + "|" + getNodeHostPort(1, 2),
            "-stripe", getNodeHostPort(2, 1) + "|" + getNodeHostPort(2, 2)),
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
        configTool("activate", "-cluster-name", "my-cluster", "-stripe", getNodeHostPort(1, 1) + "|" + getNodeHostPort(1, 2) + "," + getNodeHostPort(2, 1) + "|" + getNodeHostPort(2, 2)),
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
            "-stripe", "foo/" + getNodeHostPort(1, 1) + "|" + getNodeHostPort(1, 2),
            "-stripe", "bar/" + getNodeHostPort(2, 1) + "|" + getNodeHostPort(2, 2)),
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
            "-stripe", "foo/" + getNodeHostPort(1, 1) + "|" + getNodeHostPort(1, 2),
            "-stripe", "foo/" + getNodeHostPort(2, 1) + "|" + getNodeHostPort(2, 2)),
        allOf(not(successful()), containsOutput("Found duplicate stripe name: foo")));
  }
}
