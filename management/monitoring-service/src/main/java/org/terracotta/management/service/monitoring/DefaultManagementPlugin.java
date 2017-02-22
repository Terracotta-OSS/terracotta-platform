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
import org.terracotta.management.registry.collect.StatisticCollector;
import org.terracotta.management.service.monitoring.registry.provider.StatisticCollectorManagementProvider;

/**
 * @author Mathieu Carbou
 */
public class DefaultManagementPlugin implements ManagementPlugin {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultManagementPlugin.class);

  @Override
  public void registerServerManagementProviders(ManagementPluginContext pluginContext) {
    StatisticService statisticService = pluginContext.getStatisticService();
    EntityMonitoringService monitoringService = pluginContext.getMonitoringService();
    ConsumerManagementRegistry consumerManagementRegistry = pluginContext.getConsumerManagementRegistry();
    long consumerId = pluginContext.getConsumerId();

    LOGGER.trace("[0] registerServerManagementProviders({})", consumerId);

    // The context for the collector is created from the the registry of the entity wanting server-side providers.
    // We create a provider that will receive management calls to control the global voltron's statistic collector.
    // This provider will thus be on top of the entity wanting to collect server-side stats
    ContextContainer contextContainer = consumerManagementRegistry.getContextContainer();
    Context context = Context.create(contextContainer.getName(), contextContainer.getValue());
    StatisticCollectorManagementProvider collectorManagementProvider = new StatisticCollectorManagementProvider(context);
    consumerManagementRegistry.addManagementProvider(collectorManagementProvider);

    // add a collector service, not started by default, but that can be started through a remote management call
    StatisticCollector statisticCollector = statisticService.createStatisticCollector(
        statistics -> monitoringService.pushStatistics(statistics.toArray(new ContextualStatistics[statistics.size()])));
    consumerManagementRegistry.register(statisticCollector);
  }
}
