/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.nomad.entity.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.entity.EntityClientEndpoint;
import org.terracotta.entity.InvokeFuture;
import org.terracotta.entity.MessageCodecException;
import org.terracotta.exception.EntityException;
import org.terracotta.nomad.entity.common.NomadEntityMessage;
import org.terracotta.nomad.entity.common.NomadEntityResponse;
import org.terracotta.nomad.messages.AcceptRejectResponse;
import org.terracotta.nomad.messages.MutativeMessage;
import org.terracotta.nomad.server.NomadException;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author Mathieu Carbou
 */
class NomadEntityImpl<T> implements NomadEntity<T> {
  private static final Logger LOGGER = LoggerFactory.getLogger(NomadEntityImpl.class);

  private final EntityClientEndpoint<NomadEntityMessage, NomadEntityResponse> endpoint;
  private final Settings settings;

  public NomadEntityImpl(EntityClientEndpoint<NomadEntityMessage, NomadEntityResponse> endpoint, Settings settings) {
    this.endpoint = endpoint;
    this.settings = settings;
  }

  @Override
  public void close() {
    endpoint.close();
  }

  @Override
  public AcceptRejectResponse send(MutativeMessage mutativeMessage) throws NomadException {
    LOGGER.trace("send({})", mutativeMessage);
    Duration requestTimeout = settings.getRequestTimeout();
    try {
      InvokeFuture<NomadEntityResponse> invoke = endpoint.beginInvoke()
          .message(new NomadEntityMessage(mutativeMessage))
          .invoke();
      AcceptRejectResponse response = (requestTimeout == null ? invoke.get() : invoke.getWithTimeout(requestTimeout.toMillis(), TimeUnit.MILLISECONDS)).getResponse();
      LOGGER.trace("response({})", response);
      return response;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new NomadException(e);
    } catch (MessageCodecException | EntityException | TimeoutException e) {
      throw new NomadException(e);
    }
  }
}
