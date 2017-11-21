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

import com.tc.classloader.CommonComponent;
import org.terracotta.entity.BasicServiceConfiguration;
import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.entity.ServiceException;
import org.terracotta.entity.ServiceRegistry;
import org.terracotta.monitoring.IMonitoringProducer;

import java.util.Objects;

/**
 * @author Mathieu Carbou
 */
@CommonComponent
public abstract class AbstractManagementRegistryConfiguration implements ServiceConfiguration<EntityManagementRegistry> {

  protected final ServiceRegistry registry;
  private final boolean active;

  public AbstractManagementRegistryConfiguration(ServiceRegistry registry, boolean active) {
    this.registry = Objects.requireNonNull(registry);
    this.active = active;
  }

  public boolean isActive() {
    return active;
  }

  public Class<EntityManagementRegistry> getServiceType() {
    return EntityManagementRegistry.class;
  }

  public IMonitoringProducer getMonitoringProducer() {
    try {
      return Objects.requireNonNull(registry.getService(new BasicServiceConfiguration<>(IMonitoringProducer.class)));
    } catch (ServiceException e) {
      // IMonitoringProducer is a mandatory platform service
      throw new AssertionError(e);
    }
  }

}
