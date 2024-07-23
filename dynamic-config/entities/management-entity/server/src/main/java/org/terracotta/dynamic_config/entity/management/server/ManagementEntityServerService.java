/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
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
package org.terracotta.dynamic_config.entity.management.server;

import com.tc.classloader.PermanentEntity;
import org.terracotta.dynamic_config.api.service.TopologyService;
import org.terracotta.dynamic_config.server.api.DynamicConfigEventService;
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

@PermanentEntity(type = "org.terracotta.dynamic_config.entity.management.server.ManagementEntityServerService", name = "dynamic-config-management-entity")
public class ManagementEntityServerService implements EntityServerService<EntityMessage, EntityResponse> {

  private static final String ENTITY_TYPE = ManagementEntityServerService.class.getName();

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
    try {
      EntityManagementRegistry managementRegistry = registry.getService(new EntityManagementRegistryConfiguration(registry, true));
      DynamicConfigEventService dynamicConfigEventService = registry.getService(new BasicServiceConfiguration<>(DynamicConfigEventService.class));
      TopologyService topologyService = registry.getService(new BasicServiceConfiguration<>(TopologyService.class));
      return new ManagementActiveEntity(managementRegistry, dynamicConfigEventService, topologyService);
    } catch (ServiceException e) {
      throw new ConfigurationException("Could not retrieve service ", e);
    }
  }

  @Override
  public PassiveServerEntity<EntityMessage, EntityResponse> createPassiveEntity(ServiceRegistry registry, byte[] configuration) throws ConfigurationException {
    try {
      EntityManagementRegistry managementRegistry = registry.getService(new EntityManagementRegistryConfiguration(registry, false));
      DynamicConfigEventService dynamicConfigEventService = registry.getService(new BasicServiceConfiguration<>(DynamicConfigEventService.class));
      TopologyService topologyService = registry.getService(new BasicServiceConfiguration<>(TopologyService.class));
      return new ManagementPassiveEntity(managementRegistry, dynamicConfigEventService, topologyService);
    } catch (ServiceException e) {
      throw new ConfigurationException("Could not retrieve service ", e);
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
