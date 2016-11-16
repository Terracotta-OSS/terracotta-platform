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
package org.terracotta.management.registry.collect;

import org.terracotta.management.model.Objects;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.model.stats.ContextualStatistics;
import org.terracotta.management.registry.CapabilityManagementSupport;
import org.terracotta.management.registry.ManagementProvider;
import org.terracotta.management.registry.ResultSet;
import org.terracotta.management.registry.action.ExposedObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Mathieu Carbou
 */
public class DefaultStatisticCollector implements StatisticCollector {

  private static final Logger LOGGER = Logger.getLogger(DefaultStatisticCollector.class.getName());

  private final ConcurrentMap<String, Collection<String>> selectedStatsPerCapability = new ConcurrentHashMap<String, Collection<String>>();
  private final CapabilityManagementSupport managementRegistry;
  private final ScheduledExecutorService scheduledExecutorService;
  private final Collector collector;
  private final TimeProvider timeProvider;
  private final StatisticConfiguration statisticConfiguration;
  private ScheduledFuture<?> task;

  public DefaultStatisticCollector(CapabilityManagementSupport capabilityManagementSupport,
                                   ScheduledExecutorService scheduledExecutorService,
                                   Collector collector,
                                   TimeProvider timeProvider,
                                   StatisticConfiguration statisticConfiguration) {
    this.managementRegistry = Objects.requireNonNull(capabilityManagementSupport);
    this.scheduledExecutorService = Objects.requireNonNull(scheduledExecutorService);
    this.collector = Objects.requireNonNull(collector);
    this.timeProvider = Objects.requireNonNull(timeProvider);
    this.statisticConfiguration = Objects.requireNonNull(statisticConfiguration);
  }

  @Override
  public synchronized void startStatisticCollector() {
    if (task == null) {

      // we poll at 75% of the time to disable (before the time to disable happens)
      long pollingIntervalMs = Math.round(0.75 * TimeUnit.MILLISECONDS.convert(statisticConfiguration.timeToDisable(), statisticConfiguration.timeToDisableUnit()));

      final AtomicLong lastPoll = new AtomicLong(timeProvider.getTimeMillis());

      task = scheduledExecutorService.scheduleWithFixedDelay(new Runnable() {
        @Override
        public void run() {
          try {
            if (task != null && !selectedStatsPerCapability.isEmpty()) {
              Collection<ContextualStatistics> statistics = new ArrayList<ContextualStatistics>();
              long since = lastPoll.get();

              for (Map.Entry<String, Collection<String>> entry : selectedStatsPerCapability.entrySet()) {

                Set<Context> allContexts = new LinkedHashSet<Context>();
                for (ManagementProvider<?> managementProvider : managementRegistry.getManagementProvidersByCapability(entry.getKey())) {
                  for (ExposedObject<?> exposedObject : managementProvider.getExposedObjects()) {
                    allContexts.add(exposedObject.getContext());
                  }
                }

                ResultSet<ContextualStatistics> resultSet = managementRegistry.withCapability(entry.getKey())
                    .queryStatistics(entry.getValue())
                    .since(since)
                    .on(allContexts)
                    .build()
                    .execute();

                for (ContextualStatistics contextualStatistics : resultSet) {
                  statistics.add(contextualStatistics);
                }
              }

              // next time, only poll history from this time
              lastPoll.set(timeProvider.getTimeMillis());

              if (task != null && !statistics.isEmpty()) {
                collector.onStatistics(statistics);
              }
            }
          } catch (RuntimeException e) {
            LOGGER.log(Level.WARNING, "StatisticCollector failed: " + e.getMessage(), e);
          }
        }
      }, 0, pollingIntervalMs, TimeUnit.MILLISECONDS);
    }
  }

  @Override
  public synchronized void stopStatisticCollector() {
    if (task != null) {
      ScheduledFuture<?> _task = task;
      task = null;
      _task.cancel(false);
    }
  }

  @Override
  public void updateCollectedStatistics(String capabilityName, Collection<String> statisticNames) {
    if (!statisticNames.isEmpty()) {
      selectedStatsPerCapability.put(capabilityName, statisticNames);
    } else {
      // we clear the stats set
      selectedStatsPerCapability.remove(capabilityName);
    }
  }

}
