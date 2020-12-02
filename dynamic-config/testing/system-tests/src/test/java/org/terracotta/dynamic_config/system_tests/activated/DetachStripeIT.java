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
import org.junit.Before;
import org.junit.Test;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.dynamic_config.entity.topology.client.DynamicTopologyEntity;
import org.terracotta.dynamic_config.entity.topology.client.DynamicTopologyEntityFactory;
import org.terracotta.dynamic_config.test_support.ClusterDefinition;
import org.terracotta.dynamic_config.test_support.DynamicConfigIT;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.successful;

@ClusterDefinition(stripes = 2, nodesPerStripe = 2, autoStart = false)
public class DetachStripeIT extends DynamicConfigIT {

  public DetachStripeIT() {
    super(Duration.ofSeconds(180));
  }

  @Before
  public void setup() throws Exception {
    startNode(1, 1);
    waitForDiagnostic(1, 1);
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 1)).getNodeCount(), is(equalTo(1)));
    // start the second node
    startNode(1, 2);
    waitForDiagnostic(1, 2);
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 2)).getNodeCount(), is(equalTo(1)));
    //attach the second node
    assertThat(invokeConfigTool("attach", "-d", "localhost:" + getNodePort(1, 1), "-s", "localhost:" + getNodePort(1, 2)), is(successful()));

    // stripe-2
    startNode(2, 1);
    waitForDiagnostic(2, 1);
    assertThat(getUpcomingCluster("localhost", getNodePort(2, 1)).getNodeCount(), is(equalTo(1)));
    startNode(2, 2);
    waitForDiagnostic(2, 2);
    assertThat(getUpcomingCluster("localhost", getNodePort(2, 2)).getNodeCount(), is(equalTo(1)));
    assertThat(invokeConfigTool("attach", "-d", "localhost:" + getNodePort(2, 1), "-s", "localhost:" + getNodePort(2, 2)), is(successful()));

    // attach the two stripes to form a 2x2 cluster
    assertThat(invokeConfigTool("attach", "-t", "stripe", "-d", "localhost:" + getNodePort(1, 1), "-s", "localhost:" + getNodePort(2, 1)), is(successful()));

    //Activate cluster
    activateCluster();
    waitForNPassives(1, 1);
    waitForNPassives(2, 1);
  }

  @Test
  public void test_detach_stripe_from_activated_cluster() throws Exception {
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 1)).getNodeCount(), is(equalTo(4)));
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 1)).getStripeCount(), is(equalTo(2)));

    // detach second stripe from the activated 2x2 cluster to form a 1x2 cluster
    assertThat(invokeConfigTool("detach", "-t", "stripe", "-d", "localhost:" + getNodePort(1, 1), "-s", "localhost:" + getNodePort(2, 1)), is(successful()));

    // verify the #nodes in the new topology of the cluster
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 1)).getNodeCount(), is(equalTo(2)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, 2)).getNodeCount(), is(equalTo(2)));

    // verify the #stripes in the new topology of the cluster
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 1)).getStripeCount(), is(equalTo(1)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, 2)).getStripeCount(), is(equalTo(1)));
  }

  @Test
  public void test_topology_entity_callback_onStripeRemoval() throws Exception {
    try (DynamicTopologyEntity dynamicTopologyEntity = DynamicTopologyEntityFactory.fetch(
        new TerracottaConnectionService(),
        Collections.singletonList(InetSocketAddress.createUnresolved("localhost", getNodePort(1, 1))),
        "dynamic-config-topology-entity",
        getConnectionTimeout(),
        new DynamicTopologyEntity.Settings().setRequestTimeout(getConnectionTimeout()),
        null)) {

      CountDownLatch called = new CountDownLatch(1);

      dynamicTopologyEntity.setListener(new DynamicTopologyEntity.Listener() {
        @Override
        public void onStripeRemoval(Cluster cluster, Stripe removedStripe) {
          called.countDown();
        }
      });

      invokeConfigTool("detach", "-t", "stripe", "-d", "localhost:" + getNodePort(1, 1), "-s", "localhost:" + getNodePort(2, 1));

      called.await();
    }
  }
}
