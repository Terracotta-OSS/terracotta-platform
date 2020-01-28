/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.test_support.entity;

import org.terracotta.entity.ActiveServerEntity;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.EntityResponse;
import org.terracotta.entity.PassiveSynchronizationChannel;

public class TestActiveEntity extends TestCommonEntity implements ActiveServerEntity<EntityMessage, EntityResponse> {
  @Override
  public void connected(ClientDescriptor clientDescriptor) {
    throw new AssertionError("Client not allowed to connect to this entity");
  }

  @Override
  public void disconnected(ClientDescriptor clientDescriptor) {
    throw new AssertionError("Client not allowed to connect to this entity");
  }

  @Override
  public void loadExisting() {

  }

  @Override
  public void synchronizeKeyToPassive(PassiveSynchronizationChannel<EntityMessage> syncChannel, int concurrencyKey) {

  }

  @Override
  public ReconnectHandler startReconnect() {
    return null;
  }
}
