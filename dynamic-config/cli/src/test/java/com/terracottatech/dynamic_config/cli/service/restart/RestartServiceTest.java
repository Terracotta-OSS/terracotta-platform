/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli.service.restart;

import com.terracottatech.diagnostic.client.DiagnosticOperationTimeoutException;
import com.terracottatech.diagnostic.client.DiagnosticService;
import com.terracottatech.diagnostic.client.connection.ConcurrencySizing;
import com.terracottatech.dynamic_config.cli.service.BaseTest;
import com.terracottatech.dynamic_config.diagnostic.DynamicConfigService;
import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.dynamic_config.model.Stripe;
import com.terracottatech.utilities.Tuple2;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.terracottatech.dynamic_config.model.Node.newDefaultNode;
import static com.terracottatech.tools.detailed.state.LogicalServerState.ACTIVE;
import static com.terracottatech.tools.detailed.state.LogicalServerState.PASSIVE;
import static com.terracottatech.tools.detailed.state.LogicalServerState.STARTING;
import static com.terracottatech.tools.detailed.state.LogicalServerState.UNINITIALIZED;
import static com.terracottatech.tools.detailed.state.LogicalServerState.UNKNOWN;
import static com.terracottatech.tools.detailed.state.LogicalServerState.UNREACHABLE;
import static com.terracottatech.utilities.TimeUnit.SECONDS;
import static com.terracottatech.utilities.mockito.Mocks.sleep;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Mathieu Carbou
 */
@RunWith(MockitoJUnitRunner.class)
public class RestartServiceTest extends BaseTest {

  private static final int[] PORTS = {9411, 9412, 9413, 9421, 9422, 9423};

  private RestartService restartService;
  private Cluster cluster;

  @Before
  @Override
  public void setUp() throws Exception {
    super.setUp();
    restartService = new RestartService(diagnosticServiceProvider, new ConcurrencySizing(), Duration.ofSeconds(2));
    cluster = new Cluster(
        "my-cluster",
        new Stripe(
            newDefaultNode("node1", "localhost", PORTS[0]),
            newDefaultNode("node2", "localhost", PORTS[1]),
            newDefaultNode("node3", "localhost", PORTS[2])
        ),
        new Stripe(
            newDefaultNode("node1", "localhost", PORTS[3]),
            newDefaultNode("node2", "localhost", PORTS[4]),
            newDefaultNode("node3", "localhost", PORTS[5])
        ));
  }

  @Test
  public void test_restart() throws InterruptedException {
    mockSuccessfulServerRestart();

    Map<InetSocketAddress, Tuple2<String, Exception>> errors = restartService.restartNodes(cluster.getNodeAddresses()).await();
    assertThat(errors.toString(), errors.size(), is(equalTo(0)));

    IntStream.of(PORTS).forEach(port -> {
      verify(dynamicConfigServiceMock("localhost", port), times(1)).restart();
      verify(diagnosticServiceMock("localhost", port), times(1)).getLogicalServerState();
    });
  }

  @Test
  public void test_restart_call_throws_DiagnosticOperationTimeoutException() throws InterruptedException {
    mockSuccessfulServerRestart();

    IntStream.of(PORTS).forEach(port -> {
      DynamicConfigService dynamicConfigService = dynamicConfigServiceMock("localhost", port);
      doThrow(new DiagnosticOperationTimeoutException("")).when(dynamicConfigService).restart();
    });

    Map<InetSocketAddress, Tuple2<String, Exception>> errors = restartService.restartNodes(cluster.getNodeAddresses()).await();
    assertThat(errors.toString(), errors.size(), is(equalTo(0)));

    IntStream.of(PORTS).forEach(port -> {
      verify(dynamicConfigServiceMock("localhost", port), times(1)).restart();
      verify(diagnosticServiceMock("localhost", port), times(1)).getLogicalServerState();
    });
  }

  @Test
  public void test_restart_call_fails() throws InterruptedException {
    IntStream.of(PORTS).forEach(port -> {
      DynamicConfigService dynamicConfigService = dynamicConfigServiceMock("localhost", port);
      doThrow(new RuntimeException("error")).when(dynamicConfigService).restart();
    });

    Map<InetSocketAddress, Tuple2<String, Exception>> errors = restartService.restartNodes(cluster.getNodeAddresses()).await();
    assertThat(errors.size(), is(equalTo(6)));
    assertThat(errors.values().stream().map(Tuple2::getT1).collect(Collectors.toList()), containsInAnyOrder(
        "Failed asking node localhost:9411 to restart: error",
        "Failed asking node localhost:9412 to restart: error",
        "Failed asking node localhost:9413 to restart: error",
        "Failed asking node localhost:9421 to restart: error",
        "Failed asking node localhost:9422 to restart: error",
        "Failed asking node localhost:9423 to restart: error"
    ));

    IntStream.of(PORTS).forEach(port -> {
      verify(dynamicConfigServiceMock("localhost", port), times(1)).restart();
      verify(diagnosticServiceMock("localhost", port), times(0)).getLogicalServerState();
    });
  }

  @Test
  public void test_stats_call_times_out() throws InterruptedException {
    mockSuccessfulServerRestart();

    when(diagnosticServiceMock("localhost", 9411).getLogicalServerState()).thenAnswer(sleep(5, SECONDS));

    Map<InetSocketAddress, Tuple2<String, Exception>> errors = restartService.restartNodes(cluster.getNodeAddresses()).await();
    assertThat(errors.toString(), errors.size(), is(equalTo(1)));
    assertThat(errors.values().stream().map(Tuple2::getT1).collect(Collectors.toList()), containsInAnyOrder(
        "Waiting for node localhost:9411 to restart timed out after 2000ms"
    ));

    IntStream.of(PORTS).forEach(port -> {
      verify(dynamicConfigServiceMock("localhost", port), times(1)).restart();
      verify(diagnosticServiceMock("localhost", port), atLeast(1)).getLogicalServerState();
    });
  }

  @Test
  public void test_server_restart_with_unexpected_state() throws InterruptedException {
    mockSuccessfulServerRestart();

    when(diagnosticServiceMock("localhost", 9411).getLogicalServerState()).thenReturn(null);
    when(diagnosticServiceMock("localhost", 9412).getLogicalServerState()).thenReturn(UNREACHABLE);
    when(diagnosticServiceMock("localhost", 9413).getLogicalServerState()).thenReturn(UNKNOWN);
    when(diagnosticServiceMock("localhost", 9421).getLogicalServerState()).thenReturn(STARTING);
    when(diagnosticServiceMock("localhost", 9422).getLogicalServerState()).thenReturn(UNINITIALIZED);

    Map<InetSocketAddress, Tuple2<String, Exception>> errors = restartService.restartNodes(cluster.getNodeAddresses()).await();
    assertThat(errors.toString(), errors.size(), is(equalTo(5)));
    assertThat(errors.values().stream().map(Tuple2::getT1).collect(Collectors.toList()), containsInAnyOrder(
        "Waiting for node localhost:9411 to restart timed out after 2000ms",
        "Waiting for node localhost:9412 to restart timed out after 2000ms",
        "Waiting for node localhost:9413 to restart timed out after 2000ms",
        "Waiting for node localhost:9421 to restart timed out after 2000ms",
        "Waiting for node localhost:9422 to restart timed out after 2000ms"
    ));

    IntStream.of(PORTS).forEach(port -> {
      verify(dynamicConfigServiceMock("localhost", port), times(1)).restart();
      verify(diagnosticServiceMock("localhost", port), atLeast(1)).getLogicalServerState();
    });
  }

  private void mockSuccessfulServerRestart() {
    DiagnosticService diagnosticService11 = diagnosticServiceMock("localhost", 9411);
    DiagnosticService diagnosticService12 = diagnosticServiceMock("localhost", 9412);
    DiagnosticService diagnosticService13 = diagnosticServiceMock("localhost", 9413);
    DiagnosticService diagnosticService21 = diagnosticServiceMock("localhost", 9421);
    DiagnosticService diagnosticService22 = diagnosticServiceMock("localhost", 9422);
    DiagnosticService diagnosticService23 = diagnosticServiceMock("localhost", 9423);

    when(diagnosticService11.getLogicalServerState()).thenReturn(ACTIVE);
    when(diagnosticService12.getLogicalServerState()).thenReturn(PASSIVE);
    when(diagnosticService13.getLogicalServerState()).thenReturn(PASSIVE);
    when(diagnosticService21.getLogicalServerState()).thenReturn(ACTIVE);
    when(diagnosticService22.getLogicalServerState()).thenReturn(PASSIVE);
    when(diagnosticService23.getLogicalServerState()).thenReturn(PASSIVE);

    IntStream.of(PORTS).forEach(port -> {
      DynamicConfigService dynamicConfigService = dynamicConfigServiceMock("localhost", port);
      doNothing().when(dynamicConfigService).restart();
    });
  }

}
