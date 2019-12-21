/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli.service.command;

import com.terracottatech.diagnostic.client.DiagnosticService;
import com.terracottatech.dynamic_config.cli.service.BaseTest;
import com.terracottatech.dynamic_config.cli.service.NomadTestHelper;
import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.dynamic_config.model.NodeContext;
import com.terracottatech.dynamic_config.model.Stripe;
import com.terracottatech.dynamic_config.service.api.TopologyService;
import com.terracottatech.nomad.messages.CommitMessage;
import com.terracottatech.nomad.messages.PrepareMessage;
import com.terracottatech.nomad.messages.RejectionReason;
import com.terracottatech.nomad.server.NomadException;
import com.terracottatech.nomad.server.NomadServer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.IntStream;

import static com.terracottatech.dynamic_config.cli.Injector.inject;
import static com.terracottatech.dynamic_config.model.Node.newDefaultNode;
import static com.terracottatech.nomad.messages.AcceptRejectResponse.accept;
import static com.terracottatech.nomad.messages.AcceptRejectResponse.reject;
import static com.terracottatech.nomad.server.ChangeRequestState.COMMITTED;
import static com.terracottatech.tools.detailed.state.LogicalServerState.PASSIVE;
import static com.terracottatech.utilities.fn.IntFn.rethrow;
import static com.terracottatech.utilities.hamcrest.ExceptionMatcher.throwing;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author Mathieu Carbou
 */
@RunWith(MockitoJUnitRunner.class)
public class ActivateCommandTest extends BaseTest {

  private Path config;
  private Path license;
  private final Cluster cluster = new Cluster(
      "my-cluster",
      new Stripe(
          newDefaultNode("node1", "localhost", 9411)
      ),
      new Stripe(
          newDefaultNode("node1", "localhost", 9421),
          newDefaultNode("node2", "localhost", 9422)
      ));
  private final int[] ports = cluster.getNodeAddresses().stream().mapToInt(InetSocketAddress::getPort).toArray();

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    config = Paths.get(getClass().getResource("/my-cluster.properties").toURI());
    license = Paths.get(getClass().getResource("/license.xml").toURI());

    when(topologyServiceMock("localhost", 9411).getUpcomingNodeContext()).thenReturn(new NodeContext(cluster, 1, 1));
    when(topologyServiceMock("localhost", 9411).getRuntimeNodeContext()).thenReturn(new NodeContext(cluster, 1, 1));
  }

  @Test
  public void test_validate() {
    assertThat(
        () -> command().validate(),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("One of node or config properties file must be specified")))));

    assertThat(
        () -> command()
            .setNode(InetSocketAddress.createUnresolved("localhost", 9410))
            .setConfigPropertiesFile(Paths.get("."))
            .validate(),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Either node or config properties file should be specified, not both")))));

    assertThat(
        () -> command()
            .setNode(InetSocketAddress.createUnresolved("localhost", 9410))
            .validate(),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Cluster name should be provided when node is specified")))));
  }


  @Test
  public void test_cluster_name_discovery() {
    when(topologyServiceMock("localhost", 9411).isActivated()).thenReturn(false);
    when(topologyServiceMock("localhost", 9421).isActivated()).thenReturn(false);
    when(topologyServiceMock("localhost", 9422).isActivated()).thenReturn(false);

    ActivateCommand command = command()
        .setConfigPropertiesFile(config);
    command.validate();
    assertThat(command.getCluster().getName(), is(equalTo("my-cluster")));
  }

  @Test
  public void test_cluster_name_override() {
    when(topologyServiceMock("localhost", 9411).isActivated()).thenReturn(false);
    when(topologyServiceMock("localhost", 9421).isActivated()).thenReturn(false);
    when(topologyServiceMock("localhost", 9422).isActivated()).thenReturn(false);

    ActivateCommand command = command()
        .setClusterName("foo")
        .setConfigPropertiesFile(config);
    command.validate();
    assertThat(command.getCluster().getName(), is(equalTo("foo")));

  }

  @Test
  public void test_activation_fails_if_all_nodes_are_activated() {
    when(topologyServiceMock("localhost", 9411).isActivated()).thenReturn(true);
    when(topologyServiceMock("localhost", 9421).isActivated()).thenReturn(true);
    when(topologyServiceMock("localhost", 9422).isActivated()).thenReturn(true);

    assertThat(
        () -> {
          ActivateCommand cmd = command().setConfigPropertiesFile(config);
          cmd.validate();
          cmd.run();
        },
        is(throwing(instanceOf(IllegalStateException.class)).andMessage(is(equalTo("Cluster is already activated"))))
    );
  }

  @Test
  public void test_activation_fails_if_a_node_is_activated() {
    when(topologyServiceMock("localhost", 9411).isActivated()).thenReturn(true);
    when(topologyServiceMock("localhost", 9421).isActivated()).thenReturn(true);
    when(topologyServiceMock("localhost", 9422).isActivated()).thenReturn(false);

    assertThat(
        () -> {
          ActivateCommand cmd = command().setConfigPropertiesFile(config);
          cmd.validate();
          cmd.run();
        },
        is(throwing(instanceOf(IllegalStateException.class)).andMessage(is(equalTo("Cluster is badly formed as it contains a mix of activated and unconfigured nodes. Activated: [localhost:9411, localhost:9421], Unconfigured: [localhost:9422]"))))
    );
  }

  @Test
  public void test_nomad_prepare_fails() {
    ActivateCommand command = command()
        .setConfigPropertiesFile(config);

    IntStream.of(ports).forEach(rethrow(port -> {
      when(topologyServiceMock("localhost", port).isActivated()).thenReturn(false);

      NomadServer<NodeContext> mock = nomadServerMock("localhost", port);
      doReturn(NomadTestHelper.discovery(COMMITTED)).when(mock).discover();
      when(mock.prepare(any(PrepareMessage.class))).thenReturn(reject(RejectionReason.UNACCEPTABLE, "error", "host", "user"));
    }));

    command.validate();

    assertThat(
        command::run,
        is(throwing(instanceOf(IllegalStateException.class)).andMessage(allOf(
            containsString("Two-Phase commit failed with 3 messages(s):"),
            containsString("Prepare rejected for server localhost:9411. Reason: error"),
            containsString("Prepare rejected for server localhost:9421. Reason: error"),
            containsString("Prepare rejected for server localhost:9422. Reason: error")
        ))));

    IntStream.of(ports).forEach(rethrow(port -> {
      verify(dynamicConfigServiceMock("localhost", port), times(1)).prepareActivation(eq(command.getCluster()), anyString());

      NomadServer<NodeContext> mock = nomadServerMock("localhost", port);
      verify(mock, times(2)).discover();
      verify(mock, times(1)).prepare(any(PrepareMessage.class));
      verifyNoMoreInteractions(mock);
    }));

    assertThat(command.getCluster().getName(), is(equalTo("my-cluster")));
  }

  @Test
  public void test_nomad_commit_fails() {
    ActivateCommand command = command()
        .setConfigPropertiesFile(config);

    IntStream.of(ports).forEach(rethrow(port -> {
      when(topologyServiceMock("localhost", port).isActivated()).thenReturn(false);

      NomadServer<NodeContext> mock = nomadServerMock("localhost", port);
      doReturn(NomadTestHelper.discovery(COMMITTED)).when(mock).discover();
      when(mock.prepare(any(PrepareMessage.class))).thenReturn(accept());
      when(mock.commit(any(CommitMessage.class))).thenThrow(new NomadException("an error"));
    }));

    command.validate();

    assertThat(
        command::run,
        is(throwing(instanceOf(IllegalStateException.class)).andMessage(allOf(
            containsString("Two-Phase commit failed with 4 messages(s):"),
            containsString("Commit failed for server localhost:9411. Reason: an error"),
            containsString("Commit failed for server localhost:9421. Reason: an error"),
            containsString("Commit failed for server localhost:9422. Reason: an error")
        ))));

    IntStream.of(ports).forEach(rethrow(port -> {
      verify(dynamicConfigServiceMock("localhost", port), times(1)).prepareActivation(eq(command.getCluster()), anyString());

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
    ActivateCommand command = command()
        .setConfigPropertiesFile(config);
    doRunAndVerify("my-cluster", command);
  }

  @Test
  public void test_activate_from_config_file_and_cluster_name() {
    ActivateCommand command = command()
        .setConfigPropertiesFile(config)
        .setClusterName("foo");
    doRunAndVerify("foo", command);
  }

  @Test
  public void test_activate_from_node_and_cluster_name() {
    ActivateCommand command = command()
        .setNode(InetSocketAddress.createUnresolved("localhost", 9411))
        .setClusterName("foo");
    doRunAndVerify("foo", command);
  }

  private void doRunAndVerify(String clusterName, ActivateCommand command) {
    IntStream.of(ports).forEach(rethrow(port -> {
      TopologyService topologyService = topologyServiceMock("localhost", port);
      NomadServer<NodeContext> mock = nomadServerMock("localhost", port);
      DiagnosticService diagnosticService = diagnosticServiceMock("localhost", port);

      when(topologyService.isActivated()).thenReturn(false);
      doReturn(NomadTestHelper.discovery(COMMITTED)).when(mock).discover();
      when(mock.prepare(any(PrepareMessage.class))).thenReturn(accept());
      when(mock.commit(any(CommitMessage.class))).thenReturn(accept());
      when(diagnosticService.getLogicalServerState()).thenReturn(PASSIVE);
    }));

    command.validate();
    command.run();

    IntStream.of(ports).forEach(rethrow(port -> {
      verify(dynamicConfigServiceMock("localhost", port), times(1)).prepareActivation(eq(command.getCluster()), anyString());
      verify(dynamicConfigServiceMock("localhost", port), times(1)).restart(any());
      verify(nomadServerMock("localhost", port), times(2)).discover();
      verify(diagnosticServiceMock("localhost", port), times(1)).getLogicalServerState();
    }));

    assertThat(command.getCluster().getName(), is(equalTo(clusterName)));
  }

  private ActivateCommand command() {
    ActivateCommand command = new ActivateCommand()
        .setLicenseFile(license);
    inject(command, diagnosticServiceProvider, multiDiagnosticServiceProvider, nomadManager, restartService);
    return command;
  }

}
