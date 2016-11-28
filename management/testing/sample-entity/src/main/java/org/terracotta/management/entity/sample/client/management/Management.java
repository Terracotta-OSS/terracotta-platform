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
package org.terracotta.management.entity.sample.client.management;

import org.terracotta.connection.Connection;
import org.terracotta.management.entity.management.ManagementAgentConfig;
import org.terracotta.management.entity.management.client.ManagementAgentEntityFactory;
import org.terracotta.management.entity.management.client.ManagementAgentService;
import org.terracotta.management.entity.management.client.ManagementOperationException;
import org.terracotta.management.entity.sample.client.ClientCache;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.model.context.ContextContainer;
import org.terracotta.management.model.notification.ContextualNotification;
import org.terracotta.management.registry.DefaultManagementRegistry;
import org.terracotta.management.registry.ManagementRegistry;
import org.terracotta.management.registry.collect.DefaultStatisticCollector;
import org.terracotta.management.registry.collect.StatisticConfiguration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author Mathieu Carbou
 */
public class Management {

  private final ExecutorService executorService = Executors.newCachedThreadPool();
  private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
  private final DefaultStatisticCollector statisticCollector;
  private final Context parentContext;
  private final ManagementRegistry managementRegistry;

  private final StatisticConfiguration statisticConfiguration = new StatisticConfiguration()
      .setAverageWindowDuration(1, TimeUnit.MINUTES)
      .setHistorySize(100)
      .setHistoryInterval(1, TimeUnit.SECONDS)
      .setTimeToDisable(5, TimeUnit.SECONDS);

  private ManagementAgentService managementAgent;

  public Management(ContextContainer contextContainer) {
    this.parentContext = Context.create(contextContainer.getName(), contextContainer.getValue());

    // create a client-side management registry and add some providers for stats, calls and settings
    this.managementRegistry = new DefaultManagementRegistry(contextContainer);
    managementRegistry.addManagementProvider(new CacheSettingsManagementProvider(parentContext));
    managementRegistry.addManagementProvider(new CacheStatisticsManagementProvider(parentContext, scheduledExecutorService, statisticConfiguration));
    managementRegistry.addManagementProvider(new CacheCallManagementProvider(parentContext));
    managementRegistry.addManagementProvider(new CacheStatisticCollectorManagementProvider(parentContext));

    // create a statistic collector
    this.statisticCollector = new DefaultStatisticCollector(
        managementRegistry,
        scheduledExecutorService,
        statistics -> {
          try {
            managementAgent.pushStatistics(statistics);
          } catch (Exception e) {
            throw new RuntimeException(e); // do not do that in a real app, this is useful for testing purposes
          }
        },
        System::currentTimeMillis,
        statisticConfiguration);

    // register the collector in the registry so that we can manage it
    managementRegistry.register(statisticCollector);
  }

  public ManagementRegistry getManagementRegistry() {
    return managementRegistry;
  }

  public void init(Connection connection) throws ManagementOperationException, InterruptedException, TimeoutException {
    // activate stat collection
    statisticCollector.startStatisticCollector();

    // connect the management entity to this registry to bridge the voltorn monitoring service
    managementAgent = new ManagementAgentService(new ManagementAgentEntityFactory(connection)
        .retrieveOrCreate(new ManagementAgentConfig()));
    managementAgent.setManagementMessageExecutor(executorService);
    managementAgent.setOperationTimeout(5, TimeUnit.SECONDS);
    managementAgent.setManagementRegistry(managementRegistry);

    // initialize the agent and send the registry info inside voltron
    managementAgent.init();

    // set some tags and push a notif
    managementAgent.setTags("caches", managementRegistry.getContextContainer().getValue());
    managementAgent.pushNotification(new ContextualNotification(parentContext, "CLIENT_INIT"));
  }

  public void close() {
    try {
      managementAgent.pushNotification(new ContextualNotification(parentContext, "CLIENT_CLOSE"));
    } catch (Exception e) {
      throw new RuntimeException(e); // do not do that in a real app, this is useful for testing purposes
    }
    statisticCollector.stopStatisticCollector();
    executorService.shutdown();
    scheduledExecutorService.shutdown();
  }

  public void clientCacheCreated(ClientCache clientCache) {
    managementRegistry.register(clientCache);
    try {
      managementAgent.pushNotification(new ContextualNotification(parentContext.with("cacheName", clientCache.getName()), "CLIENT_CACHE_CREATED"));
    } catch (Exception e) {
      throw new RuntimeException(e); // do not do that in a real app, this is useful for testing purposes
    }
  }

}
