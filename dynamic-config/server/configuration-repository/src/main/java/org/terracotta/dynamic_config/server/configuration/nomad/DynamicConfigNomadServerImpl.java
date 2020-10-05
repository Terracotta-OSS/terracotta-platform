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
package org.terracotta.dynamic_config.server.configuration.nomad;

import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.service.NomadChangeInfo;
import org.terracotta.dynamic_config.server.api.DynamicConfigNomadServer;
import org.terracotta.nomad.client.change.NomadChange;
import org.terracotta.nomad.messages.AcceptRejectResponse;
import org.terracotta.nomad.server.ChangeApplicator;
import org.terracotta.nomad.server.ChangeState;
import org.terracotta.nomad.server.NomadException;
import org.terracotta.nomad.server.NomadServerImpl;
import org.terracotta.nomad.server.state.NomadServerState;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiFunction;

/**
 * @author Mathieu Carbou
 */
public class DynamicConfigNomadServerImpl extends NomadServerImpl<NodeContext> implements DynamicConfigNomadServer {
  private final NomadServerState<NodeContext> state;

  public DynamicConfigNomadServerImpl(NomadServerState<NodeContext> state) throws NomadException {
    super(state);
    this.state = state;
  }

  @Override
  public void setChangeApplicator(ChangeApplicator<NodeContext> changeApplicator) {
    if (getChangeApplicator() != null && changeApplicator != null) {
      throw new IllegalArgumentException("Variable changeApplicator is already set");
    }
    super.setChangeApplicator(changeApplicator);
  }

  @Override
  public ChangeApplicator<NodeContext> getChangeApplicator() {
    return super.getChangeApplicator();
  }

  @Override
  public void forceSync(Iterable<NomadChangeInfo> changes, BiFunction<NodeContext, NomadChange, NodeContext> fn) throws NomadException {
    ChangeApplicator<NodeContext> backup = getChangeApplicator();
    try {
      super.setChangeApplicator(ChangeApplicator.allow(fn));
      for (NomadChangeInfo change : changes) {
        switch (change.getChangeRequestState()) {
          case PREPARED: {
            long mutativeMessageCount = state.getMutativeMessageCount();
            AcceptRejectResponse response = prepare(change.toPrepareMessage(mutativeMessageCount));
            if (!response.isAccepted()) {
              throw new NomadException("Prepare failure. " +
                  "Reason: " + response + ". " +
                  "Change:" + change.getNomadChange().getSummary());
            }
            break;
          }
          case COMMITTED: {
            long mutativeMessageCount = state.getMutativeMessageCount();
            AcceptRejectResponse response = prepare(change.toPrepareMessage(mutativeMessageCount));
            if (!response.isAccepted()) {
              throw new NomadException("Prepare failure. " +
                  "Reason: " + response + ". " +
                  "Change:" + change.getNomadChange().getSummary());
            }
            response = commit(change.toCommitMessage(mutativeMessageCount + 1));
            if (!response.isAccepted()) {
              throw new NomadException("Unexpected commit failure. " +
                  "Reason: " + response + ". " +
                  "Change:" + change.getNomadChange().getSummary());
            }
            break;
          }
          case ROLLED_BACK: {
            long mutativeMessageCount = state.getMutativeMessageCount();
            AcceptRejectResponse response = prepare(change.toPrepareMessage(mutativeMessageCount));
            if (!response.isAccepted()) {
              throw new NomadException("Prepare failure. " +
                  "Reason: " + response + ". " +
                  "Change:" + change.getNomadChange().getSummary());
            }
            response = rollback(change.toRollbackMessage(mutativeMessageCount + 1));
            if (!response.isAccepted()) {
              throw new NomadException("Unexpected rollback failure. " +
                  "Reason: " + response + ". " +
                  "Change:" + change.getNomadChange().getSummary());
            }
            break;
          }
          default:
            throw new AssertionError(change.getChangeRequestState());
        }
      }
    } finally {
      super.setChangeApplicator(backup);
    }
  }

  @Override
  public List<NomadChangeInfo> getAllNomadChanges() throws NomadException {
    LinkedList<NomadChangeInfo> allNomadChanges = new LinkedList<>();
    UUID changeUuid = state.getLatestChangeUuid();
    while (changeUuid != null) {
      ChangeState<NodeContext> changeState = state.getChangeState(changeUuid);
      allNomadChanges.addFirst(
          new NomadChangeInfo(
              changeUuid,
              changeState.getChange(),
              changeState.getState(),
              changeState.getVersion(),
              changeState.getCreationHost(),
              changeState.getCreationUser(),
              changeState.getCreationTimestamp()
          )
      );
      changeUuid = changeState.getPrevChangeId();
    }
    return allNomadChanges;
  }
}
