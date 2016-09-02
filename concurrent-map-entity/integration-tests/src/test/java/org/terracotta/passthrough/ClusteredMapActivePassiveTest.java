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
package org.terracotta.passthrough;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.terracotta.connection.Connection;
import org.terracotta.connection.ConnectionException;
import org.terracotta.connection.ConnectionFactory;
import org.terracotta.connection.entity.EntityRef;
import org.terracotta.entity.map.TerracottaClusteredMapClientService;
import org.terracotta.entity.map.common.ConcurrentClusteredMap;
import org.terracotta.entity.map.server.TerracottaClusteredMapService;
import org.terracotta.exception.EntityAlreadyExistsException;
import org.terracotta.exception.EntityNotFoundException;
import org.terracotta.exception.EntityNotProvidedException;
import org.terracotta.exception.EntityVersionMismatchException;

import java.net.URI;
import java.util.Properties;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

public class ClusteredMapActivePassiveTest {

  private PassthroughClusterControl clusterControl;
  private Connection connection;

  @Before
  public void setUp() throws Exception {
    String stripeName = "stripe";
    this.clusterControl = PassthroughTestHelpers.createActivePassive(stripeName,
        new PassthroughTestHelpers.ServerInitializer() {
          @Override
          public void registerServicesForServer(PassthroughServer server) {
            server.registerClientEntityService(new TerracottaClusteredMapClientService());
            server.registerServerEntityService(new TerracottaClusteredMapService());
          }
        }
    );
    this.connection = connect(stripeName);
  }

  private static Connection connect(String stripeName) throws ConnectionException {
    URI uri = URI.create("passthrough://" + stripeName);
    return ConnectionFactory.connect(uri, new Properties());
  }

  @After
  public void tearDown() throws Exception {
    this.connection.close();
    clusterControl.tearDown();
  }

  private <K, V> ConcurrentClusteredMap<K, V> createClusteredMap(String name, Class<K> keyClass, Class<V> valueClass) throws EntityNotProvidedException, EntityAlreadyExistsException, EntityVersionMismatchException, EntityNotFoundException {
    EntityRef<ConcurrentClusteredMap, Object> entityRef = connection.getEntityRef(ConcurrentClusteredMap.class, ConcurrentClusteredMap.VERSION, name);
    entityRef.create(null);
    ConcurrentClusteredMap<K, V> clusteredMap = entityRef.fetchEntity();
    clusteredMap.setTypes(keyClass, valueClass);
    return clusteredMap;
  }

  @Test
  public void testReplicationAndSync() throws Exception {
    clusterControl.waitForActive();
    clusterControl.waitForRunningPassivesInStandby();
    ConcurrentClusteredMap<Long, String> map = createClusteredMap("foo", Long.class, String.class);
    for (long i = 0; i < 10; i++) {
      map.put(i, "value"+i);
    }
    for (long i = 0; i < 10; i++) {
      assertThat(map.get(i), is("value"+i));
    }

    // assert that replication is working
    clusterControl.terminateActive();
    clusterControl.waitForActive();
    for (long i = 0; i < 10; i++) {
      assertThat(map.get(i), is("value"+i));
    }

    // assert that passive sync is working
    clusterControl.startOneServer();
    clusterControl.waitForRunningPassivesInStandby();
    clusterControl.terminateActive();
    clusterControl.waitForActive();
    for (long i = 0; i < 10; i++) {
      assertThat(map.get(i), is("value"+i));
    }
  }
}
