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

import com.tc.classloader.CommonComponent;
import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.management.registry.ManagementProvider;
import org.terracotta.monitoring.IMonitoringProducer;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

/**
 * @author Mathieu Carbou
 */
@CommonComponent
public class ConsumerManagementRegistryConfiguration implements ServiceConfiguration<ConsumerManagementRegistry> {

  private final IMonitoringProducer monitoringProducer;
  private final Collection<ManagementProvider<?>> providers = new HashSet<>();

  public ConsumerManagementRegistryConfiguration(IMonitoringProducer monitoringProducer) {
    this.monitoringProducer = Objects.requireNonNull(monitoringProducer);
  }

  public IMonitoringProducer getMonitoringProducer() {
    return monitoringProducer;
  }

  public Collection<ManagementProvider<?>> getProviders() {
    return providers;
  }

  @Override
  public Class<ConsumerManagementRegistry> getServiceType() {
    return ConsumerManagementRegistry.class;
  }

  public ConsumerManagementRegistryConfiguration addProvider(ManagementProvider<?> provider) {
    providers.add(provider);
    return this;
  }

}
