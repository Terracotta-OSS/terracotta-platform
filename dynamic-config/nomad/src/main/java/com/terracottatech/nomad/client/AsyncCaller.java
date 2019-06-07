/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.nomad.client;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class AsyncCaller implements AutoCloseable {
  private final ExecutorService executor;
  private final ScheduledExecutorService delayer;
  private final Duration timeout;
  private final boolean ownExecutors;

  public AsyncCaller(int executionConcurrency, Duration timeout) {
    this(executionConcurrency, 1, timeout);
  }

  private AsyncCaller(int executionConcurrency, int cancelConcurrency, Duration timeout) {
    this(
        Executors.newFixedThreadPool(executionConcurrency, (r) -> new Thread(r, "Nomad RPC")),
        Executors.newScheduledThreadPool(cancelConcurrency, (r) -> new Thread(r, "Nomad RPC Timeout")),
        timeout,
        true
    );
  }

  private AsyncCaller(ExecutorService executor, ScheduledExecutorService delayer, Duration timeout, boolean ownExecutors) {
    this.executor = executor;
    this.delayer = delayer;
    this.timeout = timeout;
    this.ownExecutors = ownExecutors;
  }

  public <T> Future<Void> runTimedAsync(Callable<T> callable, Consumer<T> onSuccess, Consumer<Throwable> onError) {
    CompletableFuture<T> future = new CompletableFuture<>();

    Canceller<Void> canceller = new Canceller<>();

    canceller.set(executor.submit(() -> {
      delayer.schedule(() -> {
        future.completeExceptionally(new TimeoutException());
        canceller.cancel(true);
      }, timeout.toMillis(), MILLISECONDS);

      try {
        T result = callable.call();
        future.complete(result);
      } catch (Exception e) {
        future.completeExceptionally(e);
      }

      return null;
    }));

    return future
        .thenAccept(onSuccess)
        .exceptionally(e -> {
          Throwable t = e;
          if (e instanceof CompletionException) {
            t = e.getCause();
          }
          onError.accept(t);
          return null;
        });
  }

  @Override
  public void close() {
    if (ownExecutors) {
      delayer.shutdownNow();
      executor.shutdownNow();
    }
  }
}
