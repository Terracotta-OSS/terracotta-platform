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

/**
 * An implementation of Lease representing a granted lease, which may or may not have expired.
 */
class LeaseImpl implements LeaseInternal {
  private final TimeSource timeSource;
  private final long startOfContiguousLeasedPeriod;
  private final long leaseExpiry;

  LeaseImpl(TimeSource timeSource, long startOfContiguousLeasedPeriod, long leaseExpiry) {
    this.timeSource = timeSource;
    this.startOfContiguousLeasedPeriod = startOfContiguousLeasedPeriod;
    this.leaseExpiry = leaseExpiry;
  }

  @Override
  public boolean isValidAndContiguous(Lease previousLease) {
    if (!(previousLease instanceof LeaseImpl)) {
      return false;
    }

    LeaseImpl previousLeaseImpl = (LeaseImpl) previousLease;

    if (startOfContiguousLeasedPeriod != previousLeaseImpl.startOfContiguousLeasedPeriod) {
      return false;
    }

    return isValid();
  }

  @Override
  public LeaseInternal extend(TimeSource timeSource, long leaseStart, long leaseExpiry) {
    if (isValid()) {
      return new LeaseImpl(timeSource, startOfContiguousLeasedPeriod, leaseExpiry);
    } else {
      return new LeaseImpl(timeSource, leaseStart, leaseExpiry);
    }
  }

  private boolean isValid() {
    long now = timeSource.nanoTime();
    return now - leaseExpiry < 0;
  }

  @Override
  public String toString() {
    return "[ LeaseImpl: startOfContiguousLeasedPeriod: " + startOfContiguousLeasedPeriod + " leaseExpiry: " + leaseExpiry + " ]";
  }
}
