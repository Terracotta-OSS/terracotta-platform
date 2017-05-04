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

import org.junit.Before;
import org.junit.Test;
import org.terracotta.management.model.cluster.Cluster;
import org.terracotta.management.model.cluster.Server;
import org.terracotta.management.model.notification.ContextualNotification;

import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * @author Mathieu Carbou
 */
public class FailoverIT extends AbstractHATest {

  Server oldActive;
  Server oldPassive;

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();

    Cluster cluster = nmsService.readTopology();
    oldActive = cluster.serverStream().filter(Server::isActive).findFirst().get();
    assertThat(oldActive.getState(), equalTo(Server.State.ACTIVE));

    do {
      cluster = nmsService.readTopology();
      oldPassive = cluster.serverStream().filter(server -> !server.isActive()).findFirst().get();
    } while (!Thread.currentThread().isInterrupted() && oldPassive.getState() != Server.State.PASSIVE);
    assertThat(oldPassive.getState(), equalTo(Server.State.PASSIVE));

    // clear buffer
    nmsService.readMessages();

    // add some data from client 0
    put(0, "clients", "client1", "Mathieu");

    // kill active - passive should take the active role
    voltron.getClusterControl().terminateActive();
    voltron.getClusterControl().waitForActive();
  }

  @Test
  public void all_registries_reexposed_after_failover() throws Exception {
    int clientReconnected = 0;
    do {
      clientReconnected += nmsService.readMessages()
          .stream()
          .filter(message -> message.getType().equals("NOTIFICATION"))
          .flatMap(message -> message.unwrap(ContextualNotification.class).stream())
          .filter(contextualNotification -> contextualNotification.getType().equals("CLIENT_RECONNECTED"))
          .count();
    } while (clientReconnected < 2);

    Cluster cluster = nmsService.readTopology();

    // removes all random values
    String currentTopo = toJson(cluster.toMap()).toString();
    String actual = removeRandomValues(currentTopo);

    assertEquals(readJson("topology-after-failover.json"), readJsonStr(actual));
  }

  @Test
  public void notifications_after_failover() throws Exception {
    // read messages
    List<ContextualNotification> collected = waitForAllNotifications("SERVER_JOINED", "SERVER_STATE_CHANGED",
        "SERVER_ENTITY_CREATED", "ENTITY_REGISTRY_AVAILABLE",
        "CLIENT_CONNECTED",
        "SERVER_ENTITY_FETCHED",
        "CLIENT_TAGS_UPDATED", "CLIENT_REGISTRY_AVAILABLE", "CLIENT_RECONNECTED",
        "CLIENT_ATTACHED");
    
    ContextualNotification stateChanged = collected.stream().filter(contextualNotification -> contextualNotification.getType().equals("SERVER_STATE_CHANGED")).findFirst().get();
    assertThat(stateChanged.getAttributes().get("state"), equalTo("ACTIVE"));
    assertThat(stateChanged.getContext().get(Server.NAME_KEY), equalTo(oldPassive.getServerName()));
  }

  @Test
  public void puts_can_be_seen_on_other_clients_after_failover() throws Exception {
    assertThat(get(1, "clients", "client1"), equalTo("Mathieu"));
  }

  @Test
  public void management_call_and_stats_after_failover() throws Exception {
    triggerServerStatComputation();

    put(0, "pets", "pet1", "Cubitus");
    get(1, "pets", "pet1"); // hit

    queryAllRemoteStatsUntil(stats -> stats
        .stream()
        .filter(o -> o.hasStatistic("Cluster:HitCount"))
        .map(o -> o.getStatistic("Cluster:HitCount"))
        .anyMatch(sample -> sample.longValue() == 1L)); // 1 hit

    get(1, "pets", "pet2"); // miss

    queryAllRemoteStatsUntil(stats -> {
      boolean test = true;

      test &= stats
          .stream()
          .filter(o -> o.hasStatistic("Cluster:MissCount"))
          .map(o -> o.getStatistic("Cluster:MissCount"))
          .anyMatch(sample -> sample.longValue() == 1L); // 1 miss

      test &= stats
          .stream()
          .filter(o -> o.hasStatistic("ServerCache:Size"))
          .map(o -> o.getStatistic("ServerCache:Size"))
          .anyMatch(sample -> sample.longValue() == 1L); // size 1 on heap of entity

      return test;
    });
  }

}
