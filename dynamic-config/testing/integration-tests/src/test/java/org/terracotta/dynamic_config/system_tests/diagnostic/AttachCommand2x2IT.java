/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
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
package org.terracotta.dynamic_config.system_tests.diagnostic;

import org.junit.Test;
import org.terracotta.dynamic_config.test_support.ClusterDefinition;
import org.terracotta.dynamic_config.test_support.DynamicConfigIT;

import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.successful;

/**
 * @author Mathieu Carbou
 */
@ClusterDefinition(stripes = 2, nodesPerStripe = 2, failoverPriority = "")
public class AttachCommand2x2IT extends DynamicConfigIT {

  @Test
  public void test_attach_stripe() throws Exception {
    // create a 1x2
    assertThat(configTool("attach", "-d", "localhost:" + getNodePort(1, 1), "-s", "localhost:" + getNodePort(1, 2)), is(successful()));

    assertThat(getUpcomingCluster("localhost", getNodePort(1, 1)).getNodeCount(), is(equalTo(2)));
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 2)).getNodeCount(), is(equalTo(2)));

    // create a 1x2
    assertThat(configTool("attach", "-d", "localhost:" + getNodePort(2, 1), "-s", "localhost:" + getNodePort(2, 2)), is(successful()));

    assertThat(getUpcomingCluster("localhost", getNodePort(2, 1)).getNodeCount(), is(equalTo(2)));
    assertThat(getUpcomingCluster("localhost", getNodePort(2, 2)).getNodeCount(), is(equalTo(2)));

    assertThat(configTool("attach", "-t", "stripe", "-d", "localhost:" + getNodePort(1, 1), "-s", "localhost:" + getNodePort(2, 1)), is(successful()));

    assertThat(getUpcomingCluster("localhost", getNodePort(1, 1)).getNodeCount(), is(equalTo(4)));
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 2)).getNodeCount(), is(equalTo(4)));
    assertThat(getUpcomingCluster("localhost", getNodePort(2, 1)).getNodeCount(), is(equalTo(4)));
    assertThat(getUpcomingCluster("localhost", getNodePort(2, 2)).getNodeCount(), is(equalTo(4)));
  }

  @Test
  public void test_attach_node() throws Exception {
    // create a 2x1
    assertThat(configTool("attach", "-t", "stripe", "-d", "localhost:" + getNodePort(1, 1), "-s", "localhost:" + getNodePort(1, 2)), is(successful()));

    assertThat(getUpcomingCluster("localhost", getNodePort(1, 1)).getNodeCount(), is(equalTo(2)));
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 1)).getStripeCount(), is(equalTo(2)));

    assertThat(getUpcomingCluster("localhost", getNodePort(1, 2)).getNodeCount(), is(equalTo(2)));
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 2)).getStripeCount(), is(equalTo(2)));

    // attach node to stripe 1
    assertThat(configTool("attach", "-d", "localhost:" + getNodePort(1, 1), "-s", "localhost:" + getNodePort(2, 1)), is(successful()));

    assertThat(getUpcomingCluster("localhost", getNodePort(1, 1)).getNodeCount(), is(equalTo(3)));
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 1)).getStripeCount(), is(equalTo(2)));

    assertThat(getUpcomingCluster("localhost", getNodePort(1, 2)).getNodeCount(), is(equalTo(3)));
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 2)).getStripeCount(), is(equalTo(2)));

    assertThat(getUpcomingCluster("localhost", getNodePort(2, 1)).getNodeCount(), is(equalTo(3)));
    assertThat(getUpcomingCluster("localhost", getNodePort(2, 1)).getStripeCount(), is(equalTo(2)));
  }

  @Test
  public void test_attach_stripe_shape() {
    assertThat(configTool("attach", "-to-cluster", getNodeHostPort(1, 1).toString(), "-stripe-shape", getNodeHostPort(2, 1) + "|" + getNodeHostPort(2, 2)), is(successful()));

    Stream.of(getNodePort(1, 1), getNodePort(2, 1), getNodePort(2, 2)).forEach(port -> {
      assertThat(getUpcomingCluster("localhost", port).getNodeCount(), is(equalTo(3)));
      assertThat(getUpcomingCluster("localhost", port).getStripeCount(), is(equalTo(2)));
    });
  }

  @Test
  public void test_attach_stripe_shape_named() {
    assertThat(configTool("attach", "-to-cluster", getNodeHostPort(1, 1).toString(), "-stripe-shape", "stripe2/" + getNodeHostPort(2, 1) + "|" + getNodeHostPort(2, 2)), is(successful()));

    Stream.of(getNodePort(1, 1), getNodePort(2, 1), getNodePort(2, 2)).forEach(port -> {
      assertThat(getUpcomingCluster("localhost", port).getNodeCount(), is(equalTo(3)));
      assertThat(getUpcomingCluster("localhost", port).getStripeCount(), is(equalTo(2)));
      assertThat(getUpcomingCluster("localhost", port).getStripes().get(1).getName(), is(equalTo("stripe2")));
    });
  }
}
