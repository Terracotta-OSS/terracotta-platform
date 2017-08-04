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
import org.terracotta.entity.StateDumpCollector;
import org.terracotta.entity.StateDumpable;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.model.context.ContextContainer;
import org.terracotta.management.model.stats.ContextualStatistics;
import org.terracotta.management.registry.collect.StatisticCollector;
import org.terracotta.management.sequence.BoundaryFlakeSequenceGenerator;
import org.terracotta.management.sequence.NodeIdSource;
import org.terracotta.management.sequence.TimeSource;
import org.terracotta.management.service.monitoring.registry.provider.StatisticCollectorManagementProvider;
import org.terracotta.monitoring.IMonitoringProducer;
import org.terracotta.monitoring.IStripeMonitoring;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Mathieu Carbou
 */
@BuiltinService
public class MonitoringServiceProvider implements ServiceProvider, Closeable {

  private static final Logger LOGGER = LoggerFactory.getLogger(MonitoringServiceProvider.class);

  private static final Collection<Class<?>> providedServiceTypes = Arrays.asList(
      IStripeMonitoring.class, // for platform
      SharedEntityManagementRegistry.class, // access all registries
      EntityManagementRegistry.class, // registry for an entity
      ClientMonitoringService.class, // for NMS Agent Entity
      ManagementService.class, // for NMS Entity
      ManageableServerComponent.class // provides server-level capabilities
  );

  private final TimeSource timeSource = TimeSource.BEST;
  private final DefaultSharedEntityManagementRegistry sharedManagementRegistry = new DefaultSharedEntityManagementRegistry();
  private final BoundaryFlakeSequenceGenerator sequenceGenerator = new BoundaryFlakeSequenceGenerator(timeSource, NodeIdSource.BEST);
  private final DefaultStatisticService statisticService = new DefaultStatisticService(sharedManagementRegistry);
  private final DefaultFiringService firingService = new DefaultFiringService(sequenceGenerator);

  private PlatformConfiguration platformConfiguration;
  private TopologyService topologyService;
  private IStripeMonitoring platformListenerAdapter;
  private Collection<ManageableServerComponent> manageablePlugins;

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
    this.manageablePlugins = platformConfiguration.getExtendedConfiguration(ManageableServerComponent.class);
    this.topologyService = new TopologyService(firingService, platformConfiguration);
    this.platformListenerAdapter = new IStripeMonitoringPlatformListenerAdapter(topologyService);
    return true;
  }

  @Override
  public void addStateTo(StateDumpCollector dump) {
    TopologyService topologyService = this.topologyService;
    if (topologyService != null) {
      dump.addState("cluster", topologyService.getClusterCopy().toMap());
    }
  }

  @Override
  public void close() {
    this.statisticService.close();
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
    if (SharedEntityManagementRegistry.class == serviceType) {
      LOGGER.trace("[{}] getService({})", consumerID, SharedEntityManagementRegistry.class.getSimpleName());
      return serviceType.cast(sharedManagementRegistry);
    }

    // get or creates a client-side monitoring service
    if (ClientMonitoringService.class == serviceType) {
      if (configuration instanceof ClientMonitoringServiceConfiguration) {
        LOGGER.trace("[{}] getService({})", consumerID, ClientMonitoringService.class.getSimpleName());
        ClientMonitoringServiceConfiguration clientMonitoringServiceConfiguration = (ClientMonitoringServiceConfiguration) configuration;
        DefaultClientMonitoringService clientMonitoringService = new DefaultClientMonitoringService(
            consumerID,
            topologyService,
            firingService,
            clientMonitoringServiceConfiguration.getClientCommunicator());
        return serviceType.cast(clientMonitoringService);
      } else {
        throw new IllegalArgumentException("Missing configuration " + ClientMonitoringServiceConfiguration.class.getSimpleName() + " when requesting service " + serviceType.getName());
      }
    }

    // get or creates a monitoring accessor service (for tms)
    if (ManagementService.class == serviceType) {
      if (configuration instanceof ManagementServiceConfiguration) {
        LOGGER.trace("[{}] getService({})", consumerID, ManagementService.class.getSimpleName());
        DefaultManagementService managementService = new DefaultManagementService(consumerID, topologyService, firingService);
        return serviceType.cast(managementService);
      } else {
        throw new IllegalArgumentException("Missing configuration " + ManagementServiceConfiguration.class.getSimpleName() + " when requesting service " + serviceType.getName());
      }
    }

    if (ManageableServerComponent.class == serviceType) {
      return serviceType.cast(new ManageableServerComponent() {
        @Override
        public void onManagementRegistryCreated(EntityManagementRegistry registry) {
          LOGGER.trace("[{}] onManagementRegistryCreated({})", registry.getMonitoringService().getConsumerId());

          // The context for the collector is created from the the registry of the entity wanting server-side providers.
          // We create a provider that will receive management calls to control the global voltron's statistic collector.
          // This provider will thus be on top of the entity wanting to collect server-side stats
          ContextContainer contextContainer = registry.getContextContainer();
          Context context = Context.create(contextContainer.getName(), contextContainer.getValue());
          StatisticCollectorManagementProvider collectorManagementProvider = new StatisticCollectorManagementProvider(context);
          registry.addManagementProvider(collectorManagementProvider);

          EntityMonitoringService monitoringService = registry.getMonitoringService();
          // add a collector service, not started by default, but that can be started through a remote management call
          StatisticCollector statisticCollector = statisticService.createStatisticCollector(statistics -> monitoringService.pushStatistics(statistics.toArray(new ContextualStatistics[statistics.size()])));
          registry.register(statisticCollector);
        }

        @Override
        public void onManagementRegistryClose(EntityManagementRegistry registry) {
        }
      });
    }

    // get or creates a registry specific to this entity to handle stats and management calls
    if (EntityManagementRegistry.class == serviceType) {
      if (configuration instanceof ManagementRegistryConfiguration) {
        ManagementRegistryConfiguration managementRegistryConfiguration = (ManagementRegistryConfiguration) configuration;

        EntityMonitoringService entityMonitoringService;
        if (managementRegistryConfiguration.isActive()) {
          entityMonitoringService = new DefaultActiveEntityMonitoringService(consumerID, topologyService, firingService, platformConfiguration);
        } else {
          IMonitoringProducer monitoringProducer = managementRegistryConfiguration.getMonitoringProducer();
          entityMonitoringService = new DefaultPassiveEntityMonitoringService(consumerID, monitoringProducer, platformConfiguration);
        }

        Collection<ManageableServerComponent> manageableServerComponents = new ArrayList<>();
        if (managementRegistryConfiguration.wantsServerLevelCapabilities()) {
          manageableServerComponents.addAll(managementRegistryConfiguration.getManageableVoltronComponents());
          manageableServerComponents.addAll(manageablePlugins);
        }

        LOGGER.trace("[{}] getService({})", consumerID, EntityManagementRegistry.class.getSimpleName());
        DefaultEntityManagementRegistry managementRegistry = new DefaultEntityManagementRegistry(consumerID, entityMonitoringService, sharedManagementRegistry, topologyService, manageableServerComponents);
        manageableServerComponents.forEach(manageableServerComponent -> manageableServerComponent.onManagementRegistryCreated(managementRegistry));
        return serviceType.cast(managementRegistry);
      } else {
        throw new IllegalArgumentException("Missing configuration " + ManagementRegistryConfiguration.class.getSimpleName() + " when requesting service " + serviceType.getName());
      }
    }

    throw new IllegalStateException("Unable to provide service " + serviceType.getName() + " to consumerID: " + consumerID);
  }
}
