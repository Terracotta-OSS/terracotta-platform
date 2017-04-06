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

import static org.junit.Assert.assertEquals;

/**
 * @author Mathieu Carbou
 */
public class PassiveTopologyIT extends AbstractHATest {

  @Test
  public void topology_includes_passives() throws Exception {
    Cluster cluster = nmsService.readTopology();
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
    nmsService.readMessages();

    CacheFactory cacheFactory = new CacheFactory(cluster.getConnectionURI().resolve("/random-1"));
    cacheFactory.init();
    Cache cache = cacheFactory.getCache("my-cache");
    cache.put("key", "val");

    cacheFactory.destroyCache("my-cache");
    cacheFactory.close();

    waitForAllNotifications("CLIENT_CONNECTED",
        "SERVER_ENTITY_FETCHED",
        "CLIENT_REGISTRY_AVAILABLE",
        "CLIENT_TAGS_UPDATED",
        "CLIENT_INIT",
        "ENTITY_REGISTRY_AVAILABLE",
        "SERVER_CACHE_CREATED",
        "SERVER_ENTITY_CREATED",
        "SERVER_ENTITY_CREATED",
        "ENTITY_REGISTRY_AVAILABLE",
        "SERVER_ENTITY_FETCHED",
        "CLIENT_CACHE_CREATED",
        "SERVER_ENTITY_UNFETCHED",
        "SERVER_CACHE_DESTROYED",
        "SERVER_ENTITY_DESTROYED",
        "SERVER_ENTITY_DESTROYED",
        "CLIENT_CLOSE",
        "CLIENT_ATTACHED",
        "CLIENT_DETACHED");
  }

}
