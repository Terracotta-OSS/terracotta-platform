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

import org.terracotta.entity.EntityResponse;

/**
 * A message sent from the the server entity to the client entity to indicate the response to the LeaseRequest.
 */
public class LeaseResponse implements EntityResponse {
  private final boolean leaseGranted;
  private final long leaseLength;

  public static LeaseResponse leaseNotGranted() {
    return new LeaseResponse(false, -1L);
  }

  public static LeaseResponse leaseGranted(long leaseLength) {
    if (leaseLength <= 0) {
      throw new IllegalArgumentException("Granting a non-positive length lease is not allowed: " + leaseLength);
    }
    return new LeaseResponse(true, leaseLength);
  }

  private LeaseResponse(boolean leaseGranted, long leaseLength) {
    this.leaseGranted = leaseGranted;
    this.leaseLength = leaseLength;
  }

  public boolean isLeaseGranted() {
    return leaseGranted;
  }

  public long getLeaseLength() {
    if (leaseLength <= 0) {
      throw new IllegalStateException("Attempt to get the lease length when the lease was not granted");
    }
    return leaseLength;
  }
}
