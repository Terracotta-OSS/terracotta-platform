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
package org.terracotta.management.entity.sample.client.management;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.connection.Connection;
import org.terracotta.management.entity.nms.agent.client.DefaultNmsAgentService;
import org.terracotta.management.entity.nms.agent.client.NmsAgentEntityFactory;
import org.terracotta.management.entity.nms.agent.client.NmsAgentService;
import org.terracotta.management.entity.sample.client.ClientCache;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.model.context.ContextContainer;
import org.terracotta.management.model.notification.ContextualNotification;
import org.terracotta.management.registry.DefaultManagementRegistry;
import org.terracotta.management.registry.ManagementRegistry;
import org.terracotta.management.registry.collect.DefaultStatisticCollector;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author Mathieu Carbou
 */
public class Management {

  private static final Logger LOGGER = LoggerFactory.getLogger(Management.class);

  private final ExecutorService executorService = Executors.newCachedThreadPool();
  private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
  private final DefaultStatisticCollector statisticCollector;
  private final Context parentContext;
  private final ManagementRegistry managementRegistry;

  private NmsAgentService nmsAgentService;

  public Management(String instanceId, ContextContainer contextContainer) {
    this.parentContext = Context.create(contextContainer.getName(), contextContainer.getValue()).with("instanceId", instanceId);

    // create a client-side management registry and add some providers for stats, calls and settings
    this.managementRegistry = new DefaultManagementRegistry(contextContainer);
    managementRegistry.addManagementProvider(new CacheSettingsManagementProvider(parentContext));
    managementRegistry.addManagementProvider(new CacheStatisticsManagementProvider(parentContext, System::currentTimeMillis));
    managementRegistry.addManagementProvider(new CacheCallManagementProvider(parentContext));
    managementRegistry.addManagementProvider(new CacheStatisticCollectorManagementProvider(parentContext));

    // create a statistic collector
    this.statisticCollector = new DefaultStatisticCollector(
        managementRegistry,
        scheduledExecutorService,
        statistics -> nmsAgentService.pushStatistics(statistics),
        System::currentTimeMillis);

    // register the collector in the registry so that we can manage it
    managementRegistry.register(statisticCollector);
  }

  public ManagementRegistry getManagementRegistry() {
    return managementRegistry;
  }

  public Context getRootContext() {
    return parentContext;
  }

  public void init(Connection connection) {
    LOGGER.trace("[{}] init()", managementRegistry.getContextContainer().getValue());

    // connect the NMS Agent Entity to this registry to bridge the voltorn monitoring service
    nmsAgentService = createNmsAgentService(connection);

    // set some tags and push a notif
    nmsAgentService.setTags("caches", managementRegistry.getContextContainer().getValue());
    nmsAgentService.pushNotification(new ContextualNotification(parentContext, "CLIENT_INIT"));
  }

  public void close() {
    LOGGER.trace("[{}] close()", managementRegistry.getContextContainer().getValue());

    statisticCollector.stopStatisticCollector();

    try {
      nmsAgentService.pushNotification(new ContextualNotification(parentContext, "CLIENT_CLOSE"));
    } catch (Exception e) {
      throw new RuntimeException(e); // do not do that in a real app, this is useful for testing purposes
    }

    executorService.shutdown();

    nmsAgentService.close();
    scheduledExecutorService.shutdown();
  }

  public void clientCacheCreated(ClientCache clientCache) {
    managementRegistry.register(clientCache);
    try {
      nmsAgentService.pushNotification(new ContextualNotification(parentContext.with("cacheName", clientCache.getName()), "CLIENT_CACHE_CREATED"));
    } catch (Exception e) {
      throw new RuntimeException(e); // do not do that in a real app, this is useful for testing purposes
    }
  }

  private NmsAgentService createNmsAgentService(Connection connection) {
    DefaultNmsAgentService nmsAgentService = new DefaultNmsAgentService(parentContext, new NmsAgentEntityFactory(connection).retrieve());
    nmsAgentService.setManagementCallExecutor(executorService);
    nmsAgentService.setOperationTimeout(5, TimeUnit.SECONDS);
    nmsAgentService.setManagementRegistry(managementRegistry);
    return nmsAgentService;
  }

}
