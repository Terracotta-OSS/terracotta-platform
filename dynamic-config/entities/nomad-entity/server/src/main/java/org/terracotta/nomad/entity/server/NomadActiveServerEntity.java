/*
 * Copyright Terracotta, Inc.
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
package org.terracotta.nomad.entity.server;

import org.terracotta.dynamic_config.server.api.DynamicConfigNomadServer;
import org.terracotta.entity.ActiveInvokeContext;
import org.terracotta.entity.ActiveServerEntity;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.EntityUserException;
import org.terracotta.entity.PassiveSynchronizationChannel;
import org.terracotta.nomad.entity.common.NomadEntityMessage;
import org.terracotta.nomad.entity.common.NomadEntityResponse;
import org.terracotta.nomad.messages.AcceptRejectResponse;
import org.terracotta.nomad.messages.MutativeMessage;
import org.terracotta.nomad.server.NomadException;


public class NomadActiveServerEntity<T> extends NomadCommonServerEntity<T> implements ActiveServerEntity<NomadEntityMessage, NomadEntityResponse> {
  public NomadActiveServerEntity(DynamicConfigNomadServer nomadServer) {
    super(nomadServer);
  }

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
  public void synchronizeKeyToPassive(PassiveSynchronizationChannel<NomadEntityMessage> syncChannel, int concurrencyKey) {
  }

  @Override
  public ReconnectHandler startReconnect() {
    return (clientDescriptor, extendedReconnectData) -> {
    };
  }

  @Override
  public NomadEntityResponse invokeActive(ActiveInvokeContext<NomadEntityResponse> context, NomadEntityMessage message) throws EntityUserException {
    logger.trace("invokeActive({})", message);
    try {
      MutativeMessage nomadMessage = message.getNomadMessage();
      AcceptRejectResponse response = processMessage(nomadMessage);
      return new NomadEntityResponse(response);
    } catch (NomadException | RuntimeException e) {
      logger.error("Failure happened while processing Nomad message: {}: {}", message, e.getMessage(), e);
      throw new EntityUserException(e.getMessage(), e);
    }
  }
}
