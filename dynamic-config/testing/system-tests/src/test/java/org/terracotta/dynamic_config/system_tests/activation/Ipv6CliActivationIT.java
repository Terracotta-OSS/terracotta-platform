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
import org.terracotta.dynamic_config.test_support.ClusterDefinition;
import org.terracotta.dynamic_config.test_support.DynamicConfigIT;
import org.terracotta.dynamic_config.test_support.util.NodeOutputRule;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.terracotta.dynamic_config.test_support.util.AngelaMatchers.containsLog;
import static org.terracotta.dynamic_config.test_support.util.AngelaMatchers.successful;

@ClusterDefinition(nodesPerStripe = 2)
public class Ipv6CliActivationIT extends DynamicConfigIT {

  @Rule public final NodeOutputRule out = new NodeOutputRule();

  @Override
  protected void startNode(int stripeId, int nodeId) {
    String uniqueId = combine(stripeId, nodeId);
    startNode(stripeId, nodeId,
        "--node-name", "node" + uniqueId,
        "--node-hostname", "::1",
        "--node-bind-address", "::",
        "--node-group-bind-address", "::",
        "--node-port", String.valueOf(getNodePort(stripeId, nodeId)),
        "--node-group-port", String.valueOf(getNodeGroupPort(stripeId, nodeId)),
        "--node-log-dir", "terracotta" + uniqueId + "/logs",
        "--node-backup-dir", "terracotta" + uniqueId + "/backup",
        "--node-metadata-dir", "terracotta" + uniqueId + "/metadata",
        "--node-repository-dir", "terracotta" + uniqueId + "/repository",
        "--data-dirs", "main:terracotta" + uniqueId + "/data-dir"
    );
  }

  @Test
  public void testSingleNodeStartupFromCliParamsAndActivateCommand() {
    waitUntil(out.getLog(1, 1), containsLog("Started the server in diagnostic mode"));

    out.clearLog(1, 1);
    assertThat(configToolInvocation("activate", "-s", "[::1]:" + getNodePort(), "-n", "tc-cluster"), is(successful()));

    waitForActive(1);
    waitUntil(out.getLog(1, findActive(1).getAsInt()), containsLog("Moved to State[ ACTIVE-COORDINATOR ]"));
  }

  @Test
  public void testMultiNodeStartupFromCliParamsAndActivateCommand() {
    waitUntil(out.getLog(1, 1), containsLog("Started the server in diagnostic mode"));
    waitUntil(out.getLog(1, 2), containsLog("Started the server in diagnostic mode"));

    assertThat(configToolInvocation("attach", "-d", "[::1]:" + getNodePort(), "-s", "[::1]:" + getNodePort(1, 2)), is(successful()));

    out.clearLog();
    assertThat(configToolInvocation("activate", "-s", "[::1]:" + getNodePort(), "-n", "tc-cluster"), is(successful()));

    waitForActive(1);
    waitForSomePassives(1);
    waitUntil(out.getLog(1, findActive(1).getAsInt()), containsLog("Moved to State[ ACTIVE-COORDINATOR ]"));
    waitUntil(out.getLog(1, findPassives(1)[0]), containsLog("Moved to State[ PASSIVE-STANDBY ]"));
  }
}
