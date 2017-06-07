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

import com.tc.classloader.CommonComponent;

/**
 * The result from the LeaseService that indicates whether a lease was granted and, if so, for how long.
 */
@CommonComponent
public class LeaseResult {
  private static final long LEASE_NOT_GRANTED = -1L;

  private final long leaseLength;

  public static LeaseResult leaseNotGranted() {
    return new LeaseResult(LEASE_NOT_GRANTED);
  }

  public static LeaseResult leaseGranted(long leaseLength) {
    if (leaseLength <= 0) {
      throw new IllegalArgumentException("Illegal attempt to create a lease with a negative lease length");
    }

    return new LeaseResult(leaseLength);
  }

  private LeaseResult(long leaseLength) {
    this.leaseLength = leaseLength;
  }

  public boolean isLeaseGranted() {
    return leaseLength != LEASE_NOT_GRANTED;
  }

  /**
   * Only call this if isLeaseGranted() returns true
   * @return the length of the lease in milliseconds
   */
  public long getLeaseLength() {
    if (leaseLength == LEASE_NOT_GRANTED) {
      throw new IllegalStateException("Illegal attempt to get the length of a lease that was not granted");
    }

    return leaseLength;
  }
}
