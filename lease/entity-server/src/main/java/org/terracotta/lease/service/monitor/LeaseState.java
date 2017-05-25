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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.lease.TimeSource;
import org.terracotta.lease.service.closer.ClientConnectionCloser;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * The central component of the connection leasing code. This object holds the state of the leases for each client and
 * allows updates to that state in a thread-safe way.
 */
public class LeaseState {
  private static Logger LOGGER = LoggerFactory.getLogger(LeaseState.class);

  private final TimeSource timeSource;
  private final ClientConnectionCloser clientConnectionCloser;
  private final ConcurrentHashMap<ClientDescriptor, Lease> leases = new ConcurrentHashMap<>();

  public LeaseState(TimeSource timeSource, ClientConnectionCloser clientConnectionCloser) {
    this.timeSource = timeSource;
    this.clientConnectionCloser = clientConnectionCloser;
  }

  public void disconnected(ClientDescriptor clientDescriptor) {
    leases.remove(clientDescriptor);
  }

  public boolean acquireLease(ClientDescriptor clientDescriptor, long leaseLength) {
    long leaseExpiry = timeSource.nanoTime() + TimeUnit.MILLISECONDS.toNanos(leaseLength);
    ValidLease newLease = new ValidLease(leaseExpiry);

    while (true) {
      Lease currentLease = leases.get(clientDescriptor);

      if (currentLease == null) {
        Lease existingLease = leases.putIfAbsent(clientDescriptor, newLease);
        if (existingLease == null) {
          return true;
        }
      } else {
        if (!(currentLease instanceof ValidLease)) {
          return false; // This client's connection is being closed
        }

        ValidLease currentValidLease = (ValidLease) currentLease;
        if (newLease.expiresBefore(currentValidLease)) {
          return true;
        }

        boolean replaced = leases.replace(clientDescriptor, currentLease, newLease);
        if (replaced) {
          return true;
        }
      }
    }
  }

  void checkLeases() {
    LOGGER.debug("Checking leases");
    long now = timeSource.nanoTime();

    // The iterator from ConcurrentHashMap.iterator() is guaranteed not to throw a ConcurrentModificationException
    // We rely on that guarantee here because we may modify the leases map inside the iteration.
    for (ClientDescriptor clientDescriptor : leases.keySet()) {
      checkLease(clientDescriptor, now);
    }
  }

  private void checkLease(ClientDescriptor clientDescriptor, long now) {
    while (true) {
      Lease lease = leases.get(clientDescriptor);

      if (lease == null) {
        return; // Some other thread called checkLeases() and expired the lease
      }

      if (!(lease instanceof ValidLease)) {
        return; // Some other thread is expiring this lease - leave it alone
      }

      if (!lease.isExpired(now)) {
        LOGGER.trace("Lease for client: " + clientDescriptor + " is still valid");
        return; // The lease is still valid so no change needed
      }

      Lease expiredLease = new ExpiredLease();
      boolean replaced = leases.replace(clientDescriptor, lease, expiredLease);

      if (replaced) {
        LOGGER.info("Closing connection to client: " + clientDescriptor + " due to lease expiry");
        clientConnectionCloser.closeClientConnection(clientDescriptor);
        return;
      }

      // Otherwise loop because another thread updated the lease whilst we looked at it
    }
  }
}
