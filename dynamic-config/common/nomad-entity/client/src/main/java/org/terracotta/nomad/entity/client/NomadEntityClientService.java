/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.nomad.entity.client;

import org.terracotta.entity.EntityClientEndpoint;
import org.terracotta.entity.EntityClientService;
import org.terracotta.nomad.entity.common.NomadEntityMessage;
import org.terracotta.nomad.entity.common.NomadEntityResponse;
import org.terracotta.nomad.entity.common.NomadMessageCodec;

/**
 * @author Mathieu Carbou
 */
public class NomadEntityClientService<T> implements EntityClientService<NomadEntity<T>, Void, NomadEntityMessage, NomadEntityResponse, NomadEntity.Settings> {

  private final NomadMessageCodec messageCodec = new NomadMessageCodec();

  @Override
  public boolean handlesEntityType(Class<NomadEntity<T>> cls) {
    return NomadEntity.class.equals(cls);
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
  public NomadEntity<T> create(EntityClientEndpoint<NomadEntityMessage, NomadEntityResponse> endpoint, NomadEntity.Settings settings) {
    return new NomadEntityImpl<>(endpoint, settings);
  }

  @Override
  public NomadMessageCodec getMessageCodec() {
    return messageCodec;
  }
}
