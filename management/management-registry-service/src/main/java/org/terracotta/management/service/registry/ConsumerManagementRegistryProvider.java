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
import org.terracotta.entity.PlatformConfiguration;
import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.entity.ServiceProvider;
import org.terracotta.entity.ServiceProviderCleanupException;
import org.terracotta.entity.ServiceProviderConfiguration;
import org.terracotta.entity.ServiceRegistry;
import org.terracotta.management.service.monitoring.MonitoringService;
import org.terracotta.management.service.monitoring.MonitoringServiceConfiguration;

import java.util.Collection;
import java.util.Collections;

/**
 * @author Mathieu Carbou
 */
@BuiltinService
public class ConsumerManagementRegistryProvider implements ServiceProvider {

  @Override
  public void clear() throws ServiceProviderCleanupException {
  }

  @Override
  public boolean initialize(ServiceProviderConfiguration configuration, PlatformConfiguration platformConfiguration) {
    // useless for a @BuiltinService until https://github.com/Terracotta-OSS/terracotta-apis/issues/152 is fixed
    return true;
  }

  @Override
  public <T> T getService(long consumerID, ServiceConfiguration<T> configuration) {
    Class<T> serviceType = configuration.getServiceType();

    if (ConsumerManagementRegistry.class == serviceType) {
      if (configuration instanceof ConsumerManagementRegistryConfiguration) {
        ConsumerManagementRegistryConfiguration config = (ConsumerManagementRegistryConfiguration) configuration;
        ServiceRegistry serviceRegistry = config.getServiceRegistry();
        MonitoringService monitoringService = serviceRegistry.getService(new MonitoringServiceConfiguration(serviceRegistry));
        return serviceType.cast(new DefaultConsumerManagementRegistry(monitoringService));

      } else {
        throw new IllegalArgumentException("Missing configuration: " + ConsumerManagementRegistryConfiguration.class.getSimpleName());
      }

    } else {
      throw new IllegalArgumentException("Unknown service type " + serviceType.getName());
    }
  }

  @Override
  public Collection<Class<?>> getProvidedServiceTypes() {
    return Collections.singletonList(ConsumerManagementRegistry.class);
  }

}
