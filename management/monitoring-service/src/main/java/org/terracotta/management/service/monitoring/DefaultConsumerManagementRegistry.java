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
import org.terracotta.management.registry.AbstractManagementProvider;
import org.terracotta.management.registry.AbstractManagementRegistry;
import org.terracotta.management.registry.ManagementProvider;
import org.terracotta.management.registry.action.ExposedObject;
import org.terracotta.management.service.monitoring.registry.provider.MonitoringServiceAware;
import org.terracotta.management.service.monitoring.registry.provider.StatisticsServiceAware;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * @author Mathieu Carbou
 */
class DefaultConsumerManagementRegistry extends AbstractManagementRegistry implements ConsumerManagementRegistry {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultConsumerManagementRegistry.class);

  private final MonitoringService monitoringService;
  private final ContextContainer contextContainer;
  private final StatisticsService statisticsService;

  private Collection<Capability> previouslyExposed = Collections.emptyList();

  DefaultConsumerManagementRegistry(long consumerId, MonitoringService monitoringService, StatisticsService statisticsService) {
    this.monitoringService = Objects.requireNonNull(monitoringService);
    this.statisticsService = Objects.requireNonNull(statisticsService);
    this.contextContainer = new ContextContainer("consumerId", String.valueOf(consumerId));
  }

  @Override
  public void close() {
    managementProviders.forEach(ManagementProvider::close);
    managementProviders.clear();
  }

  @Override
  public void addManagementProvider(ManagementProvider<?> provider) {
    if (provider instanceof MonitoringServiceAware) {
      ((MonitoringServiceAware) provider).setMonitoringService(monitoringService);
    }
    if(provider instanceof StatisticsServiceAware) {
      ((StatisticsServiceAware) provider).setStatisticsService(statisticsService);
    }
    super.addManagementProvider(provider);
  }

  @Override
  public synchronized void refresh() {
    LOGGER.trace("refresh(): {}", contextContainer);
    Collection<Capability> capabilities = getCapabilities();
    if (!previouslyExposed.equals(capabilities)) {
      Capability[] capabilitiesArray = capabilities.toArray(new Capability[capabilities.size()]);
      // confirm with server team, this call won't throw because monitoringProducer.addNode() won't throw.
      monitoringService.exposeServerEntityManagementRegistry(contextContainer, capabilitiesArray);
      previouslyExposed = capabilities;
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public boolean pushServerEntityNotification(Object managedObjectSource, String type, Map<String, String> attrs) {
    for (ManagementProvider managementProvider : managementProviders) {
      if (managementProvider instanceof AbstractManagementProvider && managementProvider.getManagedType().isInstance(managedObjectSource)) {
        ExposedObject<Object> exposedObject = ((AbstractManagementProvider<Object>) managementProvider).findExposedObject(managedObjectSource);
        if(exposedObject != null) {
          monitoringService.pushServerEntityNotification(new ContextualNotification(exposedObject.getContext(), type, attrs));
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public ContextContainer getContextContainer() {
    return contextContainer;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("DefaultConsumerManagementRegistry{");
    sb.append("contextContainer=").append(contextContainer);
    sb.append(", monitoringService=").append(monitoringService);
    sb.append('}');
    return sb.toString();
  }

}
