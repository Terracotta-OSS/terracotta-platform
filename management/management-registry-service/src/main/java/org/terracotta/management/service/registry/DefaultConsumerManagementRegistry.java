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
import org.terracotta.management.registry.ManagementProvider;
import org.terracotta.management.service.monitoring.MonitoringService;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Mathieu Carbou
 */
class DefaultConsumerManagementRegistry extends NoopConsumerManagementRegistry {

  private static final Logger LOGGER = Logger.getLogger(ConsumerManagementRegistryProvider.class.getName());

  private final AtomicBoolean dirty = new AtomicBoolean();
  private final MonitoringService monitoringService;
  private final ContextContainer contextContainer;

  DefaultConsumerManagementRegistry(MonitoringService monitoringService, Collection<ManagementProvider<?>> providers) {
    super(providers);
    this.monitoringService = monitoringService;
    this.contextContainer = new ContextContainer("entityConsumerId", String.valueOf(this.monitoringService.getConsumerId()));
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
      if (LOGGER.isLoggable(Level.FINEST)) {
        LOGGER.finest("refresh(): " + contextContainer);
      }
      Collection<Capability> capabilities = getCapabilities();
      Capability[] capabilitiesArray = capabilities.toArray(new Capability[capabilities.size()]);
      monitoringService.exposeServerEntityManagementRegistry(contextContainer, capabilitiesArray);
    }
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
