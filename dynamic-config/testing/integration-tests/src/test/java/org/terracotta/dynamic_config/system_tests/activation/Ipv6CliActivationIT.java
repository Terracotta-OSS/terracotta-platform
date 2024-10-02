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
package org.terracotta.dynamic_config.system_tests.activation;

import org.junit.Test;
import org.terracotta.dynamic_config.test_support.ClusterDefinition;
import org.terracotta.dynamic_config.test_support.DynamicConfigIT;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.successful;

@ClusterDefinition(nodesPerStripe = 2)
public class Ipv6CliActivationIT extends DynamicConfigIT {

  @Override
  protected void startNode(int stripeId, int nodeId) {
    startNode(stripeId, nodeId,
        "--name", getNodeName(stripeId, nodeId),
        "--failover-priority", "availability",
        "--hostname", "::1",
        "--bind-address", "::",
        "--group-bind-address", "::",
        "--port", String.valueOf(getNodePort(stripeId, nodeId)),
        "--group-port", String.valueOf(getNodeGroupPort(stripeId, nodeId)),
        "--log-dir", "logs",
        "--backup-dir", "backup",
        "--metadata-dir", "metadata",
        "--config-dir", "config",
        "--data-dirs", "main:data-dir"
    );
  }

  @Test
  public void testSingleNodeStartupFromCliParamsAndActivateCommand() {
    assertThat(configTool("activate", "-s", "[::1]:" + getNodePort(), "-n", "tc-cluster"), is(successful()));

    waitForActive(1);
  }

  @Test
  public void testMultiNodeStartupFromCliParamsAndActivateCommand() {
    assertThat(configTool("attach", "-d", "[::1]:" + getNodePort(), "-s", "[::1]:" + getNodePort(1, 2)), is(successful()));

    assertThat(configTool("activate", "-s", "[::1]:" + getNodePort(), "-n", "tc-cluster"), is(successful()));

    waitForActive(1);
    waitForPassives(1);
  }
}
