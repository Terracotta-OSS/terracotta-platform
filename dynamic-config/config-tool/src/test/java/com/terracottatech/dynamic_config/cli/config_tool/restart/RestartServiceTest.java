/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli.config_tool.restart;

import com.terracottatech.common.struct.TimeUnit;
import com.terracottatech.diagnostic.client.DiagnosticOperationTimeoutException;
import com.terracottatech.diagnostic.client.DiagnosticService;
import com.terracottatech.diagnostic.client.connection.ConcurrencySizing;
import com.terracottatech.diagnostic.client.connection.DiagnosticServiceProviderException;
import com.terracottatech.dynamic_config.api.model.Cluster;
import com.terracottatech.dynamic_config.api.model.Stripe;
import com.terracottatech.dynamic_config.api.service.DynamicConfigService;
import com.terracottatech.dynamic_config.cli.config_tool.BaseTest;
import com.terracottatech.tools.detailed.state.LogicalServerState;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Map;
import java.util.stream.IntStream;

import static com.terracottatech.common.struct.TimeUnit.SECONDS;
import static com.terracottatech.dynamic_config.api.model.Node.newDefaultNode;
import static com.terracottatech.tools.detailed.state.LogicalServerState.ACTIVE;
import static com.terracottatech.tools.detailed.state.LogicalServerState.ACTIVE_SUSPENDED;
import static com.terracottatech.tools.detailed.state.LogicalServerState.PASSIVE;
import static com.terracottatech.tools.detailed.state.LogicalServerState.STARTING;
import static com.terracottatech.tools.detailed.state.LogicalServerState.SYNCHRONIZING;
import static com.terracottatech.tools.detailed.state.LogicalServerState.UNINITIALIZED;
import static com.terracottatech.tools.detailed.state.LogicalServerState.UNKNOWN;
import static com.terracottatech.tools.detailed.state.LogicalServerState.UNREACHABLE;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
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

    RestartProgress restartProgress = restartService.restartNodes(cluster.getNodeAddresses());
    assertThat(restartProgress.getErrors().size(), is(equalTo(0)));

    Map<InetSocketAddress, LogicalServerState> restarted = restartProgress.await(Duration.ofSeconds(10));
    assertThat(restarted.toString(), restarted.size(), is(equalTo(6)));

    IntStream.of(PORTS).forEach(port -> {
      verify(diagnosticServiceMock("localhost", port)).getProxy(DynamicConfigService.class);
      verify(dynamicConfigServiceMock("localhost", port)).restart(any());
      verify(diagnosticServiceMock("localhost", port)).getLogicalServerState();
    });
  }

  @Test
  public void test_restart_call_throws_DiagnosticOperationTimeoutException() throws InterruptedException {
    mockSuccessfulServerRestart();

    IntStream.of(PORTS).forEach(port -> {
      DynamicConfigService dynamicConfigService = dynamicConfigServiceMock("localhost", port);
      doThrow(new DiagnosticOperationTimeoutException("")).when(dynamicConfigService).restart(any());
    });

    RestartProgress restartProgress = restartService.restartNodes(cluster.getNodeAddresses());
    assertThat(restartProgress.getErrors().size(), is(equalTo(6)));

    Map<InetSocketAddress, LogicalServerState> restarted = restartProgress.await(Duration.ofSeconds(10));
    assertThat(restarted.toString(), restarted.size(), is(equalTo(0)));

    IntStream.of(PORTS).forEach(port -> {
      verify(diagnosticServiceMock("localhost", port)).getProxy(DynamicConfigService.class);
      verify(dynamicConfigServiceMock("localhost", port)).restart(any());
    });
  }

  @Test
  public void test_restart_call_fails() throws InterruptedException {
    IntStream.of(PORTS).forEach(port -> {
      DynamicConfigService dynamicConfigService = dynamicConfigServiceMock("localhost", port);
      doThrow(new DiagnosticServiceProviderException("error")).when(dynamicConfigService).restart(any());
    });

    RestartProgress restartProgress = restartService.restartNodes(cluster.getNodeAddresses());
    assertThat(restartProgress.getErrors().size(), is(equalTo(6)));

    Map<InetSocketAddress, LogicalServerState> restarted = restartProgress.await(Duration.ofSeconds(10));
    assertThat(restarted.toString(), restarted.size(), is(equalTo(0)));

    IntStream.of(PORTS).forEach(port -> {
      verify(diagnosticServiceMock("localhost", port)).getProxy(DynamicConfigService.class);
      verify(dynamicConfigServiceMock("localhost", port)).restart(any());
    });
  }

  @Test
  public void test_stats_call_times_out() throws InterruptedException {
    mockSuccessfulServerRestart();

    when(diagnosticServiceMock("localhost", 9411).getLogicalServerState()).thenAnswer(sleep(SYNCHRONIZING, 60, SECONDS));

    RestartProgress restartProgress = restartService.restartNodes(cluster.getNodeAddresses());
    assertThat(restartProgress.getErrors().size(), is(equalTo(0)));

    Map<InetSocketAddress, LogicalServerState> restarted = restartProgress.await(Duration.ofSeconds(2));
    assertThat(restarted.toString(), restarted.size(), is(equalTo(5)));

    IntStream.of(PORTS).forEach(port -> {
      verify(dynamicConfigServiceMock("localhost", port)).restart(any());
      verify(diagnosticServiceMock("localhost", port), atLeast(1)).getLogicalServerState();
    });
  }

  @Test
  @SuppressWarnings("unchecked")
  public void test_server_restart_with_unexpected_state() throws InterruptedException {
    mockSuccessfulServerRestart();

    when(diagnosticServiceMock("localhost", 9411).getLogicalServerState()).thenReturn(null);
    when(diagnosticServiceMock("localhost", 9412).getLogicalServerState()).thenReturn(UNREACHABLE);
    when(diagnosticServiceMock("localhost", 9413).getLogicalServerState()).thenReturn(UNKNOWN);
    when(diagnosticServiceMock("localhost", 9421).getLogicalServerState()).thenReturn(STARTING);
    when(diagnosticServiceMock("localhost", 9422).getLogicalServerState()).thenReturn(UNINITIALIZED);
    when(diagnosticServiceMock("localhost", 9423).getLogicalServerState()).thenReturn(ACTIVE_SUSPENDED);

    RestartProgress restartProgress = restartService.restartNodes(cluster.getNodeAddresses());
    assertThat(restartProgress.getErrors().size(), is(equalTo(0)));

    Map<InetSocketAddress, LogicalServerState> restarted = restartProgress.await(Duration.ofSeconds(2));
    assertThat(restarted.toString(), restarted.size(), is(equalTo(0)));

    IntStream.of(PORTS).forEach(port -> {
      verify(diagnosticServiceMock("localhost", port)).getProxy(DynamicConfigService.class);
      verify(dynamicConfigServiceMock("localhost", port)).restart(any());
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
      doNothing().when(dynamicConfigService).restart(any());
    });
  }

  public static <T> Answer<T> sleep(long time, TimeUnit unit) {
    return sleep(null, time, unit);
  }

  public static <T> Answer<T> sleep(T delayedReturn, long time, TimeUnit unit) {
    return invocation -> {
      Thread.sleep(unit.toMillis(time));
      return delayedReturn;
    };
  }
}
