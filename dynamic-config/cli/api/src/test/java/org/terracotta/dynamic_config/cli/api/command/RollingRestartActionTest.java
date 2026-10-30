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
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.junit.MockitoJUnitRunner;
import org.terracotta.diagnostic.model.LogicalServerState;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.Node.Endpoint;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.Testing;
import org.terracotta.dynamic_config.cli.api.BaseTest;
import org.terracotta.dynamic_config.cli.api.restart.RestartProgress;
import org.terracotta.dynamic_config.cli.api.restart.RestartService;

import java.time.Duration;
import java.util.Collection;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.terracotta.diagnostic.model.LogicalServerState.ACTIVE;
import static org.terracotta.diagnostic.model.LogicalServerState.PASSIVE;
import static org.terracotta.diagnostic.model.LogicalServerState.PASSIVE_SUSPENDED;
import static org.terracotta.dynamic_config.api.model.Testing.newTestCluster;
import static org.terracotta.dynamic_config.api.model.Testing.newTestStripe;
import static org.terracotta.dynamic_config.cli.api.command.Injector.inject;

@RunWith(MockitoJUnitRunner.class)
public class RollingRestartActionTest extends BaseTest {
  private final Node activeNode = Testing.newTestNode("node1_1", "localhost", 9410, Testing.N_UIDS[1]);
  private final Node passiveNode = Testing.newTestNode("node1_2", "localhost", 9411, Testing.N_UIDS[2]);
  private final Node suspendedNode = Testing.newTestNode("node1_3", "localhost", 9412, Testing.N_UIDS[3]);

  @Captor
  private ArgumentCaptor<Collection<Endpoint>> passiveRequest;
  @Captor
  private ArgumentCaptor<Collection<Endpoint>> activeRequest;

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    restartService = mock(RestartService.class);
  }

  @Test
  public void test_restarts_passive_before_active_and_awaits_recovery_between_stages() throws Exception {
    Cluster cluster = newTestCluster("my-cluster", newTestStripe("stripe1", Testing.S_UIDS[1]).addNodes(activeNode, passiveNode));
    Endpoint active = activeNode.determineEndpoint();
    Endpoint passive = passiveNode.determineEndpoint();

    // runtime topology is read through the node given as -connect-to
    when(topologyServiceMock(activeNode.getInternalHostPort()).getRuntimeNodeContext()).thenReturn(new NodeContext(cluster, activeNode.getUID()));
    when(diagnosticServiceMock(activeNode.getInternalHostPort()).getLogicalServerState()).thenReturn(ACTIVE);
    when(diagnosticServiceMock(passiveNode.getInternalHostPort()).getLogicalServerState()).thenReturn(PASSIVE);

    RestartProgress passiveProgress = completedProgress(passive, PASSIVE);
    RestartProgress activeProgress = completedProgress(active, PASSIVE);
    when(restartService.restartNodesIfPassives(any(), any(), any())).thenReturn(passiveProgress);
    when(restartService.restartNodesIfActives(any(), any(), any())).thenReturn(activeProgress);

    RollingRestartAction action = action();
    action.setNodes(singletonList(activeNode.getInternalHostPort()));
    action.run();

    InOrder inOrder = inOrder(restartService, passiveProgress, activeProgress);
    inOrder.verify(restartService).restartNodesIfPassives(passiveRequest.capture(), any(), any());
    inOrder.verify(passiveProgress).await(any(Duration.class));
    inOrder.verify(restartService).restartNodesIfActives(activeRequest.capture(), any(), any());
    inOrder.verify(activeProgress).await(any(Duration.class));

    assertThat(passiveRequest.getValue(), containsInAnyOrder(passive));
    assertThat(activeRequest.getValue(), containsInAnyOrder(active));

    verify(topologyServiceMock(activeNode.getInternalHostPort())).getRuntimeNodeContext();

    // the action must not run the standalone pre-check: server-side restartIfPassive / restartIfActive are authoritative
    verify(topologyServiceMock(activeNode.getInternalHostPort()), never()).mustBeRestarted();
    verify(topologyServiceMock(passiveNode.getInternalHostPort()), never()).mustBeRestarted();

    verifyNoMoreInteractions(restartService);
  }

  @Test
  public void test_only_active_and_passive_nodes_are_restart_targets() throws Exception {
    Cluster cluster = newTestCluster("my-cluster", newTestStripe("stripe1", Testing.S_UIDS[1]).addNodes(activeNode, passiveNode, suspendedNode));
    Endpoint active = activeNode.determineEndpoint();
    Endpoint passive = passiveNode.determineEndpoint();
    Endpoint suspended = suspendedNode.determineEndpoint();

    when(topologyServiceMock(activeNode.getInternalHostPort()).getRuntimeNodeContext()).thenReturn(new NodeContext(cluster, activeNode.getUID()));
    when(diagnosticServiceMock(activeNode.getInternalHostPort()).getLogicalServerState()).thenReturn(ACTIVE);
    when(diagnosticServiceMock(passiveNode.getInternalHostPort()).getLogicalServerState()).thenReturn(PASSIVE);
    when(diagnosticServiceMock(suspendedNode.getInternalHostPort()).getLogicalServerState()).thenReturn(PASSIVE_SUSPENDED);

    RestartProgress passiveProgress = completedProgress(passive, PASSIVE);
    RestartProgress activeProgress = completedProgress(active, PASSIVE);
    when(restartService.restartNodesIfPassives(any(), any(), any())).thenReturn(passiveProgress);
    when(restartService.restartNodesIfActives(any(), any(), any())).thenReturn(activeProgress);

    RollingRestartAction action = action();
    action.setNodes(singletonList(activeNode.getInternalHostPort()));
    action.run();

    verify(restartService).restartNodesIfPassives(passiveRequest.capture(), any(), any());
    verify(restartService).restartNodesIfActives(activeRequest.capture(), any(), any());
    verify(passiveProgress).await(any(Duration.class));
    verify(activeProgress).await(any(Duration.class));

    assertThat(passiveRequest.getValue(), containsInAnyOrder(passive));
    assertThat(passiveRequest.getValue(), not(hasItem(suspended)));
    assertThat(activeRequest.getValue(), containsInAnyOrder(active));
    assertThat(activeRequest.getValue(), not(hasItem(suspended)));

    verifyNoMoreInteractions(restartService);
  }

  private RestartProgress completedProgress(Endpoint restarted, LogicalServerState state) throws InterruptedException {
    RestartProgress progress = mock(RestartProgress.class);
    when(progress.await(any(Duration.class))).thenReturn(singletonMap(restarted, state));
    return progress;
  }

  private RollingRestartAction action() {
    RollingRestartAction action = new RollingRestartAction();
    inject(action, asList(diagnosticServiceProvider, multiDiagnosticServiceProvider, nomadManager, restartService, stopService, nomadEntityProvider, outputService, jsonFactory, json));
    return action;
  }
}
