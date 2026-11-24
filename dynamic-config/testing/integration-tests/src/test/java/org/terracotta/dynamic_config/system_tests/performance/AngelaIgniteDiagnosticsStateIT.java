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
package org.terracotta.dynamic_config.system_tests.performance;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.terracotta.angela.client.config.ConfigurationContext;
import org.terracotta.angela.client.support.junit.AngelaOrchestratorRule;
import org.terracotta.angela.client.support.junit.AngelaRule;
import org.terracotta.angela.common.tcconfig.TerracottaServer;
import org.terracotta.angela.common.topology.Topology;
import org.terracotta.dynamic_config.cli.config_tool.ConfigTool;

import static org.terracotta.angela.client.config.custom.CustomConfigurationContext.customConfigurationContext;
import static org.terracotta.angela.common.AngelaProperties.DISTRIBUTION;
import static org.terracotta.angela.common.distribution.Distribution.distribution;
import static org.terracotta.angela.common.dynamic_cluster.Stripe.stripe;
import static org.terracotta.angela.common.provider.DynamicConfigManager.dynamicCluster;
import static org.terracotta.angela.common.tcconfig.TerracottaServer.server;
import static org.terracotta.angela.common.topology.LicenseType.TERRACOTTA_OS;
import static org.terracotta.angela.common.topology.PackageType.KIT;
import static org.terracotta.angela.common.topology.Version.version;

public class AngelaIgniteDiagnosticsStateIT {

  private static final String CLUSTER_NAME = "tc-cluster";

  @ClassRule
  public static final AngelaOrchestratorRule ANGELA_ORCHESTRATOR_RULE = new AngelaOrchestratorRule().igniteLocal();

  @Rule
  public final AngelaRule angela = new AngelaRule(ANGELA_ORCHESTRATOR_RULE::getAngelaOrchestrator, createConfigurationContext(), false, false);

  @Test
  public void activeServerReportsActiveCoordinatorState() throws Exception {
    long startNanos = System.nanoTime();
    angela.startNode(1, 1);
    int port = angela.getNodePort(1, 1);
    new ConfigTool().run(
        "-connect-timeout", "30s",
        "activate",
        "-connect-to", "localhost:" + port,
        "-cluster-name", CLUSTER_NAME,
        "-restart-wait-time", "120s");
    angela.waitForActive(1, 1);
    DiagnosticsRootStateAssert.assertActiveCoordinator(angela.getNodeAddress(1, 1));
    long readyNanos = System.nanoTime();
    System.out.println(
        "FRAMEWORK_PERF framework=angela-ignite-local ready_ns=" + (readyNanos - startNanos));
  }

  private static ConfigurationContext createConfigurationContext() {
    TerracottaServer node = server("node-1-1", "localhost")
        .configRepo("config")
        .logs("logs")
        .dataDir("main:data-dir")
        .offheap("main:512MB,foo:1GB")
        .metaData("metadata")
        .failoverPriority("availability")
        .clientReconnectWindow("10s")
        .clusterName(CLUSTER_NAME);

    return customConfigurationContext()
        .tsa(tsa -> tsa
            .clusterName(CLUSTER_NAME)
            .topology(new Topology(
                distribution(version(DISTRIBUTION.getValue()), KIT, TERRACOTTA_OS),
                dynamicCluster(stripe(node)))));
  }
}
