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

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for Gauge.
 */
public class GaugeTest {

  @Test
  public void testGaugeReturnsSuppliedValue() {
    Gauge gauge = new Gauge(() -> 42L);
    assertEquals(42L, gauge.get());
  }

  @Test
  public void testGaugeReflectsChangingValue() {
    AtomicLong value = new AtomicLong(10);
    Gauge gauge = new Gauge(value::get);

    assertEquals(10L, gauge.get());

    value.set(20);
    assertEquals(20L, gauge.get());

    value.set(100);
    assertEquals(100L, gauge.get());
  }

  @Test
  public void testGaugeWithZeroValue() {
    Gauge gauge = new Gauge(() -> 0L);
    assertEquals(0L, gauge.get());
  }

  @Test
  public void testGaugeWithNegativeValue() {
    Gauge gauge = new Gauge(() -> -42L);
    assertEquals(-42L, gauge.get());
  }

  @Test
  public void testGaugeWithMaxValue() {
    Gauge gauge = new Gauge(() -> Long.MAX_VALUE);
    assertEquals(Long.MAX_VALUE, gauge.get());
  }

  @Test
  public void testGaugeWithMinValue() {
    Gauge gauge = new Gauge(() -> Long.MIN_VALUE);
    assertEquals(Long.MIN_VALUE, gauge.get());
  }

  @Test
  public void testGaugeToString() {
    Gauge gauge = new Gauge(() -> 123L);
    String str = gauge.toString();
    assertTrue(str.contains("Gauge"));
    assertTrue(str.contains("123"));
  }

  @Test
  public void testGaugeIsMetric() {
    Gauge gauge = new Gauge(() -> 42L);
    assertTrue(gauge instanceof Metric);
  }
}

// Made with Bob
