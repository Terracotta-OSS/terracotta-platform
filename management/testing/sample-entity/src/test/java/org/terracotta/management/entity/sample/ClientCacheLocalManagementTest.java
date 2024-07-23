/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
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
package org.terracotta.management.entity.sample;

import org.junit.Test;
import org.terracotta.management.model.call.ContextualReturn;
import org.terracotta.management.model.call.Parameter;
import org.terracotta.management.model.capabilities.descriptors.Settings;
import org.terracotta.management.model.capabilities.descriptors.StatisticDescriptor;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.model.stats.ContextualStatistics;
import org.terracotta.management.registry.CapabilityManagementSupport;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Mathieu Carbou
 */
public class ClientCacheLocalManagementTest extends AbstractTest {

  @Test
  public void can_access_local_management_registry() throws Exception {
    CapabilityManagementSupport registry = webappNodes.get(0).getManagementRegistry();

    assertThat(registry.getCapabilities().size(), equalTo(6));

    assertThat(registry.getManagementProvidersByCapability("CacheSettings").size(), equalTo(1));
    assertThat(registry.getManagementProvidersByCapability("CacheStatistics").size(), equalTo(1));
    assertThat(registry.getManagementProvidersByCapability("CacheCalls").size(), equalTo(1));
    assertThat(registry.getManagementProvidersByCapability("DiagnosticCalls").size(), equalTo(1));
    assertThat(registry.getManagementProvidersByCapability("StatisticCollectorCapability").size(), equalTo(1));
    assertThat(registry.getManagementProvidersByCapability("NmsAgentService").size(), equalTo(1));

    assertEquals(read("client-descriptors.json"), toJson(registry.getCapabilities()));
  }

  @Test
  public void can_do_local_management_calls() throws Exception {
    Context rootContext = webappNodes.get(0).getRootContext();
    CapabilityManagementSupport registry = webappNodes.get(0).getManagementRegistry();

    // put
    ContextualReturn<?> put = registry.withCapability("CacheCalls")
        .call("put", new Parameter("pet1"), new Parameter("Cat"))
        .on(rootContext.with("cacheName", "pets"))
        .build()
        .execute()
        .getSingleResult();

    assertThat(put.hasExecuted(), is(true));
    assertThat(put.errorThrown(), is(false));

    // get
    ContextualReturn<String> get = registry.withCapability("CacheCalls")
        .call("get", String.class, new Parameter("pet1"))
        .on(rootContext.with("cacheName", "pets"))
        .build()
        .execute()
        .getSingleResult();

    assertThat(get.hasExecuted(), is(true));
    assertThat(get.errorThrown(), is(false));
    assertThat(get.getValue(), equalTo("Cat"));

    // size
    ContextualReturn<Integer> size = registry.withCapability("CacheCalls")
        .call("size", int.class)
        .on(rootContext.with("cacheName", "pets"))
        .build()
        .execute()
        .getSingleResult();

    assertThat(size.hasExecuted(), is(true));
    assertThat(size.errorThrown(), is(false));
    assertThat(size.getValue(), is(1));

    // clear
    ContextualReturn<?> result = registry.withCapability("CacheCalls")
        .call("clear")
        .on(rootContext.with("cacheName", "pets"))
        .build()
        .execute()
        .getSingleResult();

    assertThat(result.hasExecuted(), is(true));
    assertThat(result.errorThrown(), is(false));
    assertThat(size(0, "pets"), equalTo(0));
  }

  @Test
  public void can_query_local_stats() throws Exception {
    put(0, "pets", "pet1", "Cubitus");

    queryAllStatsUntil(1, "pets", stats -> stats
        .<Long>getLatestSampleValue("Cache:HitCount")
        .get() == 0L); // 0 hit

    queryAllStatsUntil(1, "pets", stats -> stats
        .<Long>getLatestSampleValue("Cache:HitCount")
        .get() == 0L); // 0 miss

    get(1, "pets", "pet1"); // hit

    queryAllStatsUntil(1, "pets", stats -> stats
        .<Long>getLatestSampleValue("Cache:HitCount")
        .get() == 1L); // 1 hit

    get(1, "pets", "pet2"); // miss

    queryAllStatsUntil(1, "pets", stats -> stats
        .<Long>getLatestSampleValue("Cache:MissCount")
        .get() == 1L); // 1 miss

    queryAllStatsUntil(1, "pets", stats -> stats
        .<Integer>getLatestSampleValue("ClientCache:Size")
        .get() == 1); // size 1 on heap of client 1

    queryAllStatsUntil(0, "pets", stats -> stats
        .<Integer>getLatestSampleValue("ClientCache:Size")
        .get() == 0); // size 0 on heap of client 0
  }

  private void queryAllStatsUntil(int node, String cacheName, Predicate<ContextualStatistics> test) {
    ContextualStatistics statistics;
    do {
      statistics = queryAllStats(node)
          .filter(o -> o.getContext().get("cacheName").equals(cacheName))
          .findAny()
          .get();
      Thread.yield();
    } while (!Thread.currentThread().isInterrupted() && !test.test(statistics));
    assertFalse(Thread.currentThread().isInterrupted());
    assertTrue(test.test(statistics));
  }

  private Stream<? extends ContextualStatistics> queryAllStats(int node) {
    Context rootContext = webappNodes.get(node).getRootContext();
    CapabilityManagementSupport registry = webappNodes.get(node).getManagementRegistry();
    // get all possible stat names
    List<String> statNames = registry.getCapabilities()
        .stream()
        .filter(capability -> capability.getName().equals("CacheStatistics"))
        .flatMap(capability -> capability.getDescriptors(StatisticDescriptor.class).stream())
        .map(StatisticDescriptor::getName)
        .collect(Collectors.toList());

    // get all cache contexts
    List<Context> cacheContexts = registry.getCapabilities()
        .stream()
        .filter(capability -> capability.getName().equals("CacheSettings"))
        .flatMap(capability -> capability.getDescriptors().stream())
        .map(descriptor -> ((Settings) descriptor).getString("cacheName"))
        .map(c -> rootContext.with("cacheName", c))
        .collect(Collectors.toList());

    // do a management call to activate all stats from all contexts
    return registry.withCapability("CacheStatistics")
        .queryStatistics(statNames)
        .on(cacheContexts)
        .build()
        .execute()
        .results()
        .values()
        .stream();
  }

}
