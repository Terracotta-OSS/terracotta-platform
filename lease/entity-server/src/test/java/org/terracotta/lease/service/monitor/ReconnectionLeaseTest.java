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
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ReconnectionLeaseTest {
  @Test
  public void neverExpires() {
    ReconnectionLease lease = new ReconnectionLease();
    assertFalse(lease.isExpired(0L));
  }

  @Test
  public void shouldNeverBeAskedIfItCanBeRenewed() {
    assertThrows(AssertionError.class, () -> {
      ReconnectionLease lease = new ReconnectionLease();
      lease.allowRenewal();
    });
  }
}
