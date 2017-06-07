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

interface LeaseInternal extends Lease {
  /**
   * Creates a new lease based on this existing lease. If this existing lease has not expired then the new lease will
   * be contiguous with this existing lease. If this existing lease has expired then the new lease will not be
   * contiguous with this existing lease.
   * @param leaseStart the timestamp indicating the time the lease starts
   * @param leaseExpiry the timestamp indicating the time that the lease expires
   * @return the new lease
   */
  LeaseInternal extend(TimeSource timeSource, long leaseStart, long leaseExpiry);
}
