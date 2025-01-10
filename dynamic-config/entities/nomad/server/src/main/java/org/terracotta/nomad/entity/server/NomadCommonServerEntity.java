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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.entity.CommonServerEntity;
import org.terracotta.nomad.entity.common.NomadEntityMessage;
import org.terracotta.nomad.entity.common.NomadEntityResponse;
import org.terracotta.nomad.messages.AcceptRejectResponse;
import org.terracotta.nomad.messages.CommitMessage;
import org.terracotta.nomad.messages.MutativeMessage;
import org.terracotta.nomad.messages.PrepareMessage;
import org.terracotta.nomad.messages.RollbackMessage;
import org.terracotta.nomad.messages.TakeoverMessage;
import org.terracotta.nomad.server.ChangeRequestState;
import org.terracotta.nomad.server.ChangeState;
import org.terracotta.nomad.server.NomadException;
import org.terracotta.nomad.server.NomadServer;

import java.util.Optional;
import java.util.UUID;

import static org.terracotta.nomad.messages.RejectionReason.BAD;
import static org.terracotta.nomad.messages.RejectionReason.DEAD;

public class NomadCommonServerEntity<T> implements CommonServerEntity<NomadEntityMessage, NomadEntityResponse> {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  private final NomadServer<NodeContext> nomadServer;

  public NomadCommonServerEntity(NomadServer<NodeContext> nomadServer) {
    this.nomadServer = nomadServer;
  }

  @Override
  public final void createNew() {
  }

  @Override
  public final void destroy() {
  }

  protected AcceptRejectResponse processMessage(MutativeMessage nomadMessage) throws NomadException {
    logger.trace("Processing Nomad message: {}", nomadMessage);
    AcceptRejectResponse response;
    if (nomadMessage instanceof CommitMessage) {
      response = commit((CommitMessage) nomadMessage);
    } else if (nomadMessage instanceof RollbackMessage) {
      response = rollback((RollbackMessage) nomadMessage);
    } else if (nomadMessage instanceof TakeoverMessage) {
      throw new UnsupportedOperationException();
    } else if (nomadMessage instanceof PrepareMessage) {
      response = prepare((PrepareMessage) nomadMessage);
    } else {
      throw new IllegalArgumentException("Unsupported Nomad message: " + nomadMessage.getClass().getName());
    }
    logger.trace("Result: {}", response);
    return response;
  }

  private AcceptRejectResponse prepare(PrepareMessage nomadMessage) throws NomadException {
    final UUID uuid = nomadMessage.getChangeUuid();
    final Optional<ChangeState<NodeContext>> info = nomadServer.getConfig(uuid);
    if (!info.isPresent()) {
      // the change UUId is not yet in Nomad - first call probably
      return nomadServer.prepare(nomadMessage);
    } else {
      // the change UUID is already in Nomad : this entity is called again (i.e. failover or any other client resend)
      ChangeState<NodeContext> changeInfo = info.get();
      if (changeInfo.getState() == ChangeRequestState.PREPARED) {
        // this change has already been prepared previously
        return AcceptRejectResponse.accept();
      } else {
        // this change has already been committed or rolled back previously
        return AcceptRejectResponse.reject(DEAD, "Change: " + uuid + " is already in state: " + changeInfo.getState(), changeInfo.getCreationHost(), changeInfo.getCreationUser());
      }
    }
  }

  private AcceptRejectResponse rollback(RollbackMessage nomadMessage) throws NomadException {
    final UUID uuid = nomadMessage.getChangeUuid();
    final Optional<ChangeState<NodeContext>> info = nomadServer.getConfig(uuid);
    if (!info.isPresent()) {
      // oups! we miss an entry!
      return AcceptRejectResponse.reject(BAD, "Change: " + uuid + " is missing", nomadMessage.getMutationHost(), nomadMessage.getMutationUser());
    } else {
      // the change UUID is already in Nomad: we check its state
      ChangeState<NodeContext> changeInfo = info.get();
      switch (changeInfo.getState()) {
        case PREPARED:
          // first call
          return nomadServer.rollback(nomadMessage);
        case ROLLED_BACK:
          // duplicate call
          return AcceptRejectResponse.accept();
        case COMMITTED:
          // oups, very bad! server has committed, but we received a message to rollback ???
          // this should never happen, but just in case, the passive would restart
          return AcceptRejectResponse.reject(BAD, "Change: " + uuid + " is already committed", changeInfo.getCreationHost(), changeInfo.getCreationUser());
        default:
          throw new AssertionError(changeInfo.getState());
      }
    }
  }

  private AcceptRejectResponse commit(CommitMessage nomadMessage) throws NomadException {
    final UUID uuid = nomadMessage.getChangeUuid();
    final Optional<ChangeState<NodeContext>> info = nomadServer.getConfig(uuid);
    if (!info.isPresent()) {
      // oups! we miss an entry!
      return AcceptRejectResponse.reject(BAD, "Change: " + uuid + " is missing", nomadMessage.getMutationHost(), nomadMessage.getMutationUser());
    } else {
      // the change UUID is already in Nomad: we check its state
      ChangeState<NodeContext> changeInfo = info.get();
      switch (changeInfo.getState()) {
        case PREPARED:
          // first call
          return nomadServer.commit(nomadMessage);
        case ROLLED_BACK:
          // oups, very bad! server has rolled back, but we received a message to commit ???
          // this should never happen, but just in case, the passive would restart
          return AcceptRejectResponse.reject(BAD, "Change: " + uuid + " is already rolled back", changeInfo.getCreationHost(), changeInfo.getCreationUser());
        case COMMITTED:
          // duplicate call
          return AcceptRejectResponse.accept();
        default:
          throw new AssertionError(changeInfo.getState());
      }
    }
  }
}
