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
package org.terracotta.management.integration.tests;

import org.junit.Before;
import org.junit.Test;
import org.terracotta.management.model.cluster.Client;
import org.terracotta.management.model.cluster.Cluster;
import org.terracotta.management.model.cluster.Server;

import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

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
    oldActive = cluster.serverStream().filter(Server::isActive).findAny().get();
    assertThat(oldActive.getState(), equalTo(Server.State.ACTIVE));

    do {
      cluster = nmsService.readTopology();
      oldPassive = cluster.serverStream().filter(server -> !server.isActive()).findAny().get();
    } while (!Thread.currentThread().isInterrupted() && oldPassive.getState() != Server.State.PASSIVE);
    assertThat(oldPassive.getState(), equalTo(Server.State.PASSIVE));

    // clear buffer
    nmsService.readMessages();

    // add some data from client 0
    put(0, "clients", "client1", "Mathieu");

    // start collecting statistics
    triggerServerStatComputation();

    // kill active - passive should take the active role
    voltron.getClusterControl().terminateActive();
    voltron.getClusterControl().waitForActive();
  }

  @Test
  public void all_registries_reexposed_after_failover() throws Exception {
    long totalFetch;
    do {
      totalFetch = nmsService.readTopology()
          .clientStream()
          .flatMap(Client::fetchedServerEntityStream)
          .count();
    } while (totalFetch < 7); // 1 for NmsEntity, 3 each for the cache clients (2 pets and 1 nms agent entity)

    Cluster cluster = nmsService.readTopology();

    assertEquals(read("topology-after-failover.json"), toJson(cluster.toMap()));
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
        .map(o -> o.<Long>getLatestSampleValue("Cluster:HitCount"))
        .anyMatch(sample -> sample.get() == 1L)); // 1 hit

    get(1, "pets", "pet2"); // miss

    Set<String> serverNames = new HashSet<>();

    queryAllRemoteStatsUntil(stats -> {
      boolean test = true;

      // collect server names
      stats.stream()
          .filter(o -> o.getContext().contains(Server.NAME_KEY))
          .map(o -> o.getContext().get(Server.NAME_KEY))
          .forEach(serverNames::add);

      test &= stats
          .stream()
          .filter(o -> o.hasStatistic("Cluster:MissCount"))
          .map(o -> o.<Long>getLatestSampleValue("Cluster:MissCount"))
          .anyMatch(sample -> sample.get() == 1L); // 1 miss

      test &= stats
          .stream()
          .filter(o -> o.hasStatistic("ServerCache:Size"))
          .map(o -> o.<Integer>getLatestSampleValue("ServerCache:Size"))
          .anyMatch(sample -> sample.get() == 1); // size 1 on heap of entity

      return test;
    });

    assertThat(serverNames.size(), equalTo(1));

    /*

    If you set logback-ext.xml in trace mode, now you shouldn't see these kind of logs on the server:

[ManagementScheduler-1] TRACE org.terracotta.management.service.monitoring.DefaultPassiveEntityMonitoringService - [2] pushStatistics(3)
[ManagementScheduler-1] TRACE org.terracotta.management.service.monitoring.DefaultDataListener - [2] pushBestEffortsData(2, testServer1, server-entity-statistics)
[ManagementScheduler-1] WARN org.terracotta.management.service.monitoring.DefaultDataListener - [2] pushBestEffortsData(2, testServer1, server-entity-statistics) IGNORED: sender is the current active server

    The management registry created by the passive Nms Entity was not closed after a promotion to active, and thus the statistic collector
    was still running in background, and sending data through the voltron callback mechanism. Hopefully there was a check that dropped the
    bad incoming data to avoid sending it to the client. But the statistic collector was still consuming CPU resources.

    */
  }

}
