/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.nomad.server;

import com.terracottatech.nomad.client.change.NomadChange;
import com.terracottatech.nomad.messages.AcceptRejectResponse;
import com.terracottatech.nomad.messages.ChangeDetails;
import com.terracottatech.nomad.messages.CommitMessage;
import com.terracottatech.nomad.messages.DiscoverResponse;
import com.terracottatech.nomad.messages.MutativeMessage;
import com.terracottatech.nomad.messages.PrepareMessage;
import com.terracottatech.nomad.messages.RejectionReason;
import com.terracottatech.nomad.messages.RollbackMessage;
import com.terracottatech.nomad.messages.TakeoverMessage;
import com.terracottatech.nomad.server.state.NomadServerState;
import com.terracottatech.nomad.server.state.NomadStateChange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

import static com.terracottatech.nomad.messages.AcceptRejectResponse.accept;
import static com.terracottatech.nomad.messages.RejectionReason.BAD;
import static com.terracottatech.nomad.messages.RejectionReason.DEAD;
import static com.terracottatech.nomad.messages.RejectionReason.UNACCEPTABLE;
import static com.terracottatech.nomad.server.ChangeRequestState.COMMITTED;
import static com.terracottatech.nomad.server.ChangeRequestState.ROLLED_BACK;

public class NomadServerImpl<T> implements UpgradableNomadServer<T> {
  private static final Logger LOGGER = LoggerFactory.getLogger(NomadServerImpl.class);

  private final NomadServerState<T> state;
  private ChangeApplicator<T> changeApplicator;

  public NomadServerImpl(NomadServerState<T> state, ChangeApplicator<T> changeApplicator) throws NomadException {
    this.state = state;
    this.changeApplicator = changeApplicator;

    if (!state.isInitialized()) {
      state.applyStateChange(state.newStateChange()
          .setMode(NomadServerMode.ACCEPTING)
          .setCurrentVersion(0)
          .setHighestVersion(0)
          .setInitialized()
      );
    }
  }

  public NomadServerImpl(NomadServerState<T> state) throws NomadException {
    this(state, null);
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
  public DiscoverResponse<T> discover() throws NomadException {
    NomadServerMode mode = state.getMode();
    long mutativeMessageCount = state.getMutativeMessageCount();
    String lastMutationHost = state.getLastMutationHost();
    String lastMutationUser = state.getLastMutationUser();
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

      latestChange = new ChangeDetails<>(
          latestChangeUuid,
          changeState,
          changeVersion,
          change,
          changeResult,
          changeCreationHost,
          changeCreationUser
      );
    }

    return new DiscoverResponse<>(
        mode,
        mutativeMessageCount,
        lastMutationHost,
        lastMutationUser,
        currentVersion,
        highestVersion,
        latestChange
    );
  }

  public AcceptRejectResponse prepare(PrepareMessage message) throws NomadException {
    if (changeApplicator == null) {
      throw new IllegalStateException("Server is not yet in write mode. Change applicator is not yet set");
    }

    if (isDead(message)) {
      return reject(DEAD);
    }

    if (isWrongMode(message, NomadServerMode.ACCEPTING)) {
      return reject(BAD);
    }

    if (isLowVersionNumber(message)) {
      return reject(BAD);
    }

    UUID changeUuid = message.getChangeUuid();
    ChangeRequest<T> existingChangeRequest = state.getChangeRequest(changeUuid);
    if (existingChangeRequest != null) {
      LOGGER.error("Received an alive PrepareMessage for a change that already exists: " + changeUuid);
      return reject(BAD);
    }

    T existing = state.getCurrentCommittedChangeResult();
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

    ChangeRequest<T> changeRequest = new ChangeRequest<>(ChangeRequestState.PREPARED, versionNumber, change, newConfiguration, mutationHost, mutationUser);

    applyStateChange(state.newStateChange()
        .setMode(NomadServerMode.PREPARED)
        .setLatestChangeUuid(changeUuid)
        .setHighestVersion(versionNumber)
        .setLastMutationHost(mutationHost)
        .setLastMutationUser(mutationUser)
        .createChange(changeUuid, changeRequest)
    );

    return accept();
  }

  public AcceptRejectResponse commit(CommitMessage message) throws NomadException {
    if (changeApplicator == null) {
      throw new IllegalStateException("Server is not yet in write mode. Change applicator is not yet set");
    }

    if (isDead(message)) {
      return reject(DEAD);
    }

    if (isWrongMode(message, NomadServerMode.PREPARED)) {
      return reject(BAD);
    }

    UUID changeUuid = message.getChangeUuid();
    String mutationHost = message.getMutationHost();
    String mutationUser = message.getMutationUser();

    ChangeRequest<T> changeRequest = state.getChangeRequest(changeUuid);
    if (changeRequest == null) {
      LOGGER.error("Received an alive CommitMessage for a change that does not exist: " + changeUuid);
      return reject(BAD);
    }

    long changeVersion = changeRequest.getVersion();
    NomadChange change = changeRequest.getChange();

    changeApplicator.apply(change);

    applyStateChange(state.newStateChange()
        .setMode(NomadServerMode.ACCEPTING)
        .setCurrentVersion(changeVersion)
        .setLastMutationHost(mutationHost)
        .setLastMutationUser(mutationUser)
        .updateChangeRequestState(changeUuid, COMMITTED)
    );

    return accept();
  }

  public AcceptRejectResponse rollback(RollbackMessage message) throws NomadException {
    if (isDead(message)) {
      return reject(DEAD);
    }

    if (isWrongMode(message, NomadServerMode.PREPARED)) {
      return reject(BAD);
    }

    UUID changeUuid = message.getChangeUuid();
    String mutationHost = message.getMutationHost();
    String mutationUser = message.getMutationUser();

    applyStateChange(state.newStateChange()
        .setMode(NomadServerMode.ACCEPTING)
        .setLastMutationHost(mutationHost)
        .setLastMutationUser(mutationUser)
        .updateChangeRequestState(changeUuid, ROLLED_BACK)
    );

    return accept();
  }

  public AcceptRejectResponse takeover(TakeoverMessage message) throws NomadException {
    if (isDead(message)) {
      return reject(DEAD);
    }

    String mutationHost = message.getMutationHost();
    String mutationUser = message.getMutationUser();

    applyStateChange(state.newStateChange()
        .setLastMutationHost(mutationHost)
        .setLastMutationUser(mutationUser)
    );

    return accept();
  }

  private boolean isDead(MutativeMessage mutativeMessage) {
    long expectedMutativeMessageCount = mutativeMessage.getExpectedMutativeMessageCount();
    long actualMutativeMessageCount = state.getMutativeMessageCount();

    return expectedMutativeMessageCount != actualMutativeMessageCount;
  }

  private boolean isWrongMode(MutativeMessage message, NomadServerMode expectedMode) {
    NomadServerMode mode = state.getMode();
    boolean correctMode = mode == expectedMode;

    if (!correctMode) {
      LOGGER.error("Received an alive " + message.getClass().getSimpleName() + " but not in " + expectedMode + " mode");
    }

    return !correctMode;
  }

  private boolean isLowVersionNumber(PrepareMessage message) {
    long versionNumber = message.getVersionNumber();
    long highestVersionNumber = state.getHighestVersion();

    boolean lowVersionNumber = versionNumber <= highestVersionNumber;
    if (lowVersionNumber) {
      LOGGER.error("Received an alive " + message.getClass().getSimpleName() + " with a low version number. High: " + highestVersionNumber + " received: " + versionNumber);
    }

    return lowVersionNumber;
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

  private AcceptRejectResponse reject(RejectionReason rejectionReason) {
    return AcceptRejectResponse.reject(rejectionReason, state.getLastMutationHost(), state.getLastMutationUser());
  }

  private AcceptRejectResponse reject(RejectionReason rejectionReason, String rejectionMessage) {
    return AcceptRejectResponse.reject(rejectionReason, rejectionMessage, state.getLastMutationHost(), state.getLastMutationUser());
  }
}
