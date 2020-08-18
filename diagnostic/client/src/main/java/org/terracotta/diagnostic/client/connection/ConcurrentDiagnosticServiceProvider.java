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
import org.terracotta.common.struct.Tuple3;
import org.terracotta.diagnostic.client.DiagnosticService;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static java.util.Collections.emptyMap;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.terracotta.common.struct.Tuple3.tuple3;

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
  public DiagnosticServices fetchDiagnosticServices(Collection<InetSocketAddress> addresses) {
    if (addresses.isEmpty()) {
      return new DiagnosticServices(emptyMap(), emptyMap());
    }

    ExecutorService executor = Executors.newFixedThreadPool(
        concurrencySizing.getThreadCount(addresses.size()),
        r -> new Thread(r, "diagnostics-connect"));

    try {
      CompletionService<Tuple3<InetSocketAddress, DiagnosticService, DiagnosticServiceProviderException>> completionService = new ExecutorCompletionService<>(executor);

      // start all the fetches, record error if any
      TimeBudget timeBudget = new TimeBudget(connectionTimeout.toMillis(), MILLISECONDS);
      addresses.forEach(address -> completionService.submit(() -> {
        try {
          DiagnosticService diagnosticService = diagnosticServiceProvider.fetchDiagnosticServiceWithFallback(address, Duration.ofMillis(timeBudget.remaining()));
          return tuple3(address, diagnosticService, null);
        } catch (DiagnosticServiceProviderException e) {
          return tuple3(address, null, e);
        } catch (Exception e) {
          return tuple3(address, null, new DiagnosticServiceProviderException("Failed to create diagnostic connection to " + address, e));
        }
      }));

      Map<InetSocketAddress, DiagnosticService> online = new HashMap<>(addresses.size());
      Map<InetSocketAddress, DiagnosticServiceProviderException> offline = new HashMap<>(addresses.size());

      try {
        // capture the task output
        int count = addresses.size();
        while (count-- > 0) {
          // we do not need to handle any timeout here during a take or get because they are handled in the submitted tasks
          Future<Tuple3<InetSocketAddress, DiagnosticService, DiagnosticServiceProviderException>> completed = completionService.take();
          Tuple3<InetSocketAddress, DiagnosticService, DiagnosticServiceProviderException> tuple = completed.get();
          if (tuple.t3 == null) {
            online.put(tuple.t1, tuple.t2);
          } else {
            offline.put(tuple.t1, tuple.t3);
          }
        }
      } catch (InterruptedException e) {
        // take() has been interrupted.
        // We need to cancel all the tasks and shutdown everything
        shutdown(executor);
        throw new RuntimeException(e);
      } catch (ExecutionException e) {
        // impossible since we catch Throwable in the submitted task
        throw new AssertionError(e);
      }

      return new DiagnosticServices(online, offline);
    } finally {
      shutdown(executor);
    }
  }

  private void shutdown(ExecutorService executor) {
    executor.shutdown();
    try {
      if (!executor.awaitTermination(30, SECONDS)) {
        // timed out waiting for task closing
        executor.shutdownNow();
      }
    } catch (InterruptedException e) {
      executor.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }
}
