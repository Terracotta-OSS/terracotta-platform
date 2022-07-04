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
package org.terracotta.nomad.server.state;

import org.terracotta.nomad.client.change.NomadChange;
import org.terracotta.nomad.server.ChangeRequestState;
import org.terracotta.nomad.server.ChangeState;
import org.terracotta.nomad.server.NomadException;
import org.terracotta.nomad.server.NomadServerMode;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.terracotta.nomad.server.state.StateKeys.CURRENT_VERSION;
import static org.terracotta.nomad.server.state.StateKeys.HIGHEST_VERSION;
import static org.terracotta.nomad.server.state.StateKeys.INITIALIZED;
import static org.terracotta.nomad.server.state.StateKeys.LAST_MUTATION_HOST;
import static org.terracotta.nomad.server.state.StateKeys.LAST_MUTATION_TIMESTAMP;
import static org.terracotta.nomad.server.state.StateKeys.LAST_MUTATION_USER;
import static org.terracotta.nomad.server.state.StateKeys.LATEST_CHANGE_UUID;
import static org.terracotta.nomad.server.state.StateKeys.MODE;
import static org.terracotta.nomad.server.state.StateKeys.MUTATIVE_MESSAGE_COUNT;

public class MemoryNomadServerState<T> implements NomadServerState<T> {
  private final Map<String, Object> state = new HashMap<>();

  @Override
  @SuppressWarnings("unchecked")
  public NomadStateChange<T> newStateChange() {
    return new MemoryNomadStateChange<T>(k -> (Map<String, Object>) state.get(k));
  }

  @Override
  public void applyStateChange(NomadStateChange<T> change) {
    MemoryNomadStateChange<T> memoryChange = (MemoryNomadStateChange<T>) change;

    Map<String, Object> updateMap = memoryChange.getUpdateMap();

    Long currentMutativeMessageCount = (Long) state.get(MUTATIVE_MESSAGE_COUNT);
    if (currentMutativeMessageCount == null) {
      currentMutativeMessageCount = 0L;
    }

    updateMap.put(MUTATIVE_MESSAGE_COUNT, currentMutativeMessageCount + 1);

    state.putAll(updateMap);
  }

  @SuppressWarnings("unchecked")
  @Override
  public Optional<T> getCurrentCommittedConfig() {
    long currentVersion = getCurrentVersion();
    if (currentVersion == 0L) {
      return Optional.empty();
    }

    return Optional.ofNullable((T) state.get(Long.toString(currentVersion)));
  }

  @Override
  public void reset() throws NomadException {
    state.clear();
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
  public Instant getLastMutationTimestamp() {
    final String s = (String) state.get(LAST_MUTATION_TIMESTAMP);
    return s == null ? null : Instant.parse(s);
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
  public ChangeState<T> getChangeState(UUID changeUuid) {
    Map<String, Object> changeRequestState = (Map<String, Object>) state.get(changeUuid.toString());

    if (changeRequestState == null) {
      return null;
    }

    ChangeRequestState requestState = ChangeRequestState.valueOf((String) changeRequestState.get(StateKeys.STATE));
    long version = (long) changeRequestState.get(StateKeys.VERSION);
    NomadChange change = (NomadChange) changeRequestState.get(StateKeys.CHANGE);
    String creationHost = (String) changeRequestState.get(StateKeys.CREATION_HOST);
    String creationUser = (String) changeRequestState.get(StateKeys.CREATION_USER);
    Instant creationTimestamp = (Instant) changeRequestState.get(StateKeys.CREATION_TIMESTAMP);
    UUID prevChangeUuid = (UUID) changeRequestState.get(StateKeys.PREV_CHANGE_UUID);
    T changeResult = (T) state.get(Long.toString(version));

    return new ChangeState<T>(requestState, version, prevChangeUuid, change, changeResult, creationHost, creationUser, creationTimestamp, changeResult == null ? null : changeResult.toString());
  }
}
