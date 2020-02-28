/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.entity.client;

import org.terracotta.dynamic_config.entity.common.DynamicTopologyEntityMessage;
import org.terracotta.dynamic_config.entity.common.DynamicTopologyMessageCodec;
import org.terracotta.entity.EntityClientEndpoint;
import org.terracotta.entity.EntityClientService;

/**
 * @author Mathieu Carbou
 */
public class DynamicTopologyEntityClientService implements EntityClientService<DynamicTopologyEntity, Void, DynamicTopologyEntityMessage, DynamicTopologyEntityMessage, DynamicTopologyEntity.Settings> {

  private final DynamicTopologyMessageCodec messageCodec = new DynamicTopologyMessageCodec();

  @Override
  public boolean handlesEntityType(Class<DynamicTopologyEntity> cls) {
    return DynamicTopologyEntity.class.equals(cls);
  }

  @Override
  public byte[] serializeConfiguration(Void configuration) {
    return new byte[0];
  }

  @Override
  public Void deserializeConfiguration(byte[] configuration) {
    return null;
  }

  @Override
  public DynamicTopologyEntity create(EntityClientEndpoint<DynamicTopologyEntityMessage, DynamicTopologyEntityMessage> endpoint, DynamicTopologyEntity.Settings settings) {
    return new DynamicTopologyEntityImpl(endpoint, settings);
  }

  @Override
  public DynamicTopologyMessageCodec getMessageCodec() {
    return messageCodec;
  }
}
