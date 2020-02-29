/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.nomad.entity.client;

import org.terracotta.connection.entity.Entity;
import org.terracotta.nomad.messages.AcceptRejectResponse;
import org.terracotta.nomad.messages.CommitMessage;
import org.terracotta.nomad.messages.DiscoverResponse;
import org.terracotta.nomad.messages.MutativeMessage;
import org.terracotta.nomad.messages.PrepareMessage;
import org.terracotta.nomad.messages.RollbackMessage;
import org.terracotta.nomad.messages.TakeoverMessage;
import org.terracotta.nomad.server.NomadException;
import org.terracotta.nomad.server.NomadServer;

import java.time.Duration;

/**
 * @author Mathieu Carbou
 */
public interface NomadEntity<T> extends Entity, NomadServer<T> {
  @Override
  default DiscoverResponse<T> discover() {
    throw new UnsupportedOperationException();
  }

  @Override
  default AcceptRejectResponse prepare(PrepareMessage message) throws NomadException {
    throw new UnsupportedOperationException();
  }

  @Override
  default AcceptRejectResponse commit(CommitMessage message) throws NomadException {
    return send(message);
  }

  @Override
  default AcceptRejectResponse rollback(RollbackMessage message) throws NomadException {
    return send(message);
  }

  @Override
  default AcceptRejectResponse takeover(TakeoverMessage message) throws NomadException {
    return send(message);
  }

  AcceptRejectResponse send(MutativeMessage message) throws NomadException;

  class Settings {
    private Duration requestTimeout = Duration.ofSeconds(20);

    public Duration getRequestTimeout() {
      return requestTimeout;
    }

    public Settings setRequestTimeout(Duration requestTimeout) {
      this.requestTimeout = requestTimeout;
      return this;
    }
  }
}
