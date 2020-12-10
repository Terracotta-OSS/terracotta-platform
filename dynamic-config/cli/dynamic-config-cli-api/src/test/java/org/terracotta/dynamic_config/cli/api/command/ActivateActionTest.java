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
package org.terracotta.dynamic_config.cli.api.command;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.terracotta.diagnostic.client.DiagnosticService;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.Testing;
import org.terracotta.dynamic_config.api.service.TopologyService;
import org.terracotta.dynamic_config.cli.api.BaseTest;
import org.terracotta.dynamic_config.cli.api.NomadTestHelper;
import org.terracotta.nomad.messages.CommitMessage;
import org.terracotta.nomad.messages.PrepareMessage;
import org.terracotta.nomad.messages.RollbackMessage;
import org.terracotta.nomad.server.NomadException;
import org.terracotta.nomad.server.NomadServer;

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.terracotta.diagnostic.model.LogicalServerState.PASSIVE;
import static org.terracotta.dynamic_config.api.model.Testing.newTestCluster;
import static org.terracotta.dynamic_config.api.model.Testing.newTestStripe;
import static org.terracotta.dynamic_config.cli.api.command.Injector.inject;
import static org.terracotta.nomad.messages.AcceptRejectResponse.accept;
import static org.terracotta.nomad.messages.AcceptRejectResponse.reject;
import static org.terracotta.nomad.messages.RejectionReason.UNACCEPTABLE;
import static org.terracotta.nomad.server.ChangeRequestState.COMMITTED;
import static org.terracotta.testing.ExceptionMatcher.throwing;

/**
 * @author Mathieu Carbou
 */
@RunWith(MockitoJUnitRunner.class)
public class ActivateActionTest extends BaseTest {

  private Path config;
  private final Cluster cluster = newTestCluster(
      "my-cluster",
      newTestStripe("stripe1", Testing.S_UIDS[1]).addNode(
          Testing.newTestNode("node1", "localhost", 9411, Testing.N_UIDS[1])
      ),
      newTestStripe("stripe2", Testing.S_UIDS[2]).addNodes(
          Testing.newTestNode("node2", "localhost", 9421, Testing.N_UIDS[2]),
          Testing.newTestNode("node3", "localhost", 9422, Testing.N_UIDS[3])
      ));
  private final int[] ports = cluster.getNodes().stream().map(Node::getInternalAddress).mapToInt(InetSocketAddress::getPort).toArray();

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();

    config = Paths.get(getClass().getResource("/my-cluster.properties").toURI());

    when(topologyServiceMock("localhost", 9411).getUpcomingNodeContext()).thenReturn(new NodeContext(cluster, Testing.N_UIDS[1]));
  }

  @Test
  public void test_cluster_name_discovery() {
    when(topologyServiceMock("localhost", 9411).isActivated()).thenReturn(false);
    when(topologyServiceMock("localhost", 9421).isActivated()).thenReturn(false);
    when(topologyServiceMock("localhost", 9422).isActivated()).thenReturn(false);

    ActivateAction command = command();
    command.setConfigPropertiesFile(config);
    doRunAndVerify("my-cluster", command);
  }

  @Test
  public void test_cluster_name_override() {
    when(topologyServiceMock("localhost", 9411).isActivated()).thenReturn(false);
    when(topologyServiceMock("localhost", 9421).isActivated()).thenReturn(false);
    when(topologyServiceMock("localhost", 9422).isActivated()).thenReturn(false);

    ActivateAction command = command();
    command.setClusterName("foo");
    command.setConfigPropertiesFile(config);
    doRunAndVerify("foo", command);
  }

  @Test
  public void test_activation_fails_if_all_nodes_are_activated() {
    when(topologyServiceMock("localhost", 9411).isActivated()).thenReturn(true);
    when(topologyServiceMock("localhost", 9421).isActivated()).thenReturn(true);
    when(topologyServiceMock("localhost", 9422).isActivated()).thenReturn(true);

    assertThat(
        () -> {
          ActivateAction cmd = command();
          cmd.setConfigPropertiesFile(config);
          cmd.run();
        },
        is(throwing(instanceOf(IllegalStateException.class)).andMessage(is(startsWith("Nodes are already activated: "))))
    );
  }

  @Test
  public void test_activation_fails_if_a_node_is_activated() {
    when(topologyServiceMock("localhost", 9411).isActivated()).thenReturn(true);
    when(topologyServiceMock("localhost", 9421).isActivated()).thenReturn(true);
    when(topologyServiceMock("localhost", 9422).isActivated()).thenReturn(false);

    assertThat(
        () -> {
          ActivateAction cmd = command();
          cmd.setConfigPropertiesFile(config);
          cmd.run();
        },
        is(throwing(instanceOf(IllegalStateException.class)).andMessage(is(equalTo("Detected a mix of activated and unconfigured nodes (or being repaired). Activated: [localhost:9411, localhost:9421], Unconfigured: [localhost:9422]"))))
    );
  }

  @Test
  public void test_nomad_prepare_fails() {
    ActivateAction command = command();
    command.setConfigPropertiesFile(config);

    UUID lastChangeUUID = UUID.randomUUID();

    IntStream.of(ports).forEach(rethrow(port -> {
      when(topologyServiceMock("localhost", port).isActivated()).thenReturn(false);

      NomadServer<NodeContext> mock = nomadServerMock("localhost", port);
      doReturn(NomadTestHelper.discovery(COMMITTED, lastChangeUUID)).when(mock).discover();
      when(mock.prepare(any(PrepareMessage.class))).thenReturn(reject(UNACCEPTABLE, "error", "host", "user"));
      when(mock.rollback(any(RollbackMessage.class))).thenReturn(accept());
    }));

    assertThat(
        command::run,
        is(throwing(instanceOf(IllegalStateException.class)).andMessage(allOf(
            containsString("Two-Phase commit failed with 3 messages(s):"),
            containsString("Prepare rejected for node localhost:9411. Reason: error"),
            containsString("Prepare rejected for node localhost:9421. Reason: error"),
            containsString("Prepare rejected for node localhost:9422. Reason: error")
        ))));

    IntStream.of(ports).forEach(rethrow(port -> {
      verify(dynamicConfigServiceMock("localhost", port), times(1)).activate(eq(command.getCluster()), eq(null));

      NomadServer<NodeContext> mock = nomadServerMock("localhost", port);
      verify(mock, times(2)).discover();
      verify(mock, times(1)).prepare(any(PrepareMessage.class));
      verify(mock, times(1)).rollback(any(RollbackMessage.class));
      verifyNoMoreInteractions(mock);
    }));

    assertThat(command.getCluster().getName(), is(equalTo("my-cluster")));
  }

  @Test
  public void test_nomad_commit_fails() {
    ActivateAction command = command();
    command.setConfigPropertiesFile(config);

    UUID lastChangeUUID = UUID.randomUUID();

    IntStream.of(ports).forEach(rethrow(port -> {
      when(topologyServiceMock("localhost", port).isActivated()).thenReturn(false);

      NomadServer<NodeContext> mock = nomadServerMock("localhost", port);
      doReturn(NomadTestHelper.discovery(COMMITTED, lastChangeUUID)).when(mock).discover();
      when(mock.prepare(any(PrepareMessage.class))).thenReturn(accept());
      when(mock.commit(any(CommitMessage.class))).thenThrow(new NomadException("an error"));
    }));

    assertThat(
        command::run,
        is(throwing(instanceOf(IllegalStateException.class)).andMessage(allOf(
            containsString("Two-Phase commit failed with 4 messages(s):"),
            containsString("Commit failed for node localhost:9411. Reason: an error"),
            containsString("Commit failed for node localhost:9421. Reason: an error"),
            containsString("Commit failed for node localhost:9422. Reason: an error")
        ))));

    IntStream.of(ports).forEach(rethrow(port -> {
      verify(dynamicConfigServiceMock("localhost", port), times(1)).activate(eq(command.getCluster()), eq(null));

      NomadServer<NodeContext> mock = nomadServerMock("localhost", port);
      verify(mock, times(2)).discover();
      verify(mock, times(1)).prepare(any(PrepareMessage.class));
      verify(mock, times(1)).commit(any(CommitMessage.class));
      verifyNoMoreInteractions(mock);
    }));

    assertThat(command.getCluster().getName(), is(equalTo("my-cluster")));
  }

  @Test
  public void test_activate_from_config_file() {
    ActivateAction command = command();
    command.setConfigPropertiesFile(config);
    doRunAndVerify("my-cluster", command);
  }

  @Test
  public void test_activate_from_config_file_and_cluster_name() {
    ActivateAction command = command();
    command.setConfigPropertiesFile(config);
    command.setClusterName("foo");
    doRunAndVerify("foo", command);
  }

  @Test
  public void test_activate_from_node_and_cluster_name() {
    ActivateAction command = command();
    command.setNode(InetSocketAddress.createUnresolved("localhost", 9411));
    command.setClusterName("foo");
    doRunAndVerify("foo", command);
  }

  private void doRunAndVerify(String clusterName, ActivateAction command) {
    UUID lastChangeUUID = UUID.randomUUID();

    IntStream.of(ports).forEach(rethrow(port -> {
      TopologyService topologyService = topologyServiceMock("localhost", port);
      NomadServer<NodeContext> mock = nomadServerMock("localhost", port);
      DiagnosticService diagnosticService = diagnosticServiceMock("localhost", port);

      when(topologyService.isActivated()).thenReturn(false);
      doReturn(NomadTestHelper.discovery(COMMITTED, lastChangeUUID)).when(mock).discover();
      when(mock.prepare(any(PrepareMessage.class))).thenReturn(accept());
      when(mock.commit(any(CommitMessage.class))).thenReturn(accept());
      when(diagnosticService.getLogicalServerState()).thenReturn(PASSIVE);
    }));

    command.run();

    IntStream.of(ports).forEach(rethrow(port -> {
      verify(dynamicConfigServiceMock("localhost", port), times(1)).activate(eq(command.getCluster()), eq(null));
      verify(dynamicConfigServiceMock("localhost", port), times(1)).restart(any());
      verify(nomadServerMock("localhost", port), times(2)).discover();
      verify(diagnosticServiceMock("localhost", port), times(1)).getLogicalServerState();
    }));

    assertThat(command.getCluster().getName(), is(equalTo(clusterName)));
  }

  private ActivateAction command() {
    ActivateAction command = new ActivateAction();
    inject(command, asList(diagnosticServiceProvider, multiDiagnosticServiceProvider, nomadManager, restartService, stopService, nomadEntityProvider));
    return command;
  }

  public static IntConsumer rethrow(EIntConsumer c) {
    return t -> {
      try {
        c.accept(t);
      } catch (Exception e) {
        if (e instanceof RuntimeException) {
          throw (RuntimeException) e;
        }
        throw new RuntimeException(e);
      }
    };
  }

  @FunctionalInterface
  public interface EIntConsumer {

    void accept(int t) throws Exception;

    default EIntConsumer andThen(EIntConsumer after) {
      requireNonNull(after);
      return (int t) -> {
        accept(t);
        after.accept(t);
      };
    }
  }
}
