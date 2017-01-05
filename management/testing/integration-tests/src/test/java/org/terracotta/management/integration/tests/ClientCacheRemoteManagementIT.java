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
import org.terracotta.management.model.call.Parameter;
import org.terracotta.management.model.cluster.Client;
import org.terracotta.management.model.cluster.ManagementRegistry;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.model.stats.primitive.Counter;
import org.terracotta.management.model.stats.primitive.Size;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * @author Mathieu Carbou
 */
public class ClientCacheRemoteManagementIT extends AbstractSingleTest {

  @Test
  public void can_access_remote_management_registry_of_client() throws Exception {
    ManagementRegistry registry = tmsAgentService.readTopology()
        .clientStream()
        .filter(cli -> cli.getName().equals("pet-clinic"))
        .findFirst()
        .flatMap(Client::getManagementRegistry)
        .get();

    assertThat(registry.getCapabilities().size(), equalTo(5));

    assertThat(registry.getCapability("CacheSettings"), is(notNullValue()));
    assertThat(registry.getCapability("CacheStatistics"), is(notNullValue()));
    assertThat(registry.getCapability("CacheCalls"), is(notNullValue()));
    assertThat(registry.getCapability("StatisticCollectorCapability"), is(notNullValue()));
    assertThat(registry.getCapability("ManagementAgentService"), is(notNullValue()));

    assertEquals(readJson("client-descriptors.json"), toJson(registry.getCapabilities()));
  }

  @Test
  public void can_do_remote_management_calls_on_client() throws Exception {
    Client client = tmsAgentService.readTopology()
        .clientStream()
        .filter(e -> e.getName().equals("pet-clinic"))
        .findFirst()
        .get();

    // similar to cacheManagerName and cacheName context
    Context context = client.getContext()
        .with("appName", "pet-clinic")
        .with("cacheName", "pets");

    // put
    tmsAgentService.call(context, "CacheCalls", "put", Void.TYPE, new Parameter("pet1"), new Parameter("Cat")).waitForReturn();

    // get
    assertThat(tmsAgentService.call(context, "CacheCalls", "get", String.class, new Parameter("pet1")).waitForReturn(), equalTo("Cat"));

    // size
    assertThat(tmsAgentService.call(context, "CacheCalls", "size", int.class).waitForReturn(), is(1));

    // clear
    tmsAgentService.call(context, "CacheCalls", "clear", Void.TYPE).waitForReturn();

    // size again
    assertThat(tmsAgentService.call(context, "CacheCalls", "size", int.class).waitForReturn(), is(0));
  }

  @Test
  public void can_receive_client_statistics() throws Exception {
    triggerClientStatComputation();

    put(0, "pets", "pet1", "Cubitus");
    get(0, "pets", "pet1"); // hit on client 0
    get(1, "pets", "pet1"); // hit on client 1

    queryAllRemoteStatsUntil(stats -> stats
        .stream()
        .map(o -> o.getStatistic(Counter.class, "Cache:HitCount"))
        .anyMatch(counter -> counter.getValue() == 1L)); // 1 hit

    get(0, "pets", "pet2"); // miss on client 0
    get(1, "pets", "pet2"); // miss on client 0

    queryAllRemoteStatsUntil(stats -> {
      boolean test = true;

      test &= stats
          .stream()
          .map(o -> o.getStatistic(Counter.class, "Cache:MissCount"))
          .anyMatch(counter -> counter.getValue() == 1L); // 1 miss

      test &= stats
          .stream()
          .map(o -> o.getStatistic(Size.class, "ClientCache:Size"))
          .anyMatch(counter -> counter.getValue() == 1L); // size 1 on heap of entity

      return test;
    });

  }

  private void triggerClientStatComputation() throws Exception {
    // trigger stats computation and wait for all stats to have been computed at least once
    Client client = tmsAgentService.readTopology()
        .clientStream()
        .filter(e -> e.getName().equals("pet-clinic"))
        .findFirst()
        .get();

    // similar to cacheManagerName and cacheName context
    Context context = client.getContext()
        .with("appName", "pet-clinic");

    tmsAgentService.startStatisticCollector(
        context,
        5, TimeUnit.SECONDS
    ).waitForReturn();
  }

}
