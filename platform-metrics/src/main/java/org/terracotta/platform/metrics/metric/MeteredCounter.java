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

  /**
   * Get current delta since last snapshot, optionally resetting the snapshot to the current counter value.
   * Handles counter overflow.
   *
   * @param reset true to atomically update the snapshot to the current value, false to leave it untouched
   * @return the delta since last snapshot
   */
  public long get(boolean reset) {
    long current = counter.get();
    long last = reset ? lastSnapshot.getAndSet(current) : lastSnapshot.get();
    return delta(current, last);
  }

  /**
   * Get current delta since last snapshot, without resetting.
   *
   * @return the delta since last snapshot
   */
  @Override
  public long get() {
    return get(false);
  }

  /**
   * Returns delta since last snapshot and updates the snapshot to the current value.
   *
   * @return the delta since last snapshot
   */
  public long getAndReset() {
    return get(true);
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
