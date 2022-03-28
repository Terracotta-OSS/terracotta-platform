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
package org.terracotta.lease.service;

import com.tc.classloader.BuiltinService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.entity.PlatformConfiguration;
import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.entity.ServiceProvider;
import org.terracotta.entity.ServiceProviderCleanupException;
import org.terracotta.entity.ServiceProviderConfiguration;
import org.terracotta.entity.StateDumpCollector;
import org.terracotta.lease.TimeSource;
import org.terracotta.lease.TimeSourceProvider;
import org.terracotta.lease.service.closer.ClientConnectionCloser;
import org.terracotta.lease.service.closer.ProxyClientConnectionCloser;
import org.terracotta.lease.service.config.LeaseConfiguration;
import org.terracotta.lease.service.monitor.LeaseMonitorThread;
import org.terracotta.lease.service.monitor.LeaseState;

import java.io.Closeable;
import java.util.Arrays;
import java.util.Collection;

/**
 * LeaseServiceProvider consumes the LeaseConfiguration objects (generated from XML parsing) and then creates the
 * connection leasing components, such as LeaseState and LeaseMonitorThread.
 */
@BuiltinService
public class LeaseServiceProvider implements ServiceProvider, Closeable {
  private static Logger LOGGER = LoggerFactory.getLogger(LeaseServiceProvider.class);

  private LeaseConfiguration leaseConfiguration;
  private LeaseState leaseState;
  private LeaseMonitorThread leaseMonitorThread;
  private ProxyClientConnectionCloser proxyClientConnectionCloser;

  @Override
  public boolean initialize(ServiceProviderConfiguration configuration, PlatformConfiguration platformConfiguration) {
    if (configuration instanceof LeaseConfiguration) {
      LOGGER.info("Initializing LeaseServiceProvider with " + configuration);
      leaseConfiguration = (LeaseConfiguration) configuration;
    } else {
      LOGGER.info("Initializing LeaseServiceProvider with default lease length of " + LeaseConstants.DEFAULT_LEASE_LENGTH + " ms");
      leaseConfiguration = new LeaseConfiguration(LeaseConstants.DEFAULT_LEASE_LENGTH);
    }
    TimeSource timeSource = TimeSourceProvider.getTimeSource();
    proxyClientConnectionCloser = new ProxyClientConnectionCloser();
    leaseState = new LeaseState(timeSource, proxyClientConnectionCloser);
    leaseMonitorThread = new LeaseMonitorThread(timeSource, leaseState);
    leaseMonitorThread.start();
    return true;
  }

  @Override
  public <T> T getService(long consumerID, ServiceConfiguration<T> serviceConfiguration) {
    if (serviceConfiguration.getServiceType() == LeaseConfiguration.class) {
      return serviceConfiguration.getServiceType().cast(leaseConfiguration);
    }

    if (serviceConfiguration instanceof LeaseServiceConfiguration) {
      LOGGER.info("Creating LeaseService");

      LeaseServiceConfiguration leaseServiceConfiguration = (LeaseServiceConfiguration) serviceConfiguration;

      ClientConnectionCloser clientConnectionCloser = leaseServiceConfiguration.getClientConnectionCloser();
      LeaseService leaseService = createLeaseService(clientConnectionCloser);

      return serviceConfiguration.getServiceType().cast(leaseService);
    }

    throw new IllegalArgumentException("Unsupported service configuration: " + serviceConfiguration);
  }

  private LeaseService createLeaseService(ClientConnectionCloser clientConnectionCloser) {
    // This ugly proxy nonsense is only here because services have no way to directly depend on other services.
    // Ideally, when LeaseState gets created, we would be able to get a ClientCommunicator directly.
    proxyClientConnectionCloser.setClientConnectionCloser(clientConnectionCloser);
    return new LeaseServiceImpl(leaseConfiguration, leaseState);
  }

  @Override
  public Collection<Class<?>> getProvidedServiceTypes() {
    return Arrays.asList(LeaseService.class, LeaseConfiguration.class);
  }

  @Override
  public void prepareForSynchronization() throws ServiceProviderCleanupException {
  }

  @Override
  public void close() {
    leaseMonitorThread.interrupt();
  }

  @Override
  public void addStateTo(StateDumpCollector stateDumper) {
    stateDumper.addState("LeaseLength", Long.toString(leaseConfiguration.getLeaseLength()));
    leaseState.addStateTo(stateDumper.subStateDumpCollector("LeaseState"));
  }
}
