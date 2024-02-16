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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.model.stats.ContextualStatistics;
import org.terracotta.management.registry.CapabilityManagement;
import org.terracotta.management.registry.CapabilityManagementSupport;
import org.terracotta.management.registry.ExposedObject;
import org.terracotta.management.registry.ManagementProvider;
import org.terracotta.management.registry.ResultSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

/**
 * @author Mathieu Carbou
 */
public class DefaultStatisticCollector implements StatisticCollector {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultStatisticCollector.class);

  private final ScheduledExecutorService scheduledExecutorService;
  private final Runnable runnable;

  private volatile boolean running;
  private ScheduledFuture<?> task;
  private long intervalMs;
  private volatile long lastCollectTime;

  public DefaultStatisticCollector(final CapabilityManagementSupport managementRegistry,
                                   ScheduledExecutorService scheduledExecutorService,
                                   final Collector collector,
                                   LongSupplier systemTimeSupplier) {

    this.scheduledExecutorService = Objects.requireNonNull(scheduledExecutorService);

    Objects.requireNonNull(managementRegistry);
    Objects.requireNonNull(collector);

    this.runnable = () -> {
      try {
        if (running) {
          Collection<ContextualStatistics> statistics = new ArrayList<>();

          for (String capabilityName : managementRegistry.getCapabilityNames()) {

            Set<Context> allContexts = new LinkedHashSet<>();

            for (ManagementProvider<?> managementProvider : managementRegistry.getManagementProvidersByCapability(capabilityName)) {
              if (managementProvider.getClass().isAnnotationPresent(StatisticProvider.class)) {
                for (ExposedObject<?> exposedObject : managementProvider.getExposedObjects()) {
                  allContexts.add(exposedObject.getContext());
                }
              }
            }

            if (!allContexts.isEmpty()) {
              CapabilityManagement capabilityManagement = managementRegistry.withCapability(capabilityName);
              ResultSet<ContextualStatistics> resultSet = capabilityManagement
                  .queryAllStatistics()
                  .on(allContexts)
                  .since(lastCollectTime)
                  .build()
                  .execute();
              for (ContextualStatistics contextualStatistics : resultSet) {
                statistics.add(contextualStatistics);
              }
            }
          }

          if (running && !statistics.isEmpty()) {
            collector.onStatistics(statistics);

            // We set the time of last collect after the collector is called.
            // Thus, if any exception occurs (such as temporary network failure),
            // the next sending of stat will contains the samples of the last collect.
            lastCollectTime = systemTimeSupplier.getAsLong();
          }
        }
      } catch (RuntimeException e) {
        LOGGER.warn("StatisticCollector failed: " + e.getMessage(), e);
      }
    };
  }

  @Override
  public synchronized void startStatisticCollector(long interval, TimeUnit unit) {
    if (interval <= 0) {
      throw new IllegalArgumentException("Bad interval: " + interval);
    }

    final long itv = TimeUnit.MILLISECONDS.convert(interval, unit);

    // cancel the current task if it is scheduled with a different time
    if (running && intervalMs != itv) {
      stopStatisticCollector();
    }

    if (!running) {
      LOGGER.info("Starting collecting statistics each {} {}", interval, unit);
      intervalMs = itv;
      if (!scheduledExecutorService.isShutdown()) {
        running = true;
        try {
          task = scheduledExecutorService.scheduleWithFixedDelay(runnable, 0L, intervalMs, TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException e) {
          running = false;
          throw e;
        }
      }
    }
  }

  @Override
  public synchronized void stopStatisticCollector() {
    if (running) {
      running = false;
      LOGGER.info("Stopping collecting statistics");
      ScheduledFuture<?> task = this.task;
      if (task != null) {
        task.cancel(false);
        this.task = null;
      }
    }
  }

  @Override
  public boolean isRunning() {
    return running;
  }

}
