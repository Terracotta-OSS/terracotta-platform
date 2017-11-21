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
package org.terracotta.management.service.monitoring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.model.context.ContextContainer;
import org.terracotta.management.model.stats.ContextualStatistics;
import org.terracotta.management.registry.CombiningCapabilityManagementSupport;
import org.terracotta.management.registry.collect.DefaultStatisticCollector;
import org.terracotta.management.registry.collect.StatisticCollector;
import org.terracotta.management.service.monitoring.registry.provider.StatisticCollectorManagementProvider;

import java.io.Closeable;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This service is the central point to create statistic registries and collectors running with the scheduler provided by this service
 *
 * @author Mathieu Carbou
 */
class DefaultStatisticService implements StatisticService, Closeable {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultStatisticService.class);

  private static final AtomicLong managementSchedulerCount = new AtomicLong();

  private final ScheduledExecutorService managementScheduler = Executors.unconfigurableScheduledExecutorService(new ScheduledThreadPoolExecutor(
      1,
      r -> {
        Thread t = Executors.defaultThreadFactory().newThread(r);
        t.setDaemon(true);
        t.setName("ManagementScheduler-" + managementSchedulerCount.incrementAndGet());
        t.setUncaughtExceptionHandler((thread, err) -> LOGGER.error("UncaughtException in thread " + thread.getName() + ": " + err.getMessage(), err));
        return t;
      },
      new ThreadPoolExecutor.AbortPolicy()
  ));

  private final SharedEntityManagementRegistry sharedEntityManagementRegistry;

  DefaultStatisticService(SharedEntityManagementRegistry sharedEntityManagementRegistry) {
    this.sharedEntityManagementRegistry = Objects.requireNonNull(sharedEntityManagementRegistry);
  }

  @Override
  public void addStatisticCollector(EntityManagementRegistry registry) {
    long consumerId = registry.getMonitoringService().getConsumerId();
    LOGGER.trace("[{}] addStatisticCollector()", consumerId);

    // The context for the collector is created from the the registry of the entity wanting server-side providers.
    // We create a provider that will receive management calls to control the global voltron's statistic collector.
    // This provider will thus be on top of the entity wanting to collect server-side stats
    ContextContainer contextContainer = registry.getContextContainer();
    Context context = Context.create(contextContainer.getName(), contextContainer.getValue());
    StatisticCollectorManagementProvider collectorManagementProvider = new StatisticCollectorManagementProvider(context);
    registry.addManagementProvider(collectorManagementProvider);

    EntityMonitoringService monitoringService = registry.getMonitoringService();

    StatisticCollector statisticCollector = new DefaultStatisticCollector(
        // Create a statistics collector which can collect stats over all management registries and only the registry combined.
        // This will avoid collecting stats on a registry from another NMS entity that already has its own stat collector.
        new CombiningCapabilityManagementSupport(sharedEntityManagementRegistry, registry),
        managementScheduler,
        list -> {
          // Add a marker on the statistics to know which statistics collector has collected them (from which NMS entity)
          list.forEach(stats -> stats.setContext(stats.getContext().with("collectorId", "" + consumerId)));
          monitoringService.pushStatistics(list.toArray(new ContextualStatistics[list.size()]));
        }
    );

    // add a collector service, not started by default, but that can be started through a remote management call
    registry.register(statisticCollector);

    registry.refresh();
  }

  @Override
  public void close() {
    LOGGER.trace("close()");
    ExecutorUtil.shutdownNow(managementScheduler);
  }

}
