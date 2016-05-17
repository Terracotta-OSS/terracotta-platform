/*
 * Copyright Terracotta, Inc.
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

package org.terracotta.management.sequence;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertEquals;

/**
 * @author Mathieu Carbou
 */
@RunWith(JUnit4.class)
public class LongCyclicRangeCounterTest {

  @Test
  public void test_cycle() {
    LongCyclicRangeCounter counter = new LongCyclicRangeCounter(Integer.MAX_VALUE - 2, Integer.MAX_VALUE);
    assertEquals(Integer.MAX_VALUE - 2, counter.getAndIncrement());
    assertEquals(Integer.MAX_VALUE - 1, counter.getAndIncrement());
    assertEquals(Integer.MAX_VALUE, counter.getAndIncrement());
    assertEquals(Integer.MAX_VALUE - 2, counter.getAndIncrement());
  }

}
