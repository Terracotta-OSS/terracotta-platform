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
import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.entity.ServiceProvider;
import org.terracotta.entity.ServiceProviderCleanupException;
import org.terracotta.entity.ServiceProviderConfiguration;
import org.terracotta.management.sequence.BoundaryFlakeSequenceGenerator;
import org.terracotta.management.sequence.SequenceGenerator;
import org.terracotta.monitoring.IMonitoringProducer;
import org.terracotta.monitoring.IStripeMonitoring;

import java.util.Arrays;
import java.util.Collection;

/**
 * @author Mathieu Carbou
 */
@BuiltinService
public class MonitoringServiceProvider implements ServiceProvider {

  private static final Collection<Class<?>> providedServiceTypes = Arrays.asList(
      IMonitoringConsumer.class,
      IStripeMonitoring.class
  );

  private final VoltronMonitoringService voltronMonitoringService;

  public MonitoringServiceProvider() {
    SequenceGenerator generator = new BoundaryFlakeSequenceGenerator();
    this.voltronMonitoringService = new VoltronMonitoringService(generator);
  }

  @Override
  public boolean initialize(ServiceProviderConfiguration configuration) {
    // useless for a @BuiltinService
    return true;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T getService(long consumerID, ServiceConfiguration<T> configuration) {
    Class<T> serviceType = configuration.getServiceType();

    if (IStripeMonitoring.class.isAssignableFrom(serviceType)) {
      return serviceType.cast(voltronMonitoringService.getProducer(consumerID));
    }

    if (IMonitoringConsumer.class.isAssignableFrom(serviceType)) {
      return serviceType.cast(voltronMonitoringService.getConsumer(consumerID));
    }

    throw new IllegalStateException("Unknown service type " + serviceType.getName());
  }

  @Override
  public Collection<Class<?>> getProvidedServiceTypes() {
    return providedServiceTypes;
  }

  @Override
  public void clear() throws ServiceProviderCleanupException {
    voltronMonitoringService.clear();
  }

}
