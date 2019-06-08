/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.nomad.server.state;

import com.terracottatech.nomad.client.change.NomadChange;
import com.terracottatech.nomad.server.ChangeRequest;
import com.terracottatech.nomad.server.ChangeRequestState;
import com.terracottatech.nomad.server.NomadServerMode;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.terracottatech.nomad.server.state.StateKeys.CURRENT_VERSION;
import static com.terracottatech.nomad.server.state.StateKeys.HIGHEST_VERSION;
import static com.terracottatech.nomad.server.state.StateKeys.INITIALIZED;
import static com.terracottatech.nomad.server.state.StateKeys.LAST_MUTATION_HOST;
import static com.terracottatech.nomad.server.state.StateKeys.LAST_MUTATION_USER;
import static com.terracottatech.nomad.server.state.StateKeys.LATEST_CHANGE_UUID;
import static com.terracottatech.nomad.server.state.StateKeys.MODE;
import static com.terracottatech.nomad.server.state.StateKeys.MUTATIVE_MESSAGE_COUNT;

public class MemoryNomadServerState implements NomadServerState {
  private Map<String, Object> state = new HashMap<>();

  @Override
  @SuppressWarnings("unchecked")
  public NomadStateChange newStateChange() {
    return new MemoryNomadStateChange(k -> (Map<String, Object>) state.get(k));
  }

  @Override
  public void applyStateChange(NomadStateChange change) {
    MemoryNomadStateChange memoryChange = (MemoryNomadStateChange) change;

    Map<String, Object> updateMap = memoryChange.getUpdateMap();

    Long currentMutativeMessageCount = (Long) state.get(MUTATIVE_MESSAGE_COUNT);
    if (currentMutativeMessageCount == null) {
      currentMutativeMessageCount = 0L;
    }

    updateMap.put(MUTATIVE_MESSAGE_COUNT, currentMutativeMessageCount + 1);

    state.putAll(updateMap);
  }

  @Override
  public String getCurrentCommittedChangeResult() {
    long currentVersion = getCurrentVersion();
    if (currentVersion == 0L) {
      return null;
    }

    return (String) state.get(Long.toString(currentVersion));
  }

  @Override
  public boolean isInitialized() {
    return state.get(INITIALIZED) != null;
  }

  @Override
  public NomadServerMode getMode() {
    return NomadServerMode.valueOf((String) state.get(MODE));
  }

  @Override
  public long getMutativeMessageCount() {
    return (long) state.get(MUTATIVE_MESSAGE_COUNT);
  }

  @Override
  public String getLastMutationHost() {
    return (String) state.get(LAST_MUTATION_HOST);
  }

  @Override
  public String getLastMutationUser() {
    return (String) state.get(LAST_MUTATION_USER);
  }

  @Override
  public UUID getLatestChangeUuid() {
    String latestChangeUuid = (String) state.get(LATEST_CHANGE_UUID);

    if (latestChangeUuid == null) {
      return null;
    }

    return UUID.fromString(latestChangeUuid);
  }

  @Override
  public long getCurrentVersion() {
    return (long) state.get(CURRENT_VERSION);
  }

  @Override
  public long getHighestVersion() {
    return (long) state.get(HIGHEST_VERSION);
  }

  @Override
  @SuppressWarnings("unchecked")
  public ChangeRequest getChangeRequest(UUID changeUuid) {
    Map<String, Object> changeRequestState = (Map<String, Object>) state.get(changeUuid.toString());

    if (changeRequestState == null) {
      return null;
    }

    ChangeRequestState requestState = ChangeRequestState.valueOf((String) changeRequestState.get(StateKeys.STATE));
    long version = (long) changeRequestState.get(StateKeys.VERSION);
    NomadChange change = (NomadChange) changeRequestState.get(StateKeys.CHANGE);
    String creationHost = (String) changeRequestState.get(StateKeys.CREATION_HOST);
    String creationUser = (String) changeRequestState.get(StateKeys.CREATION_USER);

    String changeResult = (String) state.get(Long.toString(version));

    return new ChangeRequest(requestState, version, change, changeResult, creationHost, creationUser);
  }
}