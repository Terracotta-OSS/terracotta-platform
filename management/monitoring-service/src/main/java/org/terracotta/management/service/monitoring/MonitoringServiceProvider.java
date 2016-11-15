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

import com.tc.classloader.BuiltinService;
import org.terracotta.entity.PlatformConfiguration;
import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.entity.ServiceProvider;
import org.terracotta.entity.ServiceProviderCleanupException;
import org.terracotta.entity.ServiceProviderConfiguration;
import org.terracotta.management.sequence.BoundaryFlakeSequenceGenerator;
import org.terracotta.management.sequence.NodeIdSource;
import org.terracotta.management.sequence.TimeSource;
import org.terracotta.monitoring.IStripeMonitoring;

import java.util.Arrays;
import java.util.Collection;

/**
 * Provides 2 services: {@link IStripeMonitoring} for the platform, {@link MonitoringService} for the consumers (server entities)
 *
 * @author Mathieu Carbou
 */
@BuiltinService
public class MonitoringServiceProvider implements ServiceProvider {

  private static final Collection<Class<?>> providedServiceTypes = Arrays.asList(
      MonitoringService.class,
      IStripeMonitoring.class,
      SharedManagementRegistry.class,
      ConsumerManagementRegistry.class
  );

  private DefaultListener listener;
  private PlatformListenerAdapter platformListenerAdapter;
  private final DefaultSharedManagementRegistry defaultSharedManagementRegistry = new DefaultSharedManagementRegistry();

  @Override
  public boolean initialize(ServiceProviderConfiguration configuration, PlatformConfiguration platformConfiguration) {
    this.listener = new DefaultListener(new BoundaryFlakeSequenceGenerator(TimeSource.BEST, NodeIdSource.BEST), platformConfiguration);
    this.platformListenerAdapter = new PlatformListenerAdapter(listener);
    return true;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T getService(long consumerID, ServiceConfiguration<T> configuration) {
    if (this.listener == null || this.platformListenerAdapter == null) {
      throw new IllegalStateException("Service provider " + getClass().getName() + " has not been initialized");
    }

    Class<T> serviceType = configuration.getServiceType();

    if (IStripeMonitoring.class.isAssignableFrom(serviceType)) {
      return serviceType.cast(consumerID == PLATFORM_CONSUMER_ID ? platformListenerAdapter : new DataListenerAdapter(listener, consumerID));
    }

    // get or creates a monitoring service used to access the inner M&M topology
    if (MonitoringService.class.isAssignableFrom(serviceType)) {
      if (configuration instanceof MonitoringServiceConfiguration) {
        MonitoringService monitoringService = listener.getOrCreateMonitoringService(consumerID, (MonitoringServiceConfiguration) configuration);
        return serviceType.cast(monitoringService);
      } else {
        throw new IllegalArgumentException("Missing configuration " + MonitoringServiceConfiguration.class.getSimpleName() + " when requesting service " + serviceType.getName());
      }

      // get or creates a registry specific to this entity to handle stats and management calls
    } else if (ConsumerManagementRegistry.class.isAssignableFrom(serviceType)) {
      if (configuration instanceof ConsumerManagementRegistryConfiguration) {
        ConsumerManagementRegistryConfiguration managementRegistryConfiguration = (ConsumerManagementRegistryConfiguration) configuration;
        MonitoringService monitoringService = listener.getOrCreateMonitoringService(consumerID, new MonitoringServiceConfiguration(managementRegistryConfiguration.getRegistry()));
        ConsumerManagementRegistry consumerManagementRegistry = defaultSharedManagementRegistry.getOrCreateConsumerManagementRegistry(consumerID, monitoringService);
        return serviceType.cast(consumerManagementRegistry);
      } else {
        throw new IllegalArgumentException("Missing configuration " + MonitoringServiceConfiguration.class.getSimpleName() + " when requesting service " + serviceType.getName());
      }

      // get or create a shared registry used to do aggregated operations on all consumer registries (i.e. management calls)
    } else if (SharedManagementRegistry.class.isAssignableFrom(serviceType)) {
      return serviceType.cast(defaultSharedManagementRegistry);

    } else {
      throw new IllegalStateException("Unable to provide service " + serviceType.getName() + " to consumerID: " + consumerID);
    }
  }

  @Override
  public Collection<Class<?>> getProvidedServiceTypes() {
    return providedServiceTypes;
  }

  @Override
  public void clear() throws ServiceProviderCleanupException {
    listener.clear();
  }

}
