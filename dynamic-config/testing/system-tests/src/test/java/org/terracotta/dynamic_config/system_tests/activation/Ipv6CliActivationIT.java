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
import org.terracotta.dynamic_config.test_support.ClusterDefinition;
import org.terracotta.dynamic_config.test_support.DynamicConfigIT;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
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
        "--log-dir", getNodePath(stripeId, nodeId).append("/logs").toString(),
        "--backup-dir", getNodePath(stripeId, nodeId).append("/backup").toString(),
        "--metadata-dir", getNodePath(stripeId, nodeId).append("/metadata").toString(),
        "--config-dir", getNodePath(stripeId, nodeId).append("/config").toString(),
        "--data-dirs", "main:" + getNodePath(stripeId, nodeId).append("/data-dir")
    );
  }

  @Test
  public void testSingleNodeStartupFromCliParamsAndActivateCommand() {
    waitForDiagnostic(1, 1);

    assertThat(configTool("activate", "-s", "[::1]:" + getNodePort(), "-n", "tc-cluster"), is(successful()));

    waitForActive(1);
  }

  @Test
  public void testMultiNodeStartupFromCliParamsAndActivateCommand() {
    waitForDiagnostic(1, 1);
    waitForDiagnostic(1, 2);

    assertThat(configTool("attach", "-d", "[::1]:" + getNodePort(), "-s", "[::1]:" + getNodePort(1, 2)), is(successful()));

    assertThat(configTool("activate", "-s", "[::1]:" + getNodePort(), "-n", "tc-cluster"), is(successful()));

    waitForActive(1);
    waitForPassives(1);
  }
}
