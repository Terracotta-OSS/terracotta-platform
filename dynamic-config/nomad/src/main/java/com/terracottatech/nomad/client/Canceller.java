/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.nomad.client;

import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

public class Canceller<T> {
  private AtomicReference<CancellerState<T>> state = new AtomicReference<>(new CancellerState<>(null, null));

  public void set(Future<T> future) {
    state.updateAndGet(state -> {
      if (state.isCancelled()) {
        boolean mayInterruptIfRunning = state.mayInterruptIfRunning();
        future.cancel(mayInterruptIfRunning);
        return new CancellerState<>(mayInterruptIfRunning, future);
      } else {
        return new CancellerState<>(null, future);
      }
    });
  }

  public void cancel(boolean mayInterruptIfRunning) {
    state.updateAndGet(state -> {
      Future<T> future = state.getFuture();
      if (future == null) {
        return new CancellerState<>(mayInterruptIfRunning, null);
      } else {
        future.cancel(mayInterruptIfRunning);
        return new CancellerState<>(mayInterruptIfRunning, future);
      }
    });
  }

  private static class CancellerState<T> {
    private final Boolean cancelledMayInterruptIfRunning;
    private final Future<T> future;

    public CancellerState(Boolean cancelledMayInterruptIfRunning, Future<T> future) {
      this.cancelledMayInterruptIfRunning = cancelledMayInterruptIfRunning;
      this.future = future;
    }

    public boolean isCancelled() {
      return cancelledMayInterruptIfRunning != null;
    }

    public boolean mayInterruptIfRunning() {
      return cancelledMayInterruptIfRunning;
    }

    public Future<T> getFuture() {
      return future;
    }
  }
}
