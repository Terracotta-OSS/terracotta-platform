/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
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

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Only contains registries of standard entities, not management ones
 *
 * @author Mathieu Carbou
 */
class DefaultSharedEntityManagementRegistry implements SharedEntityManagementRegistry {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultSharedEntityManagementRegistry.class);
  private static final Comparator<Capability> CAPABILITY_COMPARATOR = Comparator.comparing(Capability::getName);

  private final List<EntityManagementRegistry> serverRegistries = new CopyOnWriteArrayList<>();
  private final List<EntityManagementRegistry> entityRegistries = new CopyOnWriteArrayList<>();

  @Override
  public Collection<ContextContainer> getContextContainers() {
    return entityRegistries.stream()
        .map(EntityManagementRegistry::getContextContainer)
        .collect(Collectors.toList());
  }

  @Override
  public Collection<ManagementProvider<?>> getManagementProvidersByCapability(String capabilityName) {
    return entityRegistries.stream()
        .flatMap(registry -> registry.getManagementProvidersByCapability(capabilityName).stream())
        .collect(Collectors.toList());
  }

  @Override
  public CapabilityManagement withCapability(String capabilityName) {
    return new DefaultCapabilityManagement(this, capabilityName);
  }

  @Override
  public Collection<? extends Capability> getCapabilities() {
    return entityRegistries.stream()
        .flatMap(r -> r.getCapabilities().stream())
        .sorted(CAPABILITY_COMPARATOR)
        .collect(Collectors.toList());
  }

  @Override
  public Collection<String> getCapabilityNames() {
    return entityRegistries.stream()
        .flatMap(r -> r.getCapabilityNames().stream())
        .collect(Collectors.toCollection(TreeSet::new));
  }

  /**
   * Returns the existing registry having the SAME consumerId that is not the added one
   */
  Optional<EntityManagementRegistry> addEntityManagementRegistry(EntityManagementRegistry entityManagementRegistry) {
    long consumerId = entityManagementRegistry.getMonitoringService().getConsumerId();
    boolean active = entityManagementRegistry.getMonitoringService().isActiveEntityService();
    LOGGER.trace("[{}] addEntityManagementRegistry() active={}", consumerId, active);
    entityRegistries.add(entityManagementRegistry);
    return entityRegistries.stream()
        .filter(existing -> existing != entityManagementRegistry && existing.getMonitoringService().getConsumerId() == consumerId)
        .findAny();
  }

  void removeEntityManagementRegistry(EntityManagementRegistry managementRegistry) {
    long consumerId = managementRegistry.getMonitoringService().getConsumerId();
    boolean active = managementRegistry.getMonitoringService().isActiveEntityService();
    LOGGER.trace("[{}] removeEntityManagementRegistry() active={}", consumerId, active);
    entityRegistries.remove(managementRegistry);
  }

  Optional<EntityManagementRegistry> addServerManagementRegistry(EntityManagementRegistry serverManagementRegistry) {
    long consumerId = serverManagementRegistry.getMonitoringService().getConsumerId();
    boolean active = serverManagementRegistry.getMonitoringService().isActiveEntityService();
    LOGGER.trace("[{}] addServerManagementRegistry() active={}", consumerId, active);
    serverRegistries.add(serverManagementRegistry);
    return serverRegistries.stream()
        .filter(existing -> existing != serverManagementRegistry && existing.getMonitoringService().getConsumerId() == consumerId)
        .findAny();
  }

  void removeServerManagementRegistry(EntityManagementRegistry managementRegistry) {
    long consumerId = managementRegistry.getMonitoringService().getConsumerId();
    boolean active = managementRegistry.getMonitoringService().isActiveEntityService();
    LOGGER.trace("[{}] removeServerManagementRegistry() active={}", consumerId, active);
    serverRegistries.remove(managementRegistry);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("DefaultSharedEntityManagementRegistry{");
    sb.append("registries=").append(entityRegistries);
    sb.append('}');
    return sb.toString();
  }

}
