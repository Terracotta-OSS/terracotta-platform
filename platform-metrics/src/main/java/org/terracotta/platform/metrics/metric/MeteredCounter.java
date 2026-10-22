/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2026
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.terracotta.platform.metrics.metric;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Tracks delta since last reset, handling counter overflow.
 * This acts as a view on an existing Counter and reports the delta since the last snapshot.
 * <p>
 * Backed by a monotonically increasing counter with overflow handling.
 */
public class MeteredCounter implements Metric {
  private final Counter counter;
  private final AtomicLong lastSnapshot;

  /**
   * Create a new metered counter that wraps an existing counter.
   *
   * @param parent the parent counter to track deltas for
   */
  public MeteredCounter(Counter parent) {
    this.counter = parent;
    this.lastSnapshot = new AtomicLong(parent.get());
  }

  public Counter getCounter() {
    return counter;
  }

  public void increment() {
    counter.increment();
  }

  public void increment(long n) {
    counter.increment(n);
  }

  /**
   * Get current delta since last snapshot.
   * Handles counter overflow.
   *
   * @return the delta since last snapshot
   */
  @Override
  public long get() {
    long current = counter.get();
    long last = lastSnapshot.get();
    return delta(current, last);
  }

  /**
   * Reset the metered counter to the current counter value.
   * Returns delta since last snapshot and updates the snapshot to current value.
   *
   * @return the delta since last snapshot
   */
  public long getAndReset() {
    long current = counter.get();
    long last = lastSnapshot.getAndSet(current);
    return delta(current, last);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + ":" + get();
  }

  private static long delta(long current, long last) {
    if (current >= last) {
      // No overflow
      return current - last;
    } else {
      // Overflow occurred
      return (Long.MAX_VALUE - last) + current + 1;
    }
  }
}

// Made with Bob
