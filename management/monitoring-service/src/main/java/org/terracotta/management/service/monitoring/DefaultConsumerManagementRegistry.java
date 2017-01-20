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
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.management.model.capabilities.Capability;
import org.terracotta.management.model.context.ContextContainer;
import org.terracotta.management.model.notification.ContextualNotification;
import org.terracotta.management.registry.CapabilityManagement;
import org.terracotta.management.registry.DefaultCapabilityManagement;
import org.terracotta.management.registry.ManagementProvider;
import org.terracotta.management.registry.action.ExposedObject;
import org.terracotta.management.service.monitoring.registry.provider.AbstractConsumerManagementProvider;
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
class DefaultConsumerManagementRegistry implements ConsumerManagementRegistry, TopologyEventListener {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultConsumerManagementRegistry.class);
  private static final Comparator<Capability> CAPABILITY_COMPARATOR = new Comparator<Capability>() {
    @Override
    public int compare(Capability o1, Capability o2) {
      return o1.getName().compareTo(o2.getName());
    }
  };

  private final long consumerId;
  private final EntityMonitoringService monitoringService;
  private final ContextContainer contextContainer;
  private final List<ManagementProvider<?>> managementProviders = new CopyOnWriteArrayList<>();

  private Collection<? extends Capability> previouslyExposed = Collections.emptyList();

  DefaultConsumerManagementRegistry(long consumerId, EntityMonitoringService monitoringService) {
    this.contextContainer = new ContextContainer("consumerId", String.valueOf(consumerId));
    this.consumerId = consumerId;
    this.monitoringService = Objects.requireNonNull(monitoringService);
  }

  @Override
  public ContextContainer getContextContainer() {
    return contextContainer;
  }

  @Override
  public void addManagementProvider(ManagementProvider<?> provider) {
    LOGGER.trace("[{}] addManagementProvider({})", consumerId, provider.getClass().getSimpleName());
    String name = provider.getCapabilityName();
    for (ManagementProvider<?> managementProvider : managementProviders) {
      if (managementProvider.getCapabilityName().equals(name)) {
        throw new IllegalStateException("Duplicated management provider name : " + name);
      }
    }
    if (provider instanceof MonitoringServiceAware) {
      ((MonitoringServiceAware) provider).setMonitoringService(monitoringService);
    }
    managementProviders.add(provider);
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

  @Override
  public CompletableFuture<Void> register(Object managedObject) {
    LOGGER.trace("[{}] register()", consumerId, managedObject);
    List<CompletableFuture<Void>> futures = new ArrayList<>(managementProviders.size());
    for (ManagementProvider managementProvider : managementProviders) {
      if (managementProvider.getManagedType().isInstance(managedObject)) {
        if (managementProvider instanceof AbstractConsumerManagementProvider) {
          CompletableFuture<Void> future = ((AbstractConsumerManagementProvider) managementProvider).registerAsync(managedObject);
          futures.add(future);
        } else {
          managementProvider.register(managedObject);
        }
      }
    }
    return CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[futures.size()]));
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
  public synchronized void refresh() {
    LOGGER.trace("[{}] refresh()", consumerId);
    Collection<? extends Capability> capabilities = getCapabilities();
    if (!previouslyExposed.equals(capabilities)) {
      Capability[] capabilitiesArray = capabilities.toArray(new Capability[capabilities.size()]);
      // confirm with server team, this call won't throw because monitoringProducer.addNode() won't throw.
      monitoringService.exposeManagementRegistry(getContextContainer(), capabilitiesArray);
      previouslyExposed = capabilities;
    }
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

  @Override
  public void onBecomeActive() {
    // do not clear any state because on failover, onBecomeActive() is called after the new active entities are created
    // so it would clear the providers added by entities at creation time
  }

  @Override
  public void onFetch(long consumerId, ClientDescriptor clientDescriptor) {
  }

  @Override
  public void onUnfetch(long consumerId, ClientDescriptor clientDescriptor) {
  }

  @Override
  public void onEntityDestroyed(long consumerId) {
    if (consumerId == this.consumerId) {
      LOGGER.trace("[{}] onEntityDestroyed()", consumerId);
      clear();
    }
  }

  @Override
  public void onEntityCreated(long consumerId) {
  }

  void clear() {
    LOGGER.trace("[{}] clear()", consumerId);
    managementProviders.forEach(ManagementProvider::close);
    managementProviders.clear();
    previouslyExposed.clear();
  }
}
