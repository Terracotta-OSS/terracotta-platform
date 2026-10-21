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

import java.util.function.LongSupplier;

/**
 * A gauge provides the current value via a supplier.
 * The value can increase or decrease over time.
 */
public class Gauge implements Metric {
  private final LongSupplier valueSupplier;

  /**
   * Create a new gauge with the given value supplier.
   *
   * @param valueSupplier supplier that provides the current value
   */
  public Gauge(LongSupplier valueSupplier) {
    this.valueSupplier = valueSupplier;
  }

  @Override
  public long get() {
    return valueSupplier.getAsLong();
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + ":" + get();
  }
}

// Made with Bob
