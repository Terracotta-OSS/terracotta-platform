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
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.dynamic_config.api.model.Testing;
import org.terracotta.dynamic_config.test_support.ClusterDefinition;
import org.terracotta.dynamic_config.test_support.DynamicConfigIT;

import java.nio.file.Paths;

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
        "--config-dir", getNodePath(stripeId, nodeId).resolve("config").toString(),
        "-f", copyConfigProperty("/config-property-files/single-stripe.properties").toString()
    );
  }

  @Test
  public void test_getPendingTopology() throws Exception {
    withTopologyService("localhost", getNodePort(1,1), topologyService -> {
      Cluster pendingCluster = topologyService.getUpcomingNodeContext().getCluster();

      // keep for debug please
      //System.out.println(toPrettyJson(pendingTopology));

      assertThat(pendingCluster, is(equalTo(Testing.newTestCluster(new Stripe(Testing.newTestNode("node-1-1", "localhost", getNodePort())
          .setGroupPort(getNodeGroupPort(1, 1))
          .setBindAddress("0.0.0.0")
          .setGroupBindAddress("0.0.0.0")
          .setMetadataDir(Paths.get("metadata", "stripe1"))
          .setLogDir(Paths.get("logs", "stripe1", "node-1-1"))
          .setBackupDir(Paths.get("backup", "stripe1"))
          .putDataDir("main", Paths.get("user-data", "main", "stripe1"))
      ))
          .setClientReconnectWindow(120, SECONDS)
          .setClientLeaseDuration(20, SECONDS)
          .setFailoverPriority(availability())
          .putOffheapResource("main", 512, MB))));
    });
  }

}
