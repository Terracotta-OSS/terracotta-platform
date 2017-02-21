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
package org.terracotta.offheapresource.management;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.entity.PlatformConfiguration;
import org.terracotta.management.service.monitoring.ConsumerManagementRegistry;
import org.terracotta.management.service.monitoring.ManagementPlugin;
import org.terracotta.management.service.monitoring.ManagementPluginContext;
import org.terracotta.offheapresource.OffHeapResourceIdentifier;
import org.terracotta.offheapresource.OffHeapResources;

import java.util.Collection;

/**
 * @author Mathieu Carbou
 */
public class OffHeapResourceManagementPlugin implements ManagementPlugin {

  private static final Logger LOGGER = LoggerFactory.getLogger(OffHeapResourceManagementPlugin.class);

  @Override
  public void registerServerManagementProviders(ManagementPluginContext pluginContext) {
    PlatformConfiguration platformConfiguration = pluginContext.getPlatformConfiguration();
    ConsumerManagementRegistry consumerManagementRegistry = pluginContext.getConsumerManagementRegistry();
    long consumerId = pluginContext.getConsumerId();

    // manage offheap service if it is there
    Collection<OffHeapResources> offHeapResources = platformConfiguration.getExtendedConfiguration(OffHeapResources.class);
    if (!offHeapResources.isEmpty()) {
      consumerManagementRegistry.addManagementProvider(new OffHeapResourceSettingsManagementProvider());
      consumerManagementRegistry.addManagementProvider(new OffHeapResourceStatisticsManagementProvider());
      for (OffHeapResources offHeapResource : offHeapResources) {
        for (OffHeapResourceIdentifier identifier : offHeapResource.getAllIdentifiers()) {
          LOGGER.trace("[0] registerServerManagementProviders({}, OffHeapResource:{})", consumerId, identifier.getName());
          consumerManagementRegistry.register(new OffHeapResourceBinding(identifier.getName(), offHeapResource.getOffHeapResource(identifier)));
        }
      }
    }
  }
}
