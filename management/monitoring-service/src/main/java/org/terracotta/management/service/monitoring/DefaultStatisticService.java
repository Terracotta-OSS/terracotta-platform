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
import org.terracotta.management.registry.CapabilityManagementSupport;
import org.terracotta.management.registry.collect.DefaultStatisticCollector;
import org.terracotta.management.registry.collect.StatisticCollector;

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

  private final CapabilityManagementSupport capabilityManagementSupport;

  DefaultStatisticService(CapabilityManagementSupport capabilityManagementSupport) {
    this.capabilityManagementSupport = Objects.requireNonNull(capabilityManagementSupport);
  }

  @Override
  public StatisticCollector createStatisticCollector(StatisticCollector.Collector collector) {
    LOGGER.trace("[0] createStatisticCollector()");
    return new DefaultStatisticCollector(
        capabilityManagementSupport,
        managementScheduler,
        collector
    );
  }

  @Override
  public void close() {
    LOGGER.trace("close()");
    ExecutorUtil.shutdownNow(managementScheduler);
  }

}
