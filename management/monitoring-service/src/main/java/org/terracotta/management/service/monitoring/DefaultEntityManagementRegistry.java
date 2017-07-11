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
import org.terracotta.management.model.notification.ContextualNotification;
import org.terracotta.management.registry.CapabilityManagement;
import org.terracotta.management.registry.DefaultCapabilityManagement;
import org.terracotta.management.registry.ManagementProvider;
import org.terracotta.management.registry.ExposedObject;
import org.terracotta.management.service.monitoring.registry.provider.AbstractEntityManagementProvider;
import org.terracotta.management.service.monitoring.registry.provider.MonitoringServiceAware;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Mathieu Carbou
 */
class DefaultEntityManagementRegistry implements EntityManagementRegistry, TopologyEventListener {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultEntityManagementRegistry.class);
  private static final Comparator<Capability> CAPABILITY_COMPARATOR = Comparator.comparing(Capability::getName);

  private final long consumerId;
  private final EntityMonitoringService monitoringService;
  private final DefaultSharedEntityManagementRegistry sharedManagementRegistry;
  private final TopologyService topologyService;
  private final Collection<ManageableServerComponent> manageableServerComponents;
  private final ContextContainer contextContainer;
  private final List<ManagementProvider<?>> managementProviders = new CopyOnWriteArrayList<>();

  DefaultEntityManagementRegistry(long consumerId, EntityMonitoringService monitoringService, DefaultSharedEntityManagementRegistry sharedManagementRegistry, TopologyService topologyService, Collection<ManageableServerComponent> manageableServerComponents) {
    this.contextContainer = new ContextContainer("consumerId", String.valueOf(consumerId));
    this.consumerId = consumerId;
    this.monitoringService = Objects.requireNonNull(monitoringService);
    this.sharedManagementRegistry = Objects.requireNonNull(sharedManagementRegistry);
    this.topologyService = Objects.requireNonNull(topologyService);
    this.manageableServerComponents = Objects.requireNonNull(manageableServerComponents);

    topologyService.addTopologyEventListener(this);
    sharedManagementRegistry.addManagementService(this);
  }

  @Override
  public EntityMonitoringService getMonitoringService() {
    return monitoringService;
  }

  @Override
  public ContextContainer getContextContainer() {
    return contextContainer;
  }

  @Override
  public boolean addManagementProvider(ManagementProvider<?> provider) {
    LOGGER.trace("[{}] addManagementProvider({})", consumerId, provider.getClass().getSimpleName());
    String name = provider.getCapabilityName();
    for (ManagementProvider<?> managementProvider : managementProviders) {
      if (managementProvider.getCapabilityName().equals(name)) {
        return false;
      }
    }
    boolean added = managementProviders.add(provider);
    if (added) {
      if (provider instanceof MonitoringServiceAware) {
        ((MonitoringServiceAware) provider).setMonitoringService(monitoringService);
      }
    }
    return added;
  }

  @Override
  public void removeManagementProvider(ManagementProvider<?> provider) {
    managementProviders.remove(provider);
  }

  @Override
  public CapabilityManagement withCapability(String capabilityName) {
    return new DefaultCapabilityManagement(this, capabilityName);
  }

  @Override
  public Collection<? extends Capability> getCapabilities() {
    List<Capability> capabilities = new ArrayList<Capability>();
    for (ManagementProvider<?> managementProvider : managementProviders) {
      capabilities.add(managementProvider.getCapability());
    }
    Collections.sort(capabilities, CAPABILITY_COMPARATOR);
    return capabilities;
  }

  @Override
  public Collection<String> getCapabilityNames() {
    Collection<String> names = new TreeSet<String>();
    for (ManagementProvider<?> managementProvider : managementProviders) {
      names.add(managementProvider.getCapabilityName());
    }
    return names;
  }

  @Override
  public List<ManagementProvider<?>> getManagementProvidersByCapability(String capabilityName) {
    List<ManagementProvider<?>> allProviders = new ArrayList<ManagementProvider<?>>();
    for (ManagementProvider<?> provider : managementProviders) {
      if (provider.getCapabilityName().equals(capabilityName)) {
        allProviders.add(provider);
      }
    }
    return allProviders;
  }

  @SuppressWarnings("unchecked")
  @Override
  public CompletableFuture<Void> register(Object managedObject) {
    LOGGER.trace("[{}] register()", consumerId, managedObject);
    List<CompletableFuture<Void>> futures = new ArrayList<>(managementProviders.size());
    for (ManagementProvider managementProvider : managementProviders) {
      if (managementProvider.getManagedType().isInstance(managedObject)) {
        if (managementProvider instanceof AbstractEntityManagementProvider) {
          CompletableFuture<Void> future = ((AbstractEntityManagementProvider) managementProvider).registerAsync(managedObject);
          futures.add(future);
        } else {
          managementProvider.register(managedObject);
        }
      }
    }
    return futures.isEmpty() ?
        CompletableFuture.completedFuture(null) :
        CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[futures.size()]));
  }

  @SuppressWarnings("unchecked")
  @Override
  public void unregister(Object managedObject) {
    for (ManagementProvider managementProvider : managementProviders) {
      if (managementProvider.getManagedType().isInstance(managedObject)) {
        managementProvider.unregister(managedObject);
      }
    }
  }

  @Override
  public void refresh() {
    LOGGER.trace("[{}] refresh()", consumerId);
    Collection<? extends Capability> capabilities = getCapabilities();
    Capability[] capabilitiesArray = capabilities.toArray(new Capability[capabilities.size()]);
    // confirm with server team, this call won't throw because monitoringProducer.addNode() won't throw.
    monitoringService.exposeManagementRegistry(getContextContainer(), capabilitiesArray);
  }

  @SuppressWarnings("unchecked")
  @Override
  public boolean pushServerEntityNotification(Object managedObjectSource, String type, Map<String, String> attrs) {
    LOGGER.trace("[{}] pushServerEntityNotification({})", consumerId, type);
    for (ManagementProvider managementProvider : managementProviders) {
      if (managementProvider.getManagedType().isInstance(managedObjectSource)) {
        ExposedObject<Object> exposedObject = managementProvider.findExposedObject(managedObjectSource);
        if (exposedObject != null) {
          monitoringService.pushNotification(new ContextualNotification(exposedObject.getContext(), type, attrs));
          return true;
        }
      }
    }
    return false;
  }

  void clear() {
    LOGGER.trace("[{}] clear()", consumerId);
    managementProviders.forEach(ManagementProvider::close);
    managementProviders.clear();
  }

  @Override
  public void close() {
    LOGGER.trace("[{}] close()", consumerId);
    manageableServerComponents.forEach(manageableServerComponent -> manageableServerComponent.onManagementRegistryClose(this));
    clear();
    topologyService.removeTopologyEventListener(this);
    sharedManagementRegistry.removeManagementService(this);
  }

}
