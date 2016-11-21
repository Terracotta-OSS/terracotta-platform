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

import org.terracotta.management.model.context.ContextContainer;
import org.terracotta.management.registry.CapabilityManagement;
import org.terracotta.management.registry.DefaultCapabilityManagement;
import org.terracotta.management.registry.ManagementProvider;
import org.terracotta.management.registry.ManagementRegistry;

import java.util.Collection;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * @author Mathieu Carbou
 */
class DefaultSharedManagementRegistry implements SharedManagementRegistry {

  private final WeakHashMap<ConsumerManagementRegistry, Long> registries = new WeakHashMap<>();
  private final ReadWriteLock lock = new ReentrantReadWriteLock();

  @Override
  public Collection<ContextContainer> getContextContainers() {
    lock.readLock().lock();
    try {
      return registries.keySet()
          .stream()
          .filter(registry -> registry != null)
          .map(ManagementRegistry::getContextContainer)
          .collect(Collectors.toList());
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public Collection<ManagementProvider<?>> getManagementProvidersByCapability(String capabilityName) {
    lock.readLock().lock();
    try {
      return registries.keySet()
          .stream()
          .filter(registry -> registry != null)
          .flatMap(registry -> registry.getManagementProvidersByCapability(capabilityName).stream())
          .collect(Collectors.toList());
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public CapabilityManagement withCapability(String capabilityName) {
    return new DefaultCapabilityManagement(this, capabilityName);
  }

  ConsumerManagementRegistry getOrCreateConsumerManagementRegistry(long consumerID, MonitoringService monitoringService, StatisticsService statisticsService) {
    lock.writeLock().lock();
    try {
      for (Map.Entry<ConsumerManagementRegistry, Long> entry : registries.entrySet()) {
        ConsumerManagementRegistry registry = entry.getKey();
        if (consumerID == entry.getValue() && registry != null) {
          return registry;
        }
      }
      ConsumerManagementRegistry registry = new DefaultConsumerManagementRegistry(consumerID, monitoringService, statisticsService);
      registries.put(registry, consumerID);
      return registry;
    } finally {
      lock.writeLock().unlock();
    }
  }

}
