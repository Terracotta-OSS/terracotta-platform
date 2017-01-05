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
package org.terracotta.management.entity.tms.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.entity.BasicServiceConfiguration;
import org.terracotta.entity.ClientCommunicator;
import org.terracotta.entity.ServiceRegistry;
import org.terracotta.management.entity.tms.TmsAgent;
import org.terracotta.management.entity.tms.TmsAgentConfig;
import org.terracotta.management.entity.tms.TmsAgentVersion;
import org.terracotta.management.model.call.ContextualCall;
import org.terracotta.management.model.message.Message;
import org.terracotta.management.service.monitoring.ActiveEntityMonitoringService;
import org.terracotta.management.service.monitoring.ActiveEntityMonitoringServiceConfiguration;
import org.terracotta.management.service.monitoring.ConsumerManagementRegistry;
import org.terracotta.management.service.monitoring.ConsumerManagementRegistryConfiguration;
import org.terracotta.management.service.monitoring.EntityEventListenerAdapter;
import org.terracotta.management.service.monitoring.EntityEventService;
import org.terracotta.management.service.monitoring.ManagementCallExecutor;
import org.terracotta.management.service.monitoring.ManagementService;
import org.terracotta.management.service.monitoring.ManagementServiceConfiguration;
import org.terracotta.management.service.monitoring.PassiveEntityMonitoringService;
import org.terracotta.management.service.monitoring.PassiveEntityMonitoringServiceConfiguration;
import org.terracotta.management.service.monitoring.SharedManagementRegistry;
import org.terracotta.monitoring.IMonitoringProducer;
import org.terracotta.voltron.proxy.SerializationCodec;
import org.terracotta.voltron.proxy.server.ProxyServerEntityService;

import java.util.Objects;

/**
 * @author Mathieu Carbou
 */
public class TmsAgentEntityServerService extends ProxyServerEntityService<TmsAgent, TmsAgentConfig, Void, Void, TmsAgentMessenger> {

  private static final Logger LOGGER = LoggerFactory.getLogger(TmsAgentEntityServerService.class);

  public TmsAgentEntityServerService() {
    super(TmsAgent.class, TmsAgentConfig.class, new Class<?>[]{Message.class}, null, null, TmsAgentMessenger.class);
    setCodec(new SerializationCodec());
  }

  @Override
  public ActiveTmsAgentServerEntity createActiveEntity(ServiceRegistry registry, TmsAgentConfig configuration) {
    LOGGER.trace("createActiveEntity()");

    // get services
    TmsAgentMessenger tmsAgentMessenger = createMessenger(registry);
    ClientCommunicator communicator = Objects.requireNonNull(registry.getService(new BasicServiceConfiguration<>(ClientCommunicator.class)));
    ManagementService managementService = Objects.requireNonNull(registry.getService(new ManagementServiceConfiguration(communicator, new ManagementCallExecutor() {
      @Override
      public void executeManagementCall(String managementCallIdentifier, ContextualCall<?> call) {
        LOGGER.trace("executeManagementCall({}, {})", managementCallIdentifier, call);
        tmsAgentMessenger.executeManagementCall(managementCallIdentifier, call);
      }
    })));
    ActiveEntityMonitoringService entityMonitoringService = Objects.requireNonNull(registry.getService(new ActiveEntityMonitoringServiceConfiguration()));
    ConsumerManagementRegistry consumerManagementRegistry = Objects.requireNonNull(registry.getService(new ConsumerManagementRegistryConfiguration(entityMonitoringService)
        .addServerManagementProviders()));
    SharedManagementRegistry sharedManagementRegistry = Objects.requireNonNull(registry.getService(new BasicServiceConfiguration<>(SharedManagementRegistry.class)));
    ActiveTmsAgent tmsAgent = new ActiveTmsAgent(configuration, managementService, consumerManagementRegistry, entityMonitoringService, sharedManagementRegistry);

    // workaround for https://github.com/Terracotta-OSS/terracotta-core/issues/426
    EntityEventService entityEventService = Objects.requireNonNull(registry.getService(new BasicServiceConfiguration<>(EntityEventService.class)));
    entityEventService.addEntityEventListener(new EntityEventListenerAdapter() {
      @Override
      public void onCreated() {
        tmsAgent.init();
      }
    });

    return new ActiveTmsAgentServerEntity(tmsAgent);
  }

  @Override
  protected PassiveTmsAgentServerEntity createPassiveEntity(ServiceRegistry registry, TmsAgentConfig configuration) {
    LOGGER.trace("createPassiveEntity()");
    IMonitoringProducer monitoringProducer = Objects.requireNonNull(registry.getService(new BasicServiceConfiguration<>(IMonitoringProducer.class)));
    PassiveEntityMonitoringService entityMonitoringService = Objects.requireNonNull(registry.getService(new PassiveEntityMonitoringServiceConfiguration(monitoringProducer)));
    ConsumerManagementRegistry consumerManagementRegistry = Objects.requireNonNull(registry.getService(new ConsumerManagementRegistryConfiguration(entityMonitoringService)
        .addServerManagementProviders()));
    SharedManagementRegistry sharedManagementRegistry = Objects.requireNonNull(registry.getService(new BasicServiceConfiguration<>(SharedManagementRegistry.class)));
    return new PassiveTmsAgentServerEntity(new PassiveTmsAgent(consumerManagementRegistry, entityMonitoringService, sharedManagementRegistry));
  }

  @Override
  public long getVersion() {
    return TmsAgentVersion.LATEST.version();
  }

  @Override
  public boolean handlesEntityType(String typeName) {
    return TmsAgentConfig.ENTITY_TYPE.equals(typeName);
  }


}
