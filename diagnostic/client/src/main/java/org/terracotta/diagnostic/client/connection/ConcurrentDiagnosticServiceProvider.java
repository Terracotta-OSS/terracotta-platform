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
package org.terracotta.diagnostic.client.connection;

import org.terracotta.common.struct.TimeBudget;
import org.terracotta.diagnostic.client.DiagnosticService;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;

import static java.util.Collections.emptyMap;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

public class ConcurrentDiagnosticServiceProvider implements MultiDiagnosticServiceProvider {

  private final DiagnosticServiceProvider diagnosticServiceProvider;
  private final Duration connectionTimeout;
  private final ConcurrencySizing concurrencySizing;

  public ConcurrentDiagnosticServiceProvider(DiagnosticServiceProvider diagnosticServiceProvider,
                                             Duration connectionTimeout, ConcurrencySizing concurrencySizing) {
    this.connectionTimeout = connectionTimeout;
    this.concurrencySizing = concurrencySizing;
    this.diagnosticServiceProvider = diagnosticServiceProvider;
  }

  @Override
  public Duration getConnectionTimeout() {
    return connectionTimeout;
  }

  @Override
  public <K> DiagnosticServices<K> fetchDiagnosticServices(Map<K, InetSocketAddress> addresses, Duration connectionTimeout) {
    if (addresses.isEmpty()) {
      return new DiagnosticServices<>(emptyMap(), emptyMap());
    }
    Map<K, DiagnosticService> online = new ConcurrentHashMap<>(addresses.size());
    Map<K, DiagnosticServiceProviderException> failed = new ConcurrentHashMap<>(addresses.size());
    try (Fetcher<K> fetcher = new Fetcher<>(addresses, connectionTimeout)) {
      try {
        fetcher.fetch(online::put, failed::put);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
    if (online.size() + failed.size() != addresses.size()) { // in case Thread.currentThread().isInterrupted()
      online.values().forEach(DiagnosticService::close); // no need to catch anything: DiagnosticService#close() does not throw
      throw new DiagnosticServiceProviderException("Connection process interrupted");
    } else {
      return new DiagnosticServices<>(online, failed);
    }
  }

  @Override
  public <K> DiagnosticServices<K> fetchOnlineDiagnosticServices(Map<K, InetSocketAddress> expectedOnlineNodes, Duration timeout) throws DiagnosticServiceProviderException {
    if (expectedOnlineNodes.isEmpty()) {
      throw new DiagnosticServiceProviderException("No node to connect to");
    }
    Map<K, DiagnosticService> online = new ConcurrentHashMap<>(expectedOnlineNodes.size());
    Map<K, DiagnosticServiceProviderException> failed = new ConcurrentHashMap<>(expectedOnlineNodes.size());
    try (Fetcher<K> fetcher = new Fetcher<>(expectedOnlineNodes, connectionTimeout)) {
      fetcher.fetch(online::put, (k, failure) -> {
        failed.put(k, failure);
        fetcher.interrupt(); // we interrupt the fetch as soon as we see a node we cannot connect to
      });
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    if (Thread.currentThread().isInterrupted()) {
      online.values().forEach(DiagnosticService::close); // no need to catch anything: DiagnosticService#close() does not throw
      throw new DiagnosticServiceProviderException("Connection process interrupted");
    } else if (!failed.isEmpty()) {
      // if fetch was interrupted because of a failure, we throw
      online.values().forEach(DiagnosticService::close); // no need to catch anything: DiagnosticService#close() does not throw
      throw failed.values().iterator().next();
    } else {
      return new DiagnosticServices<>(online, emptyMap());
    }
  }

  @Override
  public <K> DiagnosticServices<K> fetchAnyOnlineDiagnosticService(Map<K, InetSocketAddress> addresses, Duration connectionTimeout) throws DiagnosticServiceProviderException {
    if (addresses.isEmpty()) {
      throw new DiagnosticServiceProviderException("No node to connect to");
    }
    Map<K, DiagnosticService> online = new ConcurrentHashMap<>(addresses.size());
    Map<K, DiagnosticServiceProviderException> failed = new ConcurrentHashMap<>(addresses.size());
    try (Fetcher<K> fetcher = new Fetcher<>(addresses, connectionTimeout)) {
      fetcher.fetch((k, diagnosticService) -> {
        online.put(k, diagnosticService);
        fetcher.interrupt();
      }, failed::put);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    if (Thread.currentThread().isInterrupted()) {
      online.values().forEach(DiagnosticService::close); // no need to catch anything: DiagnosticService#close() does not throw
      throw new DiagnosticServiceProviderException("No node to connect to: connection process interrupted");
    } else if (online.isEmpty()) {
      // online is empty , no need to close
      throw failed.values().iterator().next();
    } else {
      return new DiagnosticServices<>(online, emptyMap());
    }
  }

  private class Fetcher<K> implements AutoCloseable {
    private final Map<K, InetSocketAddress> addresses;
    private final TimeBudget timeBudget;
    private final ExecutorService executor;

    Fetcher(Map<K, InetSocketAddress> addresses, Duration overriddenConnectionTimeout) {
      this.addresses = addresses;
      this.timeBudget = overriddenConnectionTimeout == null ? null : new TimeBudget(overriddenConnectionTimeout.toMillis(), MILLISECONDS);
      this.executor = Executors.newFixedThreadPool(concurrencySizing.getThreadCount(addresses.size()), r -> new Thread(r, "diagnostics-connect"));
    }

    Duration getRemainingTimeout() {
      return timeBudget == null ? null : Duration.ofMillis(timeBudget.remaining());
    }

    void fetch(BiConsumer<K, DiagnosticService> onSuccess, BiConsumer<K, DiagnosticServiceProviderException> onFailure) throws InterruptedException {
      // start all the fetches and record success and errors
      addresses.forEach((k, address) -> executor.execute(() -> {
        try {
          DiagnosticService diagnosticService = diagnosticServiceProvider.fetchDiagnosticService(address, getRemainingTimeout());
          onSuccess.accept(k, diagnosticService);
        } catch (DiagnosticServiceProviderException e) {
          onFailure.accept(k, e);
        } catch (Exception e) {
          onFailure.accept(k, new DiagnosticServiceProviderException("Failed to create diagnostic connection to: " + address + ": " + e.getMessage(), e));
        }
      }));
      executor.shutdown();
      // wait for all tasks to finish because they are linked to
      // some connection timeout decisions from user:
      // - either timeout (long or short)
      // - either null => default core timeout is used
      // - either interruption
      try {
        while (!executor.awaitTermination(5, SECONDS)) {
        }
      } catch (InterruptedException e) {
        executor.shutdownNow(); // ensure that tasks will eventually be interrupted
        throw e;
      }
    }

    public void interrupt() {
      executor.shutdownNow();
    }

    @Override
    public void close() {
      executor.shutdownNow();
    }
  }
}
