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
package org.terracotta.lease.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.lease.service.config.LeaseConfiguration;
import org.terracotta.lease.service.monitor.LeaseState;

/**
 * The implementation of LeaseService. It uses the LeaseState object to carry out the hard work of correctly issuing
 * leases.
 */
public class LeaseServiceImpl implements LeaseService {
  private static Logger LOGGER = LoggerFactory.getLogger(LeaseServiceImpl.class);

  private final LeaseConfiguration leaseConfiguration;
  private final LeaseState leaseState;

  public LeaseServiceImpl(LeaseConfiguration leaseConfiguration, LeaseState leaseState) {
    this.leaseConfiguration = leaseConfiguration;
    this.leaseState = leaseState;
  }

  @Override
  public LeaseResult acquireLease(ClientDescriptor clientDescriptor) {
    LOGGER.debug("Client requested lease: " + clientDescriptor);
    long currentLeaseLength = getLeaseLength();
    boolean acquiredLease = leaseState.acquireLease(clientDescriptor, currentLeaseLength);

    if (acquiredLease) {
      LOGGER.debug("Client acquired lease: " + clientDescriptor);
      return LeaseResult.leaseGranted(currentLeaseLength);
    } else {
      LOGGER.debug("Client lease request rejected because connection is closing: " + clientDescriptor);
      return LeaseResult.leaseNotGranted();
    }
  }

  @Override
  public void disconnected(ClientDescriptor clientDescriptor) {
    leaseState.disconnected(clientDescriptor);
  }

  @Override
  public void reconnecting(ClientDescriptor clientDescriptor) {
    leaseState.reconnecting(clientDescriptor);
  }

  @Override
  public void reconnected(ClientDescriptor clientDescriptor) {
    leaseState.reconnected(clientDescriptor, getLeaseLength());
  }

  protected long getLeaseLength() {
    return leaseConfiguration.getLeaseLength();
  }
}
