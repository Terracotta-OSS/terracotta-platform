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
package org.terracotta.dynamic_config.system_tests.diagnostic;

import org.junit.Test;
import org.terracotta.dynamic_config.test_support.ClusterDefinition;
import org.terracotta.dynamic_config.test_support.DynamicConfigIT;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.successful;

@ClusterDefinition(stripes = 2, nodesPerStripe = 1, autoStart = false)
public class DetachCommand2x1IT extends DynamicConfigIT {

  @Test
  public void test_detach_replica_stripe() {
    startNode(1, 1, getNewOptions(getNode(1, 1),
      "-replica", "true", "-relay-hostname", "localhost", "-relay-port", "9410", "-relay-group-port", "9430"));
    startNode(2, 1, getNewOptions(getNode(2, 1),
      "-replica", "true", "-relay-hostname", "localhost", "-relay-port", "9410", "-relay-group-port", "9430"));
    assertThat(configTool("attach", "-t", "stripe", "-d", "localhost:" + getNodePort(1, 1), "-s", "localhost:" + getNodePort(2, 1)), is(successful()));

    // detach stripe
    assertThat(configTool("detach", "-t", "stripe", "-d", "localhost:" + getNodePort(1, 1), "-s", "localhost:" + getNodePort(2, 1)), is(successful()));
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 1)).getNodeCount(), is(equalTo(1)));
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 1)).getStripeCount(), is(equalTo(1)));
  }
}
