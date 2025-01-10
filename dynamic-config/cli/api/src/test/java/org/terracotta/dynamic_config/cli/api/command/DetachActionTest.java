/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
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
package org.terracotta.dynamic_config.cli.api.command;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.terracotta.common.struct.MemoryUnit;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Identifier;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.RawPath;
import org.terracotta.dynamic_config.api.model.Testing;
import org.terracotta.inet.HostPort;

import java.io.IOException;
import java.util.List;

import static java.util.stream.Collectors.toSet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.terracotta.diagnostic.model.LogicalServerState.DIAGNOSTIC;
import static org.terracotta.diagnostic.model.LogicalServerState.UNREACHABLE;
import static org.terracotta.dynamic_config.api.model.Testing.newTestCluster;
import static org.terracotta.dynamic_config.api.model.Testing.newTestStripe;
import static org.terracotta.dynamic_config.cli.api.converter.OperationType.NODE;
import static org.terracotta.dynamic_config.cli.api.converter.OperationType.STRIPE;
import static org.terracotta.testing.ExceptionMatcher.throwing;

/**
 * @author Mathieu Carbou
 */
public class DetachActionTest extends TopologyActionTest<DetachAction> {

  Node node1_1 = Testing.newTestNode("node1_1", "localhost", 9410, Testing.N_UIDS[1])
      .unsetDataDirs()
      .putDataDir("cache", RawPath.valueOf("/data/cache1_1"));

  Node node1_2 = Testing.newTestNode("node1_2", "localhost", 9411, Testing.N_UIDS[2])
      .unsetDataDirs()
      .putDataDir("cache", RawPath.valueOf("/data/cache1_2"));

  Node node2_1 = Testing.newTestNode("node2_1", "localhost", 9412, Testing.N_UIDS[3])
      .unsetDataDirs()
      .putDataDir("cache", RawPath.valueOf("/data/cache2_1"));

  Node node2_2 = Testing.newTestNode("node2_2", "localhost", 9413, Testing.N_UIDS[4])
      .unsetDataDirs()
      .putDataDir("cache", RawPath.valueOf("/data/cache2_2"));

  Node strayNode = Testing.newTestNode("stray-node", "stray-host", 12345, Testing.N_UIDS[5])
      .unsetDataDirs()
      .putDataDir("cache", RawPath.valueOf("/data/stray-cache"));

  Cluster cluster = newTestCluster("my-cluster",
      newTestStripe("stripe1", Testing.S_UIDS[1]).addNodes(node1_1, node1_2),
      newTestStripe("stripe2", Testing.S_UIDS[2]).addNodes(node2_1, node2_2))
      .putOffheapResource("foo", 1, MemoryUnit.GB);

  @Captor
  ArgumentCaptor<Cluster> newCluster;

  @Override
  protected DetachAction newTopologyCommand() {
    return new DetachAction();
  }

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();

    when(topologyServiceMock(node1_1.getInternalHostPort()).isScalingDenied()).thenReturn(false);
    when(topologyServiceMock(node1_2.getInternalHostPort()).isScalingDenied()).thenReturn(false);
    when(topologyServiceMock(node2_1.getInternalHostPort()).isScalingDenied()).thenReturn(false);
    when(topologyServiceMock(node2_2.getInternalHostPort()).isScalingDenied()).thenReturn(false);

    when(topologyServiceMock(node1_1.getInternalHostPort()).getUpcomingNodeContext()).thenReturn(new NodeContext(cluster, node1_1.getUID()));
    when(topologyServiceMock(node1_1.getInternalHostPort()).getRuntimeNodeContext()).thenReturn(new NodeContext(cluster, node1_1.getUID()));
    when(topologyServiceMock(node1_1.getInternalHostPort()).isActivated()).thenReturn(false);

    when(topologyServiceMock(node1_2.getInternalHostPort()).getUpcomingNodeContext()).thenReturn(new NodeContext(cluster, node1_2.getUID()));
    when(topologyServiceMock(node1_2.getInternalHostPort()).getRuntimeNodeContext()).thenReturn(new NodeContext(cluster, node1_2.getUID()));
    when(topologyServiceMock(node1_2.getInternalHostPort()).isActivated()).thenReturn(false);

    when(topologyServiceMock(node2_1.getInternalHostPort()).getUpcomingNodeContext()).thenReturn(new NodeContext(cluster, node2_1.getUID()));
    when(topologyServiceMock(node2_1.getInternalHostPort()).getRuntimeNodeContext()).thenReturn(new NodeContext(cluster, node2_1.getUID()));
    when(topologyServiceMock(node2_1.getInternalHostPort()).isActivated()).thenReturn(false);

    when(diagnosticServiceMock(node1_1.getInternalHostPort()).getLogicalServerState()).thenReturn(DIAGNOSTIC);
    when(diagnosticServiceMock(node1_2.getInternalHostPort()).getLogicalServerState()).thenReturn(DIAGNOSTIC);
    when(diagnosticServiceMock(node2_1.getInternalHostPort()).getLogicalServerState()).thenReturn(DIAGNOSTIC);
    when(diagnosticServiceMock(node2_2.getInternalHostPort()).getLogicalServerState()).thenReturn(UNREACHABLE);
  }

  @Test
  public void test_validate_failures() {
    DetachAction command = newCommand();
    command.setSourceIdentifier(Identifier.valueOf("localhost:9410"));
    command.setOperationType(NODE);
    command.setDestinationHostPort(HostPort.create("localhost", 9410));

    assertThat(
        command::run,
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("The destination and the source nodes must not be the same")))));
  }

  @Test
  public void testDetachNode_success_srcPartOfDestStripe_by_addr() throws IOException {
    DetachAction command = newCommand();
    command.setSourceIdentifier(Identifier.valueOf(node1_2.getInternalHostPort().toString()));
    command.setOperationType(NODE);
    command.setDestinationHostPort(node1_1.getInternalHostPort());

    assertDetachNodeSucess(command);
  }

  @Test
  public void testDetachNode_success_srcPartOfDestStripe_by_uid() throws IOException {
    DetachAction command = newCommand();
    command.setSourceIdentifier(Identifier.valueOf(node1_2.getUID().toString()));
    command.setOperationType(NODE);
    command.setDestinationHostPort(node1_1.getInternalHostPort());

    assertDetachNodeSucess(command);
  }

  @Test
  public void testDetachNode_success_srcPartOfDestStripe_by_name() throws IOException {
    DetachAction command = newCommand();
    command.setSourceIdentifier(Identifier.valueOf(node1_2.getName()));
    command.setOperationType(NODE);
    command.setDestinationHostPort(node1_1.getInternalHostPort());

    assertDetachNodeSucess(command);
  }

  private void assertDetachNodeSucess(DetachAction command) throws IOException {
    command.run();

    // capture the new topology set calls
    verify(dynamicConfigServiceMock(node1_1.getInternalHostPort())).setUpcomingCluster(newCluster.capture());

    List<Cluster> allValues = newCluster.getAllValues();
    assertThat(allValues, hasSize(1));
    assertThat(
        allValues.get(0),
        is(equalTo(json.parse(getClass().getResource("/cluster3.json"), Cluster.class))));

    Cluster cluster = allValues.get(0);
    assertThat(cluster.getStripes(), hasSize(2));
    assertThat(cluster.getNodes(), hasSize(3));
    assertThat(cluster.getNodes().stream().map(Node::getUID).collect(toSet()), containsInAnyOrder(node1_1.getUID(), node2_1.getUID(), node2_2.getUID()));
  }

  @Test
  public void testDetachNode_fail_srcNotPartOfDestStripe() {
    DetachAction command = newCommand();
    command.setSourceIdentifier(Identifier.valueOf(node1_1.getInternalHostPort().toString()));
    command.setOperationType(NODE);
    command.setDestinationHostPort(node2_1.getInternalHostPort());
    assertThat(
        command::run,
        is(throwing(instanceOf(IllegalStateException.class)).andMessage(containsString("not present in the same stripe"))));
  }

  @Test
  public void testDetachNode_fail_randomSource() {
    DetachAction command = newCommand();
    command.setSourceIdentifier(Identifier.valueOf(strayNode.getInternalHostPort().toString()));
    command.setOperationType(NODE);
    command.setDestinationHostPort(cluster.getNode(Testing.N_UIDS[1]).get().getInternalHostPort());
    assertThat(
        command::run,
        is(throwing(instanceOf(IllegalStateException.class)).andMessage(containsString("not part of cluster"))));
  }

  @Test
  public void testDetachStripe_success_srcPartOfCluster_with_addr() throws IOException {
    DetachAction command = newCommand();
    command.setSourceIdentifier(Identifier.valueOf(node2_1.getInternalHostPort().toString()));
    command.setOperationType(STRIPE);
    command.setDestinationHostPort(node1_1.getInternalHostPort());

    assertDetachStripeSuccess(command);
  }

  @Test
  public void testDetachStripe_success_srcPartOfCluster_with_node_uid() throws IOException {
    DetachAction command = newCommand();
    command.setSourceIdentifier(Identifier.valueOf(node2_1.getUID().toString()));
    command.setOperationType(STRIPE);
    command.setDestinationHostPort(node1_1.getInternalHostPort());

    assertDetachStripeSuccess(command);
  }

  @Test
  public void testDetachStripe_success_srcPartOfCluster_with_node_name() throws IOException {
    DetachAction command = newCommand();
    command.setSourceIdentifier(Identifier.valueOf(node2_1.getName()));
    command.setOperationType(STRIPE);
    command.setDestinationHostPort(node1_1.getInternalHostPort());

    assertDetachStripeSuccess(command);
  }

  @Test
  public void testDetachStripe_success_srcPartOfCluster_with_stripe_uid() throws IOException {
    DetachAction command = newCommand();
    command.setSourceIdentifier(Identifier.valueOf(Testing.S_UIDS[2].toString()));
    command.setOperationType(STRIPE);
    command.setDestinationHostPort(node1_1.getInternalHostPort());

    assertDetachStripeSuccess(command);
  }

  @Test
  public void testDetachStripe_success_srcPartOfCluster_with_stripe_name() throws IOException {
    DetachAction command = newCommand();
    command.setSourceIdentifier(Identifier.valueOf("stripe2"));
    command.setOperationType(STRIPE);
    command.setDestinationHostPort(node1_1.getInternalHostPort());

    assertDetachStripeSuccess(command);
  }

  private void assertDetachStripeSuccess(DetachAction command) throws IOException {
    command.run();

    // capture the new topology set calls
    verify(dynamicConfigServiceMock("localhost", 9410)).setUpcomingCluster(newCluster.capture());

    List<Cluster> allValues = newCluster.getAllValues();
    assertThat(allValues, hasSize(1));
    assertThat(
        allValues.get(0),
        is(equalTo(json.parse(getClass().getResource("/cluster4.json"), Cluster.class))));

    Cluster cluster = allValues.get(0);
    assertThat(cluster.getStripes(), hasSize(1));
    assertThat(cluster.getNodes(), hasSize(2));
    assertThat(cluster.getNodes().stream().map(Node::getUID).collect(toSet()), containsInAnyOrder(node1_1.getUID(), node1_2.getUID()));
  }

  @Test
  public void testDetachStripe_fail_srcNotPartOfCluster() {
    DetachAction command = newCommand();
    command.setSourceIdentifier(Identifier.valueOf(strayNode.getInternalHostPort().toString()));
    command.setOperationType(STRIPE);
    command.setDestinationHostPort(node2_1.getInternalHostPort());
    assertThat(
        command::run,
        is(throwing(instanceOf(IllegalStateException.class)).andMessage(containsString("not part of cluster"))));
  }

  @Test
  public void testDetachStripe_fail_srcAndDestSameStripe() {
    DetachAction command = newCommand();
    command.setSourceIdentifier(Identifier.valueOf(node1_1.getInternalHostPort().toString()));
    command.setOperationType(STRIPE);
    command.setDestinationHostPort(node1_2.getInternalHostPort());
    assertThat(
        command::run,
        is(throwing(instanceOf(IllegalStateException.class)).andMessage(containsString("are part of the same stripe"))));
  }
}