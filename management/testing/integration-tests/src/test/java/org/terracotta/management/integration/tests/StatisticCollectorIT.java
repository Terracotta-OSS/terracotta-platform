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

import org.junit.Test;
import org.terracotta.management.model.cluster.Client;
import org.terracotta.management.model.cluster.Server;
import org.terracotta.management.model.stats.ContextualStatistics;
import org.terracotta.statistics.registry.Statistic;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tc.util.Assert.assertFalse;
import static java.lang.Thread.sleep;

/**
 * @author Mathieu Carbou
 */
public class StatisticCollectorIT extends AbstractSingleTest {

  @Test
  public void can_reschedule_client_collectors() throws Exception {
    triggerClientStatComputation(1, TimeUnit.SECONDS);
    assertFalse(readClientStatistics().isEmpty());
  }

  @Test
  public void can_reschedule_server_collectors() throws Exception {
    triggerServerStatComputation(1, TimeUnit.SECONDS);
    assertFalse(readServerStatistics().isEmpty());
  }

  @Test
  public void do_not_collect_already_collected_statistics() throws Exception {
    // In ServerCacheStatisticsManagementProvider: window is 100ms

    triggerServerStatComputation(2, TimeUnit.SECONDS);

    get(0, "pets", "pet100");
    sleep(150);
    get(0, "pets", "pet100");

    // wait for samples to come
    queryAllRemoteStatsUntil(stats -> stats
        .stream()
        .flatMap(cs -> cs.<Long>getStatistic("Cluster:GetLatency")
            .map(Statistic::getSamples)
            .map(List::stream)
            .orElse(Stream.empty()))
        .count() == 2L);

    // wait for next samples to come
    queryAllRemoteStatsUntil(stats -> stats
        .stream()
        .flatMap(cs -> cs.<Long>getStatistic("Cluster:GetLatency")
            .map(Statistic::getSamples)
            .map(List::stream)
            .orElse(Stream.empty()))
        .count() == 0L);

    get(0, "pets", "pet100");

    // wait for next samples to come, without the 2 first ones
    queryAllRemoteStatsUntil(stats -> stats
        .stream()
        .flatMap(cs -> cs.<Long>getStatistic("Cluster:GetLatency")
            .map(Statistic::getSamples)
            .map(List::stream)
            .orElse(Stream.empty()))
        .count() == 1L);
  }

  private List<? extends ContextualStatistics> readClientStatistics() throws InterruptedException {
    List<? extends ContextualStatistics> statistics;
    do {
      statistics = nmsService.readMessages()
          .stream()
          .filter(message -> message.getType().equals("STATISTICS"))
          .flatMap(message -> message.unwrap(ContextualStatistics.class).stream())
          .filter(contextualStatistics -> contextualStatistics.getContext().getOrDefault(Client.KEY, "").contains("pet-clinic"))
          .collect(Collectors.toList());
      Thread.sleep(200);
    } while (statistics.isEmpty() && !Thread.currentThread().isInterrupted());
    return statistics;
  }

  private List<? extends ContextualStatistics> readServerStatistics() throws InterruptedException {
    List<? extends ContextualStatistics> statistics;
    do {
      statistics = nmsService.readMessages()
          .stream()
          .filter(message -> message.getType().equals("STATISTICS"))
          .flatMap(message -> message.unwrap(ContextualStatistics.class).stream())
          .filter(contextualStatistics -> contextualStatistics.getContext().contains(Server.KEY))
          .collect(Collectors.toList());
      Thread.sleep(200);
    } while (statistics.isEmpty() && !Thread.currentThread().isInterrupted());
    return statistics;
  }

}