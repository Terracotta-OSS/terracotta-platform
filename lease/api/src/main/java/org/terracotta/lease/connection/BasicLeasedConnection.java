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
package org.terracotta.lease.connection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.connection.Connection;
import org.terracotta.connection.ConnectionException;
import org.terracotta.connection.entity.Entity;
import org.terracotta.connection.entity.EntityRef;
import org.terracotta.exception.EntityNotProvidedException;
import org.terracotta.lease.LeaseMaintainer;
import org.terracotta.lease.LeaseMaintainerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;


public class BasicLeasedConnection implements LeasedConnection {
  private final static Logger LOGGER = LoggerFactory.getLogger(BasicLeasedConnection.class);

  private final Connection base;
  private final LeaseMaintainer leaseMaintainer;

  public static BasicLeasedConnection create(Connection connection, TimeBudget timeBudget) throws ConnectionException {
    LeaseMaintainer leaseMaintainer = LeaseMaintainerFactory.createLeaseMaintainer(connection);

    Exception exception = null;
    try {
      boolean leased = leaseMaintainer.waitForLease(timeBudget.remaining(TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS);
      if (!leased) {
        exception = new IOException("Unable to acquire lease for connection before connection timeout");
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      exception = e;
    }

    if (exception != null) {
      try {
        connection.close();
      } catch (IOException e) {
        LOGGER.error("Failed to close connection that we failed to get a lease on", e);
      }
      throw new ConnectionException(exception);
    }

    return new BasicLeasedConnection(connection, leaseMaintainer);
  }

  private BasicLeasedConnection(Connection base, LeaseMaintainer leaseMaintainer) {
    this.base = base;
    this.leaseMaintainer = leaseMaintainer;
  }

  @Override
  public <T extends Entity, C, U> EntityRef<T, C, U> getEntityRef(Class<T> cls, long version, String name) throws EntityNotProvidedException {
    return base.getEntityRef(cls, version, name);
  }

  @Override
  public void close() throws IOException {
    /*
     * Destroy closes the connection this lease maintainer is managing, and interrupts the leasing related threads
     * (leading to their eventual termination).  The fetch leasing entity will get released when the server sees the
     * connection close.  This prevents unavailability of the server from blocking connection close due to an attempt
     * to close the lease entity fetch.
     */
    leaseMaintainer.destroy();
  }

}
