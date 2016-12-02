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
import org.terracotta.management.model.capabilities.Capability;
import org.terracotta.management.model.context.ContextContainer;
import org.terracotta.management.registry.CapabilityManagement;
import org.terracotta.management.registry.DefaultCapabilityManagement;
import org.terracotta.management.registry.ManagementProvider;
import org.terracotta.management.registry.ManagementRegistry;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Mathieu Carbou
 */
class DefaultSharedManagementRegistry implements SharedManagementRegistry {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultSharedManagementRegistry.class);
  private static final Comparator<Capability> CAPABILITY_COMPARATOR = (o1, o2) -> o1.getName().compareTo(o2.getName());

  private final Map<ConsumerManagementRegistry, Long> registries = new ConcurrentWeakIdentityHashMap<>();

  @Override
  public Collection<ContextContainer> getContextContainers() {
    return registries.keySet()
        .stream()
        .map(ManagementRegistry::getContextContainer)
        .collect(Collectors.toList());
  }

  @Override
  public Collection<ManagementProvider<?>> getManagementProvidersByCapability(String capabilityName) {
    return registries.keySet()
        .stream()
        .flatMap(registry -> registry.getManagementProvidersByCapability(capabilityName).stream())
        .collect(Collectors.toList());
  }

  @Override
  public CapabilityManagement withCapability(String capabilityName) {
    return new DefaultCapabilityManagement(this, capabilityName);
  }

  @Override
  public Collection<? extends Capability> getCapabilities() {
    return registries.keySet()
        .stream()
        .flatMap(r -> r.getCapabilities().stream())
        .sorted(CAPABILITY_COMPARATOR)
        .collect(Collectors.toList());
  }

  synchronized ConsumerManagementRegistry getOrCreateConsumerManagementRegistry(long consumerID, EntityMonitoringService monitoringService, StatisticsService statisticsService) {
    LOGGER.trace("[{}] getOrCreateConsumerManagementRegistry()", consumerID);
    for (Map.Entry<ConsumerManagementRegistry, Long> entry : registries.entrySet()) {
      ConsumerManagementRegistry registry = entry.getKey();
      if (consumerID == entry.getValue()) {
        return registry;
      }
    }
    ConsumerManagementRegistry registry = new DefaultConsumerManagementRegistry(consumerID, monitoringService, statisticsService);
    registries.put(registry, consumerID);
    return registry;
  }

}
