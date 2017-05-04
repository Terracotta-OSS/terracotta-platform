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
package org.terracotta.management.integration.tests;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.runners.MethodSorters;
import org.terracotta.management.entity.sample.client.CacheEntityFactory;
import org.terracotta.management.model.cluster.Cluster;
import org.terracotta.management.model.notification.ContextualNotification;

import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * @author Mathieu Carbou
 */
@RunWith(JUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ReconfigureEntityIT extends AbstractSingleTest {

  @Test
  public void reconfigure_entity() throws Exception {
    // client 0 and 1 are both connected to the same server. For each client
    // local cache 'clients' is connected to server entity pet-clinic/clients, which reads/writes server map pet-clinic/clients
    // local cache 'pets' is connected to server entity pet-clinic/pets, which reads/writes server map pet-clinic/pets

    put(0, "clients", "client1", "Mathieu");
    put(0, "pets", "pet1", "Cubitus");

    assertThat(get(0, "clients", "client1"), equalTo("Mathieu"));
    assertThat(get(0, "pets", "pet1"), equalTo("Cubitus"));
    assertThat(get(1, "clients", "client1"), equalTo("Mathieu"));
    assertThat(get(1, "pets", "pet1"), equalTo("Cubitus"));

    assertThat(size(0, "clients"), equalTo(1));
    assertThat(size(0, "pets"), equalTo(1));

    // clear local caches
    caches.get("clients").get(0).clear();
    caches.get("pets").get(0).clear();
    assertThat(size(0, "clients"), equalTo(0));
    assertThat(size(0, "pets"), equalTo(0));

    // we will reconfigure the pet-clinic/pets entity so that it connects to pet-clinic/clients
    // so both entities points now to the same map
    CacheEntityFactory factory0 = new CacheEntityFactory(webappNodes.get(0).getConnection());
    factory0.reconfigure("pet-clinic/pets", "pet-clinic/clients");

    // now we should be able to get pets values from clients cache and vis versa
    assertThat(get(0, "pets", "client1"), equalTo("Mathieu"));
    assertThat(size(0, "pets"), equalTo(1));

    // and just for fun, ensure we can still pass values from client to client
    put(1, "clients", "client2", "Anthony");
    // but get the values from the pets cache ;-)
    assertThat(get(0, "pets", "client2"), equalTo("Anthony"));
  }

  @Test
  public void topology_after_reconfigure() throws Exception {
    Cluster cluster = nmsService.readTopology();
    String currentTopo = toJson(cluster.toMap()).toString();
    String actual = removeRandomValues(currentTopo);
    String expected = readJson("topology-before-reconfigure.json").toString();
    assertEquals(expected, actual);

    CacheEntityFactory factory0 = new CacheEntityFactory(webappNodes.get(0).getConnection());
    factory0.reconfigure("pet-clinic/pets", "pet-clinic/clients");

    cluster = nmsService.readTopology();
    currentTopo = toJson(cluster.toMap()).toString();
    actual = removeRandomValues(currentTopo);
    expected = readJson("topology-reconfigured.json").toString();
    assertEquals(expected, actual);
  }

  @Test
  public void notification_after_reconfigure() throws Exception {
    nmsService.readMessages();

    CacheEntityFactory factory0 = new CacheEntityFactory(webappNodes.get(0).getConnection());
    factory0.reconfigure("pet-clinic/pets", "pet-clinic/clients");

    String[] latestReceivedNotifs = {"SERVER_CACHE_DESTROYED", "SERVER_CACHE_CREATED", "CLIENT_ATTACHED", "CLIENT_ATTACHED", "SERVER_ENTITY_RECONFIGURED"};
    List<ContextualNotification> notifs = waitForAllNotifications(latestReceivedNotifs);
    notifs = notifs.subList(notifs.size() - latestReceivedNotifs.length, notifs.size());


    String currentJson = toJson(notifs).toString();
    String actual = removeRandomValues(currentJson);
    String expected = readJson("notifications-after-reconfigure.json").toString();

    assertEquals(expected, actual);
  }

}
