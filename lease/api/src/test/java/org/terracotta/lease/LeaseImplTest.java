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

public class LeaseImplTest {
  @Test
  public void leasesStayBeValidAndContiguous() {
    TestTimeSource timeSource = new TestTimeSource();

    long start1 = timeSource.nanoTime();
    long leaseExpiry1 = start1 + 10L;
    LeaseInternal lease1 = new LeaseImpl(timeSource, start1, leaseExpiry1);

    timeSource.tickNanos(7L);

    long start2 = timeSource.nanoTime();
    long leaseExpiry2 = start2 + 10L;
    LeaseInternal lease2 = lease1.extend(timeSource, start2, leaseExpiry2);

    assertTrue(lease2.isValidAndContiguous(lease1));
    timeSource.tickNanos(7L);
    assertTrue(lease2.isValidAndContiguous(lease1));

    long start3 = timeSource.nanoTime();
    long leaseExpiry3 = start3 + 10L;
    LeaseInternal lease3 = lease2.extend(timeSource, start3, leaseExpiry3);

    timeSource.tickNanos(7L);
    assertFalse(lease2.isValidAndContiguous(lease1));
    assertTrue(lease3.isValidAndContiguous(lease2));
    assertTrue(lease3.isValidAndContiguous(lease1));
  }

  @Test
  public void leaseGap() {
    TestTimeSource timeSource = new TestTimeSource();

    long start1 = timeSource.nanoTime();
    long leaseExpiry1 = start1 + 10L;
    LeaseInternal lease1 = new LeaseImpl(timeSource, start1, leaseExpiry1);

    timeSource.tickNanos(12L);

    long start2 = timeSource.nanoTime();
    long leaseExpiry2 = start2 + 10L;
    LeaseInternal lease2 = lease1.extend(timeSource, start2, leaseExpiry2);

    assertFalse(lease2.isValidAndContiguous(lease1));

    timeSource.tickNanos(5L);

    long start3 = timeSource.nanoTime();
    long leaseExpiry3 = start3 + 10L;
    LeaseInternal lease3 = lease2.extend(timeSource, start3, leaseExpiry3);

    assertFalse(lease3.isValidAndContiguous(lease1));
    assertTrue(lease3.isValidAndContiguous(lease2));
  }
}
