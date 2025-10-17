/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
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
package org.terracotta.stats.entity.server;

import com.tc.classloader.PermanentEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.dynamic_config.api.service.TopologyService;
import org.terracotta.dynamic_config.api.server.DynamicConfigEventService;
import org.terracotta.entity.ActiveServerEntity;
import org.terracotta.entity.BasicServiceConfiguration;
import org.terracotta.entity.ConcurrencyStrategy;
import org.terracotta.entity.ConfigurationException;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.EntityResponse;
import org.terracotta.entity.EntityServerService;
import org.terracotta.entity.MessageCodec;
import org.terracotta.entity.NoConcurrencyStrategy;
import org.terracotta.entity.PassiveServerEntity;
import org.terracotta.entity.ServiceException;
import org.terracotta.entity.ServiceRegistry;
import org.terracotta.entity.SyncMessageCodec;
import org.terracotta.management.service.monitoring.EntityManagementRegistry;
import org.terracotta.management.service.monitoring.EntityManagementRegistryConfiguration;

@PermanentEntity(type = "org.terracotta.stats.entity.server.StatsEntityServerService", name = "stats-entity")
public class StatsEntityServerService implements EntityServerService<EntityMessage, EntityResponse> {

  private static final Logger LOGGER = LoggerFactory.getLogger(StatsEntityServerService.class);
  private static final String ENTITY_TYPE = StatsEntityServerService.class.getName();

  @Override
  public long getVersion() {
    return 1;
  }

  @Override
  public boolean handlesEntityType(String typeName) {
    return ENTITY_TYPE.equals(typeName);
  }

  @Override
  public ActiveServerEntity<EntityMessage, EntityResponse> createActiveEntity(ServiceRegistry registry, byte[] configuration) throws ConfigurationException {
    LOGGER.trace("createActiveEntity()");
    try {
      EntityManagementRegistry managementRegistry = registry.getService(new EntityManagementRegistryConfiguration(registry, true));
      DynamicConfigEventService dynamicConfigEventService = registry.getService(new BasicServiceConfiguration<>(DynamicConfigEventService.class));
      TopologyService topologyService = registry.getService(new BasicServiceConfiguration<>(TopologyService.class));
      return new StatsActiveEntity(managementRegistry, dynamicConfigEventService, topologyService);
    } catch (ServiceException e) {
      throw new ConfigurationException("Unable to retrieve service: " + e.getMessage(), e);
    }
  }

  @Override
  public PassiveServerEntity<EntityMessage, EntityResponse> createPassiveEntity(ServiceRegistry registry, byte[] configuration) throws ConfigurationException {
    LOGGER.trace("createPassiveEntity()");
    try {
      EntityManagementRegistry managementRegistry = registry.getService(new EntityManagementRegistryConfiguration(registry, false));
      DynamicConfigEventService dynamicConfigEventService = registry.getService(new BasicServiceConfiguration<>(DynamicConfigEventService.class));
      TopologyService topologyService = registry.getService(new BasicServiceConfiguration<>(TopologyService.class));
      return new StatsPassiveEntity(managementRegistry, dynamicConfigEventService, topologyService);
    } catch (ServiceException e) {
      throw new ConfigurationException("Unable to retrieve service: " + e.getMessage(), e);
    }
  }

  @Override
  public ConcurrencyStrategy<EntityMessage> getConcurrencyStrategy(byte[] configuration) {
    return new NoConcurrencyStrategy<>();
  }

  @Override
  public MessageCodec<EntityMessage, EntityResponse> getMessageCodec() {
    return null;
  }

  @Override
  public SyncMessageCodec<EntityMessage> getSyncMessageCodec() {
    return null;
  }
}
