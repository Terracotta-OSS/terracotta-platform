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

import org.junit.Rule;
import org.junit.Test;
import org.terracotta.angela.common.tcconfig.TerracottaServer;
import org.terracotta.dynamic_config.test_support.ClusterDefinition;
import org.terracotta.dynamic_config.test_support.DynamicConfigIT;
import org.terracotta.dynamic_config.test_support.util.NodeOutputRule;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.terracotta.dynamic_config.test_support.util.AngelaMatchers.containsLog;

/**
 * @author Mathieu Carbou
 */
@ClusterDefinition(nodesPerStripe = 2, autoActivate = true)
public class DiagnosticMode1x2IT extends DynamicConfigIT {

  @Rule public final NodeOutputRule out = new NodeOutputRule();

  @SuppressWarnings("OptionalGetWithoutIsPresent")
  @Test
  public void test_restart_active_in_diagnostic_mode() {
    int activeNodeId = findActive(1).getAsInt();
    TerracottaServer active = getNode(1, activeNodeId);
    tsa.stop(active);
    assertThat(tsa.getStopped().size(), is(1));

    startNode(active, "--diagnostic-mode", "--node-name", active.getServerSymbolicName().getSymbolicName(), "-r", active.getConfigRepo());
    waitUntil(out.getLog(1, activeNodeId), containsLog("Node is starting in diagnostic mode. This mode is used to manually repair a broken configuration on a node."));
    waitUntil(out.getLog(1, activeNodeId), containsLog("Started the server in diagnostic mode"));
  }

  @Test
  public void test_restart_passive_in_diagnostic_mode() {
    int passiveNodeId = findPassives(1)[0];
    TerracottaServer passive = getNode(1, passiveNodeId);
    tsa.stop(passive);
    assertThat(tsa.getStopped().size(), is(1));

    startNode(passive, "--diagnostic-mode", "--node-name", passive.getServerSymbolicName().getSymbolicName(), "-r", passive.getConfigRepo());
    waitUntil(out.getLog(1, passiveNodeId), containsLog("Node is starting in diagnostic mode. This mode is used to manually repair a broken configuration on a node."));
    waitUntil(out.getLog(1, passiveNodeId), containsLog("Started the server in diagnostic mode"));
  }
}
