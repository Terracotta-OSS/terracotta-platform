/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.nomad.entity.client;

import com.terracottatech.entity.AggregateEndpoint;
import com.terracottatech.entity.EntityAggregatingService;
import org.terracotta.nomad.entity.client.NomadEntity;

/**
 * @author Mathieu Carbou
 */
public class AggregatedNomadEntityClientService<T> implements EntityAggregatingService<NomadEntity<T>, Void> {
  @Override
  public boolean handlesEntityType(Class<NomadEntity<T>> cls) {
    return NomadEntity.class.equals(cls);
  }

  @Override
  public NomadEntity<T> aggregateEntities(AggregateEndpoint<NomadEntity<T>> endpoint) {
    return new AggregatedNomadEntity<>(endpoint.getEntities());
  }

  @Override
  public boolean targetConnectionForLifecycle(int stripeIndex, int totalStripes, String entityName, Void config) {
    return true;
  }

  @Override
  public Void formulateConfigurationForStripe(int stripeIndex, int totalStripes, String entityName, Void config) {
    return config;
  }

  @Override
  public byte[] serializeConfiguration(Void configuration) {
    return new byte[0];
  }

  @Override
  public Void deserializeConfiguration(byte[] configuration) {
    return null;
  }
}
