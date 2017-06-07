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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The implementation of LeaseMaintainer. It makes lease requests via the lease entity. Then, when lease
 * requests are granted, it updates the current lease to reflect that.
 */
class LeaseMaintainerImpl implements LeaseMaintainer {
  private static Logger LOGGER = LoggerFactory.getLogger(LeaseMaintainerImpl.class);
  private static long MAXIMUM_WAIT_LENGTH = TimeUnit.MILLISECONDS.convert(5, TimeUnit.MINUTES);

  private final LeaseAcquirer leaseAcquirer;
  private final TimeSource timeSource;
  private final AtomicReference<LeaseInternal> currentLease;

  LeaseMaintainerImpl(LeaseAcquirer leaseAcquirer) {
    this.leaseAcquirer = leaseAcquirer;
    this.timeSource = TimeSourceProvider.getTimeSource();
    this.currentLease = new AtomicReference<LeaseInternal>(new NullLease());
  }

  @Override
  public Lease getCurrentLease() {
    return currentLease.get();
  }

  @Override
  public void close() throws IOException {
    leaseAcquirer.close();
  }

  synchronized long refreshLease() throws LeaseException, InterruptedException {
    LOGGER.debug("Refreshing lease");

    long leaseRequestStartNanos = timeSource.nanoTime();
    long leaseLengthMillis = leaseAcquirer.acquireLease();
    long leaseRequestEndNanos = timeSource.nanoTime();

    updateLease(leaseRequestStartNanos, leaseRequestEndNanos, leaseLengthMillis);

    return calculateWaitLength(leaseRequestStartNanos, leaseRequestEndNanos, leaseLengthMillis);
  }

  private long calculateWaitLength(long leaseRequestStartNanos, long leaseRequestEndNanos, long leaseLengthMillis) {
    long leaseAcquisitionNanos = leaseRequestEndNanos - leaseRequestStartNanos;
    long leaseAcquisitionMillis = TimeUnit.MILLISECONDS.convert(leaseAcquisitionNanos, TimeUnit.NANOSECONDS);

    long waitLength = (leaseLengthMillis / 3) - leaseAcquisitionMillis;

    return Math.max(0, Math.min(MAXIMUM_WAIT_LENGTH, waitLength));
  }

  private void updateLease(long leaseRequestStartNanos, long leaseRequestEndNanos, long leaseLengthMillis) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("updateLease: leaseRequestStartNanos: " + leaseRequestStartNanos + " leaseRequestEndNanos: " + leaseRequestEndNanos + " leaseLengthMillis: " + leaseLengthMillis);
    }

    long leaseStart = leaseRequestEndNanos;
    long leaseExpiry = leaseRequestStartNanos + TimeUnit.MILLISECONDS.toNanos(leaseLengthMillis);

    if (leaseExpiry - leaseStart < 0) {
      LOGGER.warn("Received new lease but it expires before it starts.");
    }

    LeaseInternal lease = currentLease.get();
    LeaseInternal updatedLease = lease.extend(timeSource, leaseStart, leaseExpiry);
    currentLease.set(updatedLease); // refreshLease() is synchronized, so this is the only writer to currentLease and therefore this is safe.

    logLease(lease, updatedLease);
  }

  private void logLease(Lease previousLease, Lease newLease) {
    boolean gap = !newLease.isValidAndContiguous(previousLease) && !(previousLease instanceof NullLease);

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Lease updated. Last lease: " + previousLease + " latest lease: " + newLease);
    }

    if (gap) {
      LOGGER.warn("A gap in leases occurred. nanoTime: " + timeSource.nanoTime());
    }
  }
}
