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

import java.io.Closeable;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.RunnableScheduledFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Mathieu Carbou
 */
class DefaultStatisticsService implements StatisticsService, Closeable {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultStatisticsService.class);

  private final AtomicLong managementSchedulerCount = new AtomicLong();
  private final ScheduledExecutorService managementScheduler = Executors.unconfigurableScheduledExecutorService(new ScheduledThreadPoolExecutor(
      Runtime.getRuntime().availableProcessors(),
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

  DefaultStatisticsService(CapabilityManagementSupport capabilityManagementSupport) {
    this.capabilityManagementSupport = capabilityManagementSupport;
  }

  @Override
  public StatisticsRegistry createStatisticsRegistry(StatisticConfiguration statisticConfiguration, Object contextObject) {
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

  @Override
  public StatisticCollector createStatisticCollector(StatisticConfiguration statisticConfiguration, StatisticCollector.Collector collector) {
    return new DefaultStatisticCollector(
        capabilityManagementSupport,
        managementScheduler,
        collector,
        // TODO FIXME: there is no timesource service in voltron: https://github.com/Terracotta-OSS/terracotta-apis/issues/167
        System::currentTimeMillis,
        statisticConfiguration
    );
  }

  @Override
  public void close() {
    managementScheduler.shutdown();
  }

}
