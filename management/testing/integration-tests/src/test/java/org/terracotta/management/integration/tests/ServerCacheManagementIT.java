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
import org.terracotta.management.entity.tms.TmsAgentConfig;
import org.terracotta.management.model.call.Parameter;
import org.terracotta.management.model.capabilities.descriptors.Settings;
import org.terracotta.management.model.cluster.ManagementRegistry;
import org.terracotta.management.model.cluster.ServerEntity;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.model.stats.AbstractStatisticHistory;
import org.terracotta.management.model.stats.StatisticHistory;
import org.terracotta.management.model.stats.history.CounterHistory;
import org.terracotta.management.model.stats.history.RatioHistory;
import org.terracotta.management.model.stats.history.SizeHistory;

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
    ManagementRegistry registry = tmsAgentService.readTopology()
        .activeServerEntityStream()
        .filter(serverEntity -> serverEntity.getName().equals("pet-clinic/clients"))
        .findFirst()
        .flatMap(ServerEntity::getManagementRegistry)
        .get();

    assertThat(registry.getCapabilities().size(), equalTo(3));

    assertThat(registry.getCapability("ServerCacheSettings"), is(notNullValue()));
    assertThat(registry.getCapability("ServerCacheStatistics"), is(notNullValue()));
    assertThat(registry.getCapability("ServerCacheCalls"), is(notNullValue()));

    Settings serverCacheSettings = new ArrayList<>(registry.getCapability("ServerCacheSettings").get().getDescriptors(Settings.class)).get(1);
    serverCacheSettings.set("time", 0L);

    assertEquals(readJson("server-descriptors.json"), toJson(registry.getCapabilities()));

    registry = tmsAgentService.readTopology()
        .activeServerEntityStream()
        .filter(serverEntity -> serverEntity.getType().equals(TmsAgentConfig.ENTITY_TYPE))
        .findFirst()
        .flatMap(ServerEntity::getManagementRegistry)
        .get();

    assertThat(registry.getCapabilities().size(), equalTo(3));
    assertThat(registry.getCapability("OffHeapResourceSettings"), is(notNullValue()));
    assertThat(registry.getCapability("OffHeapResourceStatistics"), is(notNullValue()));
    assertThat(registry.getCapability("StatisticCollectorCapability"), is(notNullValue()));
  }

  @Test
  public void can_do_remote_management_calls_on_server() throws Exception {
    ServerEntity serverEntity = tmsAgentService.readTopology()
        .activeServerEntityStream()
        .filter(e -> e.getName().equals("pet-clinic/pets"))
        .findFirst()
        .get();

    Context context = serverEntity.getContext().with("cacheName", "pet-clinic/pets");

    // put
    tmsAgentService.call(context, "ServerCacheCalls", "put", Void.TYPE, new Parameter("pet1"), new Parameter("Cat")).waitForReturn();

    // get
    assertThat(tmsAgentService.call(context, "ServerCacheCalls", "get", String.class, new Parameter("pet1")).waitForReturn(), equalTo("Cat"));

    // size of server store
    assertThat(tmsAgentService.call(context, "ServerCacheCalls", "size", int.class).waitForReturn(), is(1));

    // put on client heaps
    assertThat(get(0, "pets", "pet1"), equalTo("Cat")); // hit
    assertThat(get(1, "pets", "pet1"), equalTo("Cat")); // hit
    assertThat(size(0, "pets"), equalTo(1)); // size of client's heap
    assertThat(size(1, "pets"), equalTo(1)); // size of client's heap

    // clear
    tmsAgentService.call(context, "ServerCacheCalls", "clear", Void.TYPE).waitForReturn();

    // verify invalidation propagated to clients and their heap is cleared
    assertThat(size(0, "pets"), equalTo(0)); // size of client's heap
    assertThat(size(1, "pets"), equalTo(0)); // size of client's heap

    // size again of server store
    assertThat(tmsAgentService.call(context, "ServerCacheCalls", "size", int.class).waitForReturn(), is(0));
  }

  @Test
  public void can_receive_server_statistics() throws Exception {
    System.out.println("Please be patient... Test can take about 15s...");
    triggerServerStatComputation("Cluster:HitCount", "Cluster:MissCount", "Cluster:HitRatio", "ServerCache:Size");

    put(0, "pets", "pet1", "Cubitus");
    get(1, "pets", "pet1"); // hit

    queryAllRemoteStatsUntil(stats -> stats
        .stream()
        .map(o -> o.getStatistic(CounterHistory.class, "Cluster:HitCount"))
        .map(AbstractStatisticHistory::getLast)
        .filter(sample -> sample.getValue() == 1L) // 1 hit
        .findFirst()
        .isPresent());

    get(1, "pets", "pet2"); // miss

    queryAllRemoteStatsUntil(stats -> {
      boolean test = true;

      test &= stats
          .stream()
          .map(o -> o.getStatistic(CounterHistory.class, "Cluster:MissCount"))
          .map(AbstractStatisticHistory::getLast)
          .filter(sample -> sample.getValue() == 1L) // 1 miss
          .findFirst()
          .isPresent();

      test &= stats
          .stream()
          .map(o -> o.getStatistic(RatioHistory.class, "Cluster:HitRatio"))
          .map(AbstractStatisticHistory::getLast)
          .filter(sample -> sample.getValue() == 0.5d) // 1 hit for 2 gets
          .findFirst()
          .isPresent();

      test &= stats
          .stream()
          .map(o -> o.getStatistic(SizeHistory.class, "ServerCache:Size"))
          .map(AbstractStatisticHistory::getLast)
          .filter(sample -> sample.getValue() == 1L) // size 1 on heap of entity
          .findFirst()
          .isPresent();

      return test;
    });

  }

}
