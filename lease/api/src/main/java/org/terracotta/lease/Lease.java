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
 * This object represents a lease held by the client, or potentially the absence of a lease.
 */
public interface Lease {
  /**
   * If this method returns true then a valid lease has been held for the entire time between the two leases. If it
   * returns false then it indicates that there was a period out of lease. An out of lease period does not necessarily
   * mean that the server treated this client as gone, but a valid and contiguous lease means that the server will treat
   * this client as continuously available. Note that passing the this lease as the previous lease will return true
   * if and only if the lease is valid.
   * @param previousLease a Lease object obtained at an earlier time
   * @return true if a valid lease has been held for the entire time between the two leases
   */
  boolean isValidAndContiguous(Lease previousLease);
}
