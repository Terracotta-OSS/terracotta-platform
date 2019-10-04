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
import com.terracottatech.dynamic_config.diagnostic.DynamicConfigService;
import com.terracottatech.tools.detailed.state.LogicalServerState;
import com.terracottatech.utilities.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.lease.connection.TimeBudget;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

import static com.terracottatech.tools.detailed.state.LogicalServerState.STARTING;
import static com.terracottatech.tools.detailed.state.LogicalServerState.UNINITIALIZED;
import static com.terracottatech.tools.detailed.state.LogicalServerState.UNKNOWN;
import static com.terracottatech.tools.detailed.state.LogicalServerState.UNREACHABLE;
import static com.terracottatech.utilities.Tuple2.tuple2;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * @author Mathieu Carbou
 */
public class RestartService {

  private static final Logger LOGGER = LoggerFactory.getLogger(RestartService.class);

  private final DiagnosticServiceProvider diagnosticServiceProvider;
  private final ConcurrencySizing concurrencySizing;
  private final Duration requestTimeout;

  public RestartService(DiagnosticServiceProvider diagnosticServiceProvider, ConcurrencySizing concurrencySizing, Duration requestTimeout) {
    this.diagnosticServiceProvider = diagnosticServiceProvider;
    this.concurrencySizing = concurrencySizing;
    this.requestTimeout = requestTimeout;
  }

  public RestartProgress restartNodes(Collection<InetSocketAddress> addresses) {
    LOGGER.info("Asking all cluster nodes: {} to restart themselves", addresses);

    ExecutorService executor = Executors.newFixedThreadPool(concurrencySizing.getThreadCount(addresses.size()), r -> new Thread(r, "diagnostics-restart"));
    Map<InetSocketAddress, Tuple2<String, Exception>> failures = new ConcurrentHashMap<>();

    CompletableFuture<Void> all = CompletableFuture.allOf(addresses.stream()
        .map(addr -> CompletableFuture.runAsync(() -> {
          LOGGER.debug("Asking node {} to restart", addr);
          try (DiagnosticService diagnosticService = diagnosticServiceProvider.fetchDiagnosticService(addr)) {
            diagnosticService.getProxy(DynamicConfigService.class).restart();
          } catch (DiagnosticOperationTimeoutException e) {
            // This operation times out (DiagnosticOperationTimeoutException) because the nodes have shut down. All good.
          } catch (Exception e) {
            // report the failure in the completable future
            Tuple2<String, Exception> err = tuple2("Failed asking node " + addr + " to restart: " + e.getMessage(), e);
            LOGGER.debug(err.t1, e);
            failures.put(addr, err);
          }
        }, executor).thenRun(() -> {
          if (!failures.containsKey(addr)) {
            try {
              awaitRestart(addr);
            } catch (TimeoutException e) {
              Tuple2<String, Exception> err = tuple2(e.getMessage(), null);
              LOGGER.debug(err.t1);
              failures.put(addr, err);
            }
          }
        }))
        .toArray(CompletableFuture<?>[]::new));

    return () -> {
      try {
        // wait for the restart calls to all be done
        // note: we do not use the timeout call here because it is useless.
        // all calls to diagnostic service already have a default timeout, plus the Awaitility.
        // so we can safely wait for them to finish or time out
        LOGGER.debug("Waiting for all cluster nodes to restart: {}", addresses);
        all.get();
        return failures;

      } catch (ExecutionException e) {
        // cannot happen because CompletableFuture are never completed exceptionally
        throw new RuntimeException(e.getCause());

      } finally {
        executor.shutdownNow();
        try {
          executor.awaitTermination(requestTimeout.toMillis(), MILLISECONDS);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    };
  }

  private void awaitRestart(InetSocketAddress addr) throws TimeoutException {
    LOGGER.debug("Waiting for node {} to restart", addr);
    TimeBudget timeBudget = new TimeBudget(requestTimeout.toMillis(), MILLISECONDS);
    boolean restarted = false;
    while (!restarted && timeBudget.remaining(MILLISECONDS) > 0) {
      restarted = nodeRestarted(addr);
    }
    if (!restarted) {
      throw new TimeoutException("Waiting for node " + addr + " to restart timed out after " + requestTimeout.toMillis() + "ms");
    }
  }

  private boolean nodeRestarted(InetSocketAddress addr) {
    LOGGER.debug("Checking if node {} has restarted", addr);
    try (DiagnosticService diagnosticService = diagnosticServiceProvider.fetchDiagnosticService(addr)) {
      // STARTING is the state used when starting node in diagnostic mode
      LogicalServerState state = diagnosticService.getLogicalServerState();
      // Note: STARTING is the state used when a server is blocked starting in diagnostic mode
      return state != null && state != UNREACHABLE && state != UNKNOWN && state != STARTING && state != UNINITIALIZED;
    } catch (DiagnosticServiceProviderException e) {
      LOGGER.debug("Node {} didn't restarted yet: {}", e.getMessage(), e);
      return false;
    } catch (Exception e) {
      LOGGER.debug("Unable to query status for node {}: {}", e.getMessage(), e);
      return false;
    }
  }

}
