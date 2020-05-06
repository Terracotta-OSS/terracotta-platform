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

import java.util.concurrent.TimeoutException;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.successful;

@ClusterDefinition(nodesPerStripe = 2)
public class Ipv6CliActivationIT extends DynamicConfigIT {

  @Override
  protected void startNode(int stripeId, int nodeId) {
    startNode(stripeId, nodeId,
        "--node-name", getNodeName(stripeId, nodeId),
        "--failover-priority", "availability",
        "--node-hostname", "::1",
        "--node-bind-address", "::",
        "--node-group-bind-address", "::",
        "--node-port", String.valueOf(getNodePort(stripeId, nodeId)),
        "--node-group-port", String.valueOf(getNodeGroupPort(stripeId, nodeId)),
        "--node-log-dir", getNodePath(stripeId, nodeId).resolve("logs").toString(),
        "--node-backup-dir", getNodePath(stripeId, nodeId).resolve("backup").toString(),
        "--node-metadata-dir", getNodePath(stripeId, nodeId).resolve("metadata").toString(),
        "--node-config-dir", getNodePath(stripeId, nodeId).resolve("config").toString(),
        "--data-dirs", "main:" + getNodePath(stripeId, nodeId).resolve("data-dir")
    );
  }

  @Test
  public void testSingleNodeStartupFromCliParamsAndActivateCommand() throws TimeoutException {
    waitForDiagnostic(1, 1);

    assertThat(configToolInvocation("activate", "-s", "[::1]:" + getNodePort(), "-n", "tc-cluster"), is(successful()));

    waitForActive(1);
  }

  @Test
  public void testMultiNodeStartupFromCliParamsAndActivateCommand() throws TimeoutException {
    waitForDiagnostic(1, 1);
    waitForDiagnostic(1, 2);

    assertThat(configToolInvocation("attach", "-d", "[::1]:" + getNodePort(), "-s", "[::1]:" + getNodePort(1, 2)), is(successful()));

    assertThat(configToolInvocation("activate", "-s", "[::1]:" + getNodePort(), "-n", "tc-cluster"), is(successful()));

    waitForActive(1);
    waitForPassives(1);
  }
}
