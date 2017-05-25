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
 * An implementation of Lease representing that a lease has not yet been granted.
 */
class NullLease implements LeaseInternal {
  @Override
  public boolean isValidAndContiguous(Lease previousLease) {
    return false;
  }

  @Override
  public LeaseInternal extend(TimeSource timeSource, long leaseStart, long leaseExpiry) {
    return new LeaseImpl(timeSource, leaseStart, leaseExpiry);
  }
}
