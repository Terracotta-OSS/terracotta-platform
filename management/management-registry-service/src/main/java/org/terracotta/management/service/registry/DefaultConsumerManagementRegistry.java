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
package org.terracotta.management.service.registry;

import org.terracotta.management.model.capabilities.Capability;
import org.terracotta.management.model.context.ContextContainer;
import org.terracotta.management.service.monitoring.MonitoringService;
import org.terracotta.management.service.monitoring.MonitoringServiceConfiguration;

import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Mathieu Carbou
 */
class DefaultConsumerManagementRegistry extends NoopConsumerManagementRegistry {

  private final AtomicBoolean dirty = new AtomicBoolean();
  private final ContextContainer contextContainer;
  private final MonitoringService monitoringService;

  DefaultConsumerManagementRegistry(ConsumerManagementRegistryConfiguration configuration) {
    super(configuration);
    this.monitoringService = Objects.requireNonNull(configuration.getServiceRegistry().getService(new MonitoringServiceConfiguration(configuration.getServiceRegistry())));
    this.contextContainer = new ContextContainer("entityConsumerId", String.valueOf(this.monitoringService.getConsumerId()));
    configuration.getProviders().forEach(this::addManagementProvider);
  }

  @Override
  public boolean register(Object managedObject) {
    boolean b = super.register(managedObject);
    if (b) {
      dirty.set(true);
    }
    return b;
  }

  @Override
  public boolean unregister(Object managedObject) {
    boolean b = super.unregister(managedObject);
    if (b) {
      dirty.set(true);
    }
    return b;
  }

  @Override
  public synchronized void refresh() {
    if (dirty.compareAndSet(true, false)) {
      Collection<Capability> capabilities = getCapabilities();
      Capability[] capabilitiesArray = capabilities.toArray(new Capability[capabilities.size()]);
      monitoringService.exposeServerEntityManagementRegistry(contextContainer, capabilitiesArray);
    }
  }

  @Override
  public ContextContainer getContextContainer() {
    return contextContainer;
  }

}
