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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.entity.ServiceProvider;
import org.terracotta.entity.ServiceProviderConfiguration;

import static org.terracotta.lease.service.config.LeaseConstants.DEFAULT_LEASE_LENGTH;
import static org.terracotta.lease.service.config.LeaseConstants.MAX_LEASE_LENGTH;

/**
 * Represents the connection leasing configuration from the server's XML config
 */
public class LeaseConfigurationImpl implements ServiceProviderConfiguration, LeaseConfiguration {
  private static final Logger LOGGER = LoggerFactory.getLogger(LeaseConfigurationImpl.class);

  private volatile long leaseLength;

  public LeaseConfigurationImpl(long initialLength) {
    if (initialLength <= 0) {
      throw new IllegalArgumentException("Only positive lease lengths are acceptable");
    }
    setLeaseLength(initialLength);
  }

  @Override
  public long getLeaseLength() {
    return leaseLength;
  }

  @Override
  public void setLeaseLength(long leaseLength) {
    if (leaseLength <= 0) {
      LOGGER.warn("Non-positive lease length: " + leaseLength + ", ignoring it");
      this.leaseLength = use(DEFAULT_LEASE_LENGTH);
    }
    if (leaseLength > MAX_LEASE_LENGTH) {
      LOGGER.warn("Excessive lease length: " + leaseLength + ", using smaller value: " + MAX_LEASE_LENGTH);
      this.leaseLength = use(MAX_LEASE_LENGTH);
    }
    this.leaseLength = leaseLength;
  }

  @Override
  public Class<? extends ServiceProvider> getServiceProviderType() {
    return LeaseServiceProvider.class;
  }

  private static long use(long leaseLength) {
    LOGGER.info("Using lease length of " + leaseLength + "ms");
    return leaseLength;
  }
}
