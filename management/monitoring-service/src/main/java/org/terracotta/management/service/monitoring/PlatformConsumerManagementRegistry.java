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

import org.terracotta.entity.PlatformConfiguration;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.registry.collect.StatisticConfiguration;
import org.terracotta.management.service.monitoring.registry.OffHeapResourceBinding;
import org.terracotta.management.service.monitoring.registry.OffHeapResourceSettingsManagementProvider;
import org.terracotta.management.service.monitoring.registry.OffHeapResourceStatisticsManagementProvider;
import org.terracotta.management.service.monitoring.registry.provider.StatisticCollectorManagementProvider;
import org.terracotta.offheapresource.OffHeapResourceIdentifier;
import org.terracotta.offheapresource.OffHeapResources;

import java.util.Collection;

/**
 * @author Mathieu Carbou
 */
class PlatformConsumerManagementRegistry extends DefaultConsumerManagementRegistry implements PlatformManagementRegistry {

  private final PlatformConfiguration platformConfiguration;
  private final StatisticConfiguration statisticConfiguration;

  PlatformConsumerManagementRegistry(long consumerId, MonitoringService monitoringService, StatisticsService statisticsService, PlatformConfiguration platformConfiguration, StatisticConfiguration statisticConfiguration) {
    super(consumerId, monitoringService, statisticsService);
    this.platformConfiguration = platformConfiguration;
    this.statisticConfiguration = statisticConfiguration;
  }

  @Override
  public void init() {
    // manage offheap service if it is there
    Collection<OffHeapResources> offHeapResources = platformConfiguration.getExtendedConfiguration(OffHeapResources.class);
    if (!offHeapResources.isEmpty()) {
      addManagementProvider(new OffHeapResourceSettingsManagementProvider());
      addManagementProvider(new OffHeapResourceStatisticsManagementProvider(statisticConfiguration));
      for (OffHeapResources offHeapResource : offHeapResources) {
        for (OffHeapResourceIdentifier identifier : offHeapResource.getAllIdentifiers()) {
          register(new OffHeapResourceBinding(identifier.getName(), offHeapResource.getOffHeapResource(identifier)));
        }
      }
    }

    // the context for the collector, created from the the registry of the tms entity
    Context context = Context.create(getContextContainer().getName(), getContextContainer().getValue());

    // we create a provider that will receive management calls to control the global voltron's statistic collector
    // this provider will thus be on top of the tms entity
    StatisticCollectorManagementProvider collectorManagementProvider = new StatisticCollectorManagementProvider(context, statisticConfiguration);
    addManagementProvider(collectorManagementProvider);

    // start the stat collector (it won't collect any stats though, because they need to be configured through a management call)
    collectorManagementProvider.init();
  }
}
