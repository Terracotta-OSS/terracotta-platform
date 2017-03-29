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
package org.terracotta.management.entity.nms.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.entity.BasicServiceConfiguration;
import org.terracotta.entity.ServiceRegistry;
import org.terracotta.management.entity.nms.Nms;
import org.terracotta.management.entity.nms.NmsConfig;
import org.terracotta.management.entity.nms.NmsVersion;
import org.terracotta.management.model.message.Message;
import org.terracotta.management.service.monitoring.ActiveEntityMonitoringServiceConfiguration;
import org.terracotta.management.service.monitoring.ConsumerManagementRegistry;
import org.terracotta.management.service.monitoring.ConsumerManagementRegistryConfiguration;
import org.terracotta.management.service.monitoring.EntityMonitoringService;
import org.terracotta.management.service.monitoring.ManagementService;
import org.terracotta.management.service.monitoring.ManagementServiceConfiguration;
import org.terracotta.management.service.monitoring.PassiveEntityMonitoringServiceConfiguration;
import org.terracotta.management.service.monitoring.SharedManagementRegistry;
import org.terracotta.monitoring.IMonitoringProducer;
import org.terracotta.voltron.proxy.SerializationCodec;
import org.terracotta.voltron.proxy.server.ProxyServerEntityService;

import java.util.Objects;

/**
 * @author Mathieu Carbou
 */
public class NmsEntityServerService extends ProxyServerEntityService<NmsConfig, Void, Void, NmsCallback> {

  private static final Logger LOGGER = LoggerFactory.getLogger(NmsEntityServerService.class);

  public NmsEntityServerService() {
    super(Nms.class, NmsConfig.class, new Class<?>[]{Message.class}, null, null, NmsCallback.class);
    setCodec(new SerializationCodec());
  }

  @Override
  public ActiveNmsServerEntity createActiveEntity(ServiceRegistry registry, NmsConfig configuration) {
    LOGGER.trace("createActiveEntity()");
    // get services
    ManagementService managementService = Objects.requireNonNull(registry.getService(new ManagementServiceConfiguration()));
    EntityMonitoringService entityMonitoringService = Objects.requireNonNull(registry.getService(new ActiveEntityMonitoringServiceConfiguration()));
    ConsumerManagementRegistry consumerManagementRegistry = Objects.requireNonNull(registry.getService(new ConsumerManagementRegistryConfiguration(entityMonitoringService)
        .addServerManagementProviders()));
    SharedManagementRegistry sharedManagementRegistry = Objects.requireNonNull(registry.getService(new BasicServiceConfiguration<>(SharedManagementRegistry.class)));
    ActiveNmsServerEntity entity = new ActiveNmsServerEntity(configuration, managementService, consumerManagementRegistry, entityMonitoringService, sharedManagementRegistry);
    managementService.setManagementExecutor(entity);
    return entity;
  }

  @Override
  protected PassiveNmsServerEntity createPassiveEntity(ServiceRegistry registry, NmsConfig configuration) {
    LOGGER.trace("createPassiveEntity()");
    IMonitoringProducer monitoringProducer = Objects.requireNonNull(registry.getService(new BasicServiceConfiguration<>(IMonitoringProducer.class)));
    EntityMonitoringService entityMonitoringService = Objects.requireNonNull(registry.getService(new PassiveEntityMonitoringServiceConfiguration(monitoringProducer)));
    ConsumerManagementRegistry consumerManagementRegistry = Objects.requireNonNull(registry.getService(new ConsumerManagementRegistryConfiguration(entityMonitoringService)
        .addServerManagementProviders()));
    SharedManagementRegistry sharedManagementRegistry = Objects.requireNonNull(registry.getService(new BasicServiceConfiguration<>(SharedManagementRegistry.class)));
    return new PassiveNmsServerEntity(consumerManagementRegistry, entityMonitoringService, sharedManagementRegistry);
  }

  @Override
  public long getVersion() {
    return NmsVersion.LATEST.version();
  }

  @Override
  public boolean handlesEntityType(String typeName) {
    return NmsConfig.ENTITY_TYPE.equals(typeName);
  }


}
