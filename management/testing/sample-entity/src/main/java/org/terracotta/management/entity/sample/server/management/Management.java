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
package org.terracotta.management.entity.sample.server.management;

import org.terracotta.entity.ServiceRegistry;
import org.terracotta.management.entity.sample.server.ServerCache;
import org.terracotta.management.registry.collect.StatisticConfiguration;
import org.terracotta.management.service.monitoring.ActiveEntityMonitoringService;
import org.terracotta.management.service.monitoring.ActiveEntityMonitoringServiceConfiguration;
import org.terracotta.management.service.monitoring.ConsumerManagementRegistry;
import org.terracotta.management.service.monitoring.ConsumerManagementRegistryConfiguration;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @author Mathieu Carbou
 */
public class Management {

  private final ConsumerManagementRegistry managementRegistry;

  public Management(ServiceRegistry serviceRegistry) {
    ActiveEntityMonitoringService activeEntityMonitoringService = Objects.requireNonNull(serviceRegistry.getService(new ActiveEntityMonitoringServiceConfiguration()));
    this.managementRegistry = Objects.requireNonNull(serviceRegistry.getService(new ConsumerManagementRegistryConfiguration(activeEntityMonitoringService)
        .setStatisticConfiguration(new StatisticConfiguration()
            .setAverageWindowDuration(1, TimeUnit.MINUTES)
            .setHistorySize(100)
            .setHistoryInterval(1, TimeUnit.SECONDS)
            .setTimeToDisable(5, TimeUnit.SECONDS))));
    this.managementRegistry.addManagementProvider(new ServerCacheSettingsManagementProvider());
    this.managementRegistry.addManagementProvider(new ServerCacheCallManagementProvider());
    this.managementRegistry.addManagementProvider(new ServerCacheStatisticsManagementProvider());
  }

  public void init() {
    managementRegistry.refresh(); // send to voltron the registry at entity init
  }

  public void serverCacheCreated(ServerCache cache) {
    managementRegistry.register(new ServerCacheBinding(cache));
    managementRegistry.refresh();

    managementRegistry.pushServerEntityNotification(new ServerCacheBinding(cache), "SERVER_CACHE_CREATED");
  }

  public void serverCacheDestroyed(ServerCache cache) {
    managementRegistry.pushServerEntityNotification(new ServerCacheBinding(cache), "SERVER_CACHE_DESTROYED");
  }
}
