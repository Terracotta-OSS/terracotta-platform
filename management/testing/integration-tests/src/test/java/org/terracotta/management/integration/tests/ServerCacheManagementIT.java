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

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;
import org.terracotta.management.entity.nms.NmsConfig;
import org.terracotta.management.model.call.Parameter;
import org.terracotta.management.model.capabilities.descriptors.Settings;
import org.terracotta.management.model.cluster.ManagementRegistry;
import org.terracotta.management.model.cluster.ServerEntity;
import org.terracotta.management.model.context.Context;
import org.terracotta.statistics.Table;

import java.util.ArrayList;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * @author Mathieu Carbou
 */
public class ServerCacheManagementIT extends AbstractSingleTest {

  @Test
  public void can_access_remote_management_registry_on_server() throws Exception {
    ManagementRegistry registry = nmsService.readTopology()
        .activeServerEntityStream()
        .filter(serverEntity -> serverEntity.getName().equals("pet-clinic/clients"))
        .findAny()
        .flatMap(ServerEntity::getManagementRegistry)
        .get();

    assertThat(registry.getCapabilities().size(), equalTo(4));

    assertThat(registry.getCapability("ServerCacheSettings"), is(notNullValue()));
    assertThat(registry.getCapability("ServerCacheStatistics"), is(notNullValue()));
    assertThat(registry.getCapability("ServerCacheCalls"), is(notNullValue()));
    assertThat(registry.getCapability("ClientStateSettings"), is(notNullValue()));

    Settings serverCacheSettings = new ArrayList<>(registry.getCapability("ServerCacheSettings").get().getDescriptors(Settings.class)).get(1);
    serverCacheSettings.set("time", 0L);

    JsonNode actual = removeRandomValues(toJson(registry.getCapabilities()));
    JsonNode expected = readJson("server-descriptors.json");
    assertEquals(expected, actual);

    registry = nmsService.readTopology()
        .activeServerEntityStream()
        .filter(serverEntity -> serverEntity.getType().equals(NmsConfig.ENTITY_TYPE))
        .findAny()
        .flatMap(ServerEntity::getManagementRegistry)
        .get();

    assertThat(registry.getCapabilities().size(), equalTo(3));
    assertThat(registry.getCapability("OffHeapResourceSettings"), is(notNullValue()));
    assertThat(registry.getCapability("OffHeapResourceStatistics"), is(notNullValue()));
    assertThat(registry.getCapability("StatisticCollectorCapability"), is(notNullValue()));
  }

  @Test
  public void can_do_remote_management_calls_on_server() throws Exception {
    ServerEntity serverEntity = nmsService.readTopology()
        .activeServerEntityStream()
        .filter(e -> e.getName().equals("pet-clinic/pets"))
        .findAny()
        .get();

    Context context = serverEntity.getContext().with("cacheName", "pet-clinic/pets");

    // put
    nmsService.call(context, "ServerCacheCalls", "put", Void.TYPE, new Parameter("pet1"), new Parameter("Cat")).waitForReturn();

    // get
    assertThat(nmsService.call(context, "ServerCacheCalls", "get", String.class, new Parameter("pet1")).waitForReturn(), equalTo("Cat"));

    // size of server store
    assertThat(nmsService.call(context, "ServerCacheCalls", "size", int.class).waitForReturn(), is(1));

    // put on client heaps
    assertThat(get(0, "pets", "pet1"), equalTo("Cat")); // hit
    assertThat(get(1, "pets", "pet1"), equalTo("Cat")); // hit
    assertThat(size(0, "pets"), equalTo(1)); // size of client's heap
    assertThat(size(1, "pets"), equalTo(1)); // size of client's heap

    // clear
    nmsService.call(context, "ServerCacheCalls", "clear", Void.TYPE).waitForReturn();

    // verify invalidation propagated to clients and their heap is cleared
    assertThat(size(0, "pets"), equalTo(0)); // size of client's heap
    assertThat(size(1, "pets"), equalTo(0)); // size of client's heap

    // size again of server store
    assertThat(nmsService.call(context, "ServerCacheCalls", "size", int.class).waitForReturn(), is(0));
  }

  @Test
  public void can_receive_server_statistics() throws Exception {
    triggerServerStatComputation();

    put(0, "pets", "pet1", "Cubitus");
    get(1, "pets", "pet1"); // hit

    queryAllRemoteStatsUntil(stats -> stats
        .stream()
        .filter(o -> o.hasStatistic("Cluster:HitCount") && o.hasStatistic("Cluster:CacheEntryLength"))
        .map(o -> o.<Long>getLatestSampleValue("Cluster:HitCount"))
        .anyMatch(counter -> counter.get() == 1L));

    JsonNode expected = readJson("stats.json");
    queryAllRemoteStatsUntil(stats -> {
      JsonNode actual = removeRandomValues(toJson(stats));
      System.out.println(actual.toPrettyString());
      return actual.equals(expected);
    });

    get(1, "pets", "pet2"); // miss

    queryAllRemoteStatsUntil(stats -> {
      boolean test = true;

      test &= stats
          .stream()
          .filter(o -> o.hasStatistic("Cluster:MissCount"))
          .map(o -> o.<Long>getLatestSampleValue("Cluster:MissCount"))
          .anyMatch(counter -> counter.get() == 1L); // 1 miss

      test &= stats
          .stream()
          .filter(o -> o.hasStatistic("ServerCache:Size"))
          .map(o -> o.<Integer>getLatestSampleValue("ServerCache:Size"))
          .anyMatch(size -> size.get() == 1); // // size 1 on heap of entity

      return test;
    });

  }

  @Test
  public void can_receive_server_table_statistics() throws Exception {
    triggerServerStatComputation();

    put(0, "pets", "pet1", "Cubitus");
    put(1, "pets", "pet2", "Radius");

    queryAllRemoteStatsUntil(stats -> stats
        .stream()
        .filter(o -> o.hasStatistic("Cluster:CacheEntryLength"))
        .map(o -> o.<Table>getLatestSampleValue("Cluster:CacheEntryLength"))
        .anyMatch(table -> table.get().getRowCount() == 2 && table.get().getRowLabels().containsAll(Arrays.asList("pet1", "pet2"))));
  }

}
