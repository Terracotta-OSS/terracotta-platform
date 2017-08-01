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
 * Represents a lease that expired and so acts as a placeholder for a connection that is in the process of being closed,
 * thus preventing any new leases from being issued for that connection.
 */
class ExpiredLease implements Lease {
  @Override
  public boolean isExpired(long now) {
    return true;
  }

  @Override
  public boolean allowRenewal() {
    return false;
  }
}
