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

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * @author Mathieu Carbou
 */
class ManagementPlugins {

  private final PlatformConfiguration platformConfiguration;
  private final StatisticService statisticService;
  private final List<ManagementPlugin> plugins = new ArrayList<>();

  ManagementPlugins(PlatformConfiguration platformConfiguration, StatisticService statisticService) {
    this.platformConfiguration = platformConfiguration;
    this.statisticService = statisticService;
    ServiceLoader.load(ManagementPlugin.class).forEach(plugins::add);
  }

  void registerServerManagementProviders(long consumerId, ConsumerManagementRegistry consumerManagementRegistry, EntityMonitoringService monitoringService) {
    ManagementPluginContext context = new ManagementPluginContext(platformConfiguration, statisticService, consumerId, consumerManagementRegistry, monitoringService);
    for (ManagementPlugin plugin : plugins) {
      plugin.registerServerManagementProviders(context);
    }
  }

}
