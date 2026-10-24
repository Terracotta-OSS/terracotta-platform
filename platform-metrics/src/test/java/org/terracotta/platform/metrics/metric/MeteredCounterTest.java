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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Unit tests for MeteredCounter.
 */
public class MeteredCounterTest {

  @Test
  public void testMeteredCounterInitializedToZero() {
    MeteredCounter counter = new MeteredCounter(new Counter());
    assertEquals(0L, counter.get(false));
  }

  @Test
  public void testIncrementByOne() {
    MeteredCounter counter = new MeteredCounter(new Counter());
    counter.increment();
    assertEquals(1L, counter.get(false));

    counter.increment();
    assertEquals(2L, counter.get(false));

    counter.increment();
    assertEquals(3L, counter.get(false));
  }

  @Test
  public void testIncrementByN() {
    MeteredCounter counter = new MeteredCounter(new Counter());
    counter.increment(5);
    assertEquals(5L, counter.get(false));

    counter.increment(10);
    assertEquals(15L, counter.get(false));

    counter.increment(100);
    assertEquals(115L, counter.get(false));
  }

  @Test
  public void testIncrementByNegativeThrowsException() {
    MeteredCounter counter = new MeteredCounter(new Counter());
    try {
      counter.increment(-1);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      assertTrue(e.getMessage().contains("positive"));
    }
  }

  @Test
  public void testGetAndResetReturnsDelta() {
    MeteredCounter counter = new MeteredCounter(new Counter());
    counter.increment(10);
    assertEquals(10L, counter.get(true));

    // After reset, delta should be 0
    assertEquals(0L, counter.get(false));

    counter.increment(5);
    assertEquals(5L, counter.get(true));

    // After reset, delta should be 0 again
    assertEquals(0L, counter.get(false));
  }

  @Test
  public void testGetDoesNotReset() {
    MeteredCounter counter = new MeteredCounter(new Counter());
    counter.increment(10);
    assertEquals(10L, counter.get(false));

    // get() should not reset
    assertEquals(10L, counter.get(false));
    assertEquals(10L, counter.get(false));

    // getAndReset() should reset
    assertEquals(10L, counter.get(true));
    assertEquals(0L, counter.get(false));
  }

  @Test
  public void testMultipleResets() {
    MeteredCounter counter = new MeteredCounter(new Counter());

    counter.increment(10);
    assertEquals(10L, counter.get(true));

    counter.increment(20);
    assertEquals(20L, counter.get(true));

    counter.increment(30);
    assertEquals(30L, counter.get(true));

    assertEquals(0L, counter.get(false));
  }

  @Test
  public void testOverflowHandling() {
    MeteredCounter counter = new MeteredCounter(new Counter());

    // Increment to MAX_VALUE
    counter.increment(Long.MAX_VALUE);
    assertEquals(Long.MAX_VALUE, counter.get(false));

    // Reset snapshot
    long delta1 = counter.get(true);
    assertEquals(Long.MAX_VALUE, delta1);

    // Increment by 1 should wrap to 0
    counter.increment(1);
    assertEquals(1L, counter.get(false));

    // Increment by 10 more
    counter.increment(10);
    assertEquals(11L, counter.get(false));

    // Reset and verify delta
    long delta2 = counter.get(true);
    assertEquals(11L, delta2);
  }

  @Test
  public void testOverflowDeltaCalculation() {
    MeteredCounter counter = new MeteredCounter(new Counter());

    // Set counter to MAX_VALUE - 5
    counter.increment(Long.MAX_VALUE - 5);
    counter.get(true); // Reset snapshot to MAX_VALUE - 5

    // Increment by 10 to cause overflow
    // Expected: wraps to 4
    counter.increment(10);

    // Delta should be: (MAX_VALUE - (MAX_VALUE - 5)) + 4 + 1 = 5 + 4 + 1 = 10
    long delta = counter.get(false);
    assertEquals(10L, delta);
  }

  @Test
  public void testConcurrentIncrements() throws InterruptedException {
    MeteredCounter counter = new MeteredCounter(new Counter());
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

    assertEquals(numThreads * incrementsPerThread, counter.get(false));
  }

  @Test
  public void testConcurrentIncrementsAndResets() throws InterruptedException {
    MeteredCounter counter = new MeteredCounter(new Counter());
    int numIncrementThreads = 5;
    int numResetThreads = 2;
    int incrementsPerThread = 1000;
    ExecutorService executor = Executors.newFixedThreadPool(numIncrementThreads + numResetThreads);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch completionLatch = new CountDownLatch(numIncrementThreads + numResetThreads);

    // Start increment threads
    for (int i = 0; i < numIncrementThreads; i++) {
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

    // Start reset threads
    for (int i = 0; i < numResetThreads; i++) {
      executor.submit(() -> {
        try {
          startLatch.await();
          for (int j = 0; j < 100; j++) {
            counter.get(true);
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

    // Final delta should be small (only increments since last reset)
    long finalDelta = counter.get(false);
    assertTrue(finalDelta >= 0);
    assertTrue(finalDelta <= numIncrementThreads * incrementsPerThread);
  }

  @Test
  public void testToString() {
    MeteredCounter counter = new MeteredCounter(new Counter());
    counter.increment(42);
    String str = counter.toString();
    assertTrue(str.contains("MeteredCounter"));
    assertTrue(str.contains("42"));
  }

  @Test
  public void testMeteredCounterIsMetric() {
    MeteredCounter counter = new MeteredCounter(new Counter());
    assertTrue(counter instanceof Metric);
  }

  @Test
  public void testResetWithNoIncrements() {
    MeteredCounter counter = new MeteredCounter(new Counter());
    assertEquals(0L, counter.get(true));
    assertEquals(0L, counter.get(true));
    assertEquals(0L, counter.get(true));
  }

  @Test
  public void testIncrementAfterOverflow() {
    MeteredCounter counter = new MeteredCounter(new Counter());

    // Increment to MAX_VALUE and reset
    counter.increment(Long.MAX_VALUE);
    counter.get(true);

    // Cause overflow
    counter.increment(1);
    assertEquals(1L, counter.get(false));

    // Continue incrementing after overflow
    counter.increment(5);
    assertEquals(6L, counter.get(false));

    // Reset and verify
    assertEquals(6L, counter.get(true));
    assertEquals(0L, counter.get(false));
  }
}

// Made with Bob
