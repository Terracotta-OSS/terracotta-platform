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
package org.terracotta.dynamic_config.cli.config_tool.command;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.terracotta.common.struct.MemoryUnit;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.json.Json;

import java.nio.file.Paths;
import java.util.List;

import static java.net.InetSocketAddress.createUnresolved;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.terracotta.diagnostic.model.LogicalServerState.STARTING;
import static org.terracotta.dynamic_config.cli.config_tool.converter.OperationType.NODE;
import static org.terracotta.dynamic_config.cli.config_tool.converter.OperationType.STRIPE;

/**
 * @author Mathieu Carbou
 */
public class DetachCommandTest extends TopologyCommandTest<DetachCommand> {

  Node node0 = Node.newDefaultNode("node0", "localhost", 9410)
      .setDataDir("cache", Paths.get("/data/cache0"));

  Node node1 = Node.newDefaultNode("node1", "localhost", 9411)
      .setDataDir("cache", Paths.get("/data/cache1"));

  Cluster cluster = Cluster.newDefaultCluster(new Stripe(node0), new Stripe(node1))
      .setOffheapResource("foo", 1, MemoryUnit.GB);

  @Captor
  ArgumentCaptor<Cluster> newCluster;

  @Override
  protected DetachCommand newTopologyCommand() {
    return new DetachCommand();
  }

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();

    when(topologyServiceMock("localhost", 9410).getUpcomingNodeContext()).thenReturn(new NodeContext(cluster, node0.getNodeAddress()));

    when(topologyServiceMock("localhost", 9410).getRuntimeNodeContext()).thenReturn(new NodeContext(cluster, node0.getNodeAddress()));

    when(topologyServiceMock("localhost", 9410).isActivated()).thenReturn(false);
    when(topologyServiceMock("localhost", 9411).isActivated()).thenReturn(false);

    when(diagnosticServiceMock("localhost", 9410).getLogicalServerState()).thenReturn(STARTING);
    when(diagnosticServiceMock("localhost", 9411).getLogicalServerState()).thenReturn(STARTING);
  }

  @Test
  public void test_detach_nodes_from_stripe() {
    TopologyCommand command = newCommand()
        .setOperationType(NODE)
        .setDestination("localhost", 9410)
        .setSource(createUnresolved("localhost", 9411));
    command.validate();
    command.run();

    // capture the new topology set calls
    verify(dynamicConfigServiceMock("localhost", 9410)).setUpcomingCluster(newCluster.capture());

    List<Cluster> allValues = newCluster.getAllValues();
    assertThat(allValues, hasSize(1));
    assertThat(Json.toJson(allValues.get(0)), allValues.get(0), is(equalTo(Json.parse(getClass().getResource("/cluster3.json"), Cluster.class))));

    Cluster cluster = allValues.get(0);
    assertThat(cluster.getStripes(), hasSize(1));
    assertThat(cluster.getNodeAddresses(), hasSize(1));
    assertThat(cluster.getNodeAddresses(), contains(createUnresolved("localhost", 9410)));
  }

  @Test
  public void test_detach_stripe() {
    TopologyCommand command = newCommand()
        .setOperationType(STRIPE)
        .setDestination("localhost", 9410)
        .setSource(createUnresolved("localhost", 9411));
    command.validate();
    command.run();

    // capture the new topology set calls
    verify(dynamicConfigServiceMock("localhost", 9410)).setUpcomingCluster(newCluster.capture());

    List<Cluster> allValues = newCluster.getAllValues();
    assertThat(allValues, hasSize(1));
    assertThat(allValues.get(0), is(equalTo(Json.parse(getClass().getResource("/cluster3.json"), Cluster.class))));

    Cluster cluster = allValues.get(0);
    assertThat(cluster.getStripes(), hasSize(1));
    assertThat(cluster.getNodeAddresses(), hasSize(1));
    assertThat(cluster.getNodeAddresses(), contains(createUnresolved("localhost", 9410)));
  }
}