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
import org.terracotta.entity.ConfigurationException;
import org.terracotta.entity.ServiceException;
import org.terracotta.entity.ServiceRegistry;
import org.terracotta.management.entity.nms.Nms;
import org.terracotta.management.entity.nms.NmsConfig;
import org.terracotta.management.entity.nms.NmsVersion;
import org.terracotta.management.model.message.Message;
import org.terracotta.management.service.monitoring.EntityManagementRegistry;
import org.terracotta.management.service.monitoring.ManagementRegistryConfiguration;
import org.terracotta.management.service.monitoring.ManagementService;
import org.terracotta.management.service.monitoring.ManagementServiceConfiguration;
import org.terracotta.management.service.monitoring.SharedEntityManagementRegistry;
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
  public ActiveNmsServerEntity createActiveEntity(ServiceRegistry registry, NmsConfig configuration) throws ConfigurationException {
    LOGGER.trace("createActiveEntity()");
    // get services
    try {
      ManagementService managementService = Objects.requireNonNull(registry.getService(new ManagementServiceConfiguration()));
      EntityManagementRegistry entityManagementRegistry = Objects.requireNonNull(registry.getService(new ManagementRegistryConfiguration(registry, true, true)));
      SharedEntityManagementRegistry sharedEntityManagementRegistry = Objects.requireNonNull(registry.getService(new BasicServiceConfiguration<>(SharedEntityManagementRegistry.class)));
      ActiveNmsServerEntity entity = new ActiveNmsServerEntity(configuration, managementService, entityManagementRegistry, sharedEntityManagementRegistry);
      managementService.setManagementExecutor(entity);
      return entity;
    } catch (ServiceException e) {
      throw new ConfigurationException("Unable to retrieve service: " + e.getMessage());
    }
  }

  @Override
  protected PassiveNmsServerEntity createPassiveEntity(ServiceRegistry registry, NmsConfig configuration) throws ConfigurationException {
    LOGGER.trace("createPassiveEntity()");
    try {
      EntityManagementRegistry entityManagementRegistry = Objects.requireNonNull(registry.getService(new ManagementRegistryConfiguration(registry, false, true)));
      SharedEntityManagementRegistry sharedEntityManagementRegistry = Objects.requireNonNull(registry.getService(new BasicServiceConfiguration<>(SharedEntityManagementRegistry.class)));
      return new PassiveNmsServerEntity(entityManagementRegistry, sharedEntityManagementRegistry);
    } catch (ServiceException e) {
      throw new ConfigurationException("Unable to retrieve service: " + e.getMessage());
    }
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
