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
import org.terracotta.management.registry.ExposedObject;
import org.terracotta.management.registry.ManagementProvider;
import org.terracotta.management.sequence.TimeSource;
import org.terracotta.management.service.monitoring.registry.provider.AbstractEntityManagementProvider;
import org.terracotta.management.service.monitoring.registry.provider.MonitoringServiceAware;

import java.util.ArrayList;
import java.util.Collection;
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
  private final TimeSource timeSource;
  private final ContextContainer contextContainer;
  private final List<ManagementProvider<?>> managementProviders = new CopyOnWriteArrayList<>();
  private final CompletableFuture<?> onEntityPromotionCompleted = new CompletableFuture<>();
  private final CompletableFuture<?> onEntityCreated = new CompletableFuture<>();
  private final CompletableFuture<?> onClose = new CompletableFuture<>();
  private volatile boolean closed;

  DefaultEntityManagementRegistry(long consumerId, EntityMonitoringService monitoringService, TimeSource timeSource) {
    this.contextContainer = new ContextContainer("consumerId", String.valueOf(consumerId));
    this.consumerId = consumerId;
    this.monitoringService = Objects.requireNonNull(monitoringService);
    this.timeSource = Objects.requireNonNull(timeSource);
  }

  void onEntityPromotionCompleted(Runnable r) {
    onEntityPromotionCompleted.thenRun(r);
  }

  void onEntityCreated(Runnable r) {
    onEntityCreated.thenRun(r);
  }

  void onClose(Runnable r) {
    onClose.thenRun(r);
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
    LOGGER.trace("[{}] addManagementProvider({}) active={}", consumerId, provider.getClass().getSimpleName(), monitoringService.isActiveEntityService());
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
        ((MonitoringServiceAware) provider).setTimeSource(timeSource);
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
    List<Capability> capabilities = new ArrayList<>();
    for (ManagementProvider<?> managementProvider : managementProviders) {
      capabilities.add(managementProvider.getCapability());
    }
    capabilities.sort(CAPABILITY_COMPARATOR);
    return capabilities;
  }

  @Override
  public Collection<String> getCapabilityNames() {
    Collection<String> names = new TreeSet<>();
    for (ManagementProvider<?> managementProvider : managementProviders) {
      names.add(managementProvider.getCapabilityName());
    }
    return names;
  }

  @Override
  public List<ManagementProvider<?>> getManagementProvidersByCapability(String capabilityName) {
    List<ManagementProvider<?>> allProviders = new ArrayList<>();
    for (ManagementProvider<?> provider : managementProviders) {
      if (provider.getCapabilityName().equals(capabilityName)) {
        allProviders.add(provider);
      }
    }
    return allProviders;
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  @Override
  public CompletableFuture<Void> register(Object managedObject) {
    LOGGER.trace("[{}] register() active={}", consumerId, monitoringService.isActiveEntityService());
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
        CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0]));
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
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
    LOGGER.info("[{}] Updating {} entity management registry", consumerId, monitoringService.isActiveEntityService() ? "active" : "passive");
    Collection<? extends Capability> capabilities = getCapabilities();
    Capability[] capabilitiesArray = capabilities.toArray(new Capability[0]);
    // confirm with server team, this call won't throw because monitoringProducer.addNode() won't throw.
    monitoringService.exposeManagementRegistry(getContextContainer(), capabilitiesArray);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
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

  @Override
  public void close() {
    if (!closed) {
      closed = true;
      LOGGER.info("[{}] Closing {} entity management registry", consumerId, monitoringService.isActiveEntityService() ? "active" : "passive");
      managementProviders.forEach(ManagementProvider::close);
      managementProviders.clear();
      onClose.complete(null);
    }
  }

  @Override
  public void entityPromotionCompleted() {
    LOGGER.trace("[{}] entityPromotionCompleted() active={}", consumerId, monitoringService.isActiveEntityService());
    onEntityPromotionCompleted.complete(null);
  }

  @Override
  public void entityCreated() {
    LOGGER.trace("[{}] entityCreated() active={}", consumerId, monitoringService.isActiveEntityService());
    onEntityCreated.complete(null);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("DefaultEntityManagementRegistry{");
    sb.append("consumerId=").append(consumerId);
    sb.append(", active=").append(monitoringService.isActiveEntityService());
    sb.append('}');
    return sb.toString();
  }
}
