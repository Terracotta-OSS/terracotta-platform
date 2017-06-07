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
package org.terracotta.lease;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class SystemTimeSourceTest {
  @Test
  public void nanoTime() {
    SystemTimeSource timeSource = new SystemTimeSource();

    long start = System.nanoTime();
    long measured = timeSource.nanoTime();
    long end = System.nanoTime();

    assertTrue(start - measured <= 0);
    assertTrue(measured - end <= 0);
  }

  @Test
  public void sleep() throws Exception {
    SystemTimeSource timeSource = new SystemTimeSource();

    long start = System.currentTimeMillis();
    timeSource.sleep(200L);
    long end = System.currentTimeMillis();

    long duration = end - start;

    assertTrue(duration >= 200L);
  }
}
