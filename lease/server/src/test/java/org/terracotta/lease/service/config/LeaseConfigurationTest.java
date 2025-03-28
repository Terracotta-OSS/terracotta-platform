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
package org.terracotta.lease.service.config;

import org.junit.Test;
import org.terracotta.entity.ServiceProviderConfiguration;
import org.terracotta.lease.service.LeaseServiceProvider;

import static org.junit.Assert.assertEquals;

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

  @Test(expected = IllegalArgumentException.class)
  public void zeroLeaseLength() {
    new LeaseConfiguration(0L);
  }

  @Test(expected = IllegalArgumentException.class)
  public void negativeLeaseLength() {
    new LeaseConfiguration(-1L);
  }
}
