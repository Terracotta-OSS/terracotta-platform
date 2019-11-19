/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli.service.restart;

import com.terracottatech.diagnostic.client.DiagnosticOperationTimeoutException;
import com.terracottatech.diagnostic.client.DiagnosticService;
import com.terracottatech.diagnostic.client.connection.ConcurrencySizing;
import com.terracottatech.diagnostic.client.connection.DiagnosticServiceProvider;
import com.terracottatech.diagnostic.client.connection.DiagnosticServiceProviderException;
import com.terracottatech.diagnostic.common.DiagnosticException;
import com.terracottatech.dynamic_config.diagnostic.DynamicConfigService;
import com.terracottatech.tools.detailed.state.LogicalServerState;
import com.terracottatech.utilities.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.lease.connection.TimeBudget;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import static com.terracottatech.utilities.Tuple2.tuple2;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * @author Mathieu Carbou
 */
public class RestartService {

  private static final Logger LOGGER = LoggerFactory.getLogger(RestartService.class);

  private final DiagnosticServiceProvider diagnosticServiceProvider;
  private final ConcurrencySizing concurrencySizing;
  private final Duration maximumWaitTime;
  private final Duration restartDelay;

  public RestartService(DiagnosticServiceProvider diagnosticServiceProvider, ConcurrencySizing concurrencySizing, Duration restartDelay, Duration maximumWaitTime) {
    this.diagnosticServiceProvider = requireNonNull(diagnosticServiceProvider);
    this.concurrencySizing = requireNonNull(concurrencySizing);
    this.restartDelay = requireNonNull(restartDelay);
    this.maximumWaitTime = requireNonNull(maximumWaitTime);
  }

  public RestartProgress restartNodes(Collection<InetSocketAddress> addresses) {
    LOGGER.debug("Asking all nodes: {} to restart themselves", addresses);

    int threadCount = concurrencySizing.getThreadCount(addresses.size());
    ExecutorService executorService = Executors.newFixedThreadPool(threadCount, r -> new Thread(r, getClass().getName()));
    CompletionService<Tuple2<InetSocketAddress, Exception>> completionService = new ExecutorCompletionService<>(executorService);

    for (InetSocketAddress addr : addresses) {
      completionService.submit(() -> {
        // trigger restart
        // this call should be pretty fast and should not timeout if restart delay is long enough
        try (DiagnosticService diagnosticService = diagnosticServiceProvider.fetchDiagnosticService(addr)) {
          diagnosticService.getProxy(DynamicConfigService.class).restart(restartDelay);
        } catch (DiagnosticOperationTimeoutException e) {
          // This operation times out (DiagnosticOperationTimeoutException) because the nodes have shut down. All good.
          LOGGER.debug("Diagnostic operation timed out", e);
        } catch (DiagnosticServiceProviderException | DiagnosticException e) {
          // report the failure in the completable future
          LOGGER.debug("Failed asking node {} to restart: {}", addr, e.getMessage(), e);
          return tuple2(addr, e);
        }
        // wait for node to be restarted
        try {
          awaitRestart(addr);
          return tuple2(addr, null); // no error
        } catch (TimeoutException e) {
          return tuple2(addr, e);
        }
      });
    }
    return () -> {
      Map<InetSocketAddress, Exception> failures = new TreeMap<>(Comparator.comparing(InetSocketAddress::toString));
      int count = addresses.size();
      while (count-- > 0) {
        Future<Tuple2<InetSocketAddress, Exception>> completedTask = completionService.take();
        try {
          Tuple2<InetSocketAddress, Exception> result = completedTask.get();
          if (result.t2 != null) {
            failures.put(result.t1, result.t2);
          }
        } catch (ExecutionException e) {
          // should never happen since we catch all exceptions in the callable
          throw new AssertionError(e);
        }
      }
      shutdown(executorService);
      return failures;
    };
  }

  private void shutdown(ExecutorService executorService) {
    executorService.shutdownNow();
    try {
      executorService.awaitTermination(maximumWaitTime.toMillis(), MILLISECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  /**
   * Await for a node to be restarted.
   * The method is waiting for a maximum wait time and tries to poll until
   * it succeeds or until the maximum wait time is reached
   */
  private void awaitRestart(InetSocketAddress addr) throws TimeoutException {
    LOGGER.debug("Waiting for node {} to restart (maximum: {}s)", addr, maximumWaitTime.getSeconds());
    long start = System.nanoTime();
    TimeBudget timeBudget = new TimeBudget(maximumWaitTime.toMillis(), MILLISECONDS);
    boolean restarted = false;
    while (!restarted && timeBudget.remaining() > 0) {
      restarted = pollNode(addr);
      if (!restarted) {
        // introduce a minimal 1sec sleep if poll fails to not flood connection requests
        try {
          Thread.sleep(1_000);
        } catch (InterruptedException e) {
          // await restart interrupted
          Thread.currentThread().interrupt();
          return;
        }
      }
    }
    if (!restarted) {
      throw new TimeoutException("Waiting for node " + addr + " to restart timed out");
    } else {
      LOGGER.debug("Node {} has restarted in {} seconds", addr, Duration.ofNanos(System.nanoTime() - start).getSeconds());
    }
  }

  /**
   * Poll a node to see if it has restarted.
   * We should specify ideally a connect timeout that is in relation with the restart delay.
   * Also, the connect timeout must not be to low, otherwise the poll will return false in case of a slow network.
   * Using the default connect timeout provided by user should be enough. If not, the user can increase it and it will apply to all connections.
   */
  private boolean pollNode(InetSocketAddress addr) {
    LOGGER.debug("Checking if node: {} has restarted", addr);
    try (DiagnosticService diagnosticService = diagnosticServiceProvider.fetchDiagnosticService(addr)) {
      LogicalServerState state = diagnosticService.getLogicalServerState();
      // STARTING is the state when server hasn't finished its startup yet
      return state != null && (state.isPassive() || state.isActive());
    } catch (DiagnosticServiceProviderException | DiagnosticException e) {
      LOGGER.debug("Status query for node: {} failed", addr, e);
      return false;
    }
  }
}
