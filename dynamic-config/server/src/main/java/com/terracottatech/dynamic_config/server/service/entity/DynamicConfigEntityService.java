/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.server.service.entity;

import com.tc.classloader.PermanentEntity;
import com.terracottatech.dynamic_config.api.service.DynamicConfigEventService;
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

@PermanentEntity(type = "com.terracottatech.dynamic_config.server.service.management.DynamicConfigEntityService", names = {"dynamic-config-entity"})
public class DynamicConfigEntityService implements EntityServerService<EntityMessage, EntityResponse> {

  private static final String ENTITY_TYPE = "com.terracottatech.dynamic_config.server.service.management.DynamicConfigEntityService";

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
      return new DynamicConfigActiveEntity(managementRegistry, dynamicConfigEventService);
    } catch (ServiceException e) {
      throw new ConfigurationException("Could not retrieve service ", e);
    }
  }

  @Override
  public PassiveServerEntity<EntityMessage, EntityResponse> createPassiveEntity(ServiceRegistry registry, byte[] configuration) throws ConfigurationException {
    try {
      EntityManagementRegistry managementRegistry = registry.getService(new EntityManagementRegistryConfiguration(registry, false));
      DynamicConfigEventService dynamicConfigEventService = registry.getService(new BasicServiceConfiguration<>(DynamicConfigEventService.class));
      return new DynamicConfigPassiveEntity(managementRegistry, dynamicConfigEventService);
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
