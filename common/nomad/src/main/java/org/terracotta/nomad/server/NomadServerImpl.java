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
package org.terracotta.nomad.server;

import org.terracotta.nomad.client.change.NomadChange;
import org.terracotta.nomad.messages.AcceptRejectResponse;
import org.terracotta.nomad.messages.ChangeDetails;
import org.terracotta.nomad.messages.CommitMessage;
import org.terracotta.nomad.messages.DiscoverResponse;
import org.terracotta.nomad.messages.MutativeMessage;
import org.terracotta.nomad.messages.PrepareMessage;
import org.terracotta.nomad.messages.RejectionReason;
import org.terracotta.nomad.messages.RollbackMessage;
import org.terracotta.nomad.messages.TakeoverMessage;
import org.terracotta.nomad.server.state.NomadServerState;
import org.terracotta.nomad.server.state.NomadStateChange;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.terracotta.nomad.messages.AcceptRejectResponse.accept;
import static org.terracotta.nomad.messages.RejectionReason.BAD;
import static org.terracotta.nomad.messages.RejectionReason.DEAD;
import static org.terracotta.nomad.messages.RejectionReason.UNACCEPTABLE;
import static org.terracotta.nomad.server.ChangeRequestState.COMMITTED;
import static org.terracotta.nomad.server.ChangeRequestState.PREPARED;
import static org.terracotta.nomad.server.ChangeRequestState.ROLLED_BACK;

public class NomadServerImpl<T> implements NomadServer<T> {
  private final NomadServerState<T> state;
  private ChangeApplicator<T> changeApplicator;

  public NomadServerImpl(NomadServerState<T> state, ChangeApplicator<T> changeApplicator) throws NomadException {
    this.state = state;
    this.changeApplicator = changeApplicator;
    init();
  }

  public NomadServerImpl(NomadServerState<T> state) throws NomadException {
    this(state, null);
  }

  public ChangeApplicator<T> getChangeApplicator() {
    return changeApplicator;
  }

  public void setChangeApplicator(ChangeApplicator<T> changeApplicator) {
    this.changeApplicator = changeApplicator;
  }

  @Override
  public void reset() throws NomadException {
    state.reset();
    init();
  }

  @Override
  public void close() {
    // has to be empty in this implementation.
    // Only there to support closing server "stubs" used for client <-> server communication through diagnostic or entity channels
  }

  @Override
  public boolean hasIncompleteChange() {
    if (!state.isInitialized()) {
      return false;
    }
    try {
      DiscoverResponse<T> discover = discover();
      ChangeDetails<T> latestChange = discover.getLatestChange();
      if (latestChange == null) {
        return false;
      }
      // current configuration in force is 1 (we have activated a cluster) plus we have a prepared change at the end
      return state.getMode() == NomadServerMode.PREPARED || latestChange.getState() == PREPARED;
    } catch (NomadException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public Optional<ChangeState<T>> getConfig(UUID changeUUID) throws NomadException {
    return Optional.ofNullable(state.getChangeState(changeUUID));
  }

  @Override
  public Optional<T> getCurrentCommittedConfig() throws NomadException {
    return state.getCurrentCommittedConfig();
  }

  @Override
  public DiscoverResponse<T> discover() throws NomadException {
    NomadServerMode mode = state.getMode();
    long mutativeMessageCount = state.getMutativeMessageCount();
    String lastMutationHost = state.getLastMutationHost();
    String lastMutationUser = state.getLastMutationUser();
    Instant lastMutationTimestamp = state.getLastMutationTimestamp();
    long currentVersion = state.getCurrentVersion();
    long highestVersion = state.getHighestVersion();
    UUID latestChangeUuid = state.getLatestChangeUuid();

    if (latestChangeUuid != null) {
      ChangeState<T> changeState = state.getChangeState(latestChangeUuid);

      // find the latest change that is not rolled back in the append log
      ChangeState<T> latestCommittedChangeState = changeState;
      UUID latestCommittedChangeUuid = latestChangeUuid;
      if (changeState.getState() != COMMITTED) {
        while ((latestCommittedChangeUuid = latestCommittedChangeState.getPrevChangeId()) != null
            && (latestCommittedChangeState = state.getChangeState(latestCommittedChangeUuid)).getState() != COMMITTED) ;
      }

      return new DiscoverResponse<>(
          mode,
          mutativeMessageCount,
          lastMutationHost,
          lastMutationUser,
          lastMutationTimestamp,
          currentVersion,
          highestVersion,
          new ChangeDetails<>(
              latestChangeUuid,
              changeState.getState(),
              changeState.getVersion(),
              changeState.getChange(),
              changeState.getChangeResult(),
              changeState.getCreationHost(),
              changeState.getCreationUser(),
              changeState.getCreationTimestamp(),
              changeState.getChangeResultHash()),
          latestCommittedChangeUuid == null ? null : new ChangeDetails<>(
              latestCommittedChangeUuid,
              latestCommittedChangeState.getState(),
              latestCommittedChangeState.getVersion(),
              latestCommittedChangeState.getChange(),
              latestCommittedChangeState.getChangeResult(),
              latestCommittedChangeState.getCreationHost(),
              latestCommittedChangeState.getCreationUser(),
              latestCommittedChangeState.getCreationTimestamp(),
              latestCommittedChangeState.getChangeResultHash()
          ));

    } else {
      return new DiscoverResponse<>(
          mode,
          mutativeMessageCount,
          lastMutationHost,
          lastMutationUser,
          lastMutationTimestamp,
          currentVersion,
          highestVersion,
          null,
          null);
    }
  }

  public AcceptRejectResponse prepare(PrepareMessage message) throws NomadException {
    if (changeApplicator == null) {
      throw new IllegalStateException("Server is not yet in write mode. Change applicator is not yet set");
    }

    if (isDead(message)) {
      return reject(DEAD, "expectedMutativeMessageCount != actualMutativeMessageCount");
    }

    if (isWrongMode(NomadServerMode.ACCEPTING)) {
      return reject(BAD, "Expected mode: " + NomadServerMode.ACCEPTING + ". Was: " + state.getMode());
    }

    if (isLowVersionNumber(message)) {
      return reject(BAD, "Wrong change version number");
    }

    UUID changeUuid = message.getChangeUuid();
    ChangeState<T> existingChangeState = state.getChangeState(changeUuid);
    if (existingChangeState != null) {
      return reject(BAD, "Received an alive PrepareMessage for a change that already exists: " + changeUuid);
    }

    // null when preparing for the first time, when no config is available
    T existing = getCurrentCommittedConfig().orElse(null);
    NomadChange change = message.getChange();

    PotentialApplicationResult<T> result = changeApplicator.tryApply(existing, change);

    long versionNumber = message.getVersionNumber();
    T newConfiguration = result.getNewConfiguration();
    String mutationHost = message.getMutationHost();
    String mutationUser = message.getMutationUser();
    Instant mutationTimestamp = message.getMutationTimestamp();
    UUID prevChangeUuid = state.getLatestChangeUuid();
    ChangeRequest<T> changeRequest = new ChangeRequest<>(ChangeRequestState.PREPARED, versionNumber, prevChangeUuid, change, newConfiguration, mutationHost, mutationUser, mutationTimestamp);

    // All nodes must have the same append log.
    // So we always run `applyStateChange` regardless of the result, which will "sync" sanskrit state with the prepared change
    // Then, the response is sent back to the client and the client might roll back in case of rejection.
    // This is required so that a failover happening just after a prepare + rollback ensures that the active nodes that will restart
    // will all have the same prepared / rollback sequence in their append log.

    applyStateChange(state.newStateChange()
        .setRequest(NomadServerRequest.PREPARE)
        .setMode(NomadServerMode.PREPARED)
        .setLatestChangeUuid(changeUuid)
        .setHighestVersion(versionNumber)
        .setLastMutationHost(mutationHost)
        .setLastMutationUser(mutationUser)
        .setLastMutationTimestamp(mutationTimestamp)
        .createChange(changeUuid, changeRequest)
    );

    return result.isAllowed() ? accept() : reject(UNACCEPTABLE, result.getRejectionReason());
  }

  public AcceptRejectResponse commit(CommitMessage message) throws NomadException {
    if (changeApplicator == null) {
      throw new IllegalStateException("Server is not yet in write mode. Change applicator is not yet set");
    }

    if (isDead(message)) {
      return reject(DEAD, "expectedMutativeMessageCount != actualMutativeMessageCount");
    }

    if (isWrongMode(NomadServerMode.PREPARED)) {
      return reject(BAD, "Expected mode: " + PREPARED + ". Was: " + state.getMode());
    }

    UUID changeUuid = message.getChangeUuid();
    String mutationHost = message.getMutationHost();
    String mutationUser = message.getMutationUser();
    Instant mutationTimestamp = message.getMutationTimestamp();

    ChangeState<T> changeState = state.getChangeState(changeUuid);
    if (changeState == null) {
      return reject(BAD, "Received an alive CommitMessage for a change that does not exist: " + changeUuid);
    }

    long changeVersion = changeState.getVersion();
    NomadChange change = changeState.getChange();

    changeApplicator.apply(change);

    applyStateChange(state.newStateChange()
        .setRequest(NomadServerRequest.COMMIT)
        .setMode(NomadServerMode.ACCEPTING)
        .setLatestChangeUuid(changeUuid)
        .setCurrentVersion(changeVersion)
        .setLastMutationHost(mutationHost)
        .setLastMutationUser(mutationUser)
        .setLastMutationTimestamp(mutationTimestamp)
        .updateChangeRequestState(changeUuid, COMMITTED)
    );

    return accept();
  }

  public AcceptRejectResponse rollback(RollbackMessage message) throws NomadException {
    if (isDead(message)) {
      return reject(DEAD, "expectedMutativeMessageCount != actualMutativeMessageCount");
    }

    if (isWrongMode(NomadServerMode.PREPARED)) {
      return reject(BAD, "Expected mode: " + PREPARED + ". Was: " + state.getMode());
    }

    UUID changeUuid = message.getChangeUuid();
    String mutationHost = message.getMutationHost();
    String mutationUser = message.getMutationUser();
    Instant mutationTimestamp = message.getMutationTimestamp();

    applyStateChange(state.newStateChange()
        .setRequest(NomadServerRequest.ROLLBACK)
        .setMode(NomadServerMode.ACCEPTING)
        .setLastMutationHost(mutationHost)
        .setLastMutationUser(mutationUser)
        .setLastMutationTimestamp(mutationTimestamp)
        .updateChangeRequestState(changeUuid, ROLLED_BACK)
    );

    return accept();
  }

  public AcceptRejectResponse takeover(TakeoverMessage message) throws NomadException {
    if (isDead(message)) {
      return reject(DEAD, "expectedMutativeMessageCount != actualMutativeMessageCount");
    }

    String mutationHost = message.getMutationHost();
    String mutationUser = message.getMutationUser();
    Instant mutationTimestamp = message.getMutationTimestamp();

    applyStateChange(state.newStateChange()
        .setRequest(NomadServerRequest.TAKEOVER)
        .setLastMutationHost(mutationHost)
        .setLastMutationUser(mutationUser)
        .setLastMutationTimestamp(mutationTimestamp)
    );

    return accept();
  }

  private void init() throws NomadException {
    if (!state.isInitialized()) {
      state.applyStateChange(state.newStateChange()
          .setMode(NomadServerMode.ACCEPTING)
          .setCurrentVersion(0)
          .setHighestVersion(0)
          .setInitialized()
      );
    }
  }

  private boolean isDead(MutativeMessage mutativeMessage) {
    long expectedMutativeMessageCount = mutativeMessage.getExpectedMutativeMessageCount();
    long actualMutativeMessageCount = state.getMutativeMessageCount();

    return expectedMutativeMessageCount != actualMutativeMessageCount;
  }

  private boolean isWrongMode(NomadServerMode expectedMode) {
    NomadServerMode mode = state.getMode();
    return mode != expectedMode;
  }

  private boolean isLowVersionNumber(PrepareMessage message) {
    long versionNumber = message.getVersionNumber();
    long highestVersionNumber = state.getHighestVersion();
    return versionNumber <= highestVersionNumber;
  }

  private void applyStateChange(NomadStateChange<T> stateChange) throws NomadException {
    long currentMutativeMessageCount = state.getMutativeMessageCount();
    state.applyStateChange(stateChange);

    long expectedNewMutativeMessageCount = currentMutativeMessageCount + 1;
    long newMutativeMessageCount = state.getMutativeMessageCount();

    if (newMutativeMessageCount != expectedNewMutativeMessageCount) {
      throw new AssertionError("Expected increment in mutative message count. Expected: " + expectedNewMutativeMessageCount + " found: " + newMutativeMessageCount);
    }
  }

  private AcceptRejectResponse reject(RejectionReason rejectionReason, String rejectionMessage) {
    return AcceptRejectResponse.reject(rejectionReason, rejectionMessage, state.getLastMutationHost(), state.getLastMutationUser());
  }
}