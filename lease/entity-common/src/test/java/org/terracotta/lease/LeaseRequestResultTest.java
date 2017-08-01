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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LeaseRequestResultTest {
  @Test
  public void notGranted() {
    LeaseRequestResult response = LeaseRequestResult.leaseNotGranted();
    assertFalse(response.isLeaseGranted());
  }

  @Test
  public void granted() {
    LeaseRequestResult response = LeaseRequestResult.leaseGranted(200L);
    assertTrue(response.isLeaseGranted());
    assertEquals(200L, response.getLeaseLength());
  }

  @Test(expected = IllegalArgumentException.class)
  public void zeroLease() {
    LeaseRequestResult.leaseGranted(0L);
  }

  @Test(expected = IllegalArgumentException.class)
  public void negativeLease() {
    LeaseRequestResult.leaseGranted(-1L);
  }
}
