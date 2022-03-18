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
import org.terracotta.dynamic_config.api.model.RawPath;
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.dynamic_config.api.model.Testing;

import java.io.IOException;
import java.util.List;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.terracotta.diagnostic.model.LogicalServerState.STARTING;
import static org.terracotta.diagnostic.model.LogicalServerState.UNREACHABLE;
import static org.terracotta.dynamic_config.cli.config_tool.converter.OperationType.NODE;
import static org.terracotta.dynamic_config.cli.config_tool.converter.OperationType.STRIPE;
import static org.terracotta.testing.ExceptionMatcher.throwing;

/**
 * @author Mathieu Carbou
 */
public class DetachCommandTest extends TopologyCommandTest<DetachCommand> {

  Node node1_1 = Testing.newTestNode("node1_1", "localhost", 9410)
      .unsetDataDirs()
      .putDataDir("cache", RawPath.valueOf("/data/cache1_1"));

  Node node1_2 = Testing.newTestNode("node1_2", "localhost", 9411)
      .unsetDataDirs()
      .putDataDir("cache", RawPath.valueOf("/data/cache1_2"));

  Node node2_1 = Testing.newTestNode("node2_1", "localhost", 9412)
      .unsetDataDirs()
      .putDataDir("cache", RawPath.valueOf("/data/cache2_1"));

  Node node2_2 = Testing.newTestNode("node2_2", "localhost", 9413)
      .unsetDataDirs()
      .putDataDir("cache", RawPath.valueOf("/data/cache2_2"));

  Node strayNode = Testing.newTestNode("stray-node", "stray-host", 12345)
      .unsetDataDirs()
      .putDataDir("cache", RawPath.valueOf("/data/stray-cache"));

  Cluster cluster = Testing.newTestCluster("my-cluster", new Stripe().setName("stripe1").addNodes(node1_1, node1_2), new Stripe().setName("stripe2").addNodes(node2_1, node2_2))
      .putOffheapResource("foo", 1, MemoryUnit.GB);

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

    when(topologyServiceMock(node1_1.getAddress()).getUpcomingNodeContext()).thenReturn(new NodeContext(cluster, node1_1.getAddress()));
    when(topologyServiceMock(node1_1.getAddress()).getRuntimeNodeContext()).thenReturn(new NodeContext(cluster, node1_1.getAddress()));
    when(topologyServiceMock(node1_1.getAddress()).isActivated()).thenReturn(false);

    when(topologyServiceMock(node1_2.getAddress()).getUpcomingNodeContext()).thenReturn(new NodeContext(cluster, node1_2.getAddress()));
    when(topologyServiceMock(node1_2.getAddress()).getRuntimeNodeContext()).thenReturn(new NodeContext(cluster, node1_2.getAddress()));
    when(topologyServiceMock(node1_2.getAddress()).isActivated()).thenReturn(false);

    when(topologyServiceMock(node2_1.getAddress()).getUpcomingNodeContext()).thenReturn(new NodeContext(cluster, node2_1.getAddress()));
    when(topologyServiceMock(node2_1.getAddress()).getRuntimeNodeContext()).thenReturn(new NodeContext(cluster, node2_1.getAddress()));
    when(topologyServiceMock(node2_1.getAddress()).isActivated()).thenReturn(false);

    when(diagnosticServiceMock(node1_1.getAddress()).getLogicalServerState()).thenReturn(STARTING);
    when(diagnosticServiceMock(node1_2.getAddress()).getLogicalServerState()).thenReturn(STARTING);
    when(diagnosticServiceMock(node2_1.getAddress()).getLogicalServerState()).thenReturn(STARTING);
    when(diagnosticServiceMock(node2_2.getAddress()).getLogicalServerState()).thenReturn(UNREACHABLE);
  }

  @Test
  public void testDetachNode_success_srcPartOfDestStripe() throws IOException {
    DetachCommand command = (DetachCommand) newCommand()
        .setOperationType(NODE)
        .setSource(node1_2.getAddress())
        .setDestination(node1_1.getAddress());
    command.validate();
    command.run();

    // capture the new topology set calls
    verify(dynamicConfigServiceMock(node1_1.getAddress())).setUpcomingCluster(newCluster.capture());

    List<Cluster> allValues = newCluster.getAllValues();
    assertThat(allValues, hasSize(1));
    assertThat(
        objectMapper.writeValueAsString(allValues.get(0)),
        allValues.get(0),
        is(equalTo(objectMapper.readValue(getClass().getResource("/cluster3.json"), Cluster.class))));

    Cluster cluster = allValues.get(0);
    assertThat(cluster.getStripes(), hasSize(2));
    assertThat(cluster.getNodeAddresses(), hasSize(3));
    assertThat(cluster.getNodeAddresses(), contains(node1_1.getAddress(), node2_1.getAddress(), node2_2.getAddress()));
  }

  @Test
  public void testDetachNode_fail_srcNotPartOfDestStripe() {
    DetachCommand command = (DetachCommand) newCommand()
        .setOperationType(NODE)
        .setSource(node1_1.getAddress())
        .setDestination(node2_1.getAddress());
    assertThat(
        command::validate,
        is(throwing(instanceOf(IllegalStateException.class)).andMessage(containsString("not present in the same stripe"))));
  }

  @Test
  public void testDetachNode_fail_randomSource() {
    TopologyCommand command = newCommand()
        .setOperationType(NODE)
        .setSource(strayNode.getAddress())
        .setDestination(cluster.getNode(1, 1).get().getAddress());
    assertThat(
        command::validate,
        is(throwing(instanceOf(IllegalStateException.class)).andMessage(containsString("not part of cluster"))));
  }

  @Test
  public void testDetachStripe_success_srcPartOfCluster() throws IOException {
    TopologyCommand command = newCommand()
        .setOperationType(STRIPE)
        .setDestination(node1_1.getAddress())
        .setSource(node2_1.getAddress());
    command.validate();
    command.run();

    // capture the new topology set calls
    verify(dynamicConfigServiceMock("localhost", 9410)).setUpcomingCluster(newCluster.capture());

    List<Cluster> allValues = newCluster.getAllValues();
    assertThat(allValues, hasSize(1));
    assertThat(
        objectMapper.writeValueAsString(allValues.get(0)),
        allValues.get(0),
        is(equalTo(objectMapper.readValue(getClass().getResource("/cluster4.json"), Cluster.class))));

    Cluster cluster = allValues.get(0);
    assertThat(cluster.getStripes(), hasSize(1));
    assertThat(cluster.getNodeAddresses(), hasSize(2));
    assertThat(cluster.getNodeAddresses(), contains(node1_1.getAddress(), node1_2.getAddress()));
  }

  @Test
  public void testDetachStripe_fail_srcNotPartOfCluster() {
    DetachCommand command = (DetachCommand) newCommand()
        .setOperationType(STRIPE)
        .setSource(strayNode.getAddress())
        .setDestination(node2_1.getAddress());
    assertThat(
        command::validate,
        is(throwing(instanceOf(IllegalStateException.class)).andMessage(containsString("not part of cluster"))));
  }

  @Test
  public void testDetachStripe_fail_srcAndDestSameStripe() {
    TopologyCommand command = newCommand()
        .setOperationType(STRIPE)
        .setSource(node1_1.getAddress())
        .setDestination(node1_2.getAddress());
    assertThat(
        command::validate,
        is(throwing(instanceOf(IllegalStateException.class)).andMessage(containsString("are part of the same stripe"))));
  }
}