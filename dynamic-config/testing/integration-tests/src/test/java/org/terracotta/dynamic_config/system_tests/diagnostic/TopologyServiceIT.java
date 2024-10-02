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
package org.terracotta.dynamic_config.system_tests.diagnostic;

import org.junit.Test;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.RawPath;
import org.terracotta.dynamic_config.api.model.Testing;
import org.terracotta.dynamic_config.api.service.ClusterFactory;
import org.terracotta.dynamic_config.api.service.Props;
import org.terracotta.dynamic_config.test_support.ClusterDefinition;
import org.terracotta.dynamic_config.test_support.DynamicConfigIT;

import java.nio.file.Path;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.terracotta.dynamic_config.api.model.FailoverPriority.availability;
import static org.terracotta.dynamic_config.api.model.Testing.newTestCluster;
import static org.terracotta.dynamic_config.api.model.Testing.newTestNode;
import static org.terracotta.dynamic_config.api.model.Testing.newTestStripe;

/**
 * @author Mathieu Carbou
 */
@ClusterDefinition(failoverPriority = "")
public class TopologyServiceIT extends DynamicConfigIT {

  Path config;
  Cluster cluster;

  @Override
  protected void startNode(int stripeId, int nodeId) {
    config = copyConfigProperty("/config-property-files/single-stripe.properties");
    cluster = new ClusterFactory().create(Props.load(config));
    startNode(1, 1,
        "--config-dir", getBaseDir().resolve(getNodeName(stripeId, nodeId)).resolve("config").toString(),
        "-f", config.toString()
    );
  }

  @Test
  public void test_getPendingTopology() throws Exception {
    withTopologyService("localhost", getNodePort(1, 1), topologyService -> {
      Cluster pendingCluster = topologyService.getUpcomingNodeContext().getCluster();

      Testing.replaceUIDs(pendingCluster);
      Testing.replaceUIDs(cluster);

      assertThat(pendingCluster, is(equalTo(cluster)));
      assertThat(pendingCluster, is(equalTo(newTestCluster(
          newTestStripe("stripe1")
              .setUID(Testing.S_UIDS[1])
              .addNodes(
                  newTestNode("node-1-1", "localhost", getNodePort())
                      .setUID(Testing.N_UIDS[2])
                      .setGroupPort(getNodeGroupPort(1, 1))
                      .setMetadataDir(RawPath.valueOf("metadata/stripe1"))
                      .setLogDir(RawPath.valueOf("logs/stripe1"))
                      .setBackupDir(RawPath.valueOf("backup/stripe1"))
                      .unsetDataDirs()
                      .putDataDir("main", RawPath.valueOf("user-data/main/stripe1"))
              ))
          .setUID(Testing.C_UIDS[0])
          .setClientLeaseDuration(20, SECONDS)
          .setFailoverPriority(availability()))));
    });
  }

}
