/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2026
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
package org.terracotta.platform.metrics.metric;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for Counter.
 */
public class CounterTest {

  @Test
  public void testCounterInitializedToZero() {
    Counter counter = new Counter();
    assertEquals(0L, counter.get());
  }

  @Test
  public void testIncrementByOne() {
    Counter counter = new Counter();
    counter.increment();
    assertEquals(1L, counter.get());

    counter.increment();
    assertEquals(2L, counter.get());

    counter.increment();
    assertEquals(3L, counter.get());
  }

  @Test
  public void testOverflowWrapsToZero() {
    Counter counter = new Counter(Long.MAX_VALUE - 1);
    assertEquals(Long.MAX_VALUE - 1, counter.get());

    // Increment by 1 should reach MAX_VALUE
    counter.increment();
    assertEquals(Long.MAX_VALUE, counter.get());

    // Increment by 1 should wrap to 0
    counter.increment();
    assertEquals(0L, counter.get());
  }

  @Test
  public void testMultipleOverflows() {
    Counter counter = new Counter(Long.MAX_VALUE);
    assertEquals(Long.MAX_VALUE, counter.get());

    // Wraps to 0, then keeps counting
    counter.increment();
    assertEquals(0L, counter.get());
    counter.increment();
    assertEquals(1L, counter.get());
  }

  @Test
  public void testGetIsNeverNegativeAcrossOverflow() {
    Counter counter = new Counter(Long.MAX_VALUE - 1);
    for (int i = 0; i < 5; i++) {
      counter.increment();
      assertTrue(counter.get() >= 0);
    }
    assertEquals(3L, counter.get());
  }

  @Test
  public void testConcurrentIncrements() throws InterruptedException {
    Counter counter = new Counter();
    int numThreads = 10;
    int incrementsPerThread = 1000;
    ExecutorService executor = Executors.newFixedThreadPool(numThreads);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch completionLatch = new CountDownLatch(numThreads);

    for (int i = 0; i < numThreads; i++) {
      executor.submit(() -> {
        try {
          startLatch.await();
          for (int j = 0; j < incrementsPerThread; j++) {
            counter.increment();
          }
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        } finally {
          completionLatch.countDown();
        }
      });
    }

    startLatch.countDown();
    completionLatch.await();
    executor.shutdown();

    assertEquals(numThreads * incrementsPerThread, counter.get());
  }

  @Test
  public void testConcurrentReadsNeverGoBackwardsWhileIncrementing() throws Exception {
    Counter counter = new Counter();
    int numThreads = 4;
    int incrementsPerThread = 50_000;
    ExecutorService executor = Executors.newFixedThreadPool(numThreads + 1);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch writersDone = new CountDownLatch(numThreads);

    try {
      for (int i = 0; i < numThreads; i++) {
        executor.submit(() -> {
          try {
            startLatch.await();
            for (int j = 0; j < incrementsPerThread; j++) {
              counter.increment();
            }
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          } finally {
            writersDone.countDown();
          }
        });
      }

      Future<Boolean> reader = executor.submit(() -> {
        startLatch.await();
        long previous = 0;
        boolean monotonic = true;
        while (writersDone.getCount() > 0) {
          long current = counter.get();
          monotonic &= current >= previous;
          previous = current;
        }
        return monotonic;
      });

      startLatch.countDown();
      assertTrue("reads must never go backwards", reader.get());
      writersDone.await();
    } finally {
      executor.shutdown();
    }

    assertEquals((long) numThreads * incrementsPerThread, counter.get());
  }

  @Test
  public void testToString() {
    Counter counter = new Counter(42);
    String str = counter.toString();
    assertTrue(str.contains("Counter"));
    assertTrue(str.contains("42"));
  }

  @Test
  public void testCounterIsMetric() {
    Counter counter = new Counter();
    assertTrue(counter instanceof Metric);
  }
}

// Made with Bob
