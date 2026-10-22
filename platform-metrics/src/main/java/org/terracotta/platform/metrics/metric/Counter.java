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

import java.util.concurrent.atomic.LongAdder;

/**
 * Monotonically increasing counter with overflow handling.
 * When the counter goes past Long.MAX_VALUE, it wraps to 0.
 *
 * <p>This class is optimized for many concurrent writers (it is incremented from core hot paths)
 * and a rare reader (periodic logging). It is backed by a {@link LongAdder}: concurrent updates
 * are spread over several cells, so writers do not contend on a single memory location and
 * never loop on a compare-and-set of a shared value. {@link #increment()} takes no lock,
 * does not block, and does not allocate once the adder is warm.
 *
 * <p>The cost is moved to {@link #get()}, which sums the cells. The sum is not an atomic
 * snapshot: increments that race with a read may or may not be included, but a completed
 * increment is never lost, and successive reads never go backwards.
 *
 * <p>Wrap-around is free: {@code LongAdder} wraps modulo 2<sup>64</sup>, and {@link #get()}
 * masks the sign bit, which gives a value in {@code [0, Long.MAX_VALUE]} that wraps modulo
 * 2<sup>63</sup> (i.e. {@code Long.MAX_VALUE + 1} reads as 0).
 */
public class Counter implements Metric {
  private final LongAdder value = new LongAdder();

  /**
   * Create a new counter initialized to 0.
   */
  public Counter() {
  }

  /**
   * Create a counter starting at {@code initial}. Package-private: only used by tests to
   * reach the wrap-around without performing 2<sup>63</sup> increments.
   */
  Counter(long initial) {
    value.add(initial);
  }

  /**
   * Get the current value, in {@code [0, Long.MAX_VALUE]}.
   * This sums the internal cells, so it is more expensive than {@link #increment()}
   * and is meant to be called rarely.
   *
   * @return the current value
   */
  @Override
  public long get() {
    return value.sum() & Long.MAX_VALUE;
  }

  /**
   * Increment the counter by 1.
   * Wraps to 0 after Long.MAX_VALUE.
   * This is the fast path: lock-free and non-blocking.
   */
  public void increment() {
    value.increment();
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + ":" + get();
  }
}

// Made with Bob
