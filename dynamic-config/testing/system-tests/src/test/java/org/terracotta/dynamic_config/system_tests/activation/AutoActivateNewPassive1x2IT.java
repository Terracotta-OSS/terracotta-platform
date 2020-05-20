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
import org.terracotta.angela.client.support.junit.NodeOutputRule;
import org.terracotta.dynamic_config.test_support.ClusterDefinition;
import org.terracotta.dynamic_config.test_support.DynamicConfigIT;

import java.nio.file.Path;

import static com.tc.util.Assert.fail;
import static org.junit.Assert.assertThat;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.containsLog;

@ClusterDefinition(nodesPerStripe = 2, autoStart = false)
public class AutoActivateNewPassive1x2IT extends DynamicConfigIT {

  @Rule public final NodeOutputRule out = new NodeOutputRule();

  @Test
  public void test_auto_activation_success_for_1x1_cluster() {
    Path configurationFile = copyConfigProperty("/config-property-files/1x1.properties");
    startNode(1, 1, "--auto-activate", "-f", configurationFile.toString(), "--config-dir", "config/stripe1/1-1");
    waitForActive(1, 1);
  }

  @Test
  public void test_auto_activation_failure_for_different_1x2_cluster() {
    startNode(1, 1, "--auto-activate", "-f", copyConfigProperty("/config-property-files/1x2.properties").toString(), "-s", "localhost", "-p", String.valueOf(getNodePort(1, 1)), "--config-dir", "config/stripe1/1-1");
    waitForActive(1, 1);

    out.clearLog(1, 2);
    try {
      startNode(1, 2,
          "--auto-activate", "-f", copyConfigProperty("/config-property-files/1x2-diff.properties").toString(),
          "-s", "localhost", "-p", String.valueOf(getNodePort(1, 2)),
          "--config-dir", "config/stripe1/node-1-2");
      fail();
    } catch (Exception e) {
      assertThat(out.getLog(1, 2), containsLog("Unable to find any change in active node matching the topology used to activate this passive node"));
    }
  }

  @Test
  public void test_auto_activation_success_for_1x2_cluster() {
    Path configurationFile = copyConfigProperty("/config-property-files/1x2.properties");
    startNode(1, 1, "--auto-activate", "-f", configurationFile.toString(), "-s", "localhost", "-p", String.valueOf(getNodePort(1, 1)), "--config-dir", "config/stripe1/1-1");
    waitForActive(1, 1);

    startNode(1, 2, "--auto-activate", "-f", configurationFile.toString(), "-s", "localhost", "-p", String.valueOf(getNodePort(1, 2)), "--config-dir", "config/stripe1/1-2");
    waitForPassive(1, 2);
  }

  @Test
  public void test_auto_activation_success_for_1x2_cluster_usingNodeName() {
    Path configurationFile = copyConfigProperty("/config-property-files/1x2.properties");
    startNode(1, 1, "--auto-activate", "-f", configurationFile.toString(), "-n", "node-1-1", "--config-dir", "config/stripe1/1-1");
    waitForActive(1, 1);

    startNode(1, 2, "--auto-activate", "-f", configurationFile.toString(), "-n", "node-1-2", "--config-dir", "config/stripe1/1-2");
    waitForPassive(1, 2);
  }
}