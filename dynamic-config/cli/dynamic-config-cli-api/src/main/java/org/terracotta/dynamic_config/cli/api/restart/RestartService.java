/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
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
package org.terracotta.dynamic_config.cli.api.restart;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.diagnostic.client.DiagnosticService;
import org.terracotta.diagnostic.client.connection.ConcurrencySizing;
import org.terracotta.diagnostic.client.connection.DiagnosticServiceProvider;
import org.terracotta.diagnostic.client.connection.DiagnosticServiceProviderException;
import org.terracotta.diagnostic.common.DiagnosticException;
import org.terracotta.diagnostic.model.LogicalServerState;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.service.DynamicConfigService;

import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
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

  public RestartService(DiagnosticServiceProvider diagnosticServiceProvider, ConcurrencySizing concurrencySizing) {
    this.diagnosticServiceProvider = requireNonNull(diagnosticServiceProvider);
    this.concurrencySizing = requireNonNull(concurrencySizing);
  }

  /**
   * Restart a list of nodes. They will be restarted after the specified delay.
   * To detect that a node has been restarted, we will wait until the node reaches one of the status given.
   */
  public RestartProgress restartNodes(Collection<Node.Endpoint> endpoints, Duration restartDelay, Collection<LogicalServerState> acceptedStates) {
    return restartNodes(endpoints, restartDelay, acceptedStates, DynamicConfigService::restart);
  }

  public RestartProgress restartNodesIfActives(Collection<Node.Endpoint> endpoints, Duration restartDelay, Collection<LogicalServerState> acceptedStates) {
    return restartNodes(endpoints, restartDelay, acceptedStates, DynamicConfigService::restartIfActive);
  }

  public RestartProgress restartNodesIfPassives(Collection<Node.Endpoint> endpoints, Duration restartDelay, Collection<LogicalServerState> acceptedStates) {
    return restartNodes(endpoints, restartDelay, acceptedStates, DynamicConfigService::restartIfPassive);
  }

  private RestartProgress restartNodes(Collection<Node.Endpoint> endpoints, Duration restartDelay, Collection<LogicalServerState> acceptedStates, BiConsumer<DynamicConfigService, Duration> restart) {
    if (restartDelay.getSeconds() < 1) {
      throw new IllegalArgumentException("Restart delay must be at least 1 second");
    }

    LOGGER.debug("Asking all nodes: {} to restart themselves", endpoints);

    // list of nodes that we asked for a restart
    Map<Node.Endpoint, DiagnosticService> restartRequested = new HashMap<>();

    // list of nodes we failed to ask for a restart
    Map<Node.Endpoint, Exception> restartRequestFailed = new HashMap<>();

    // trigger a restart request for all nodes
    for (Node.Endpoint endpoint : endpoints) {
      // this call should be pretty fast and should not time out if restart delay is long enough
      try {
        // do not close DiagnosticService: connection is used after to detect when server is stopped
        DiagnosticService diagnosticService = diagnosticServiceProvider.fetchDiagnosticService(endpoint.getHostPort().createInetSocketAddress());
        restartRequested.put(endpoint, diagnosticService);
        restart.accept(diagnosticService.getProxy(DynamicConfigService.class), restartDelay);
      } catch (Exception e) {
        // timeout should not occur with a restart delay. Any error is recorded, and we won't wait for this node to restart
        restartRequestFailed.put(endpoint, e);
        LOGGER.debug("Failed asking node {} to restart: {}", endpoint, e.getMessage(), e);
      }
    }

    // latch on which the requestor will wait. We will decrement it for each restarted node
    CountDownLatch done = new CountDownLatch(restartRequested.size());

    // record restarted nodes
    Map<Node.Endpoint, LogicalServerState> restartedNodes = new ConcurrentHashMap<>();

    // this is an optional callback the requestor can add to be made aware in real time about the nodes that have been restarted
    AtomicReference<BiConsumer<Node.Endpoint, LogicalServerState>> progressCallback = new AtomicReference<>();

    // stop all threads ?
    AtomicBoolean continuePolling = new AtomicBoolean(true);

    ExecutorService executorService = Executors.newFixedThreadPool(concurrencySizing.getThreadCount(endpoints.size()), r -> new Thread(r, getClass().getName()));
    restartRequested.forEach((endpoint, diagnosticService) -> executorService.submit(() -> {
      while (continuePolling.get() && !Thread.currentThread().isInterrupted() && diagnosticService.isConnected()) {
        try {
          LOGGER.debug("Waiting for node: {} to stop...", endpoint);
          Thread.sleep(500);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
      if (!diagnosticService.isConnected()) {
        LOGGER.debug("Node: {} has stopped", endpoint);
        LOGGER.debug("Waiting for node: {} to restart...", endpoint);
        LogicalServerState state = null;
        while (state == null && continuePolling.get() && !Thread.currentThread().isInterrupted()) {
          try {
            state = isRestarted(endpoint, acceptedStates);
            if (state != null) {
              LOGGER.debug("Node: {} has restarted", endpoint);
              restartedNodes.put(endpoint, state);
              BiConsumer<Node.Endpoint, LogicalServerState> cb = progressCallback.get();
              if (cb != null) {
                cb.accept(endpoint, state);
              }
              done.countDown();
            } else {
              // introduce a force sleep because otherwise this can loop pretty fast
              Thread.sleep(500);
            }
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
        }
      } else {
        LOGGER.warn("Restart of node: {} has been interrupted", endpoint);
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
      public Map<Node.Endpoint, LogicalServerState> await(Duration duration) throws InterruptedException {
        try {
          done.await(duration.toMillis(), MILLISECONDS);
          return new HashMap<>(restartedNodes);
        } finally {
          continuePolling.set(false);
          shutdown(executorService);
        }
      }

      @Override
      public void onRestarted(BiConsumer<Node.Endpoint, LogicalServerState> c) {
        progressCallback.set(c);
        // call the callback with previously restarted servers if the callback was set up after some servers were recorded
        restartedNodes.forEach(c);
      }

      @Override
      public Map<Node.Endpoint, Exception> getErrors() {
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
   * Using the default connect timeout provided by user should be enough. If not, the user can increase it, and it will apply to all connections.
   */
  private LogicalServerState isRestarted(Node.Endpoint endpoint, Collection<LogicalServerState> acceptedStates) {
    LOGGER.debug("Checking if node: {} has restarted", endpoint);
    try (DiagnosticService diagnosticService = diagnosticServiceProvider.fetchDiagnosticService(endpoint.getHostPort().createInetSocketAddress())) {
      LogicalServerState state = diagnosticService.getLogicalServerState();
      return state == null || !acceptedStates.contains(state) ? null : state;
    } catch (DiagnosticServiceProviderException | DiagnosticException e) {
      LOGGER.debug("Status query for node: {} failed: {}", endpoint, e.getMessage());
      return null;
    } catch (Exception e) {
      LOGGER.error("Unexpected error during status query for node: {}", endpoint, e);
      return null;
    }
  }
}
