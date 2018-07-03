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
package org.terracotta.lease.service.config;

import org.junit.jupiter.api.Test;
import org.terracotta.entity.ServiceProviderConfiguration;
import org.terracotta.lease.service.LeaseServiceProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class LeaseConfigurationTest {
  @Test
  public void hasCorrectServiceProviderType() {
    ServiceProviderConfiguration configuration = new LeaseConfiguration(1000L);
    assertEquals(LeaseServiceProvider.class, configuration.getServiceProviderType());
  }

  @Test
  public void givesCorrectLeaseLength() {
    LeaseConfiguration configuration = new LeaseConfiguration(4L);
    assertEquals(4L, configuration.getLeaseLength());
  }

  @Test
  public void zeroLeaseLength() {
    assertThrows(IllegalArgumentException.class, () -> {
      new LeaseConfiguration(0L);
    });
  }

  @Test
  public void negativeLeaseLength() {
    assertThrows(IllegalArgumentException.class, () -> {
      new LeaseConfiguration(-1L);
    });
  }
}
