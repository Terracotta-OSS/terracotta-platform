/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.service.management;

import com.terracottatech.dynamic_config.service.DynamicConfigEventing;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.EntityResponse;
import org.terracotta.entity.PassiveServerEntity;
import org.terracotta.management.service.monitoring.EntityManagementRegistry;

public class DynamicConfigPassiveEntity extends DynamicConfigCommonEntity implements PassiveServerEntity<EntityMessage, EntityResponse> {

  DynamicConfigPassiveEntity(EntityManagementRegistry managementRegistry, DynamicConfigEventing dynamicConfigEventing) {
    super(managementRegistry, dynamicConfigEventing);
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
