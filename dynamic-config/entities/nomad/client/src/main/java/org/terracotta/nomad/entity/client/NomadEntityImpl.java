/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.nomad.entity.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.entity.EntityClientEndpoint;
import org.terracotta.entity.Invocation;
import org.terracotta.exception.EntityException;
import org.terracotta.nomad.entity.common.NomadEntityMessage;
import org.terracotta.nomad.entity.common.NomadEntityResponse;
import org.terracotta.nomad.messages.AcceptRejectResponse;
import org.terracotta.nomad.messages.MutativeMessage;
import org.terracotta.nomad.server.NomadException;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
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
    this.settings = settings == null ? new Settings() : settings;
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
      Invocation<NomadEntityResponse> builder = endpoint.message(new NomadEntityMessage(mutativeMessage));
      AcceptRejectResponse response;
      if (requestTimeout == null) {
        response = builder.invokeAndRetire().get().getResponse();
      } else {
        response = builder.invokeAndRetire().get(requestTimeout.toMillis(), TimeUnit.MILLISECONDS).getResponse();
      }
      LOGGER.trace("response({})", response);
      return response;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new NomadException(e);
    } catch (ExecutionException e) {
      // unwrap the execution exception eventually thrown by nomad in the server entity
      Throwable cause = e.getCause();
      if (cause instanceof EntityException) {
        Throwable t = cause.getCause() == null ? cause : cause.getCause();
        throw new NomadException(t.getMessage(), t.getCause());
      } else {
        throw new NomadException(cause.getMessage(), cause);
      }
    } catch (TimeoutException e) {
      throw new NomadException(e);
    }
  }
}
