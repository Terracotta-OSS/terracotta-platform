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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.entity.PlatformConfiguration;
import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.entity.ServiceProvider;
import org.terracotta.entity.ServiceProviderCleanupException;
import org.terracotta.entity.ServiceProviderConfiguration;
import org.terracotta.management.sequence.BoundaryFlakeSequenceGenerator;
import org.terracotta.management.sequence.NodeIdSource;
import org.terracotta.management.sequence.TimeSource;
import org.terracotta.management.service.monitoring.registry.OffHeapResourceBinding;
import org.terracotta.management.service.monitoring.registry.OffHeapResourceSettingsManagementProvider;
import org.terracotta.management.service.monitoring.registry.OffHeapResourceStatisticsManagementProvider;
import org.terracotta.monitoring.IStripeMonitoring;
import org.terracotta.offheapresource.OffHeapResourceIdentifier;
import org.terracotta.offheapresource.OffHeapResources;

import java.io.Closeable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

/**
 * @author Mathieu Carbou
 */
@BuiltinService
public class MonitoringServiceProvider implements ServiceProvider, Closeable {

  private static final Logger LOGGER = LoggerFactory.getLogger(MonitoringServiceProvider.class);

  private static final Collection<Class<?>> providedServiceTypes = Arrays.asList(
      IStripeMonitoring.class, // for platform
      SharedManagementRegistry.class, // access all registries
      ConsumerManagementRegistry.class, // registry for an entity
      ClientMonitoringService.class, // for management entity
      ManagementService.class, // for TMS Entity
      ActiveEntityMonitoringService.class, // monitoring of an active entity
      PassiveEntityMonitoringService.class // monitoring of a passive entity
  );

  private final Map<DefaultManagementService, Boolean> managementServices = new ConcurrentWeakIdentityHashMap<>();
  private final Map<DefaultClientMonitoringService, Boolean> clientMonitoringServices = new ConcurrentWeakIdentityHashMap<>();
  private final TimeSource timeSource = TimeSource.BEST;
  private final DefaultSharedManagementRegistry sharedManagementRegistry = new DefaultSharedManagementRegistry();
  private final BoundaryFlakeSequenceGenerator sequenceGenerator = new BoundaryFlakeSequenceGenerator(timeSource, NodeIdSource.BEST);
  private final StatisticsServiceFactory statisticsServiceFactory = new StatisticsServiceFactory(sharedManagementRegistry, timeSource);

  private PlatformConfiguration platformConfiguration;
  private DefaultEventService eventService;
  private TopologyService topologyService;
  private IStripeMonitoring platformListenerAdapter;

  @Override
  public Collection<Class<?>> getProvidedServiceTypes() {
    return providedServiceTypes;
  }

  @Override
  public void prepareForSynchronization() throws ServiceProviderCleanupException {
  }

  @Override
  public boolean initialize(ServiceProviderConfiguration configuration, PlatformConfiguration platformConfiguration) {
    this.platformConfiguration = platformConfiguration;
    this.eventService = new DefaultEventService(sequenceGenerator, platformConfiguration, sharedManagementRegistry, managementServices, clientMonitoringServices);
    this.topologyService = new TopologyService(eventService, timeSource, platformConfiguration);
    this.platformListenerAdapter = new IStripeMonitoringPlatformListenerAdapter(topologyService);
    return true;
  }

  @Override
  public void close() {
    this.statisticsServiceFactory.close();
    this.eventService.close();
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T getService(long consumerID, ServiceConfiguration<T> configuration) {
    Class<T> serviceType = configuration.getServiceType();

    // for platform, which requests either a IStripeMonitoring to send platform events or a IStripeMonitoring to send callbacks from passive entities
    if (IStripeMonitoring.class == serviceType) {
      if (consumerID == PLATFORM_CONSUMER_ID) {
        return serviceType.cast(platformListenerAdapter);
      } else {
        DataListener dataListener = new DefaultDataListener(consumerID, topologyService, eventService);
        return serviceType.cast(new IStripeMonitoringDataListenerAdapter(consumerID, dataListener));
      }
    }

    // get or create a shared registry used to do aggregated operations on all consumer registries (i.e. management calls)
    if (SharedManagementRegistry.class == serviceType) {
      return serviceType.cast(sharedManagementRegistry);
    }

    // get or creates a registry specific to this entity to handle stats and management calls
    if (ConsumerManagementRegistry.class == serviceType) {
      if (configuration instanceof ConsumerManagementRegistryConfiguration) {
        ConsumerManagementRegistryConfiguration consumerManagementRegistryConfiguration = (ConsumerManagementRegistryConfiguration) configuration;
        StatisticsService statisticsService = statisticsServiceFactory.createStatisticsService(consumerManagementRegistryConfiguration.getStatisticConfiguration());
        ConsumerManagementRegistry consumerManagementRegistry = sharedManagementRegistry.getOrCreateConsumerManagementRegistry(
            consumerID,
            consumerManagementRegistryConfiguration.getEntityMonitoringService(),
            statisticsService);
        if (consumerManagementRegistryConfiguration.wantsServerManagementProviders()) {
          addServerManagementProviders(consumerID, consumerManagementRegistry);
        }
        return serviceType.cast(consumerManagementRegistry);
      } else {
        throw new IllegalArgumentException("Missing configuration " + ConsumerManagementRegistryConfiguration.class.getSimpleName() + " when requesting service " + serviceType.getName());
      }
    }

    // get or creates a client-side monitoring service
    if (ClientMonitoringService.class == serviceType) {
      if (configuration instanceof ClientMonitoringServiceConfiguration) {
        if (!topologyService.isCurrentServerActive()) {
          throw new IllegalStateException("Server " + platformConfiguration.getServerName() + " is not active!");
        }
        ClientMonitoringServiceConfiguration clientMonitoringServiceConfiguration = (ClientMonitoringServiceConfiguration) configuration;
        DefaultClientMonitoringService clientMonitoringService = new DefaultClientMonitoringService(
            consumerID,
            topologyService,
            eventService,
            clientMonitoringServiceConfiguration.getClientCommunicator());
        clientMonitoringServices.put(clientMonitoringService, Boolean.TRUE);
        return serviceType.cast(clientMonitoringService);
      } else {
        throw new IllegalArgumentException("Missing configuration " + ClientMonitoringServiceConfiguration.class.getSimpleName() + " when requesting service " + serviceType.getName());
      }
    }

    // get or creates a monitoring accessor service (for tms)
    if (ManagementService.class == serviceType) {
      if (configuration instanceof ManagementServiceConfiguration) {
        if (!topologyService.isCurrentServerActive()) {
          throw new IllegalStateException("Server " + platformConfiguration.getServerName() + " is not active!");
        }
        ManagementServiceConfiguration managementServiceConfiguration = (ManagementServiceConfiguration) configuration;
        DefaultManagementService managementService = new DefaultManagementService(
            consumerID,
            topologyService,
            eventService,
            managementServiceConfiguration.getClientCommunicator(),
            sequenceGenerator);
        managementServices.put(managementService, Boolean.TRUE);
        return serviceType.cast(managementService);
      } else {
        throw new IllegalArgumentException("Missing configuration " + ManagementServiceConfiguration.class.getSimpleName() + " when requesting service " + serviceType.getName());
      }
    }

    // get or creates a monitoring service for an active entity
    if (ActiveEntityMonitoringService.class == serviceType) {
      if (configuration instanceof ActiveEntityMonitoringServiceConfiguration) {
        if (!topologyService.isCurrentServerActive()) {
          throw new IllegalStateException("Server " + platformConfiguration.getServerName() + " is not active!");
        }
        ActiveEntityMonitoringServiceConfiguration activeEntityMonitoringServiceConfiguration = (ActiveEntityMonitoringServiceConfiguration) configuration;
        DefaultActiveEntityMonitoringService activeEntityMonitoringService = new DefaultActiveEntityMonitoringService(
            consumerID,
            topologyService,
            eventService);
        return serviceType.cast(activeEntityMonitoringService);
      } else {
        throw new IllegalArgumentException("Missing configuration " + ActiveEntityMonitoringServiceConfiguration.class.getSimpleName() + " when requesting service " + serviceType.getName());
      }
    }

    // get or creates a monitoring service for a passive entity, bridging calls to IMonitoringProducer
    if (PassiveEntityMonitoringService.class == serviceType) {
      if (configuration instanceof PassiveEntityMonitoringServiceConfiguration) {
        if (!topologyService.isCurrentServerActive()) {
          throw new IllegalStateException("Server " + platformConfiguration.getServerName() + " is not passive!");
        }
        PassiveEntityMonitoringServiceConfiguration passiveEntityMonitoringServiceConfiguration = (PassiveEntityMonitoringServiceConfiguration) configuration;
        DefaultPassiveEntityMonitoringService passiveEntityMonitoringService = new DefaultPassiveEntityMonitoringService(
            consumerID,
            passiveEntityMonitoringServiceConfiguration.getMonitoringProducer());
        return serviceType.cast(passiveEntityMonitoringService);
      } else {
        throw new IllegalArgumentException("Missing configuration " + PassiveEntityMonitoringServiceConfiguration.class.getSimpleName() + " when requesting service " + serviceType.getName());
      }
    }

    throw new IllegalStateException("Unable to provide service " + serviceType.getName() + " to consumerID: " + consumerID);
  }

  private void addServerManagementProviders(long consumerId, ConsumerManagementRegistry consumerManagementRegistry) {
    LOGGER.trace("[{}] addServerManagementProviders()", consumerId);
    // manage offheap service if it is there
    Collection<OffHeapResources> offHeapResources = platformConfiguration.getExtendedConfiguration(OffHeapResources.class);
    if (!offHeapResources.isEmpty()) {
      consumerManagementRegistry.addManagementProvider(new OffHeapResourceSettingsManagementProvider());
      consumerManagementRegistry.addManagementProvider(new OffHeapResourceStatisticsManagementProvider());
      for (OffHeapResources offHeapResource : offHeapResources) {
        for (OffHeapResourceIdentifier identifier : offHeapResource.getAllIdentifiers()) {
          LOGGER.trace("[{}] addServerManagementProviders() OffHeapResource:{}", consumerId, identifier.getName());
          consumerManagementRegistry.register(new OffHeapResourceBinding(identifier.getName(), offHeapResource.getOffHeapResource(identifier)));
        }
      }
    }
  }

}
