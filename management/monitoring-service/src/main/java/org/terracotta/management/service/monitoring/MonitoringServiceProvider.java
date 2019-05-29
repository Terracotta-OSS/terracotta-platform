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
import org.terracotta.entity.ServiceProviderConfiguration;
import org.terracotta.entity.StateDumpCollector;
import org.terracotta.management.sequence.BoundaryFlakeSequenceGenerator;
import org.terracotta.management.sequence.NodeIdSource;
import org.terracotta.management.sequence.TimeSource;
import org.terracotta.monitoring.IMonitoringProducer;
import org.terracotta.monitoring.IStripeMonitoring;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

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
      ManagementService.class // for NMS Entity
  );

  private final TimeSource timeSource = TimeSource.BEST;

  private final DefaultSharedEntityManagementRegistry sharedManagementRegistry = new DefaultSharedEntityManagementRegistry();
  private final BoundaryFlakeSequenceGenerator sequenceGenerator = new BoundaryFlakeSequenceGenerator(timeSource, NodeIdSource.BEST);
  private final DefaultStatisticService statisticService = new DefaultStatisticService(sharedManagementRegistry, timeSource);
  private final DefaultFiringService firingService = new DefaultFiringService(sequenceGenerator);

  private TopologyService topologyService;
  private IStripeMonitoring platformListenerAdapter;
  private DefaultManagementDataListener managementDataListener;
  private Collection<ManageableServerComponent> manageablePlugins;

  @Override
  public Collection<Class<?>> getProvidedServiceTypes() {
    return providedServiceTypes;
  }

  @Override
  public void prepareForSynchronization() {
  }

  @Override
  public boolean initialize(ServiceProviderConfiguration configuration, PlatformConfiguration platformConfiguration) {
    this.manageablePlugins = platformConfiguration.getExtendedConfiguration(ManageableServerComponent.class);
    this.topologyService = new TopologyService(firingService, platformConfiguration);
    this.platformListenerAdapter = new IStripeMonitoringPlatformListenerAdapter(topologyService);
    this.managementDataListener = new DefaultManagementDataListener(topologyService, firingService);
    this.topologyService.addTopologyEventListener(managementDataListener);
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
  public <T> T getService(long consumerID, ServiceConfiguration<T> configuration) {
    Class<T> serviceType = configuration.getServiceType();

    // for platform, which requests either a IStripeMonitoring to send platform events or a IStripeMonitoring to send callbacks from passive entities
    if (IStripeMonitoring.class == serviceType) {
      if (consumerID == PLATFORM_CONSUMER_ID) {
        return serviceType.cast(platformListenerAdapter);
      } else {
        return serviceType.cast(new IStripeMonitoringDataListenerAdapter(consumerID, managementDataListener));
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

    // get or creates a registry specific to this entity to handle stats and management calls
    if (EntityManagementRegistry.class == serviceType) {
      if (configuration instanceof AbstractManagementRegistryConfiguration) {
        AbstractManagementRegistryConfiguration managementRegistryConfiguration = (AbstractManagementRegistryConfiguration) configuration;
        boolean activeEntity = managementRegistryConfiguration.isActive();

        LOGGER.trace("[{}] getService({}) isActive={}, config={}", consumerID, EntityManagementRegistry.class.getSimpleName(), activeEntity, configuration.getClass().getSimpleName());

        // create an active or passive monitoring service
        IMonitoringProducer monitoringProducer = managementRegistryConfiguration.getMonitoringProducer();
        DefaultEntityMonitoringService entityMonitoringService = new DefaultEntityMonitoringService(consumerID, monitoringProducer, topologyService, activeEntity);

        // create a registry for the entity
        DefaultEntityManagementRegistry managementRegistry = new DefaultEntityManagementRegistry(consumerID, entityMonitoringService, timeSource);

        // ensure voltron's monitoring tree is created or reseted on entity creation or failover
        managementRegistry.onEntityCreated(entityMonitoringService::init);
        managementRegistry.onEntityPromotionCompleted(entityMonitoringService::init);

        // make the registry listen for topology events
        topologyService.addTopologyEventListener(managementRegistry);
        managementRegistry.onClose(() -> topologyService.removeTopologyEventListener(managementRegistry));

        // previous entity registry that might have been created by the passive entity, in case it gets promoted to active
        Optional<EntityManagementRegistry> previous = Optional.empty();

        if (configuration instanceof EntityManagementRegistryConfiguration) {
          // here, this is the case of a normal entity that wants to expose management stuff
          previous = sharedManagementRegistry.addEntityManagementRegistry(managementRegistry);
          managementRegistry.onClose(() -> sharedManagementRegistry.removeEntityManagementRegistry(managementRegistry));

        } else if (configuration instanceof ServerManagementRegistryConfiguration) {
          // here, this is the case of an entity that wants to both expose management stuff, plus contains server-side management 
          // information, plus a statistics collector
          previous = sharedManagementRegistry.addServerManagementRegistry(managementRegistry);
          managementRegistry.onClose(() -> sharedManagementRegistry.removeServerManagementRegistry(managementRegistry));

          // add additional management providers from server plugins and services
          List<ManageableServerComponent> manageableVoltronComponents = new ArrayList<>();
          ServerManagementRegistryConfiguration serverManagementRegistryConfiguration = (ServerManagementRegistryConfiguration) configuration;
          manageableVoltronComponents.addAll(serverManagementRegistryConfiguration.getManageableVoltronComponents());
          manageableVoltronComponents.addAll(manageablePlugins);
          manageableVoltronComponents.forEach(manageableServerComponent -> manageableServerComponent.onManagementRegistryCreated(managementRegistry));
          managementRegistry.onClose(() -> manageableVoltronComponents.forEach(manageableServerComponent -> manageableServerComponent.onManagementRegistryClose(managementRegistry)));

          // add a statistics collector
          statisticService.addStatisticCollector(managementRegistry);
        }

        // if we found a previously existing registry, it means that the current passive entity that was existing is being promoting
        // as an active entity. So when this will be completed, we will close the passive entity registry, which will remove it from
        // the shared management registry where statistics are collected, and will also close any running statistics collector on this
        // registry if it is a server management registry.
        previous.ifPresent(passiveRegistry -> managementRegistry.onEntityPromotionCompleted(passiveRegistry::close));

        return serviceType.cast(managementRegistry);
      } else {
        throw new IllegalArgumentException("Missing configuration of type " + AbstractManagementRegistryConfiguration.class.getSimpleName() + " when requesting service " + serviceType.getName());
      }
    }

    throw new IllegalStateException("Unable to provide service " + serviceType.getName() + " to consumerID: " + consumerID);
  }
}
