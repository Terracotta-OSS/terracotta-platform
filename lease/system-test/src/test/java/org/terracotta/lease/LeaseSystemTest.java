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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.terracotta.connection.Connection;
import org.terracotta.connection.ConnectionFactory;
import org.terracotta.connection.ConnectionPropertyNames;
import org.terracotta.connection.entity.Entity;
import org.terracotta.connection.entity.EntityRef;
import org.terracotta.exception.EntityAlreadyExistsException;
import org.terracotta.exception.EntityConfigurationException;
import org.terracotta.exception.EntityNotFoundException;
import org.terracotta.exception.EntityNotProvidedException;
import org.terracotta.exception.EntityVersionMismatchException;
import org.terracotta.exception.PermanentEntityException;
import org.terracotta.lease.service.LeaseServiceProvider;
import org.terracotta.lease.service.config.LeaseConfiguration;
import org.terracotta.passthrough.PassthroughClusterControl;
import org.terracotta.passthrough.PassthroughTestHelpers;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Properties;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LeaseSystemTest {
  private static PassthroughClusterControl cluster;

  @BeforeClass
  public static void beforeClass() {
    cluster = PassthroughTestHelpers.createActiveOnly("stripe",
            server -> {
              server.registerClientEntityService(new LeaseAcquirerClientService());
              server.registerServerEntityService(new LeaseAcquirerServerService());
              server.registerOverrideServiceProvider(new LeaseServiceProvider(), new LeaseConfiguration(500L));
            });

    cluster.startAllServers();
  }

  @Test
  public void operationLongerThanLeaseLength() throws Exception {
    Properties properties = new Properties();
    properties.put(ConnectionPropertyNames.CONNECTION_NAME, "LeaseSystemTest");

    URI clusterURI = URI.create("passthrough://stripe");
    Connection connection = ConnectionFactory.connect(clusterURI, properties);
    LeaseMaintainer leaseMaintainer = LeaseMaintainerFactory.createLeaseMaintainer(connection);

    LeaseTestUtil.waitForValidLease(leaseMaintainer);

    Lease lease1 = leaseMaintainer.getCurrentLease();
    Thread.sleep(1000L);
    Lease lease2 = leaseMaintainer.getCurrentLease();

    assertTrue(lease2.isValidAndContiguous(lease1));
  }

  @Test
  public void leaseWithDelayedConnection() throws Exception {
    Properties properties = new Properties();
    properties.put(ConnectionPropertyNames.CONNECTION_NAME, "LeaseSystemTest");

    URI clusterURI = URI.create("passthrough://stripe");
    Connection connection = ConnectionFactory.connect(clusterURI, properties);
    LeaseDelayedConnection delayedConnection = new LeaseDelayedConnection(connection);
    LeaseMaintainer leaseMaintainer = LeaseMaintainerFactory.createLeaseMaintainer(delayedConnection);

    LeaseTestUtil.waitForValidLease(leaseMaintainer);

    delayedConnection.setDelay(700L);

    Lease lease1 = leaseMaintainer.getCurrentLease();
    Thread.sleep(1000L);
    Lease lease2 = leaseMaintainer.getCurrentLease();

    assertFalse(lease2.isValidAndContiguous(lease1));
  }

  @AfterClass
  public static void afterClass() throws Exception {
    cluster.tearDown();
  }

  private static class LeaseDelayedConnection implements Connection {
    private final Connection delegate;
    private final Collection<LeaseDelayedEntityRef> entityRefs = Collections.synchronizedCollection(new ArrayList<>());

    public LeaseDelayedConnection(Connection delegate) {
      this.delegate = delegate;
    }

    public void setDelay(long delay) {
      for (LeaseDelayedEntityRef entityRef : entityRefs) {
        entityRef.setDelay(delay);
      }
    }

    @Override
    public <T extends Entity, C, U> EntityRef<T, C, U> getEntityRef(Class<T> aClass, long l, String s) throws EntityNotProvidedException {
      EntityRef<T, C, U> entityRef = delegate.getEntityRef(aClass, l, s);
      if (aClass.equals(LeaseAcquirer.class)) {
        EntityRef<LeaseAcquirer, Properties, Object> leaseEntityRef = (EntityRef<LeaseAcquirer, Properties, Object>) entityRef;
        LeaseDelayedEntityRef result = new LeaseDelayedEntityRef(leaseEntityRef);
        entityRefs.add(result);
        return (EntityRef<T, C, U>) result;
      }

      return entityRef;
    }

    @Override
    public void close() throws IOException {
      delegate.close();
    }
  }

  private static class LeaseDelayedEntityRef implements EntityRef<LeaseAcquirer, Properties, Object> {
    private final EntityRef<LeaseAcquirer, Properties, Object> delegate;
    private final Collection<DelayedLeaseAcquirer> entities = Collections.synchronizedCollection(new ArrayList<>());

    public LeaseDelayedEntityRef(EntityRef<LeaseAcquirer, Properties, Object> delegate) {
      this.delegate = delegate;
    }

    public void setDelay(long delay) {
      for (DelayedLeaseAcquirer entity : entities) {
        entity.setDelay(delay);
      }
    }

    @Override
    public void create(Properties properties) throws EntityNotProvidedException, EntityAlreadyExistsException, EntityVersionMismatchException, EntityConfigurationException {
      delegate.create(properties);
    }

    @Override
    public Properties reconfigure(Properties properties) throws EntityNotProvidedException, EntityNotFoundException, EntityConfigurationException {
      return delegate.reconfigure(properties);
    }

    @Override
    public boolean destroy() throws EntityNotProvidedException, EntityNotFoundException, PermanentEntityException {
      return delegate.destroy();
    }

    @Override
    public LeaseAcquirer fetchEntity(Object o) throws EntityNotFoundException, EntityVersionMismatchException {
      DelayedLeaseAcquirer result = new DelayedLeaseAcquirer(delegate.fetchEntity(o));
      entities.add(result);
      return result;
    }

    @Override
    public String getName() {
      return delegate.getName();
    }
  }

  private static class DelayedLeaseAcquirer implements LeaseAcquirer {
    private final LeaseAcquirer delegate;
    private volatile long delay;

    public DelayedLeaseAcquirer(LeaseAcquirer delegate) {
      this.delegate = delegate;
    }

    public void setDelay(long delay) {
      this.delay = delay;
    }

    @Override
    public long acquireLease() throws LeaseException, InterruptedException {
      Thread.sleep(delay);
      return delegate.acquireLease();
    }

    @Override
    public void close() {
      delegate.close();
    }
  }
}
