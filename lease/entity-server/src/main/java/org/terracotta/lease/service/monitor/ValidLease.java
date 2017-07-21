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

/**
 * Represents a lease that has been issued to a client. It may have expired, but if so, that has not been detected yet.
 */
class ValidLease implements Lease {
  private final long leaseExpiry;

  ValidLease(long leaseExpiry) {
    this.leaseExpiry = leaseExpiry;
  }

  @Override
  public boolean isExpired(long now) {
    return leaseExpiry - now < 0;
  }

  @Override
  public boolean allowRenewal() {
    return true;
  }

  boolean expiresBefore(ValidLease newLease) {
    return leaseExpiry - newLease.leaseExpiry < 0;
  }

  @Override
  public String toString() {
    return "ValidLease{ leaseExpiry:" + leaseExpiry + " }";
  }
}
