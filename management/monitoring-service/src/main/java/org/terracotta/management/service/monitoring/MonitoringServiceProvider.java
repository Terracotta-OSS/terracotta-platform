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
      IStripeMonitoring.class
  );

  private final DefaultListener listener = new DefaultListener(new BoundaryFlakeSequenceGenerator(TimeSource.BEST, NodeIdSource.BEST));
  private final PlatformListenerAdapter platformListenerAdapter = new PlatformListenerAdapter(listener);

  @Override
  public boolean initialize(ServiceProviderConfiguration configuration, PlatformConfiguration platformConfiguration) {
    // useless for a @BuiltinService until https://github.com/Terracotta-OSS/terracotta-apis/issues/152 is fixed
    return true;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T getService(long consumerID, ServiceConfiguration<T> configuration) {
    Class<T> serviceType = configuration.getServiceType();

    if (IStripeMonitoring.class.isAssignableFrom(serviceType)) {
      return serviceType.cast(consumerID == PLATFORM_CONSUMER_ID ? platformListenerAdapter : new DataListenerAdapter(listener, consumerID));
    }

    if (MonitoringService.class.isAssignableFrom(serviceType)) {

      if (configuration instanceof MonitoringServiceConfiguration) {
        return serviceType.cast(listener.getOrCreateMonitoringService(consumerID, (MonitoringServiceConfiguration) configuration));

      } else {
        throw new IllegalArgumentException("Missing configuration: " + MonitoringServiceConfiguration.class.getSimpleName());
      }

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
