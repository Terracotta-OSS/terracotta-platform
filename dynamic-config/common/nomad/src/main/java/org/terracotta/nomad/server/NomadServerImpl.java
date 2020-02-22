/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
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
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.terracotta.nomad.messages.AcceptRejectResponse.accept;
import static org.terracotta.nomad.messages.RejectionReason.BAD;
import static org.terracotta.nomad.messages.RejectionReason.DEAD;
import static org.terracotta.nomad.messages.RejectionReason.UNACCEPTABLE;
import static org.terracotta.nomad.server.ChangeRequestState.COMMITTED;
import static org.terracotta.nomad.server.ChangeRequestState.PREPARED;
import static org.terracotta.nomad.server.ChangeRequestState.ROLLED_BACK;

public class NomadServerImpl<T> implements UpgradableNomadServer<T> {
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

  @Override
  public void reset() throws NomadException {
    state.reset();
    init();
  }

  @Override
  public void close() {
  }

  @Override
  public void setChangeApplicator(ChangeApplicator<T> changeApplicator) {
    if (changeApplicator == null) {
      throw new NullPointerException("Can not set NULL changeApplicator");
    }
    if (this.changeApplicator != null) {
      throw new IllegalArgumentException("Variable changeApplicator is already set");
    }
    this.changeApplicator = changeApplicator;
  }

  @Override
  public List<NomadChangeInfo<T>> getAllNomadChanges() throws NomadException {
    LinkedList<NomadChangeInfo<T>> allNomadChanges = new LinkedList<>();
    UUID changeUuid = state.getLatestChangeUuid();
    while (changeUuid != null) {
      ChangeRequest<T> changeRequest = state.getChangeRequest(changeUuid);
      allNomadChanges.addFirst(
          new NomadChangeInfo<>(
              changeUuid,
              changeRequest.getChange(),
              changeRequest.getState(),
              changeRequest.getVersion(),
              changeRequest.getCreationHost(),
              changeRequest.getCreationUser(),
              changeRequest.getCreationTimestamp()
          )
      );
      if (changeRequest.getPrevChangeId() != null) {
        changeUuid = UUID.fromString(changeRequest.getPrevChangeId());
      } else {
        changeUuid = null;
      }
    }
    return allNomadChanges;
  }

  @Override
  public Optional<NomadChangeInfo<T>> getNomadChange(UUID changeUuid) throws NomadException {
    ChangeRequest<T> changeRequest = state.getChangeRequest(changeUuid);
    if (changeRequest == null) {
      return Optional.empty();
    }
    return Optional.of(new NomadChangeInfo<>(
        changeUuid,
        changeRequest.getChange(),
        changeRequest.getState(),
        changeRequest.getVersion(),
        changeRequest.getCreationHost(),
        changeRequest.getCreationUser(),
        changeRequest.getCreationTimestamp()));
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
  public Optional<T> getCurrentCommittedChangeResult() throws NomadException {
    return state.getCurrentCommittedChangeResult();
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

    ChangeDetails<T> latestChange = null;
    if (latestChangeUuid != null) {
      ChangeRequest<T> changeRequest = state.getChangeRequest(latestChangeUuid);
      ChangeRequestState changeState = changeRequest.getState();
      long changeVersion = changeRequest.getVersion();
      NomadChange change = changeRequest.getChange();
      T changeResult = changeRequest.getChangeResult();
      String changeCreationHost = changeRequest.getCreationHost();
      String changeCreationUser = changeRequest.getCreationUser();
      Instant changeCreationTimestamp = changeRequest.getCreationTimestamp();

      latestChange = new ChangeDetails<>(
          latestChangeUuid,
          changeState,
          changeVersion,
          change,
          changeResult,
          changeCreationHost,
          changeCreationUser,
          changeCreationTimestamp
      );
    }

    List<NomadChangeInfo<T>> checkpoints = getAllNomadChanges().stream()
        .filter(nomadChangeInfo -> nomadChangeInfo.getChangeRequestState() == COMMITTED)
        .collect(Collectors.toList());

    return new DiscoverResponse<>(
        mode,
        mutativeMessageCount,
        lastMutationHost,
        lastMutationUser,
        lastMutationTimestamp,
        currentVersion,
        highestVersion,
        latestChange,
        checkpoints
    );
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
    ChangeRequest<T> existingChangeRequest = state.getChangeRequest(changeUuid);
    if (existingChangeRequest != null) {
      return reject(BAD, "Received an alive PrepareMessage for a change that already exists: " + changeUuid);
    }

    // null when preparing for the first time, when no config is available
    T existing = getCurrentCommittedChangeResult().orElse(null);
    NomadChange change = message.getChange();

    PotentialApplicationResult<T> result = changeApplicator.tryApply(existing, change);
    if (!result.isAllowed()) {
      String rejectionMessage = result.getRejectionReason();
      return reject(UNACCEPTABLE, rejectionMessage);
    }

    long versionNumber = message.getVersionNumber();
    T newConfiguration = result.getNewConfiguration();
    String mutationHost = message.getMutationHost();
    String mutationUser = message.getMutationUser();
    Instant mutationTimestamp = message.getMutationTimestamp();
    UUID prevChangeUuid = state.getLatestChangeUuid();
    String prevChangeId = null;
    if (prevChangeUuid != null) {
      prevChangeId = prevChangeUuid.toString();
    }
    ChangeRequest<T> changeRequest = new ChangeRequest<>(ChangeRequestState.PREPARED, versionNumber, prevChangeId, change, newConfiguration, mutationHost, mutationUser, mutationTimestamp);

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

    return accept();
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

    ChangeRequest<T> changeRequest = state.getChangeRequest(changeUuid);
    if (changeRequest == null) {
      return reject(BAD, "Received an alive CommitMessage for a change that does not exist: " + changeUuid);
    }

    long changeVersion = changeRequest.getVersion();
    NomadChange change = changeRequest.getChange();

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
