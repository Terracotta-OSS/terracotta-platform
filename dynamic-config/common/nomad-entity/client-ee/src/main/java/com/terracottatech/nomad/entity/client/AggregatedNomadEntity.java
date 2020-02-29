/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.nomad.entity.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.nomad.entity.client.NomadEntity;
import org.terracotta.nomad.messages.AcceptRejectResponse;
import org.terracotta.nomad.messages.MutativeMessage;
import org.terracotta.nomad.server.NomadException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Mathieu Carbou
 */
class AggregatedNomadEntity<T> implements NomadEntity<T> {
  private static final Logger LOGGER = LoggerFactory.getLogger(AggregatedNomadEntity.class);

  private final List<NomadEntity<T>> entities;

  public AggregatedNomadEntity(List<NomadEntity<T>> entities) {
    this.entities = entities;
  }

  @Override
  public void close() {
    // close each entities one by one and capture any exception happening,
    // merging them together,
    // and rethrowing the first one if any
    entities.stream()
        .map(entity -> {
          try {
            entity.close();
            return null;
          } catch (RuntimeException e) {
            return e;
          }
        })
        .filter(Objects::nonNull)
        .reduce((result, e) -> {
          result.addSuppressed(e);
          return result;
        })
        .ifPresent(e -> {
          throw e;
        });
  }

  @Override
  public AcceptRejectResponse send(MutativeMessage message) throws NomadException {
    LOGGER.trace("send({})", message);
    List<AcceptRejectResponse> responses = new ArrayList<>(entities.size());
    // ensure all stripes are called
    for (NomadEntity<T> entity : entities) {
      responses.add(entity.send(message));
    }
    return responses.stream()
        .filter(AcceptRejectResponse::isRejected)
        .findFirst()
        .orElseGet(AcceptRejectResponse::accept);
  }
}
