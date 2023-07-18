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
package org.terracotta.dynamic_config.server.configuration.nomad.persistence.sanskrit;

import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.Version;
import org.terracotta.dynamic_config.server.configuration.nomad.persistence.Config;
import org.terracotta.dynamic_config.server.configuration.nomad.persistence.HashComputer;
import org.terracotta.nomad.server.ChangeRequest;
import org.terracotta.nomad.server.ChangeRequestState;
import org.terracotta.nomad.server.NomadException;
import org.terracotta.nomad.server.NomadServerMode;
import org.terracotta.nomad.server.NomadServerRequest;
import org.terracotta.nomad.server.state.NomadStateChange;
import org.terracotta.persistence.sanskrit.MutableSanskritObject;
import org.terracotta.persistence.sanskrit.Sanskrit;
import org.terracotta.persistence.sanskrit.SanskritException;
import org.terracotta.persistence.sanskrit.SanskritObject;
import org.terracotta.persistence.sanskrit.change.SanskritChange;
import org.terracotta.persistence.sanskrit.change.SanskritChangeBuilder;

import java.time.Instant;
import java.util.UUID;

import static org.terracotta.dynamic_config.server.configuration.nomad.persistence.NomadSanskritKeys.CHANGE_CREATION_HOST;
import static org.terracotta.dynamic_config.server.configuration.nomad.persistence.NomadSanskritKeys.CHANGE_CREATION_TIMESTAMP;
import static org.terracotta.dynamic_config.server.configuration.nomad.persistence.NomadSanskritKeys.CHANGE_CREATION_USER;
import static org.terracotta.dynamic_config.server.configuration.nomad.persistence.NomadSanskritKeys.CHANGE_FORMAT_VERSION;
import static org.terracotta.dynamic_config.server.configuration.nomad.persistence.NomadSanskritKeys.CHANGE_OPERATION;
import static org.terracotta.dynamic_config.server.configuration.nomad.persistence.NomadSanskritKeys.CHANGE_RESULT_HASH;
import static org.terracotta.dynamic_config.server.configuration.nomad.persistence.NomadSanskritKeys.CHANGE_STATE;
import static org.terracotta.dynamic_config.server.configuration.nomad.persistence.NomadSanskritKeys.CHANGE_VERSION;
import static org.terracotta.dynamic_config.server.configuration.nomad.persistence.NomadSanskritKeys.CURRENT_VERSION;
import static org.terracotta.dynamic_config.server.configuration.nomad.persistence.NomadSanskritKeys.HIGHEST_VERSION;
import static org.terracotta.dynamic_config.server.configuration.nomad.persistence.NomadSanskritKeys.LAST_MUTATION_HOST;
import static org.terracotta.dynamic_config.server.configuration.nomad.persistence.NomadSanskritKeys.LAST_MUTATION_TIMESTAMP;
import static org.terracotta.dynamic_config.server.configuration.nomad.persistence.NomadSanskritKeys.LAST_MUTATION_USER;
import static org.terracotta.dynamic_config.server.configuration.nomad.persistence.NomadSanskritKeys.LATEST_CHANGE_UUID;
import static org.terracotta.dynamic_config.server.configuration.nomad.persistence.NomadSanskritKeys.MODE;
import static org.terracotta.dynamic_config.server.configuration.nomad.persistence.NomadSanskritKeys.PREV_CHANGE_UUID;
import static org.terracotta.dynamic_config.server.configuration.nomad.persistence.NomadSanskritKeys.REQUEST;

public class SanskritNomadStateChange implements NomadStateChange<NodeContext> {
  private final Sanskrit sanskrit;
  private final SanskritChangeBuilder changeBuilder;
  private final HashComputer hashComputer;
  private volatile Long changeVersion;
  private volatile NodeContext changeResult;

  public SanskritNomadStateChange(Sanskrit sanskrit, SanskritChangeBuilder changeBuilder, HashComputer hashComputer) {
    this.sanskrit = sanskrit;
    this.changeBuilder = changeBuilder;
    this.hashComputer = hashComputer;
  }

  @Override
  public NomadStateChange<NodeContext> setInitialized() {
    setMode(NomadServerMode.ACCEPTING);
    return this;
  }

  @Override
  public NomadStateChange<NodeContext> setMode(NomadServerMode mode) {
    changeBuilder.setString(MODE, mode.name());
    return this;
  }

  @Override
  public NomadStateChange<NodeContext> setRequest(NomadServerRequest request) {
    changeBuilder.setString(REQUEST, request.name());
    return this;
  }

  @Override
  public NomadStateChange<NodeContext> setLatestChangeUuid(UUID changeUuid) {
    changeBuilder.setString(LATEST_CHANGE_UUID, changeUuid.toString());
    return this;
  }

  @Override
  public NomadStateChange<NodeContext> setCurrentVersion(long versionNumber) {
    changeBuilder.setLong(CURRENT_VERSION, versionNumber);
    return this;
  }

  @Override
  public NomadStateChange<NodeContext> setHighestVersion(long versionNumber) {
    changeBuilder.setLong(HIGHEST_VERSION, versionNumber);
    return this;
  }

  @Override
  public NomadStateChange<NodeContext> setLastMutationHost(String lastMutationHost) {
    changeBuilder.setString(LAST_MUTATION_HOST, lastMutationHost);
    return this;
  }

  @Override
  public NomadStateChange<NodeContext> setLastMutationUser(String lastMutationUser) {
    changeBuilder.setString(LAST_MUTATION_USER, lastMutationUser);
    return this;
  }

  @Override
  public NomadStateChange<NodeContext> setLastMutationTimestamp(Instant lastMutationTimestamp) {
    changeBuilder.setString(LAST_MUTATION_TIMESTAMP, lastMutationTimestamp.toString());
    return this;
  }

  @Override
  public NomadStateChange<NodeContext> createChange(UUID changeUuid, ChangeRequest<NodeContext> changeRequest) throws NomadException {
    changeVersion = changeRequest.getVersion();
    changeResult = changeRequest.getChangeResult();
    String resultHash = hashComputer.computeHash(new Config(changeResult, Version.CURRENT));

    MutableSanskritObject child = sanskrit.newMutableSanskritObject();

    try {
      child.setString(CHANGE_STATE, changeRequest.getState().name());
      child.setLong(CHANGE_VERSION, changeRequest.getVersion());
      if (changeRequest.getPrevChangeId() != null) {
        child.setString(PREV_CHANGE_UUID, changeRequest.getPrevChangeId().toString());
      }
      child.set(CHANGE_OPERATION, changeRequest.getChange(), Version.CURRENT.getValue());
      child.setString(CHANGE_FORMAT_VERSION, Version.CURRENT.getValue());
      child.setString(CHANGE_RESULT_HASH, resultHash);
      child.setString(CHANGE_CREATION_HOST, changeRequest.getCreationHost());
      child.setString(CHANGE_CREATION_USER, changeRequest.getCreationUser());
      child.setString(CHANGE_CREATION_TIMESTAMP, changeRequest.getCreationTimestamp().toString());
    } catch (SanskritException e) {
      throw new NomadException(e);
    }

    changeBuilder.setObject(changeUuid.toString(), child);

    return this;
  }

  @Override
  public NomadStateChange<NodeContext> updateChangeRequestState(UUID changeUuid, ChangeRequestState newState) throws NomadException {
    String uuidString = changeUuid.toString();
    MutableSanskritObject updated = sanskrit.newMutableSanskritObject();
    try {
      SanskritObject existing = sanskrit.getObject(uuidString);
      existing.accept(updated);
      updated.setString(CHANGE_STATE, newState.name());
    } catch (SanskritException e) {
      throw new NomadException(e);
    }
    changeBuilder.setObject(uuidString, updated);
    return this;
  }

  public SanskritChange getSanskritChange() {
    return changeBuilder.build();
  }

  public Long getChangeVersion() {
    return changeVersion;
  }

  public NodeContext getChangeResult() {
    return changeResult;
  }
}
