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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
  public void testIncrementByN() {
    Counter counter = new Counter();
    counter.increment(5);
    assertEquals(5L, counter.get());

    counter.increment(10);
    assertEquals(15L, counter.get());

    counter.increment(100);
    assertEquals(115L, counter.get());
  }

  @Test
  public void testIncrementByZeroDoesNothing() {
    Counter counter = new Counter();
    counter.increment(5);
    counter.increment(0);
    assertEquals(5L, counter.get());
  }

  @Test
  public void testIncrementByNegativeThrowsException() {
    Counter counter = new Counter();
    try {
      counter.increment(-1);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      assertTrue(e.getMessage().contains("positive"));
    }
  }

  @Test
  public void testOverflowWrapsToZero() {
    Counter counter = new Counter();
    // Set counter to MAX_VALUE - 1
    counter.increment(Long.MAX_VALUE - 1);
    assertEquals(Long.MAX_VALUE - 1, counter.get());

    // Increment by 1 should reach MAX_VALUE
    counter.increment(1);
    assertEquals(Long.MAX_VALUE, counter.get());

    // Increment by 1 should wrap to 0
    counter.increment(1);
    assertEquals(0L, counter.get());
  }

  @Test
  public void testOverflowWithLargeIncrement() {
    Counter counter = new Counter();
    // Set counter to MAX_VALUE - 5
    counter.increment(Long.MAX_VALUE - 5);
    assertEquals(Long.MAX_VALUE - 5, counter.get());

    // Increment by 10 should wrap: 10 - (MAX_VALUE - (MAX_VALUE - 5)) - 1 = 10 - 5 - 1 = 4
    counter.increment(10);
    assertEquals(4L, counter.get());
  }

  @Test
  public void testMultipleOverflows() {
    Counter counter = new Counter();
    counter.increment(Long.MAX_VALUE);
    assertEquals(Long.MAX_VALUE, counter.get());

    // First overflow: wraps to 0
    counter.increment(1);
    assertEquals(0L, counter.get());

    // Increment to MAX_VALUE again
    counter.increment(Long.MAX_VALUE);
    assertEquals(Long.MAX_VALUE, counter.get());

    // Second overflow: wraps to 0 again
    counter.increment(1);
    assertEquals(0L, counter.get());
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
  public void testConcurrentIncrementsWithDifferentValues() throws InterruptedException {
    Counter counter = new Counter();
    int numThreads = 10;
    ExecutorService executor = Executors.newFixedThreadPool(numThreads);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch completionLatch = new CountDownLatch(numThreads);
    List<Long> increments = new ArrayList<>();

    for (int i = 0; i < numThreads; i++) {
      final long increment = (i + 1) * 10;
      increments.add(increment);
      executor.submit(() -> {
        try {
          startLatch.await();
          counter.increment(increment);
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

    long expectedTotal = increments.stream().mapToLong(Long::longValue).sum();
    assertEquals(expectedTotal, counter.get());
  }

  @Test
  public void testToString() {
    Counter counter = new Counter();
    counter.increment(42);
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
