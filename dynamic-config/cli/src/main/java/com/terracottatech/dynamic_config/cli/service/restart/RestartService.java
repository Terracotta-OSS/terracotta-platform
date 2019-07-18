/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli.service.restart;

import com.terracottatech.diagnostic.client.DiagnosticOperationTimeoutException;
import com.terracottatech.diagnostic.client.DiagnosticService;
import com.terracottatech.diagnostic.client.connection.ConcurrencySizing;
import com.terracottatech.diagnostic.client.connection.DiagnosticServiceProvider;
import com.terracottatech.dynamic_config.diagnostic.TopologyService;
import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.utilities.Tuple2;
import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.awaitility.core.ConditionTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.terracottatech.utilities.Tuple2.tuple2;

/**
 * @author Mathieu Carbou
 */
public class RestartService {

  private static final Logger LOGGER = LoggerFactory.getLogger(RestartService.class);

  private final DiagnosticServiceProvider diagnosticServiceProvider;
  private final ConcurrencySizing concurrencySizing;
  private final long requestTimeoutMillis;

  public RestartService(DiagnosticServiceProvider diagnosticServiceProvider, ConcurrencySizing concurrencySizing, long requestTimeoutMillis) {
    this.diagnosticServiceProvider = diagnosticServiceProvider;
    this.concurrencySizing = concurrencySizing;
    this.requestTimeoutMillis = requestTimeoutMillis;
  }

  public RestartProgress restart(Cluster cluster) {
    Collection<InetSocketAddress> addresses = cluster.getNodeAddresses();
    LOGGER.info("Asking all cluster nodes: {} to restart themselves", addresses);

    ExecutorService executor = Executors.newFixedThreadPool(concurrencySizing.getThreadCount(addresses.size()), r -> new Thread(r, "diagnostics-restart"));
    Map<InetSocketAddress, Tuple2<String, Throwable>> failures = new ConcurrentHashMap<>();

    CompletableFuture<Void> all = CompletableFuture.allOf(addresses.stream()
        .map(addr -> CompletableFuture.runAsync(() -> {
          LOGGER.debug("Asking node {} to restart", addr);
          try (DiagnosticService diagnosticService = diagnosticServiceProvider.fetchDiagnosticService(addr)) {
            diagnosticService.getProxy(TopologyService.class).restart();
          } catch (DiagnosticOperationTimeoutException e) {
            // This operation times out (DiagnosticOperationTimeoutException) because the nodes have shut down. All good.
            // Or we cancel the call, interrupts, etc..
          } catch (Exception e) {
            // report the failure in the completable future
            Tuple2<String, Throwable> err = tuple2("Asking node " + addr + " to restart failed: " + e.getMessage(), e);
            LOGGER.debug(err.t1, e);
            failures.put(addr, err);
          }
        }, executor).thenRun(() -> {
          if (!failures.containsKey(addr)) {
            try {
              LOGGER.debug("Waiting for node {} to restart", addr);
              Awaitility.await()
                  .pollInterval(Duration.ONE_SECOND)
                  .atMost(requestTimeoutMillis, TimeUnit.MILLISECONDS)
                  .until(nodeRestarted(addr));
            } catch (ConditionTimeoutException e) {
              Tuple2<String, Throwable> err = tuple2("Waiting for node " + addr + " timed out after " + requestTimeoutMillis + "ms", null);
              LOGGER.debug(err.t1);
              failures.put(addr, err);
            } catch (Throwable e) {
              Tuple2<String, Throwable> err = tuple2("Waiting for node " + addr + " to restart failed: " + e.getMessage(), e);
              LOGGER.debug(err.t1, e);
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
          executor.awaitTermination(requestTimeoutMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    };
  }

  private Callable<Boolean> nodeRestarted(InetSocketAddress addr) {
    return () -> {
      LOGGER.debug("Checking if node {} has restarted", addr);
      try (DiagnosticService diagnosticService = diagnosticServiceProvider.fetchDiagnosticService(addr)) {
        diagnosticService.getLogicalServerState();
        return true;
      } catch (Exception e) {
        // report the failure in the completable future
        LOGGER.debug("Node {} didn't restarted: {}", e.getMessage(), e);
        return false;
      }
    };
  }

}
