/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.diagnostic.client.connection;

import org.terracotta.common.struct.Tuple3;
import org.terracotta.diagnostic.client.DiagnosticService;
import org.terracotta.lease.connection.TimeBudget;

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
          DiagnosticService diagnosticService = diagnosticServiceProvider.fetchDiagnosticService(address, Duration.ofMillis(timeBudget.remaining()));
          return tuple3(address, diagnosticService, null);
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
