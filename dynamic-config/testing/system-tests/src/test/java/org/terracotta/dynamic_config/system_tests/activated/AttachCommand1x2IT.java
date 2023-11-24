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

import com.terracotta.connection.api.TerracottaConnectionService;
import org.junit.Test;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.UID;
import org.terracotta.dynamic_config.entity.topology.client.DynamicTopologyEntity;
import org.terracotta.dynamic_config.entity.topology.client.DynamicTopologyEntityFactory;
import org.terracotta.dynamic_config.test_support.ClusterDefinition;
import org.terracotta.dynamic_config.test_support.DynamicConfigIT;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.containsOutput;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.successful;

/**
 * @author Mathieu Carbou
 */
@ClusterDefinition(nodesPerStripe = 2, autoStart = false)
public class AttachCommand1x2IT extends DynamicConfigIT {

  @Test
  public void test_attach_to_activated_cluster() throws Exception {
    // activate a 1x1 cluster
    startNode(1, 1);
    activateCluster();
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 1)).getNodeCount(), is(equalTo(1)));

    // start a second node
    startNode(1, 2);
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 2)).getNodeCount(), is(equalTo(1)));

    // attach
    assertThat(
        configTool("attach", "-d", "localhost:" + getNodePort(1, 1), "-s", "localhost:" + getNodePort(1, 2)),
        is(successful()));
    waitForPassive(1, 2);

    assertThat(getUpcomingCluster("localhost", getNodePort(1, 1)).getNodeCount(), is(equalTo(2)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, 1)).getNodeCount(), is(equalTo(2)));

    assertThat(getUpcomingCluster("localhost", getNodePort(1, 2)).getNodeCount(), is(equalTo(2)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, 2)).getNodeCount(), is(equalTo(2)));

    stopNode(1, 1);
    waitForActive(1, 2);
  }

  @Test
  public void test_topology_entity_callback_onNodeAddition() throws Exception {
    // activate a 1x1 cluster
    startNode(1, 1);
    activateCluster();

    // start a second node
    startNode(1, 2);

    try (DynamicTopologyEntity dynamicTopologyEntity = DynamicTopologyEntityFactory.fetch(
        new TerracottaConnectionService(),
        Collections.singletonList(InetSocketAddress.createUnresolved("localhost", getNodePort())),
        "dynamic-config-topology-entity",
        getConnectionTimeout(),
        new DynamicTopologyEntity.Settings().setRequestTimeout(getDiagnosticOperationTimeout()),
        null)) {

      CountDownLatch called = new CountDownLatch(1);

      dynamicTopologyEntity.setListener(new DynamicTopologyEntity.Listener() {
        @Override
        public void onNodeAddition(Cluster cluster, UID addedNodeUID) {
          called.countDown();
        }
      });

      // attach
      assertThat(configTool("attach", "-d", "localhost:" + getNodePort(1, 1), "-s", "localhost:" + getNodePort(1, 2)), is(successful()));

      called.await();
    }
  }

  @Test
  public void test_attach_to_activated_cluster_requiring_restart() throws Exception {
    String destination = "localhost:" + getNodePort();

    // activate a 1x1 cluster
    startNode(1, 1);
    activateCluster();

    // do a change requiring a restart
    assertThat(
        configTool("set", "-s", destination, "-c", "stripe.1.node.1.tc-properties.foo=bar"),
        containsOutput("Restart required for nodes:"));

    // start a second node
    startNode(1, 2);

    // try to attach this node to the cluster
    assertThat(
        configTool("attach", "-d", destination, "-s", "localhost:" + getNodePort(1, 2)),
        containsOutput("is waiting to be restarted to apply some pending changes. Please refer to the Troubleshooting Guide for more help."));

    // try forcing the attach
    assertThat(configTool("attach", "-f", "-d", destination, "-s", "localhost:" + getNodePort(1, 2)), is(successful()));

    assertThat(getUpcomingCluster("localhost", getNodePort(1, 1)).getNodeCount(), is(equalTo(2)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, 1)).getNodeCount(), is(equalTo(2)));

    assertThat(getUpcomingCluster("localhost", getNodePort(1, 2)).getNodeCount(), is(equalTo(2)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, 2)).getNodeCount(), is(equalTo(2)));
  }
}
