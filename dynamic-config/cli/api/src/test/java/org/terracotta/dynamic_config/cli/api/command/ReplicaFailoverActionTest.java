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
package org.terracotta.dynamic_config.cli.api.command;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.terracotta.common.struct.MemoryUnit;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.Testing;
import org.terracotta.dynamic_config.cli.api.BaseTest;
import org.terracotta.dynamic_config.cli.api.output.OutputService;
import org.terracotta.inet.HostPort;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.terracotta.diagnostic.model.LogicalServerState.REPLICA;
import static org.terracotta.diagnostic.model.LogicalServerState.REPLICA_SUSPENDED;
import static org.terracotta.diagnostic.model.LogicalServerState.SYNCHRONIZING;
import static org.terracotta.diagnostic.model.LogicalServerState.UNREACHABLE;
import static org.terracotta.dynamic_config.api.model.Testing.newTestCluster;
import static org.terracotta.dynamic_config.api.model.Testing.newTestStripe;
import static org.terracotta.dynamic_config.cli.api.command.Injector.inject;
import static org.terracotta.testing.ExceptionMatcher.throwing;

@RunWith(MockitoJUnitRunner.class)
public class ReplicaFailoverActionTest extends BaseTest {
  private final Node node1_1 = Testing.newTestNode("node1_1", "localhost", 9410, Testing.N_UIDS[1]);
  private final Node node2_1 = Testing.newTestNode("node2_1", "localhost", 9411, Testing.N_UIDS[2]);
  Cluster cluster = newTestCluster("my-cluster",
    newTestStripe("stripe1", Testing.S_UIDS[1]).addNodes(node1_1),
    newTestStripe("stripe2", Testing.S_UIDS[2]).addNodes(node2_1))
    .putOffheapResource("foo", 1, MemoryUnit.GB);

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    when(topologyServiceMock("localhost", 9410).getRuntimeNodeContext()).thenReturn(new NodeContext(cluster, node1_1.getUID()));
    when(topologyServiceMock("localhost", 9410).getUpcomingNodeContext()).thenReturn(new NodeContext(cluster, node1_1.getUID()));
    when(topologyServiceMock("localhost", 9411).getUpcomingNodeContext()).thenReturn(new NodeContext(cluster, node2_1.getUID()));
    when(topologyServiceMock("localhost", 9411).getRuntimeNodeContext()).thenReturn(new NodeContext(cluster, node2_1.getUID()));
  }

  @Test
  public void test_replica_failover_fails_if_any_node_is_unreachable() {
    when(diagnosticServiceMock("localhost", 9410).getLogicalServerState()).thenReturn(UNREACHABLE);
    when(diagnosticServiceMock("localhost", 9411).getLogicalServerState()).thenReturn(REPLICA);

    ReplicaFailoverAction command = command();
    command.setNodes(singletonList(HostPort.create("localhost", 9410)));
    assertThat(
      command::run,
      is(throwing(instanceOf(IllegalStateException.class)).andMessage(containsString("Cannot execute replica-failover: the following nodes are unreachable:"))));
  }

  @Test
  public void test_replica_failover_fails_if_any_node_is_not_replica() {
    when(diagnosticServiceMock("localhost", 9410).getLogicalServerState()).thenReturn(REPLICA);
    when(diagnosticServiceMock("localhost", 9411).getLogicalServerState()).thenReturn(SYNCHRONIZING);

    ReplicaFailoverAction command = command();
    command.setNodes(singletonList(HostPort.create("localhost", 9410)));
    assertThat(
      command::run,
      is(throwing(instanceOf(IllegalStateException.class)).andMessage(containsString("Cannot trigger replica-failover as one or more nodes are not in the expected PASSIVE-REPLICA or PASSIVE-REPLICA-START state"))));
  }

  @Test
  public void test_replica_failover_warn_if_node_is_replica_suspended() {
    outputService = mock(OutputService.class);

    when(diagnosticServiceMock("localhost", 9410).getLogicalServerState()).thenReturn(REPLICA_SUSPENDED);
    when(diagnosticServiceMock("localhost", 9411).getLogicalServerState()).thenReturn(REPLICA);

    ReplicaFailoverAction command = command();
    command.setNodes(singletonList(HostPort.create("localhost", 9410)));
    command.run();
    verify(outputService).warn("The following nodes are in PASSIVE-REPLICA-START state (not fully synced): {}. The replica flag will be unset on these nodes," +
      " please restart them, and if they fail to transition to ACTIVE, please seek support.", "node1_1@localhost:9410"
    );
  }

  private ReplicaFailoverAction command() {
    ReplicaFailoverAction command = new ReplicaFailoverAction();
    inject(command, asList(diagnosticServiceProvider, multiDiagnosticServiceProvider, nomadManager, restartService, stopService, nomadEntityProvider, outputService, jsonFactory, json));
    return command;
  }
}
