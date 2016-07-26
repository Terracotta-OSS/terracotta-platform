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

package org.terracotta.galvan;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.terracotta.connection.Connection;
import org.terracotta.connection.entity.EntityRef;
import org.terracotta.entity.map.common.ConcurrentClusteredMap;
import org.terracotta.exception.EntityAlreadyExistsException;
import org.terracotta.exception.EntityNotFoundException;
import org.terracotta.exception.EntityNotProvidedException;
import org.terracotta.exception.EntityVersionMismatchException;
import org.terracotta.passthrough.IClusterControl;
import org.terracotta.testing.rules.BasicExternalCluster;

import java.io.File;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ClusteredMapActivePassiveIT {

  @ClassRule
  public static BasicExternalCluster CLUSTER = new BasicExternalCluster(new File("target/cluster"), 2);
  private static IClusterControl clusterControl;

  @BeforeClass
  public static void setUp() throws Exception {
    clusterControl = CLUSTER.getClusterControl();
  }

  private <K, V> ConcurrentClusteredMap<K, V> createClusteredMap(Connection connection, String name, Class<K> keyClass, Class<V> valueClass)
      throws EntityNotProvidedException, EntityAlreadyExistsException, EntityVersionMismatchException, EntityNotFoundException {
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
    Connection connection = CLUSTER.newConnection();
    ConcurrentClusteredMap<Long, String> map = createClusteredMap(connection, "foo", Long.class, String.class);
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
