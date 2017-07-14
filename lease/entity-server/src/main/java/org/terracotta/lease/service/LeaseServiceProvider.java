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
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * LeaseServiceProvider consumes the LeaseConfiguration objects (generated from XML parsing) and then creates the
 * connection leasing components, such as LeaseState and LeaseMonitorThread.
 */
@BuiltinService
public class LeaseServiceProvider implements ServiceProvider, Closeable {
  public static long MAX_LEASE_LENGTH = TimeUnit.MILLISECONDS.convert(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
  private static long DEFAULT_LEASE_LENGTH = TimeUnit.MILLISECONDS.convert(150, TimeUnit.SECONDS);
  private static Logger LOGGER = LoggerFactory.getLogger(LeaseServiceProvider.class);

  private long leaseLength;
  private LeaseState leaseState;
  private LeaseMonitorThread leaseMonitorThread;
  private ProxyClientConnectionCloser proxyClientConnectionCloser;

  @Override
  public boolean initialize(ServiceProviderConfiguration configuration, PlatformConfiguration platformConfiguration) {
    LOGGER.info("Initializing LeaseServiceProvider");

    leaseLength = getLeaseLength(configuration);

    TimeSource timeSource = TimeSourceProvider.getTimeSource();
    proxyClientConnectionCloser = new ProxyClientConnectionCloser();
    leaseState = new LeaseState(timeSource, proxyClientConnectionCloser);
    leaseMonitorThread = new LeaseMonitorThread(timeSource, leaseState);
    leaseMonitorThread.start();

    LOGGER.info("LeaseServiceProvider initialized");

    return true;
  }

  @Override
  public <T> T getService(long consumerID, ServiceConfiguration<T> serviceConfiguration) {
    if (!(serviceConfiguration instanceof LeaseServiceConfiguration)) {
      throw new IllegalArgumentException("LeaseServiceProvider expected a LeaseServiceConfiguration, but a " + serviceConfiguration.getClass() + " was used by " + consumerID);
    }

    LOGGER.info("Creating LeaseService");

    LeaseServiceConfiguration leaseServiceConfiguration = (LeaseServiceConfiguration) serviceConfiguration;

    ClientConnectionCloser clientConnectionCloser = leaseServiceConfiguration.getClientConnectionCloser();
    LeaseService leaseService = createLeaseService(clientConnectionCloser);

    return serviceConfiguration.getServiceType().cast(leaseService);
  }

  private LeaseService createLeaseService(ClientConnectionCloser clientConnectionCloser) {
    // This ugly proxy nonsense is only here because services have no way to directly depend on other services.
    // Ideally, when LeaseState gets created, we would be able to get a ClientCommunicator directly.
    proxyClientConnectionCloser.setClientConnectionCloser(clientConnectionCloser);
    return new LeaseServiceImpl(leaseLength, leaseState);
  }

  @Override
  public Collection<Class<?>> getProvidedServiceTypes() {
    return Collections.singletonList(LeaseService.class);
  }

  @Override
  public void prepareForSynchronization() throws ServiceProviderCleanupException {
  }

  @Override
  public void close() {
    leaseMonitorThread.interrupt();
  }

  private static long getLeaseLength(ServiceProviderConfiguration configuration) {
    if (!(configuration instanceof LeaseConfiguration)) {
      LOGGER.info("No lease configuration provided. Instead configuration was: " + configuration);
      return withLeaseLengthLog(DEFAULT_LEASE_LENGTH);
    }

    LeaseConfiguration leaseConfiguration = (LeaseConfiguration) configuration;
    long leaseLength = leaseConfiguration.getLeaseLength();

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Found lease configuration with lease length: " + leaseLength);
    }

    if (leaseLength <= 0) {
      LOGGER.warn("Non-positive lease length: " + leaseLength + ", ignoring it");
      return withLeaseLengthLog(DEFAULT_LEASE_LENGTH);
    }

    if (leaseLength > MAX_LEASE_LENGTH) {
      LOGGER.warn("Excessive lease length: " + leaseLength + ", using smaller value: " + MAX_LEASE_LENGTH);
      return withLeaseLengthLog(MAX_LEASE_LENGTH);
    }

    return withLeaseLengthLog(leaseLength);
  }

  private static long withLeaseLengthLog(long leaseLength) {
    LOGGER.info("Using lease length of " + leaseLength + "ms");
    return leaseLength;
  }

  @Override
  public void addStateTo(StateDumpCollector stateDumper) {
    stateDumper.addState("LeaseLength", Long.toString(leaseLength));
    leaseState.addStateTo(stateDumper.subStateDumpCollector("LeaseState"));
  }
}
