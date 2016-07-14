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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.terracotta.connection.entity.EntityRef;
import org.terracotta.entity.map.TerracottaClusteredMapClientService;
import org.terracotta.entity.map.common.ConcurrentClusteredMap;
import org.terracotta.entity.map.server.TerracottaClusteredMapService;
import org.terracotta.passthrough.PassthroughConnection;
import org.terracotta.passthrough.PassthroughServer;

import java.io.Serializable;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * ClusteredConcurrentMapPassthroughTest
 */
public class ClusteredConcurrentMapPassthroughTest {

  private static final String MAP_NAME = "my-map";

  private PassthroughServer server;
  private ConcurrentClusteredMap<Long, String> clusteredMap;

  @Before
  public void setUp() throws Exception {
    server = new PassthroughServer();
    server.registerClientEntityService(new TerracottaClusteredMapClientService());
    server.registerServerEntityService(new TerracottaClusteredMapService());
    boolean isActive = true;
    boolean shouldLoadStorage = false;
    server.start(isActive, shouldLoadStorage);
    PassthroughConnection connection = server.connectNewClient("connectionName");
    EntityRef<ConcurrentClusteredMap, Object> entityRef = connection.getEntityRef(ConcurrentClusteredMap.class, ConcurrentClusteredMap.VERSION, MAP_NAME);
    entityRef.create(null);
    clusteredMap = entityRef.fetchEntity();
    clusteredMap.setTypes(Long.class, String.class);
  }

  @After
  public void tearDown() {
    server.stop();
  }

  @Test
  public void testBasicMapInteraction() throws Exception {
    long key = 42L;
    String value = "The answer!";
    clusteredMap.put(key, value);
    assertThat(clusteredMap.get(key), is(value));
    assertThat(clusteredMap.remove(key), is(value));
  }

  @Test
  public void testMultiClientBasicInteraction() throws Exception {
    long key = 1L;
    String value = "see that?";
    clusteredMap.put(key, value);

    PassthroughConnection connection = server.connectNewClient("connectionName");
    EntityRef<ConcurrentClusteredMap, Object> entityRef = connection.getEntityRef(ConcurrentClusteredMap.class, ConcurrentClusteredMap.VERSION, MAP_NAME);
    ConcurrentClusteredMap<Long, String> mapFromOtherClient = entityRef.fetchEntity();
    mapFromOtherClient.setTypes(Long.class, String.class);

    assertThat(mapFromOtherClient.get(key), is(value));
  }

  @Test
  public void testCASOperations() throws Exception {
    long key = 244L;
    String value1 = "Tadam!";
    String value2 = "Youhou";
    String value3 = "Boom";

    assertThat(clusteredMap.putIfAbsent(key, value1), nullValue());

    assertThat(clusteredMap.putIfAbsent(key, value2), is(value1));

    assertThat(clusteredMap.replace(key, value2), is(value1));

    assertThat(clusteredMap.replace(key, value2, value3), is(true));

    assertThat(clusteredMap.remove(key, value3), is(true));
  }

  @Test
  public void testBulkOps() throws Exception {
    clusteredMap.put(1L, "One");
    clusteredMap.put(2L, "Two");
    clusteredMap.put(3L, "Three");

    assertThat(clusteredMap.keySet(), containsInAnyOrder(1L, 2L, 3L));
    assertThat(clusteredMap.values(), containsInAnyOrder("One", "Two", "Three"));
    assertThat(clusteredMap.entrySet().size(), is(3));
  }

  @Test
  public void testWithCustomType() throws Exception {
    PassthroughConnection connection = server.connectNewClient("connectionName");
    EntityRef<ConcurrentClusteredMap, Object> entityRef = connection.getEntityRef(ConcurrentClusteredMap.class, ConcurrentClusteredMap.VERSION, "person-map");
    entityRef.create(null);
    ConcurrentClusteredMap<Long, Person> map = entityRef.fetchEntity();
    map.setTypes(Long.class, Person.class);

    map.put(33L, new Person("Iron Man", 33));
    map.close();

    connection = server.connectNewClient("connectionName");
    entityRef = connection.getEntityRef(ConcurrentClusteredMap.class, ConcurrentClusteredMap.VERSION, "person-map");
    map = entityRef.fetchEntity();
    map.setTypes(Long.class, Person.class);

    assertThat(map.get(33L).name, is("Iron Man"));
    map.close();
  }

  public static class Person implements Serializable  {
    final String name;
    final int age;

    public Person(String name, int age) {
      this.name = name;
      this.age = age;
    }
  }
}
