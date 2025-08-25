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
package org.terracotta.nomad.entity.server;

import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.entity.EntityUserException;
import org.terracotta.entity.InvokeContext;
import org.terracotta.entity.PassiveServerEntity;
import org.terracotta.nomad.entity.common.NomadEntityMessage;
import org.terracotta.nomad.entity.common.NomadEntityResponse;
import org.terracotta.nomad.messages.AcceptRejectResponse;
import org.terracotta.nomad.server.NomadException;
import org.terracotta.nomad.server.NomadServer;
import org.terracotta.server.Server;
import org.terracotta.server.StopAction;

public class NomadPassiveServerEntity<T> extends NomadCommonServerEntity<T> implements PassiveServerEntity<NomadEntityMessage, NomadEntityResponse> {
  private final Server server;

  public NomadPassiveServerEntity(Server server, NomadServer<NodeContext> nomadServer) {
    super(nomadServer);
    this.server = server;
  }

  @Override
  public void startSyncEntity() {
  }

  @Override
  public void endSyncEntity() {
  }

  @Override
  public void startSyncConcurrencyKey(int concurrencyKey) {
  }

  @Override
  public void endSyncConcurrencyKey(int concurrencyKey) {
  }

  @Override
  public void invokePassive(InvokeContext context, NomadEntityMessage message) throws EntityUserException {
    logger.trace("invokePassive({})", message);
    boolean restarted = true;
    try {
      AcceptRejectResponse response = processMessage(message.getNomadMessage());
      if (!response.isAccepted()) {
        // if message is not accepted, we just log (error) but we do not crash the passive:
        switch (response.getRejectionReason()) {
          case DEAD:
            logger.warn("Node was unable to process Nomad message: {}. Response: {}. This can happen when the same message is received more than once and the first one was already processed.", message, response);
            break;
          case BAD:
            logger.error("RESTARTING: Node was unable to process Nomad message: {}. Response: {}. This can happen when the Nomad system is not accepting changes or when the change does not exist.", message, response);
            restarted = server.stopIfPassive(StopAction.RESTART);
            break;
          case UNACCEPTABLE:
            logger.error("RESTARTING: Node was unable to process Nomad message: {}. Response: {}. This can happen when the Nomad system is not able to execute the requested change", message, response);
            restarted = server.stopIfPassive(StopAction.RESTART);
            break;
        }
      }
    } catch (NomadException | RuntimeException e) {
      logger.error("RESTARTING: Failure happened while processing Nomad message: {}: {}", message, e.getMessage(), e);
      server.stopIfPassive(StopAction.RESTART);
      throw new EntityUserException(e.getMessage(), e);
    }
    if (!restarted) {
      logger.error("Failed restarting node");
      throw new EntityUserException("failed to restart");
    }
  }
}
