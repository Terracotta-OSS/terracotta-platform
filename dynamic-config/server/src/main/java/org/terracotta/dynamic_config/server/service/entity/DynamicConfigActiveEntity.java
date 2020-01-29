/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.server.service.entity;

import org.terracotta.dynamic_config.api.service.DynamicConfigEventService;
import org.terracotta.entity.ActiveServerEntity;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.EntityResponse;
import org.terracotta.entity.PassiveSynchronizationChannel;
import org.terracotta.management.service.monitoring.EntityManagementRegistry;


public class DynamicConfigActiveEntity extends DynamicConfigCommonEntity implements ActiveServerEntity<EntityMessage, EntityResponse> {

  DynamicConfigActiveEntity(EntityManagementRegistry managementRegistry, DynamicConfigEventService dynamicConfigEventService) {
    super(managementRegistry, dynamicConfigEventService);
  }

  @Override
  public void loadExisting() {
    if (active) {
      managementRegistry.entityPromotionCompleted();
      managementRegistry.refresh();
      listen();
    }
  }

  @Override
  public void connected(ClientDescriptor clientDescriptor) {
    throw new AssertionError("Client not allowed to connect to this entity");
  }

  @Override
  public void disconnected(ClientDescriptor clientDescriptor) {
    throw new AssertionError("Client not allowed to connect to this entity");
  }

  @Override
  public void synchronizeKeyToPassive(PassiveSynchronizationChannel<EntityMessage> syncChannel, int concurrencyKey) {
  }

  @Override
  public ReconnectHandler startReconnect() {
    return (clientDescriptor, extendedReconnectData) -> {
    };
  }
}
