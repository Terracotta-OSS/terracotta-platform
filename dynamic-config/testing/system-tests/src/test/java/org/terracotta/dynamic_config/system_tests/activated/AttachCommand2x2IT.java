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

import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.containsOutput;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.successful;

/**
 * @author Mathieu Carbou
 */
@ClusterDefinition(stripes = 2, nodesPerStripe = 2)
public class AttachCommand2x2IT extends DynamicConfigIT {

  @Test
  public void test_attach_stripe_shape() {
    assertThat(
        configTool("activate", "-cluster-name", "my-cluster", "-stripe-shape", getNodeHostPort(1, 1) + "|" + getNodeHostPort(1, 2)),
        allOf(successful(), containsOutput("came back up")));

    waitForActive(1);
    waitForPassives(1);

    assertThat(configTool("attach", "-to-cluster", getNodeHostPort(1, 1).toString(), "-stripe-shape", getNodeHostPort(2, 1) + "|" + getNodeHostPort(2, 2)), is(successful()));

    Stream.of(getNodePort(1, 1), getNodePort(1, 2), getNodePort(2, 1), getNodePort(2, 2)).forEach(port -> {
      assertThat(getUpcomingCluster("localhost", port).getNodeCount(), is(equalTo(4)));
      assertThat(getUpcomingCluster("localhost", port).getStripeCount(), is(equalTo(2)));
    });
  }

  @Test
  public void test_attach_stripe_shape_named() {
    assertThat(
        configTool("activate", "-cluster-name", "my-cluster", "-stripe-shape", "stripe1/" + getNodeHostPort(1, 1) + "|" + getNodeHostPort(1, 2)),
        allOf(successful(), containsOutput("came back up")));

    waitForActive(1);
    waitForPassives(1);

    assertThat(configTool("attach", "-to-cluster", getNodeHostPort(1, 1).toString(), "-stripe-shape", "stripe2/" + getNodeHostPort(2, 1) + "|" + getNodeHostPort(2, 2)), is(successful()));

    Stream.of(getNodePort(1, 1), getNodePort(1, 2), getNodePort(2, 1), getNodePort(2, 2)).forEach(port -> {
      assertThat(getUpcomingCluster("localhost", port).getNodeCount(), is(equalTo(4)));
      assertThat(getUpcomingCluster("localhost", port).getStripeCount(), is(equalTo(2)));
      assertThat(getUpcomingCluster("localhost", port).getStripes().get(0).getName(), is(equalTo("stripe1")));
      assertThat(getUpcomingCluster("localhost", port).getStripes().get(1).getName(), is(equalTo("stripe2")));
    });
  }

}
