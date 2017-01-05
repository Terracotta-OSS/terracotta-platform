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

import org.junit.Test;
import org.terracotta.management.entity.sample.Cache;
import org.terracotta.management.entity.sample.client.CacheFactory;
import org.terracotta.management.model.cluster.Cluster;
import org.terracotta.management.model.cluster.Server;
import org.terracotta.management.model.notification.ContextualNotification;
import org.terracotta.management.registry.collect.StatisticConfiguration;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * @author Mathieu Carbou
 */
public class PassiveTopologyIT extends AbstractHATest {

  @Test
  public void topology_includes_passives() throws Exception {
    Cluster cluster = tmsAgentService.readTopology();
    Server passive = cluster.serverStream().filter(server -> !server.isActive()).findFirst().get();
    final String[] currentPassive = {toJson(passive.toMap()).toString()};
    cluster.clientStream().forEach(client -> currentPassive[0] = currentPassive[0]
        .replace(passive.getServerName(), "stripe-PASSIVE"));

    String actual = removeRandomValues(currentPassive[0]);

    // and compare
    assertEquals(readJson("passive.json").toString(), actual);
  }

  @Test
  public void notification_on_entity_creation_and_destruction() throws Exception {
    // clear buffer
    tmsAgentService.readMessages();

    StatisticConfiguration statisticConfiguration = new StatisticConfiguration()
        .setAverageWindowDuration(1, TimeUnit.MINUTES)
        .setHistorySize(100)
        .setHistoryInterval(1, TimeUnit.SECONDS)
        .setTimeToDisable(5, TimeUnit.SECONDS);
    CacheFactory cacheFactory = new CacheFactory(cluster.getConnectionURI().resolve("/random-1"), statisticConfiguration);
    cacheFactory.init();
    Cache cache = cacheFactory.getCache("my-cache");
    cache.put("key", "val");

    cacheFactory.destroyCache("my-cache");
    cacheFactory.close();

    List<ContextualNotification> notifs = tmsAgentService.readMessages()
        .stream()
        .filter(message -> message.getType().equals("NOTIFICATION"))
        .flatMap(message -> message.unwrap(ContextualNotification.class).stream())
        .collect(Collectors.toList());

    while (!Thread.currentThread().isInterrupted() && notifs.stream().filter(contextualNotification -> contextualNotification.getType().equals("SERVER_ENTITY_DESTROYED")).count() != 2) {
      notifs.addAll(tmsAgentService.readMessages()
          .stream()
          .filter(message -> message.getType().equals("NOTIFICATION"))
          .flatMap(message -> message.unwrap(ContextualNotification.class).stream())
          .collect(Collectors.toList()));
    }

    assertThat(notifs.stream().map(ContextualNotification::getType).collect(Collectors.toList()), hasItems(
        "CLIENT_CONNECTED",
        "SERVER_ENTITY_FETCHED",
        "CLIENT_REGISTRY_AVAILABLE",
        "CLIENT_TAGS_UPDATED",
        "CLIENT_INIT",
        "ENTITY_REGISTRY_AVAILABLE",
        "ENTITY_REGISTRY_UPDATED",
        "SERVER_CACHE_CREATED",
        "SERVER_ENTITY_CREATED",
        "SERVER_ENTITY_CREATED",
        "ENTITY_REGISTRY_AVAILABLE",
        "ENTITY_REGISTRY_UPDATED",
        "SERVER_ENTITY_FETCHED",
        "CLIENT_REGISTRY_UPDATED",
        "CLIENT_CACHE_CREATED",
        "SERVER_ENTITY_UNFETCHED",
        "SERVER_CACHE_DESTROYED",
        "SERVER_ENTITY_DESTROYED",
        "SERVER_ENTITY_DESTROYED",
        "CLIENT_CLOSE"
    ));
  }

}
