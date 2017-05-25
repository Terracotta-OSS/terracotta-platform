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

import java.io.Closeable;

/**
 * This object attempts to keep a lease on the connection used to create it.
 */
public interface LeaseMaintainer extends Closeable {
  /**
   * Returns the lease that the LeaseMaintainer currently has. The lease may have expired. If so, the LeaseMaintainer
   * will be trying to acquire a new lease and, when that is available, this method will return it.
   * The Lease object returned can be combined with an earlier Lease object using the isValidAndContiguous() method to
   * ensure that an operation was carried out whilst a lease was held.
   * @return the current lease
   */
  Lease getCurrentLease();
}
