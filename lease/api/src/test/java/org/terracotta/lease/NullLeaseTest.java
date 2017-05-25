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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NullLeaseTest {
  @Test
  public void notValid() {
    TestTimeSource timeSource = new TestTimeSource();

    NullLease lease1 = new NullLease();

    long start2 = timeSource.nanoTime();
    long end2 = start2 + 10L;
    LeaseInternal lease2 = lease1.extend(timeSource, start2, end2);

    assertFalse(lease2.isValidAndContiguous(lease1));

    timeSource.tickNanos(5L);

    long start3 = timeSource.nanoTime();
    long end3 = start3 + 10L;
    LeaseInternal lease3 = lease2.extend(timeSource, start3, end3);

    assertFalse(lease3.isValidAndContiguous(lease1));
    assertTrue(lease3.isValidAndContiguous(lease2));
  }
}
