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
import org.terracotta.monitoring.IMonitoringProducer;
import org.terracotta.monitoring.IStripeMonitoring;

import java.io.Closeable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
      ClientMonitoringService.class, // for NMS Agent Entity
      ManagementService.class, // for NMS Entity
      EntityMonitoringService.class // monitoring of an active or passive entity
  );

  private final Map<Long, DefaultManagementService> managementServices = new ConcurrentHashMap<>();
  private final Map<Long, DefaultClientMonitoringService> clientMonitoringServices = new ConcurrentHashMap<>();
  private final Map<Long, DefaultConsumerManagementRegistry> consumerManagementRegistries = new ConcurrentHashMap<>();
  private final Map<Long, DefaultActiveEntityMonitoringService> activeEntityMonitoringServices = new ConcurrentHashMap<>();
  private final Map<Long, DefaultPassiveEntityMonitoringService> passiveEntityMonitoringServices = new ConcurrentHashMap<>();

  private final TimeSource timeSource = TimeSource.BEST;
  private final DefaultSharedManagementRegistry sharedManagementRegistry = new DefaultSharedManagementRegistry(consumerManagementRegistries);
  private final BoundaryFlakeSequenceGenerator sequenceGenerator = new BoundaryFlakeSequenceGenerator(timeSource, NodeIdSource.BEST);
  private final DefaultStatisticService statisticsService = new DefaultStatisticService(sharedManagementRegistry);
  private final DefaultFiringService firingService = new DefaultFiringService(sequenceGenerator, managementServices, clientMonitoringServices);

  private PlatformConfiguration platformConfiguration;
  private TopologyService topologyService;
  private IStripeMonitoring platformListenerAdapter;
  private ManagementPlugins managementPlugins;

  public MonitoringServiceProvider() {
    // because only passthrough is calling close(), not tc-core, so this is to cleanly close services (thread pools) at shutdown
    Runtime.getRuntime().addShutdownHook(new Thread(this::close));
  }

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
    this.managementPlugins = new ManagementPlugins(platformConfiguration, statisticsService);
    this.topologyService = new TopologyService(firingService, platformConfiguration);
    this.platformListenerAdapter = new IStripeMonitoringPlatformListenerAdapter(topologyService);
    this.topologyService.addTopologyEventListener(new TopologyEventListenerAdapter() {
      @Override
      public void onEntityDestroyed(long consumerId) {
        LOGGER.trace("[{}] onEntityDestroyed()", consumerId);
        topologyService.removeTopologyEventListener(managementServices.remove(consumerId));
        topologyService.removeTopologyEventListener(clientMonitoringServices.remove(consumerId));
        topologyService.removeTopologyEventListener(consumerManagementRegistries.remove(consumerId));
        passiveEntityMonitoringServices.remove(consumerId);
        activeEntityMonitoringServices.remove(consumerId);
      }
    });
    return true;
  }

  @Override
  public void close() {
    this.statisticsService.close();
  }

  @SuppressWarnings("unchecked")
  @Override
  public synchronized <T> T getService(long consumerID, ServiceConfiguration<T> configuration) {
    Class<T> serviceType = configuration.getServiceType();

    // for platform, which requests either a IStripeMonitoring to send platform events or a IStripeMonitoring to send callbacks from passive entities
    if (IStripeMonitoring.class == serviceType) {
      if (consumerID == PLATFORM_CONSUMER_ID) {
        return serviceType.cast(platformListenerAdapter);
      } else {
        DataListener dataListener = new DefaultDataListener(consumerID, topologyService, firingService);
        return serviceType.cast(new IStripeMonitoringDataListenerAdapter(consumerID, dataListener));
      }
    }

    // get or create a shared registry used to do aggregated operations on all consumer registries (i.e. management calls)
    if (SharedManagementRegistry.class == serviceType) {
      LOGGER.trace("[{}] getService({})", consumerID, SharedManagementRegistry.class.getSimpleName());
      return serviceType.cast(sharedManagementRegistry);
    }

    // get or creates a registry specific to this entity to handle stats and management calls
    if (ConsumerManagementRegistry.class == serviceType) {
      if (configuration instanceof ConsumerManagementRegistryConfiguration) {
        ConsumerManagementRegistryConfiguration consumerManagementRegistryConfiguration = (ConsumerManagementRegistryConfiguration) configuration;
        EntityMonitoringService monitoringService = consumerManagementRegistryConfiguration.getEntityMonitoringService();
        // Workaround for https://github.com/Terracotta-OSS/terracotta-core/issues/409
        // in a failover, we are not aware of passive entity destruction so if we find a previous service with the same consumer id, we clean it
        // this is true for this service specifically
        DefaultConsumerManagementRegistry managementRegistry = consumerManagementRegistries.remove(consumerID);
        if (managementRegistry != null) {
          LOGGER.trace("[{}] getService({}): clearing previous instance", consumerID, ConsumerManagementRegistry.class.getSimpleName());
          topologyService.removeTopologyEventListener(managementRegistry);
          managementRegistry.clear();
        }
        // create a new registry
        LOGGER.trace("[{}] getService({})", consumerID, ConsumerManagementRegistry.class.getSimpleName());
        managementRegistry = new DefaultConsumerManagementRegistry(
            consumerID,
            monitoringService);
        if (consumerManagementRegistryConfiguration.wantsServerManagementProviders()) {
          managementPlugins.registerServerManagementProviders(consumerID, managementRegistry, monitoringService);
        }
        topologyService.addTopologyEventListener(managementRegistry);
        consumerManagementRegistries.put(consumerID, managementRegistry);
        return serviceType.cast(managementRegistry);
      } else {
        throw new IllegalArgumentException("Missing configuration " + ConsumerManagementRegistryConfiguration.class.getSimpleName() + " when requesting service " + serviceType.getName());
      }
    }

    // get or creates a client-side monitoring service
    if (ClientMonitoringService.class == serviceType) {
      if (configuration instanceof ClientMonitoringServiceConfiguration) {
        DefaultClientMonitoringService clientMonitoringService = clientMonitoringServices.get(consumerID);
        if (clientMonitoringService == null) {
          LOGGER.trace("[{}] getService({})", consumerID, ClientMonitoringService.class.getSimpleName());
          ClientMonitoringServiceConfiguration clientMonitoringServiceConfiguration = (ClientMonitoringServiceConfiguration) configuration;
          clientMonitoringService = new DefaultClientMonitoringService(
              consumerID,
              topologyService,
              firingService,
              clientMonitoringServiceConfiguration.getClientCommunicator());
          topologyService.addTopologyEventListener(clientMonitoringService);
          clientMonitoringServices.put(consumerID, clientMonitoringService);
        } else {
          LOGGER.trace("[{}] getService({}): re-using.", consumerID, ClientMonitoringService.class.getSimpleName());
        }
        return serviceType.cast(clientMonitoringService);
      } else {
        throw new IllegalArgumentException("Missing configuration " + ClientMonitoringServiceConfiguration.class.getSimpleName() + " when requesting service " + serviceType.getName());
      }
    }

    // get or creates a monitoring accessor service (for tms)
    if (ManagementService.class == serviceType) {
      if (configuration instanceof ManagementServiceConfiguration) {
        DefaultManagementService managementService = managementServices.get(consumerID);
        if (managementService == null) {
          LOGGER.trace("[{}] getService({})", consumerID, ManagementService.class.getSimpleName());
          ManagementServiceConfiguration managementServiceConfiguration = (ManagementServiceConfiguration) configuration;
          managementService = new DefaultManagementService(
              consumerID,
              topologyService,
              firingService);
          topologyService.addTopologyEventListener(managementService);
          managementServices.put(consumerID, managementService);
        } else {
          LOGGER.trace("[{}] getService({}): re-using.", consumerID, ManagementService.class.getSimpleName());
        }
        return serviceType.cast(managementService);
      } else {
        throw new IllegalArgumentException("Missing configuration " + ManagementServiceConfiguration.class.getSimpleName() + " when requesting service " + serviceType.getName());
      }
    }

    // get or creates a monitoring service for an active or passive entity
    if (EntityMonitoringService.class == serviceType) {

      // active case
      if (configuration instanceof ActiveEntityMonitoringServiceConfiguration) {
        DefaultActiveEntityMonitoringService activeEntityMonitoringService = this.activeEntityMonitoringServices.get(consumerID);
        if (activeEntityMonitoringService == null) {
          LOGGER.trace("[{}] getService({})", consumerID, EntityMonitoringService.class.getSimpleName());
          ActiveEntityMonitoringServiceConfiguration activeEntityMonitoringServiceConfiguration = (ActiveEntityMonitoringServiceConfiguration) configuration;
          activeEntityMonitoringService = new DefaultActiveEntityMonitoringService(
              consumerID,
              topologyService,
              firingService,
              platformConfiguration);
          activeEntityMonitoringServices.put(consumerID, activeEntityMonitoringService);
        } else {
          LOGGER.trace("[{}] getService({}): re-using.", consumerID, EntityMonitoringService.class.getSimpleName());
        }
        return serviceType.cast(activeEntityMonitoringService);

        // passive case
      } else if (configuration instanceof PassiveEntityMonitoringServiceConfiguration) {
        DefaultPassiveEntityMonitoringService passiveEntityMonitoringService = passiveEntityMonitoringServices.get(consumerID);
        if (passiveEntityMonitoringService == null) {
          LOGGER.trace("[{}] getService({})", consumerID, EntityMonitoringService.class.getSimpleName());
          PassiveEntityMonitoringServiceConfiguration passiveEntityMonitoringServiceConfiguration = (PassiveEntityMonitoringServiceConfiguration) configuration;
          IMonitoringProducer monitoringProducer = passiveEntityMonitoringServiceConfiguration.getMonitoringProducer();
          if (monitoringProducer == null) {
            LOGGER.warn("Platform service " + IMonitoringProducer.class.getSimpleName() + " is not accessible.");
            return null;
          }
          passiveEntityMonitoringService = new DefaultPassiveEntityMonitoringService(consumerID, monitoringProducer, platformConfiguration);
          passiveEntityMonitoringServices.put(consumerID, passiveEntityMonitoringService);
        } else {
          LOGGER.trace("[{}] getService({}): re-using.", consumerID, EntityMonitoringService.class.getSimpleName());
        }
        return serviceType.cast(passiveEntityMonitoringService);
      } else {
        throw new IllegalArgumentException("Missing configuration " + PassiveEntityMonitoringServiceConfiguration.class.getSimpleName() + " when requesting service " + serviceType.getName());
      }
    }

    throw new IllegalStateException("Unable to provide service " + serviceType.getName() + " to consumerID: " + consumerID);
  }

}
