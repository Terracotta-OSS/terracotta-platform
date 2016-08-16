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

import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.entity.ServiceProvider;
import org.terracotta.entity.ServiceProviderCleanupException;
import org.terracotta.entity.ServiceProviderConfiguration;
import org.terracotta.management.sequence.BoundaryFlakeSequenceGenerator;
import org.terracotta.management.sequence.SequenceGenerator;

import java.util.Arrays;
import java.util.Collection;

/**
 * @author Mathieu Carbou
 */
public class MonitoringServiceProvider implements ServiceProvider {

  private static final Collection<Class<?>> providedServiceTypes = Arrays.asList(
      IMonitoringConsumer.class,
      IMonitoringProducer.class,
      org.terracotta.monitoring.IMonitoringProducer.class
  );

  private MonitoringService monitoringService;

  @Override
  public boolean initialize(ServiceProviderConfiguration configuration) {
    MonitoringServiceConfiguration config = configuration instanceof MonitoringServiceConfiguration ?
        (MonitoringServiceConfiguration) configuration :
        new MonitoringServiceConfiguration();
    SequenceGenerator generator = new BoundaryFlakeSequenceGenerator();
    this.monitoringService = new MonitoringService(config, generator);
    return true;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T getService(long consumerID, ServiceConfiguration<T> configuration) {
    Class<T> serviceType = configuration.getServiceType();

    if (org.terracotta.monitoring.IMonitoringProducer.class.isAssignableFrom(serviceType)) {
      return serviceType.cast(monitoringService.getProducer(consumerID));
    }

    if (IMonitoringConsumer.class.isAssignableFrom(serviceType)) {
      MonitoringConsumerConfiguration config = configuration instanceof MonitoringConsumerConfiguration ?
          (MonitoringConsumerConfiguration) configuration :
          new MonitoringConsumerConfiguration();
      return serviceType.cast(monitoringService.getConsumer(consumerID, config));
    }

    throw new IllegalStateException("Unknown service type " + serviceType.getName());
  }

  @Override
  public Collection<Class<?>> getProvidedServiceTypes() {
    return providedServiceTypes;
  }

  @Override
  public void clear() throws ServiceProviderCleanupException {
    monitoringService.clear();
  }

}
