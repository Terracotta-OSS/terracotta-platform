/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli.service.restart;

import com.terracottatech.diagnostic.client.DiagnosticService;
import com.terracottatech.diagnostic.client.connection.ConcurrencySizing;
import com.terracottatech.diagnostic.client.connection.DiagnosticServiceProvider;
import com.terracottatech.diagnostic.client.connection.DiagnosticServiceProviderException;
import com.terracottatech.diagnostic.common.DiagnosticException;
import com.terracottatech.dynamic_config.diagnostic.DynamicConfigService;
import com.terracottatech.tools.detailed.state.LogicalServerState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

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
    Collection<InetSocketAddress> restartedNodes = new CopyOnWriteArrayList<>();

    // this is an optional callback the requestor can add to be made aware in real time about the nodes that have been restarted
    AtomicReference<Consumer<InetSocketAddress>> progressCallback = new AtomicReference<>();

    // stop all threads ?
    AtomicBoolean continuePolling = new AtomicBoolean(true);

    ExecutorService executorService = Executors.newFixedThreadPool(concurrencySizing.getThreadCount(addresses.size()), r -> new Thread(r, getClass().getName()));
    restartRequested.forEach(address -> executorService.submit(() -> {
      boolean hasRestarted = false;
      while (!hasRestarted && continuePolling.get() && !Thread.currentThread().isInterrupted()) {
        try {
          hasRestarted = isRestarted(address);
          if (hasRestarted) {
            LOGGER.debug("Node: {} has restarted", address);
            restartedNodes.add(address);
            Consumer<InetSocketAddress> cb = progressCallback.get();
            if (cb != null) {
              cb.accept(address);
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
      public Collection<InetSocketAddress> await(Duration duration) throws InterruptedException {
        try {
          done.await(duration.toMillis(), MILLISECONDS);
          return new ArrayList<>(restartedNodes);
        } finally {
          continuePolling.set(false);
          shutdown(executorService);
        }
      }

      @Override
      public void onRestarted(Consumer<InetSocketAddress> c) {
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
  private boolean isRestarted(InetSocketAddress addr) {
    LOGGER.debug("Checking if node: {} has restarted", addr);
    try (DiagnosticService diagnosticService = diagnosticServiceProvider.fetchDiagnosticService(addr)) {
      LogicalServerState state = diagnosticService.getLogicalServerState();
      // STARTING is the state when server hasn't finished its startup yet
      return state != null && (state.isPassive() || state.isActive());
    } catch (DiagnosticServiceProviderException | DiagnosticException e) {
      LOGGER.debug("Status query for node: {} failed", addr, e);
      return false;
    } catch (Exception e) {
      LOGGER.error("Unexpected error during status query for node: {}", addr, e);
      return false;
    }
  }
}
