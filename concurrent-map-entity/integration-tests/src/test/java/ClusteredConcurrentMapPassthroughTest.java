/*
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is
 * Terracotta, Inc., a Software AG company
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
    server = new PassthroughServer(true);
    server.registerClientEntityService(new TerracottaClusteredMapClientService());
    server.registerServerEntityService(new TerracottaClusteredMapService());
    server.start();
    PassthroughConnection connection = server.connectNewClient();
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

    PassthroughConnection connection = server.connectNewClient();
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
  public void testWithCustomType() throws Exception {
    PassthroughConnection connection = server.connectNewClient();
    EntityRef<ConcurrentClusteredMap, Object> entityRef = connection.getEntityRef(ConcurrentClusteredMap.class, ConcurrentClusteredMap.VERSION, "person-map");
    entityRef.create(null);
    ConcurrentClusteredMap<Long, Person> map = entityRef.fetchEntity();
    map.setTypes(Long.class, Person.class);

    map.put(33L, new Person("Iron Man", 33));
    map.close();

    connection = server.connectNewClient();
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
