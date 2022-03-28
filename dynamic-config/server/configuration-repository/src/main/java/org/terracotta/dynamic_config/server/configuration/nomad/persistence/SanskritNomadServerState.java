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
package org.terracotta.dynamic_config.server.configuration.nomad.persistence;

import org.terracotta.json.Json;
import org.terracotta.nomad.client.change.NomadChange;
import org.terracotta.nomad.server.ChangeRequest;
import org.terracotta.nomad.server.ChangeRequestState;
import org.terracotta.nomad.server.NomadException;
import org.terracotta.nomad.server.NomadServerMode;
import org.terracotta.nomad.server.state.NomadServerState;
import org.terracotta.nomad.server.state.NomadStateChange;
import org.terracotta.persistence.sanskrit.Sanskrit;
import org.terracotta.persistence.sanskrit.SanskritException;
import org.terracotta.persistence.sanskrit.SanskritObject;
import org.terracotta.persistence.sanskrit.change.SanskritChangeBuilder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.terracotta.dynamic_config.server.configuration.nomad.persistence.NomadSanskritKeys.CHANGE_CREATION_HOST;
import static org.terracotta.dynamic_config.server.configuration.nomad.persistence.NomadSanskritKeys.CHANGE_CREATION_TIMESTAMP;
import static org.terracotta.dynamic_config.server.configuration.nomad.persistence.NomadSanskritKeys.CHANGE_CREATION_USER;
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
import static org.terracotta.dynamic_config.server.configuration.nomad.persistence.NomadSanskritKeys.MUTATIVE_MESSAGE_COUNT;
import static org.terracotta.dynamic_config.server.configuration.nomad.persistence.NomadSanskritKeys.PREV_CHANGE_UUID;

public class SanskritNomadServerState<T> implements NomadServerState<T> {
  private final Sanskrit sanskrit;
  private final ConfigStorage<T> configStorage;
  private final HashComputer<T> hashComputer;

  public SanskritNomadServerState(Sanskrit sanskrit, ConfigStorage<T> configStorage, HashComputer<T> hashComputer) {
    this.sanskrit = sanskrit;
    this.configStorage = configStorage;
    this.hashComputer = hashComputer;
  }

  @Override
  public boolean isInitialized() {
    try {
      return sanskrit.getString(MODE) != null;
    } catch (SanskritException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public NomadServerMode getMode() {
    String mode = getString(MODE);
    return NomadServerMode.valueOf(mode);
  }

  @Override
  public long getMutativeMessageCount() {
    return getLong(MUTATIVE_MESSAGE_COUNT);
  }

  @Override
  public String getLastMutationHost() {
    return getString(LAST_MUTATION_HOST);
  }

  @Override
  public String getLastMutationUser() {
    return getString(LAST_MUTATION_USER);
  }

  @Override
  public Instant getLastMutationTimestamp() {
    final String s = getString(LAST_MUTATION_TIMESTAMP);
    return s == null ? null : Instant.parse(s);
  }

  @Override
  public UUID getLatestChangeUuid() {
    String uuidString = getString(LATEST_CHANGE_UUID);
    if (uuidString == null) {
      return null;
    }
    return UUID.fromString(uuidString);
  }

  @Override
  public long getCurrentVersion() {
    final Long l = getLong(CURRENT_VERSION);
    return l == null ? 0 : l;
  }

  @Override
  public long getHighestVersion() {
    return getLong(HIGHEST_VERSION);
  }

  @Override
  public ChangeRequest<T> getChangeRequest(UUID changeUuid) throws NomadException {
    try {
      String uuidString = changeUuid.toString();
      SanskritObject child = getObject(uuidString);

      if (child == null) {
        return null;
      }

      ChangeRequestState state = ChangeRequestState.valueOf(child.getString(CHANGE_STATE));
      long version = child.getLong(CHANGE_VERSION);
      NomadChange change = child.getObject(CHANGE_OPERATION, NomadChange.class);
      String prevChangeUuid = child.getString(PREV_CHANGE_UUID);
      String expectedHash = child.getString(CHANGE_RESULT_HASH);
      String creationHost = child.getString(CHANGE_CREATION_HOST);
      String creationUser = child.getString(CHANGE_CREATION_USER);
      Instant creationTimestamp = Instant.parse(child.getString(CHANGE_CREATION_TIMESTAMP));

      T newConfiguration = configStorage.getConfig(version);
      String actualHash = hashComputer.computeHash(newConfiguration);
      if (!actualHash.equals(expectedHash)) {
        throw new NomadException("Bad hash for change: " + changeUuid + ". Hash: " + actualHash + ". Expected: " + expectedHash + ". Loaded configuration:\n" + Json.toPrettyJson(newConfiguration));
      }

      return new ChangeRequest<>(state, version, prevChangeUuid, change, newConfiguration, creationHost, creationUser, creationTimestamp);
    } catch (ConfigStorageException e) {
      throw new NomadException("Failed to read configuration: " + changeUuid, e);
    }
  }

  @Override
  public NomadStateChange<T> newStateChange() {
    SanskritChangeBuilder changeBuilder = SanskritChangeBuilder.newChange();

    long newMutativeMessageCount = getNewMutativeMessageCount();
    changeBuilder.setLong(MUTATIVE_MESSAGE_COUNT, newMutativeMessageCount);

    long currentVersion = getCurrentVersion();
    changeBuilder.setLong(CURRENT_VERSION, currentVersion);

    return new SanskritNomadStateChange<>(sanskrit, changeBuilder, hashComputer);
  }

  @Override
  public void applyStateChange(NomadStateChange<T> change) throws NomadException {
    try {
      SanskritNomadStateChange<T> sanskritChange = (SanskritNomadStateChange<T>) change;

      Long version = sanskritChange.getChangeVersion();
      if (version != null) {
        T changeResult = sanskritChange.getChangeResult();
        configStorage.saveConfig(version, changeResult);
      }

      sanskrit.applyChange(sanskritChange.getSanskritChange());
    } catch (SanskritException | ConfigStorageException e) {
      throw new NomadException("Failed to update distributed transaction state", e);
    }
  }

  @Override
  public Optional<T> getCurrentCommittedChangeResult() throws NomadException {
    long currentVersion = getCurrentVersion();
    if (currentVersion == 0L) {
      return Optional.empty();
    }
    try {
      return Optional.ofNullable(configStorage.getConfig(currentVersion));
    } catch (ConfigStorageException e) {
      throw new NomadException("Failed to load current configuration", e);
    }
  }

  @Override
  public void reset() throws NomadException {
    NomadException error = null;
    try {
      sanskrit.reset();
    } catch (SanskritException e) {
      error = new NomadException(e);
    }
    try {
      configStorage.reset();
    } catch (ConfigStorageException e) {
      if (error == null) {
        error = new NomadException(e);
      } else {
        error.addSuppressed(e);
      }
    }
    if (error != null) {
      throw error;
    }
  }

  private String getString(String key) {
    try {
      return sanskrit.getString(key);
    } catch (SanskritException e) {
      throw new RuntimeException(e);
    }
  }

  private long getNewMutativeMessageCount() {
    Long mutativeMessageCount = getLong(MUTATIVE_MESSAGE_COUNT);

    if (mutativeMessageCount == null) {
      return 1L;
    }

    return mutativeMessageCount + 1L;
  }

  private Long getLong(String key) {
    try {
      return sanskrit.getLong(key);
    } catch (SanskritException e) {
      throw new RuntimeException(e);
    }
  }

  private SanskritObject getObject(String key) {
    try {
      return sanskrit.getObject(key);
    } catch (SanskritException e) {
      throw new RuntimeException(e);
    }
  }
}
