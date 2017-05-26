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

import java.util.Collection;
import java.util.Objects;

/**
 * @author Mathieu Carbou
 */
@CommonComponent
public class ManagementRegistryConfiguration implements ServiceConfiguration<EntityManagementRegistry> {

  private final ServiceRegistry registry;
  private final boolean active;
  private final boolean addServerLevelCapabilities;

  public ManagementRegistryConfiguration(ServiceRegistry registry, boolean active) {
    this(registry, active, false);
  }

  public ManagementRegistryConfiguration(ServiceRegistry registry, boolean active, boolean addServerLevelCapabilities) {
    this.registry = Objects.requireNonNull(registry);
    this.active = active;
    this.addServerLevelCapabilities = addServerLevelCapabilities;
  }

  public boolean isActive() {
    return active;
  }

  public boolean wantsServerLevelCapabilities() {
    return addServerLevelCapabilities;
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

  public Collection<ManageableServerComponent> getManageableVoltronComponents() {
    return registry.getServices(new BasicServiceConfiguration<>(ManageableServerComponent.class));
  }
}
