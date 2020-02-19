/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.server.service.management;

import org.terracotta.dynamic_config.api.service.DynamicConfigEventService;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.EntityResponse;
import org.terracotta.entity.PassiveServerEntity;
import org.terracotta.management.service.monitoring.EntityManagementRegistry;

public class ManagementPassiveEntity extends ManagementCommonEntity implements PassiveServerEntity<EntityMessage, EntityResponse> {

  ManagementPassiveEntity(EntityManagementRegistry managementRegistry, DynamicConfigEventService dynamicConfigEventService) {
    super(managementRegistry, dynamicConfigEventService);
  }

  @Override
  public void startSyncEntity() {
  }

  @Override
  public void endSyncEntity() {
  }

  @Override
  public void startSyncConcurrencyKey(int concurrencyKey) {
  }

  @Override
  public void endSyncConcurrencyKey(int concurrencyKey) {
  }
}