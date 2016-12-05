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
import org.terracotta.management.registry.AbstractManagementProvider;
import org.terracotta.management.registry.DefaultManagementRegistry;
import org.terracotta.management.registry.ManagementProvider;
import org.terracotta.management.registry.action.ExposedObject;
import org.terracotta.management.service.monitoring.registry.provider.MonitoringServiceAware;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * @author Mathieu Carbou
 */
class DefaultConsumerManagementRegistry extends DefaultManagementRegistry implements ConsumerManagementRegistry, ClientDescriptorListener {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultConsumerManagementRegistry.class);

  private final long consumerId;
  private final EntityMonitoringService monitoringService;
  private final StatisticsService statisticsService;

  private Collection<? extends Capability> previouslyExposed = Collections.emptyList();

  DefaultConsumerManagementRegistry(long consumerId, EntityMonitoringService monitoringService, StatisticsService statisticsService) {
    super(new ContextContainer("consumerId", String.valueOf(consumerId)));
    this.consumerId = consumerId;
    this.monitoringService = Objects.requireNonNull(monitoringService);
    this.statisticsService = Objects.requireNonNull(statisticsService);
  }

  @Override
  public void addManagementProvider(ManagementProvider<?> provider) {
    LOGGER.trace("[{}] addManagementProvider({})", consumerId, provider.getClass());
    if (provider instanceof MonitoringServiceAware) {
      ((MonitoringServiceAware) provider).setMonitoringService(monitoringService);
      ((MonitoringServiceAware) provider).setStatisticsService(statisticsService);
    }
    super.addManagementProvider(provider);
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
      if (managementProvider instanceof AbstractManagementProvider && managementProvider.getManagedType().isInstance(managedObjectSource)) {
        ExposedObject<Object> exposedObject = ((AbstractManagementProvider<Object>) managementProvider).findExposedObject(managedObjectSource);
        if (exposedObject != null) {
          monitoringService.pushNotification(new ContextualNotification(exposedObject.getContext(), type, attrs));
          return true;
        }
      }
    }
    return false;
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
      managementProviders.forEach(ManagementProvider::close);
      managementProviders.clear();
    }
  }

}
