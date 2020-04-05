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
package org.terracotta.dynamic_config.system_tests.diagnostic;

import org.junit.Test;
import org.terracotta.diagnostic.client.DiagnosticService;
import org.terracotta.diagnostic.client.DiagnosticServiceFactory;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.dynamic_config.api.service.TopologyService;
import org.terracotta.dynamic_config.test_support.ClusterDefinition;
import org.terracotta.dynamic_config.test_support.DynamicConfigIT;

import java.nio.file.Paths;
import java.time.Duration;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.terracotta.common.struct.MemoryUnit.MB;
import static org.terracotta.dynamic_config.api.model.FailoverPriority.availability;

/**
 * @author Mathieu Carbou
 */
@ClusterDefinition
public class TopologyServiceIT extends DynamicConfigIT {

  @Override
  protected void startNode(int stripeId, int nodeId) {
    startNode(1, 1,
        "--node-repository-dir", "terracotta" + combine(stripeId, nodeId) + "/repository",
        "-f", copyConfigProperty("/config-property-files/single-stripe.properties").toString()
    );
  }

  @Test
  public void test_getPendingTopology() throws Exception {
    try (DiagnosticService diagnosticService = DiagnosticServiceFactory.fetch(
        getNodeAddress(),
        getClass().getSimpleName(),
        Duration.ofSeconds(5),
        Duration.ofSeconds(5),
        null)
    ) {

      TopologyService proxy = diagnosticService.getProxy(TopologyService.class);
      Cluster pendingCluster = proxy.getUpcomingNodeContext().getCluster();

      // keep for debug please
      //System.out.println(toPrettyJson(pendingTopology));

      assertThat(pendingCluster, is(equalTo(Cluster.newDefaultCluster(new Stripe(Node.newDefaultNode("node-1-1", "localhost", getNodePort())
          .setNodeGroupPort(getNodeGroupPort())
          .setNodeBindAddress("0.0.0.0")
          .setNodeGroupBindAddress("0.0.0.0")
          .setNodeMetadataDir(Paths.get("metadata", "stripe1"))
          .setNodeLogDir(Paths.get("logs", "stripe1", "node-1-1"))
          .setNodeBackupDir(Paths.get("backup", "stripe1"))
          .setDataDir("main", Paths.get("user-data", "main", "stripe1"))
      ))
          .setClientReconnectWindow(120, SECONDS)
          .setClientLeaseDuration(20, SECONDS)
          .setFailoverPriority(availability())
          .setOffheapResource("main", 512, MB))));
    }
  }

}
