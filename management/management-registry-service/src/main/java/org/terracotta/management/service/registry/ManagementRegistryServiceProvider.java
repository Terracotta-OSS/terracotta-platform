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

import com.tc.classloader.BuiltinService;
import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.entity.ServiceProvider;
import org.terracotta.entity.ServiceProviderCleanupException;
import org.terracotta.entity.ServiceProviderConfiguration;
import org.terracotta.management.sequence.BoundaryFlakeSequenceGenerator;
import org.terracotta.management.sequence.NodeIdSource;
import org.terracotta.management.sequence.TimeSource;

import java.util.Collection;
import java.util.Collections;

/**
 * @author Mathieu Carbou
 */
@BuiltinService
public class ManagementRegistryServiceProvider implements ServiceProvider {

  private final ManagementRegistryService managementRegistryService = new ManagementRegistryService(new BoundaryFlakeSequenceGenerator(TimeSource.BEST, NodeIdSource.BEST));

  @Override
  public void clear() throws ServiceProviderCleanupException {
    managementRegistryService.clear();
  }

  @Override
  public boolean initialize(ServiceProviderConfiguration configuration) {
    // @BuiltinService cannot be initialized
    return true;
  }

  @Override
  public <T> T getService(long consumerID, ServiceConfiguration<T> configuration) {
    Class<T> serviceType = configuration.getServiceType();

    if (ConsumerManagementRegistry.class == serviceType) {
      if (configuration instanceof ConsumerManagementRegistryConfiguration) {
        ConsumerManagementRegistryConfiguration config = (ConsumerManagementRegistryConfiguration) configuration;
        return serviceType.cast(managementRegistryService.getManagementRegistry(consumerID, config));

      } else {
        return serviceType.cast(managementRegistryService.getNoopManagementRegistry(consumerID));
      }
    }

    throw new IllegalStateException("Unknown service type " + serviceType.getName());
  }

  @Override
  public Collection<Class<?>> getProvidedServiceTypes() {
    return Collections.singletonList(ConsumerManagementRegistry.class);
  }

}
