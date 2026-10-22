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
 * Monotonically increasing counter with overflow handling.
 * When the counter reaches Long.MAX_VALUE, it wraps to 0.
 */
public class Counter implements Metric {
  private final AtomicLong value;

  /**
   * Create a new counter initialized to 0.
   */
  public Counter() {
    this.value = new AtomicLong(0);
  }

  @Override
  public long get() {
    return value.get();
  }

  /**
   * Increment the counter by 1.
   * Handles Long.MAX_VALUE wrap to 0.
   */
  public void increment() {
    increment(1);
  }

  /**
   * Increment the counter by n.
   * Handles Long.MAX_VALUE wrap to 0.
   *
   * @param n positive value to add (must be > 0)
   * @throws IllegalArgumentException if n is negative
   */
  public void increment(long n) {
    if (n == 0) {
      return;
    }
    if (n < 0) {
      throw new IllegalArgumentException("Increment value must be positive: " + n);
    }
    long current;
    long next;
    do {
      current = value.get();
      // Check if adding n would overflow
      if (current > Long.MAX_VALUE - n) {
        // Overflow: wrap around
        next = n - (Long.MAX_VALUE - current) - 1;
      } else {
        next = current + n;
      }
    } while (!value.compareAndSet(current, next));
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + ":" + get();
  }
}

// Made with Bob
