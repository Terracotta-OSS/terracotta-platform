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

import org.terracotta.entity.ActiveServerEntity;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.ConfigurationException;
import org.terracotta.entity.PassiveSynchronizationChannel;
import org.terracotta.lease.service.LeaseResult;
import org.terracotta.lease.service.LeaseService;

/**
 * The active server-side entity for connection leasing. Pretty much just delegates to the LeaseService.
 */
class ActiveLeaseAcquirer implements ActiveServerEntity<LeaseRequest, LeaseResponse> {
  private final LeaseService leaseService;

  ActiveLeaseAcquirer(LeaseService leaseService) {
    this.leaseService = leaseService;
  }

  @Override
  public void connected(ClientDescriptor clientDescriptor) {
  }

  @Override
  public void disconnected(ClientDescriptor clientDescriptor) {
    leaseService.disconnected(clientDescriptor);
  }

  @Override
  public LeaseResponse invoke(ClientDescriptor clientDescriptor, LeaseRequest leaseRequest) {
    LeaseResult leaseResult = leaseService.acquireLease(clientDescriptor);

    if (leaseResult.isLeaseGranted()) {
      long leaseLength = leaseResult.getLeaseLength();
      return LeaseResponse.leaseGranted(leaseLength);
    } else {
      return LeaseResponse.leaseNotGranted();
    }
  }

  @Override
  public void loadExisting() {
  }

  @Override
  public void handleReconnect(ClientDescriptor clientDescriptor, byte[] bytes) {
  }

  @Override
  public void synchronizeKeyToPassive(PassiveSynchronizationChannel<LeaseRequest> passiveSynchronizationChannel, int concurrencyKey) {
  }

  @Override
  public void createNew() throws ConfigurationException {
  }

  @Override
  public void destroy() {
  }
}
