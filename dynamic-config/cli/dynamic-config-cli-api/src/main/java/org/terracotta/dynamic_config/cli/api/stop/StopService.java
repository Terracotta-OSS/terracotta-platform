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
package org.terracotta.dynamic_config.cli.api.stop;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.diagnostic.client.DiagnosticService;
import org.terracotta.diagnostic.client.connection.ConcurrencySizing;
import org.terracotta.diagnostic.client.connection.DiagnosticServiceProvider;
import org.terracotta.diagnostic.model.LogicalServerState;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.service.DynamicConfigService;

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
public class StopService {

  private static final Logger LOGGER = LoggerFactory.getLogger(StopService.class);

  private final DiagnosticServiceProvider diagnosticServiceProvider;
  private final ConcurrencySizing concurrencySizing;

  public StopService(DiagnosticServiceProvider diagnosticServiceProvider, ConcurrencySizing concurrencySizing) {
    this.diagnosticServiceProvider = requireNonNull(diagnosticServiceProvider);
    this.concurrencySizing = requireNonNull(concurrencySizing);
  }

  /**
   * Stop a list of nodes. They will be stopped after the specified delay.
   * To detect that a node has been stopped, we will do some request until we reach a request timeout or amy other error.
   */
  public StopProgress stopNodes(Collection<Node.Endpoint> endpoints, Duration stopDelay) {
    if (stopDelay.getSeconds() < 1) {
      throw new IllegalArgumentException("Stop delay must be at least 1 second");
    }

    LOGGER.debug("Asking all nodes: {} to stop themselves", endpoints);

    // list of nodes that we asked for a stop
    Collection<Node.Endpoint> stopRequested = new HashSet<>();

    // list of nodes we failed to ask for a stop
    Map<Node.Endpoint, Exception> stopRequestFailed = new HashMap<>();

    // trigger a stop request for all nodes
    for (Node.Endpoint addr : endpoints) {
      // this call should be pretty fast and should not timeout if stop delay is long enough
      try (DiagnosticService diagnosticService = diagnosticServiceProvider.fetchDiagnosticService(addr.getAddress())) {
        diagnosticService.getProxy(DynamicConfigService.class).stop(stopDelay);
        stopRequested.add(addr);
      } catch (Exception e) {
        // timeout should not occur with a stop delay. Any error is recorded an we won't wait for this node to stop
        stopRequestFailed.put(addr, e);
        LOGGER.debug("Failed asking node {} to stop: {}", addr, e.getMessage(), e);
      }
    }

    // latch on which the requestor will wait. We will decrement it for each stopped node
    CountDownLatch done = new CountDownLatch(stopRequested.size());

    // record stopped nodes
    Collection<Node.Endpoint> stoppedNodes = new CopyOnWriteArrayList<>();

    // this is an optional callback the requestor can add to be made aware in real time about the nodes that have been stopped
    AtomicReference<Consumer<Node.Endpoint>> progressCallback = new AtomicReference<>();

    // stop all threads ?
    AtomicBoolean continuePolling = new AtomicBoolean(true);

    ExecutorService executorService = Executors.newFixedThreadPool(concurrencySizing.getThreadCount(endpoints.size()), r -> new Thread(r, getClass().getName()));
    stopRequested.forEach(endpoint -> executorService.submit(() -> {

      // wait for the stop delay to end so that servers gets stopped
      try {
        Thread.sleep(stopDelay.toMillis() + 5_000);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return;
      }

      boolean stopped = false;
      while (!stopped && continuePolling.get() && !Thread.currentThread().isInterrupted()) {
        try {
          stopped = isStopped(endpoint);
          if (stopped) {
            LOGGER.debug("Node: {} has stopped", endpoint);
            stoppedNodes.add(endpoint);
            Consumer<Node.Endpoint> cb = progressCallback.get();
            if (cb != null) {
              cb.accept(endpoint);
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

    return new StopProgress() {
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
      public Collection<Node.Endpoint> await(Duration duration) throws InterruptedException {
        try {
          done.await(duration.toMillis(), MILLISECONDS);
          return new ArrayList<>(stoppedNodes);
        } finally {
          continuePolling.set(false);
          shutdown(executorService);
        }
      }

      @Override
      public void onStopped(Consumer<Node.Endpoint> c) {
        progressCallback.set(c);
        // call the callback with previously stopped servers if the callback was setup after some servers were recorded
        stoppedNodes.forEach(c);
      }

      @Override
      public Map<Node.Endpoint, Exception> getErrors() {
        return stopRequestFailed;
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
   * Poll a node to see if it has stopped.
   * We should specify ideally a connect timeout that is in relation with the stop delay.
   * Also, the connect timeout must not be to low, otherwise the poll will return false in case of a slow network.
   * Using the default connect timeout provided by user should be enough. If not, the user can increase it and it will apply to all connections.
   */
  private boolean isStopped(Node.Endpoint endpoint) {
    LOGGER.debug("Checking if node: {} has stopped", endpoint);
    try (DiagnosticService logicalServerState = diagnosticServiceProvider.fetchDiagnosticService(endpoint.getAddress(), Duration.ofSeconds(5))) {
      LogicalServerState state = logicalServerState.getLogicalServerState();
      return state == LogicalServerState.UNREACHABLE;
    } catch (RuntimeException e) {
      // DetailedConnectionException, DiagnosticServiceProviderException, DiagnosticException
      return true;
    }
  }
}
