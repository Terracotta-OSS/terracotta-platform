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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The implementation of LeaseMaintainer. It makes lease requests via the lease entity. Then, when lease
 * requests are granted, it updates the current lease to reflect that.
 */
class LeaseMaintainerImpl implements LeaseMaintainer, LeaseReconnectListener {
  private static final Logger LOGGER = LoggerFactory.getLogger(LeaseMaintainerImpl.class);
  private static final long MAXIMUM_WAIT_LENGTH = TimeUnit.MILLISECONDS.convert(5, TimeUnit.MINUTES);
  private static final long RETRY_MILLIS_DURING_RECONNECT = 200L;

  private final LeaseAcquirer leaseAcquirer;
  private final TimeSource timeSource;
  private final AtomicReference<LeaseInternal> currentLease;
  private final CountDownLatch hasLease;

  LeaseMaintainerImpl(LeaseAcquirer leaseAcquirer) {
    this.leaseAcquirer = leaseAcquirer;
    this.timeSource = TimeSourceProvider.getTimeSource();
    this.currentLease = new AtomicReference<LeaseInternal>(new NullLease());
    this.hasLease = new CountDownLatch(1);
  }

  @Override
  public Lease getCurrentLease() {
    return currentLease.get();
  }

  @Override
  public void waitForLease() throws InterruptedException {
    hasLease.await();
  }

  @Override
  public boolean waitForLease(long timeout, TimeUnit timeUnit) throws InterruptedException {
    return hasLease.await(timeout, timeUnit);
  }

  @Override
  public void close() throws IOException {
    leaseAcquirer.close();
  }

  long refreshLease() throws LeaseException, InterruptedException {
    try {
      LOGGER.debug("Refreshing lease");

      while (true) {
        LeaseInternal lease = currentLease.get();

        long leaseRequestStartNanos = timeSource.nanoTime();
        long leaseLengthMillis = leaseAcquirer.acquireLease();
        long leaseRequestEndNanos = timeSource.nanoTime();

        boolean updated = updateLease(lease, leaseRequestStartNanos, leaseRequestEndNanos, leaseLengthMillis);

        if (updated) {
          hasLease.countDown();
          return calculateWaitLength(leaseRequestStartNanos, leaseRequestEndNanos, leaseLengthMillis);
        }

        if (Thread.interrupted()) {
          throw new InterruptedException();
        }
      }
    } catch (LeaseReconnectingException e) {
      LOGGER.debug(e.getMessage());
      return RETRY_MILLIS_DURING_RECONNECT;
    }
  }

  @Override
  public void reconnecting() {
    currentLease.set(new NullLease());
  }

  @Override
  public void reconnected() {
  }

  private long calculateWaitLength(long leaseRequestStartNanos, long leaseRequestEndNanos, long leaseLengthMillis) {
    long leaseAcquisitionNanos = leaseRequestEndNanos - leaseRequestStartNanos;
    long leaseAcquisitionMillis = TimeUnit.MILLISECONDS.convert(leaseAcquisitionNanos, TimeUnit.NANOSECONDS);

    long waitLength = (leaseLengthMillis / 3) - leaseAcquisitionMillis;

    return Math.max(0, Math.min(MAXIMUM_WAIT_LENGTH, waitLength));
  }

  private boolean updateLease(LeaseInternal lease, long leaseRequestStartNanos, long leaseRequestEndNanos, long leaseLengthMillis) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("updateLease: leaseRequestStartNanos: " + leaseRequestStartNanos + " leaseRequestEndNanos: " + leaseRequestEndNanos + " leaseLengthMillis: " + leaseLengthMillis);
    }

    long leaseStart = leaseRequestEndNanos;
    long leaseExpiry = leaseRequestStartNanos + TimeUnit.MILLISECONDS.toNanos(leaseLengthMillis);

    if (leaseExpiry - leaseStart < 0) {
      LOGGER.warn("Received new lease but it expires before it starts.");
    }

    LeaseInternal updatedLease = lease.extend(timeSource, leaseStart, leaseExpiry);
    boolean updated = currentLease.compareAndSet(lease, updatedLease);

    logLease(updated, lease, updatedLease);

    return updated;
  }

  private void logLease(boolean updated, Lease previousLease, Lease newLease) {
    if (!updated) {
      LOGGER.info("Received new lease, but could not use it because another lease was more recent, perhaps due to reconnect.");
      return;
    }

    boolean gap = !newLease.isValidAndContiguous(previousLease) && !(previousLease instanceof NullLease);

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Lease updated. Last lease: " + previousLease + " latest lease: " + newLease);
    }

    if (gap) {
      LOGGER.warn("A gap in leases occurred. nanoTime: " + timeSource.nanoTime());
    }
  }

  @Override
  public void destroy() throws IOException {
    throw new UnsupportedOperationException();
  }
}
