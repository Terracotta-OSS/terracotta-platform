/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
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
package org.terracotta.lease.service;

import com.tc.classloader.CommonComponent;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class LeaseResultTest {
  @Test
  public void leaseGranted() {
    LeaseResult result = LeaseResult.leaseGranted(5L);
    assertTrue(result.isLeaseGranted());
    assertEquals(5L, result.getLeaseLength());
  }

  @Test
  public void leaseNotGranted() {
    LeaseResult result = LeaseResult.leaseNotGranted();
    assertFalse(result.isLeaseGranted());
  }

  @Test(expected = IllegalStateException.class)
  public void leaseNotGrantedDoesNotAllowGettingTheLength() {
    LeaseResult result = LeaseResult.leaseNotGranted();
    result.getLeaseLength();
  }

  @Test(expected = IllegalArgumentException.class)
  public void noZeroLengthLeases() {
    LeaseResult.leaseGranted(0L);
  }

  @Test(expected = IllegalArgumentException.class)
  public void noNegativeLengthLeases() {
    LeaseResult.leaseGranted(-1L);
  }

  @Test
  public void isCommonComponent() {
    assertNotNull(LeaseResult.class.getAnnotation(CommonComponent.class));
  }
}
