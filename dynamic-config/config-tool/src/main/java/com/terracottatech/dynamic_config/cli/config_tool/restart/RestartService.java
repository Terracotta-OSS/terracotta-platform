/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli.config_tool.restart;

import com.terracottatech.diagnostic.client.DiagnosticService;
import com.terracottatech.diagnostic.client.connection.ConcurrencySizing;
import com.terracottatech.diagnostic.client.connection.DiagnosticServiceProvider;
import com.terracottatech.diagnostic.client.connection.DiagnosticServiceProviderException;
import com.terracottatech.diagnostic.common.DiagnosticException;
import com.terracottatech.dynamic_config.api.service.DynamicConfigService;
import com.terracottatech.tools.detailed.state.LogicalServerState;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * @author Mathieu Carbou
 */
public class RestartService {

  private static final Logger LOGGER = LoggerFactory.getLogger(RestartService.class);

  private final DiagnosticServiceProvider diagnosticServiceProvider;
  private final ConcurrencySizing concurrencySizing;
  private final Duration restartDelay;

  public RestartService(DiagnosticServiceProvider diagnosticServiceProvider, ConcurrencySizing concurrencySizing, Duration restartDelay) {
    this.diagnosticServiceProvider = requireNonNull(diagnosticServiceProvider);
    this.concurrencySizing = requireNonNull(concurrencySizing);
    this.restartDelay = requireNonNull(restartDelay);
    if (restartDelay.getSeconds() < 1) {
      throw new IllegalArgumentException("Restart delay must be at least 1 second");
    }
  }

  public RestartProgress restartNodes(Collection<InetSocketAddress> addresses) {
    LOGGER.debug("Asking all nodes: {} to restart themselves", addresses);

    // list of nodes that we asked for a restart
    Collection<InetSocketAddress> restartRequested = new HashSet<>();

    // list of nodes we failed to ask for a restart
    Map<InetSocketAddress, Exception> restartRequestFailed = new HashMap<>();

    // trigger a restart request for all nodes
    for (InetSocketAddress addr : addresses) {
      // this call should be pretty fast and should not timeout if restart delay is long enough
      try (DiagnosticService diagnosticService = diagnosticServiceProvider.fetchDiagnosticService(addr)) {
        diagnosticService.getProxy(DynamicConfigService.class).restart(restartDelay);
        restartRequested.add(addr);
      } catch (Exception e) {
        // timeout should not occur with a restart delay. Any error is recorded an we won't wait for this node to restart
        restartRequestFailed.put(addr, e);
        LOGGER.debug("Failed asking node {} to restart: {}", addr, e.getMessage(), e);
      }
    }

    // latch on which the requestor will wait. We will decrement it for each restarted node
    CountDownLatch done = new CountDownLatch(restartRequested.size());

    // record restarted nodes
    Map<InetSocketAddress, LogicalServerState> restartedNodes = new ConcurrentHashMap<>();

    // this is an optional callback the requestor can add to be made aware in real time about the nodes that have been restarted
    AtomicReference<BiConsumer<InetSocketAddress, LogicalServerState>> progressCallback = new AtomicReference<>();

    // stop all threads ?
    AtomicBoolean continuePolling = new AtomicBoolean(true);

    ExecutorService executorService = Executors.newFixedThreadPool(concurrencySizing.getThreadCount(addresses.size()), r -> new Thread(r, getClass().getName()));
    restartRequested.forEach(address -> executorService.submit(() -> {
      LogicalServerState state = null;
      while (state == null && continuePolling.get() && !Thread.currentThread().isInterrupted()) {
        try {
          state = isRestarted(address);
          if (state != null) {
            LOGGER.debug("Node: {} has restarted", address);
            restartedNodes.put(address, state);
            BiConsumer<InetSocketAddress, LogicalServerState> cb = progressCallback.get();
            if (cb != null) {
              cb.accept(address, state);
            }
            done.countDown();
          } else {
            // introduce a force sleep because otherwise this cn loop pretty fast
            Thread.sleep(1_000);
          }
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    }));

    return new RestartProgress() {
      @Override
      public void await() throws InterruptedException {
        try {
          done.await();
        } finally {
          continuePolling.set(false);
          shutdown(executorService);
        }
      }

      @Override
      @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED")
      public Map<InetSocketAddress, LogicalServerState> await(Duration duration) throws InterruptedException {
        try {
          done.await(duration.toMillis(), MILLISECONDS);
          return new HashMap<>(restartedNodes);
        } finally {
          continuePolling.set(false);
          shutdown(executorService);
        }
      }

      @Override
      public void onRestarted(BiConsumer<InetSocketAddress, LogicalServerState> c) {
        progressCallback.set(c);
        // call the callback with previously restarted servers if the callback was setup after some servers were recorded
        restartedNodes.forEach(c);
      }

      @Override
      public Map<InetSocketAddress, Exception> getErrors() {
        return restartRequestFailed;
      }
    };
  }

  private void shutdown(ExecutorService executorService) {
    executorService.shutdownNow();
    try {
      if (!executorService.awaitTermination(30, SECONDS)) {
        executorService.shutdownNow();
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  /**
   * Poll a node to see if it has restarted.
   * We should specify ideally a connect timeout that is in relation with the restart delay.
   * Also, the connect timeout must not be to low, otherwise the poll will return false in case of a slow network.
   * Using the default connect timeout provided by user should be enough. If not, the user can increase it and it will apply to all connections.
   */
  private LogicalServerState isRestarted(InetSocketAddress addr) {
    LOGGER.debug("Checking if node: {} has restarted", addr);
    try (DiagnosticService diagnosticService = diagnosticServiceProvider.fetchDiagnosticService(addr)) {
      LogicalServerState state = diagnosticService.getLogicalServerState();
      // STARTING is the state when server hasn't finished its startup yet
      return state != null && (state.isPassive() || state.isActive()) ? state : null;
    } catch (DiagnosticServiceProviderException | DiagnosticException e) {
      LOGGER.debug("Status query for node: {} failed", addr, e);
      return null;
    } catch (Exception e) {
      LOGGER.error("Unexpected error during status query for node: {}", addr, e);
      return null;
    }
  }
}