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

import org.terracotta.connection.entity.Entity;

public interface LeaseAcquirer extends Entity {
  /**
   * Acquires a lease on the connection. Whilst the lease is held, the server will assume that the client is operating
   * on the basis that the connection exists or may be re-established.
   * @return the number of milliseconds for which the connection lease was issued. Note that the server issued the
   * lease at some point before this method returns, but after it was invoked.
   * @throws LeaseException if a lease could not be obtained. Note that this usually indicates some deeper problem
   * such as incorrect deployment, rather than just that the server didn't want to issue a lease.
   * @throws InterruptedException if the thread was interrupted
   */
  long acquireLease() throws LeaseException, InterruptedException;
}
