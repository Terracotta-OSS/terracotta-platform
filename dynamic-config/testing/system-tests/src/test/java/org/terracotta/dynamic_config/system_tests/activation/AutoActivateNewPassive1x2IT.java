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
import org.terracotta.dynamic_config.api.service.Props;
import org.terracotta.dynamic_config.test_support.ClusterDefinition;
import org.terracotta.dynamic_config.test_support.DynamicConfigIT;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.tc.util.Assert.fail;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.containsOutput;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.successful;

@ClusterDefinition(nodesPerStripe = 2, autoStart = false)
public class AutoActivateNewPassive1x2IT extends DynamicConfigIT {

  @Test
  public void test_auto_activation_success_for_1x1_cluster() {
    Path configurationFile = copyConfigProperty("/config-property-files/1x1.properties");
    startNode(1, 1, "--auto-activate", "-f", configurationFile.toString(), "--config-dir", "config/stripe1/1-1");
    waitForActive(1, 1);
  }

  @Test
  public void test_auto_activation_success_for_1x1_cluster_no_failover() {
    Path configurationFile = copyConfigProperty("/config-property-files/1x1-no-fo.properties");
    startNode(1, 1, "--auto-activate", "-f", configurationFile.toString(), "--config-dir", "config/stripe1/1-1");
    waitForActive(1, 1);
  }

  @Test
  public void test_auto_activation_failure_for_different_1x2_cluster() {
    startNode(1, 1, "--auto-activate", "-f", copyConfigProperty("/config-property-files/1x2.properties").toString(), "-s", "localhost", "-p", String.valueOf(getNodePort(1, 1)), "--config-dir", "config/stripe1/1-1");
    waitForActive(1, 1);

    try {
      startNode(1, 2,
          "--auto-activate", "-f", copyConfigProperty("/config-property-files/1x2-diff.properties").toString(),
          "-s", "localhost", "-p", String.valueOf(getNodePort(1, 2)),
          "--config-dir", "config/stripe1/node-1-2");
      fail();
    } catch (Throwable e) {
      assertThatServerStdOut(getNode(1, 2), "Unable to find any change in the source node matching the topology used to activate this node");
    }
  }

  @Test
  public void test_auto_activation_failure_1x2_cluster_no_failover() {
    Path configurationFile = copyConfigProperty("/config-property-files/1x2-no-fo.properties");
    try {
      startNode(1, 1, "--auto-activate", "-f", configurationFile.toString(), "-s", "localhost", "-p", String.valueOf(getNodePort(1, 1)), "--config-dir", "config/stripe1/1-1");
      fail();
    } catch (Throwable e) {
      assertThatServerStdOut(getNode(1, 1), "failover-priority setting is not configured");
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
  public void test_auto_activation_success_for_1x2_cluster_with_default_stripe_name() {
    Path configurationFile = copyConfigProperty("/config-property-files/1x2_default_stripe_name.properties");
    startNode(1, 1, "--auto-activate", "-f", configurationFile.toString(), "-s", "localhost", "-p", String.valueOf(getNodePort(1, 1)), "--config-dir", "config/stripe1/1-1");
    waitForActive(1, 1);

    startNode(1, 2, "--auto-activate", "-f", configurationFile.toString(), "-s", "localhost", "-p", String.valueOf(getNodePort(1, 2)), "--config-dir", "config/stripe1/1-2");
    waitForPassive(1, 2);
  }

  @Test
  public void test_auto_activation_success_for_1x2_cluster_usingNodeName() {
    Path configurationFile = copyConfigProperty("/config-property-files/1x2.properties");
    startNode(1, 1, "--auto-activate", "-f", configurationFile.toString(), "-n", "node-1-1", "--config-dir", getBaseDir().resolve(Paths.get("config", "stripe1", "node-1-1")).toString());
    waitForActive(1, 1);

    startNode(1, 2, "--auto-activate", "-f", configurationFile.toString(), "-n", "node-1-2", "--config-dir", getBaseDir().resolve(Paths.get("config", "stripe1", "node-1-2")).toString());
    waitForPassive(1, 2);
  }

  @Test
  public void testNodeCanJoinAClusterWithChanges() throws IOException {
    // auto activating a stripe
    // The goal is to have an activated cluster with inside its topology some "room" to add a node that is not yet created
    // this situation can happen in case of node failure we need to replace, when auto-activating at startup, etc.
    Path configurationFile = copyConfigProperty("/config-property-files/1x2.properties");
    startNode(1, 1, "--auto-activate", "-f", configurationFile.toString(), "-s", "localhost", "-p", String.valueOf(getNodePort(1, 1)), "--config-dir", getBaseDir().resolve("node-1-1").toString());
    waitForActive(1, 1);

    // trigger some changes
    assertThat(configTool("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.logger-overrides=org.terracotta:TRACE"), is(successful()));

    // let's say we need to repair / or make a node join...
    // we will be able to add it through a restrictive activation
    // For a node to be able to join a topology, it needs to have EXACTLY the same topology information of the target cluster
    Path exportedConfigPath = tmpDir.getRoot().resolve("cluster.properties").toAbsolutePath();
    assertThat(configTool("export", "-s", "localhost:" + getNodePort(1, 1), "-f", exportedConfigPath.toString(), "-t", "properties"), is(successful()));
    //System.out.println(new String(Files.readAllBytes(exportedConfigPath), StandardCharsets.UTF_8));
    assertThat(Props.toString(Props.load(exportedConfigPath)), Props.load(exportedConfigPath).stringPropertyNames(), hasItem("stripe.1.node.1.logger-overrides"));

    startNode(1, 2);

    assertThat(
        configTool("activate", "-R", "-s", "localhost:" + getNodePort(1, 2), "-f", exportedConfigPath.toString()),
        allOf(containsOutput("No license specified for activation"), containsOutput("came back up")));
  }
}