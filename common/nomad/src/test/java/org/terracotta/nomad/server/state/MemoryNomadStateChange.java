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
package org.terracotta.nomad.server.state;

import org.terracotta.nomad.server.ChangeRequest;
import org.terracotta.nomad.server.ChangeRequestState;
import org.terracotta.nomad.server.NomadServerMode;
import org.terracotta.nomad.server.NomadServerRequest;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

public class MemoryNomadStateChange<T> implements NomadStateChange<T> {
  private final Function<String, Map<String, Object>> changeLoader;
  private Map<String, Object> changes = new HashMap<>();

  Map<String, Object> getUpdateMap() {
    return changes;
  }

  MemoryNomadStateChange(Function<String, Map<String, Object>> changeLoader) {
    this.changeLoader = changeLoader;
  }

  @Override
  public NomadStateChange<T> setInitialized() {
    changes.put(StateKeys.INITIALIZED, "true");
    return this;
  }

  @Override
  public NomadStateChange<T> setMode(NomadServerMode mode) {
    changes.put(StateKeys.MODE, mode.toString());
    return this;
  }

  @Override
  public NomadStateChange<T> setRequest(NomadServerRequest request) {
    changes.put(StateKeys.REQUEST, request.toString());
    return this;
  }

  @Override
  public NomadStateChange<T> setLatestChangeUuid(UUID changeUuid) {
    changes.put(StateKeys.LATEST_CHANGE_UUID, changeUuid.toString());
    return this;
  }

  @Override
  public NomadStateChange<T> setCurrentVersion(long versionNumber) {
    changes.put(StateKeys.CURRENT_VERSION, versionNumber);
    return this;
  }

  @Override
  public NomadStateChange<T> setHighestVersion(long versionNumber) {
    changes.put(StateKeys.HIGHEST_VERSION, versionNumber);
    return this;
  }

  @Override
  public NomadStateChange<T> setLastMutationHost(String lastMutationHost) {
    changes.put(StateKeys.LAST_MUTATION_HOST, lastMutationHost);
    return this;
  }

  @Override
  public NomadStateChange<T> setLastMutationUser(String lastMutationUser) {
    changes.put(StateKeys.LAST_MUTATION_USER, lastMutationUser);
    return this;
  }

  @Override
  public NomadStateChange<T> setLastMutationTimestamp(Instant lastMutationTimestamp) {
    changes.put(StateKeys.LAST_MUTATION_TIMESTAMP, lastMutationTimestamp.toString());
    return this;
  }

  @Override
  public NomadStateChange<T> createChange(UUID changeUuid, ChangeRequest<T> changeRequest) {
    long version = changeRequest.getVersion();

    Map<String, Object> changeRequestState = new HashMap<>();
    changeRequestState.put(StateKeys.STATE, changeRequest.getState().toString());
    changeRequestState.put(StateKeys.VERSION, version);
    if (changeRequest.getPrevChangeId() != null) {
      changeRequestState.put(StateKeys.PREV_CHANGE_UUID, changeRequest.getPrevChangeId());
    }
    changeRequestState.put(StateKeys.CHANGE, changeRequest.getChange());
    changeRequestState.put(StateKeys.CREATION_HOST, changeRequest.getCreationHost());
    changeRequestState.put(StateKeys.CREATION_USER, changeRequest.getCreationUser());
    changeRequestState.put(StateKeys.CREATION_TIMESTAMP, changeRequest.getCreationTimestamp());

    changes.put(changeUuid.toString(), changeRequestState);
    changes.put(Long.toString(version), changeRequest.getChangeResult());

    return this;
  }

  @Override
  public NomadStateChange<T> updateChangeRequestState(UUID changeUuid, ChangeRequestState newState) {
    String changeKey = changeUuid.toString();
    Map<String, Object> changeRequestState = changeLoader.apply(changeKey);

    Map<String, Object> newChangeRequestState = new HashMap<>(changeRequestState);
    newChangeRequestState.put(StateKeys.STATE, newState.toString());

    changes.put(changeKey, newChangeRequestState);

    return this;
  }
}
