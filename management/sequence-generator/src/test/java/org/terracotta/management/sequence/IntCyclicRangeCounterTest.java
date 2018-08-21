/*
 * Copyright Terracotta, Inc.
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
package org.terracotta.management.sequence;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Mathieu Carbou
 */
public class IntCyclicRangeCounterTest {
  @Test
  public void test_cycle() {
    IntCyclicRangeCounter counter = new IntCyclicRangeCounter(4, 6);
    assertEquals(4, counter.getAndIncrement());
    assertEquals(5, counter.getAndIncrement());
    assertEquals(6, counter.getAndIncrement());
    assertEquals(4, counter.getAndIncrement());
  }

  @Test
  public void test_cycle_min_less_than_zero() {
    assertThrows(IllegalArgumentException.class, () -> {
      new IntCyclicRangeCounter(-1, 3);
    });
  }

  @Test
  public void test_cycle_min_and_max_zero() {
    assertThrows(IllegalArgumentException.class, () -> {
      new IntCyclicRangeCounter(0, 0);
    });
  }

  @Test
  public void test_cycle_max_less_than_zero() {
    assertThrows(IllegalArgumentException.class, () -> {
      new IntCyclicRangeCounter(5, -12);
    });
  }

  @Test
  public void test_cycle_all_negatives() {
    assertThrows(IllegalArgumentException.class, () -> {
      new IntCyclicRangeCounter(-5, -1);
    });
  }

  @Test
  public void test_cycle_min_negative_max_ok() {
    assertThrows(IllegalArgumentException.class, () -> {
      new IntCyclicRangeCounter(-5, 10);
    });
  }
}
