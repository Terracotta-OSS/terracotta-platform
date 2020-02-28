/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.server.service.entity;

import org.terracotta.dynamic_config.entity.common.DynamicTopologyEntityMessage;
import org.terracotta.entity.ActiveServerEntity;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.ConfigurationException;
import org.terracotta.entity.PassiveSynchronizationChannel;


public class DisabledDynamicTopologyActiveServerEntity implements ActiveServerEntity<DynamicTopologyEntityMessage, DynamicTopologyEntityMessage> {
  @Override
  public void loadExisting() {
  }

  @Override
  public void connected(ClientDescriptor clientDescriptor) {
  }

  @Override
  public void disconnected(ClientDescriptor clientDescriptor) {
  }

  @Override
  public void synchronizeKeyToPassive(PassiveSynchronizationChannel<DynamicTopologyEntityMessage> syncChannel, int concurrencyKey) {
  }

  @Override
  public void createNew() throws ConfigurationException {
  }

  @Override
  public void destroy() {
  }

  @Override
  public ReconnectHandler startReconnect() {
    return (clientDescriptor, extendedReconnectData) -> {
    };
  }
}
