package org.terracotta.management.entity.server;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author Mathieu Carbou
 */
class Utils<V> {

  static <T> T[] array(T... o) {
    return o;
  }

  static <V> Future<V> completedFuture(final V value) {
    return new Future<V>() {
      @Override
      public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
      }

      @Override
      public boolean isCancelled() {
        return false;
      }

      @Override
      public boolean isDone() {
        return true;
      }

      @Override
      public V get() throws InterruptedException, ExecutionException {
        return value;
      }

      @Override
      public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return value;
      }
    };
  }

}
