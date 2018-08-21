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
package org.terracotta.lease.service.monitor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ValidLeaseTest {
  @Test
  public void notExpiredUntilExpired() {
    ValidLease lease = new ValidLease(10L);
    assertFalse(lease.isExpired(9L));
    assertTrue(lease.isExpired(11L));
  }

  @Test
  public void compareTheExpiryOnTwoLeases() {
    ValidLease lease1 = new ValidLease(10L);
    ValidLease lease2 = new ValidLease(20L);
    assertTrue(lease1.expiresBefore(lease2));
    assertFalse(lease2.expiresBefore(lease1));
    assertFalse(lease1.expiresBefore(lease1));
    assertFalse(lease2.expiresBefore(lease2));
  }

  @Test
  public void alwaysAllowRenewal() {
    ValidLease lease = new ValidLease(10L);
    assertTrue(lease.allowRenewal());
  }
}
