/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.server.nomad.persistence;

import com.terracottatech.nomad.server.ChangeRequest;
import com.terracottatech.nomad.server.ChangeRequestState;
import com.terracottatech.nomad.server.NomadServerMode;
import com.terracottatech.nomad.server.NomadServerRequest;
import com.terracottatech.nomad.server.state.NomadStateChange;
import com.terracottatech.persistence.sanskrit.MutableSanskritObject;
import com.terracottatech.persistence.sanskrit.Sanskrit;
import com.terracottatech.persistence.sanskrit.SanskritException;
import com.terracottatech.persistence.sanskrit.SanskritObject;
import com.terracottatech.persistence.sanskrit.change.SanskritChange;
import com.terracottatech.persistence.sanskrit.change.SanskritChangeBuilder;

import java.time.Instant;
import java.util.UUID;

import static com.terracottatech.dynamic_config.server.nomad.persistence.NomadSanskritKeys.CHANGE_CREATION_HOST;
import static com.terracottatech.dynamic_config.server.nomad.persistence.NomadSanskritKeys.CHANGE_CREATION_TIMESTAMP;
import static com.terracottatech.dynamic_config.server.nomad.persistence.NomadSanskritKeys.CHANGE_CREATION_USER;
import static com.terracottatech.dynamic_config.server.nomad.persistence.NomadSanskritKeys.CHANGE_OPERATION;
import static com.terracottatech.dynamic_config.server.nomad.persistence.NomadSanskritKeys.CHANGE_RESULT_HASH;
import static com.terracottatech.dynamic_config.server.nomad.persistence.NomadSanskritKeys.CHANGE_STATE;
import static com.terracottatech.dynamic_config.server.nomad.persistence.NomadSanskritKeys.CHANGE_VERSION;
import static com.terracottatech.dynamic_config.server.nomad.persistence.NomadSanskritKeys.CURRENT_VERSION;
import static com.terracottatech.dynamic_config.server.nomad.persistence.NomadSanskritKeys.HIGHEST_VERSION;
import static com.terracottatech.dynamic_config.server.nomad.persistence.NomadSanskritKeys.LAST_MUTATION_HOST;
import static com.terracottatech.dynamic_config.server.nomad.persistence.NomadSanskritKeys.LAST_MUTATION_TIMESTAMP;
import static com.terracottatech.dynamic_config.server.nomad.persistence.NomadSanskritKeys.LAST_MUTATION_USER;
import static com.terracottatech.dynamic_config.server.nomad.persistence.NomadSanskritKeys.LATEST_CHANGE_UUID;
import static com.terracottatech.dynamic_config.server.nomad.persistence.NomadSanskritKeys.MODE;
import static com.terracottatech.dynamic_config.server.nomad.persistence.NomadSanskritKeys.PREV_CHANGE_UUID;
import static com.terracottatech.dynamic_config.server.nomad.persistence.NomadSanskritKeys.REQUEST;

public class SanskritNomadStateChange<T> implements NomadStateChange<T> {
  private final Sanskrit sanskrit;
  private final SanskritChangeBuilder changeBuilder;
  private final HashComputer<T> hashComputer;
  private volatile Long changeVersion;
  private volatile T changeResult;

  public SanskritNomadStateChange(Sanskrit sanskrit, SanskritChangeBuilder changeBuilder, HashComputer<T> hashComputer) {
    this.sanskrit = sanskrit;
    this.changeBuilder = changeBuilder;
    this.hashComputer = hashComputer;
  }

  @Override
  public NomadStateChange<T> setInitialized() {
    setMode(NomadServerMode.ACCEPTING);
    return this;
  }

  @Override
  public NomadStateChange<T> setMode(NomadServerMode mode) {
    changeBuilder.setString(MODE, mode.name());
    return this;
  }

  @Override
  public NomadStateChange<T> setRequest(NomadServerRequest request) {
    changeBuilder.setString(REQUEST, request.name());
    return this;
  }

  @Override
  public NomadStateChange<T> setLatestChangeUuid(UUID changeUuid) {
    changeBuilder.setString(LATEST_CHANGE_UUID, changeUuid.toString());
    return this;
  }

  @Override
  public NomadStateChange<T> setCurrentVersion(long versionNumber) {
    changeBuilder.setLong(CURRENT_VERSION, versionNumber);
    return this;
  }

  @Override
  public NomadStateChange<T> setHighestVersion(long versionNumber) {
    changeBuilder.setLong(HIGHEST_VERSION, versionNumber);
    return this;
  }

  @Override
  public NomadStateChange<T> setLastMutationHost(String lastMutationHost) {
    changeBuilder.setString(LAST_MUTATION_HOST, lastMutationHost);
    return this;
  }

  @Override
  public NomadStateChange<T> setLastMutationUser(String lastMutationUser) {
    changeBuilder.setString(LAST_MUTATION_USER, lastMutationUser);
    return this;
  }

  @Override
  public NomadStateChange<T> setLastMutationTimestamp(Instant lastMutationTimestamp) {
    changeBuilder.setString(LAST_MUTATION_TIMESTAMP, lastMutationTimestamp.toString());
    return this;
  }

  @Override
  public NomadStateChange<T> createChange(UUID changeUuid, ChangeRequest<T> changeRequest) {
    changeVersion = changeRequest.getVersion();
    changeResult = changeRequest.getChangeResult();
    String resultHash = hashComputer.computeHash(changeResult);

    MutableSanskritObject child = sanskrit.newMutableSanskritObject();
    child.setString(CHANGE_STATE, changeRequest.getState().name());
    child.setLong(CHANGE_VERSION, changeRequest.getVersion());
    if (changeRequest.getPrevChangeId() != null) {
      child.setString(PREV_CHANGE_UUID, changeRequest.getPrevChangeId());
    }
    child.setExternal(CHANGE_OPERATION, changeRequest.getChange());
    child.setString(CHANGE_RESULT_HASH, resultHash);
    child.setString(CHANGE_CREATION_HOST, changeRequest.getCreationHost());
    child.setString(CHANGE_CREATION_USER, changeRequest.getCreationUser());
    child.setString(CHANGE_CREATION_TIMESTAMP, changeRequest.getCreationTimestamp().toString());

    changeBuilder.setObject(changeUuid.toString(), child);

    return this;
  }

  @Override
  public NomadStateChange<T> updateChangeRequestState(UUID changeUuid, ChangeRequestState newState) {
    String uuidString = changeUuid.toString();
    SanskritObject existing = getObject(uuidString);
    MutableSanskritObject updated = sanskrit.newMutableSanskritObject();
    existing.accept(updated);

    updated.setString(CHANGE_STATE, newState.name());
    changeBuilder.setObject(uuidString, updated);
    return this;
  }

  public SanskritChange getSanskritChange() {
    return changeBuilder.build();
  }

  public Long getChangeVersion() {
    return changeVersion;
  }

  public T getChangeResult() {
    return changeResult;
  }

  private SanskritObject getObject(String key) {
    try {
      return sanskrit.getObject(key);
    } catch (SanskritException e) {
      throw new RuntimeException(e);
    }
  }
}
