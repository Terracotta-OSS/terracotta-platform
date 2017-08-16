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
import java.io.IOException;
import java.util.concurrent.TimeUnit;

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

  /**
   * If the LeaseMaintainer already has a lease then this method returns immediately. Otherwise, it waits until a
   * lease has been acquired. When the method returns, this does not imply that the lease is still valid (i.e. not
   * expired). After this method returns normally, subsequent calls will return immediately.
   * @throws InterruptedException if the current thread is interrupted while waiting
   */
  void waitForLease() throws InterruptedException;

  /**
   * If the LeaseMaintainer already has a lease then this method returns immediately. Otherwise, it waits until a
   * lease has been acquired or the specified timeout is exceeded. The method returns true if the LeaseMaintainer now
   * has a lease, otherwise false. When the method returns, this does not imply that the lease is still valid (i.e. not
   * expired). After this method returns true, subsequent calls will return true immediately.
   * @param timeout the maximum time to wait
   * @param timeUnit the time unit of the timeout argument
   * @return true if the LeaseMaintainer now has a lease and false if the timeout was exceeded
   * @throws InterruptedException if the current thread is interrupted while waiting
   */
  boolean waitForLease(long timeout, TimeUnit timeUnit) throws InterruptedException;

  /**
   * Destroys this lease maintainer and closes all associated resources <em>without interacting with the server</em>
   */
  void destroy() throws IOException;
}
