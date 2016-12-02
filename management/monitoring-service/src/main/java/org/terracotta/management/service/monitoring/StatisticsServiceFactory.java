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
import org.terracotta.context.extended.StatisticsRegistry;
import org.terracotta.management.registry.CapabilityManagementSupport;
import org.terracotta.management.registry.collect.DefaultStatisticCollector;
import org.terracotta.management.registry.collect.StatisticCollector;
import org.terracotta.management.registry.collect.StatisticConfiguration;
import org.terracotta.management.sequence.TimeSource;

import java.io.Closeable;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.RunnableScheduledFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This service is the central point to create statistic registries and collectors running with the scheduler provided by this service
 *
 * @author Mathieu Carbou
 */
class StatisticsServiceFactory implements Closeable {

  private static final Logger LOGGER = LoggerFactory.getLogger(StatisticsServiceFactory.class);

  private static final AtomicLong managementSchedulerCount = new AtomicLong();

  //TODO: keep an eye on the thread count, also, do we want 2 scheduled executor service, one for stats sampling (1 sec rate) and another one for stats collector (22 sec delay) ? Or keep 1 for both use cases ?
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
  ) {
    @Override
    protected <V> RunnableScheduledFuture<V> decorateTask(Runnable runnable, RunnableScheduledFuture<V> task) {
      return super.decorateTask(runnable, task);
    }

    @Override
    protected <V> RunnableScheduledFuture<V> decorateTask(Callable<V> callable, RunnableScheduledFuture<V> task) {
      return super.decorateTask(callable, task);
    }
  });

  private final CapabilityManagementSupport capabilityManagementSupport;
  private final TimeSource timeSource;

  StatisticsServiceFactory(CapabilityManagementSupport capabilityManagementSupport, TimeSource timeSource) {
    this.capabilityManagementSupport = Objects.requireNonNull(capabilityManagementSupport);
    this.timeSource = Objects.requireNonNull(timeSource);
  }

  public StatisticsRegistry createStatisticsRegistry(StatisticConfiguration statisticConfiguration, Object contextObject) {
    LOGGER.trace("[0] createStatisticsRegistry({})", statisticConfiguration);
    return new StatisticsRegistry(
        contextObject,
        managementScheduler,
        statisticConfiguration.averageWindowDuration(),
        statisticConfiguration.averageWindowUnit(),
        statisticConfiguration.historySize(),
        statisticConfiguration.historyInterval(),
        statisticConfiguration.historyIntervalUnit(),
        statisticConfiguration.timeToDisable(),
        statisticConfiguration.timeToDisableUnit());
  }

  public StatisticCollector createStatisticCollector(StatisticConfiguration statisticConfiguration, StatisticCollector.Collector collector) {
    LOGGER.trace("[0] createStatisticCollector({})", statisticConfiguration);
    return new DefaultStatisticCollector(
        capabilityManagementSupport,
        managementScheduler,
        collector,
        timeSource::getTimestamp,
        statisticConfiguration
    );
  }

  @Override
  public void close() {
    managementScheduler.shutdown();
  }

  public StatisticsService createStatisticsService(StatisticConfiguration statisticConfiguration) {
    return new StatisticsService() {
      @Override
      public StatisticsRegistry createStatisticsRegistry(Object contextObject) {
        return StatisticsServiceFactory.this.createStatisticsRegistry(statisticConfiguration, contextObject);
      }

      @Override
      public StatisticCollector createStatisticCollector(StatisticCollector.Collector collector) {
        return StatisticsServiceFactory.this.createStatisticCollector(statisticConfiguration, collector);
      }

      @Override
      public StatisticConfiguration getStatisticConfiguration() {
        return statisticConfiguration;
      }
    };
  }
}
