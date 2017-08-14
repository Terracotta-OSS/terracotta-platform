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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.connection.Connection;
import org.terracotta.connection.entity.EntityRef;
import org.terracotta.exception.EntityNotFoundException;
import org.terracotta.exception.EntityNotProvidedException;
import org.terracotta.exception.EntityVersionMismatchException;

import java.util.Properties;

import static org.terracotta.lease.LeaseEntityConstants.ENTITY_NAME;
import static org.terracotta.lease.LeaseEntityConstants.ENTITY_VERSION;

/**
 * Allows creation of a LeaseMaintainer on a specified connection.
 */
public class LeaseMaintainerFactory {
  private static final Logger LOGGER = LoggerFactory.getLogger(LeaseMaintainerFactory.class);
  /**
   * Creates a LeaseMaintainer to maintain a lease on the connection. This should only be called once for any one
   * Connection.
   *
   * @param connection the connection on which leases should be maintained
   * @return the LeaseMaintainer that will maintain leases on the connection
   * @throws LeaseException if the LeaseMaintainer could not be created - probably because the LeaseAcquirer entity is
   * not correctly installed.
   */
  public static LeaseMaintainer createLeaseMaintainer(Connection connection) {
    LOGGER.info("Creating LeaseMaintainer for connection: " + connection);
    ProxyLeaseReconnectListener leaseReconnectListener = new ProxyLeaseReconnectListener();
    LeaseAcquirer leaseAcquirer = getLeaseAcquirer(connection, leaseReconnectListener);

    LeaseMaintainerImpl leaseMaintainer = new LeaseMaintainerImpl(leaseAcquirer);
    leaseReconnectListener.setUnderlying(leaseMaintainer);

    LeaseMaintenanceThread leaseMaintenanceThread = new LeaseMaintenanceThread(leaseMaintainer);
    LeaseExpiryConnectionKillingThread leaseExpiryConnectionKillingThread = new LeaseExpiryConnectionKillingThread(leaseMaintainer, connection);

    leaseMaintenanceThread.start();
    leaseExpiryConnectionKillingThread.start();

    return new CleaningLeaseMaintainer(leaseMaintainer, connection, leaseMaintenanceThread, leaseExpiryConnectionKillingThread);
  }

  private static LeaseAcquirer getLeaseAcquirer(Connection connection, LeaseReconnectListener leaseReconnectListener) {
    try {
      EntityRef<LeaseAcquirer, Properties, LeaseReconnectListener> entityRef = connection.getEntityRef(LeaseAcquirer.class, ENTITY_VERSION, ENTITY_NAME);
      return entityRef.fetchEntity(leaseReconnectListener);
    } catch (EntityNotProvidedException e) {
      throw new IllegalStateException("LeaseAcquirer entity is not installed", e);
    } catch (EntityNotFoundException e) {
      throw new IllegalStateException("LeaseAcquirer entity is not installed with the name " + ENTITY_NAME, e);
    } catch (EntityVersionMismatchException e) {
      throw new IllegalStateException("LeaseAcquirer entity is not version " + ENTITY_VERSION + " for the name " + ENTITY_NAME, e);
    }
  }
}
