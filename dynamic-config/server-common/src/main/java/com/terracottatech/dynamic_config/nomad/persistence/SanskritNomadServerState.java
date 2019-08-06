/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.nomad.persistence;

import com.terracottatech.nomad.client.change.NomadChange;
import com.terracottatech.nomad.server.ChangeRequest;
import com.terracottatech.nomad.server.ChangeRequestState;
import com.terracottatech.nomad.server.NomadException;
import com.terracottatech.nomad.server.NomadServerMode;
import com.terracottatech.nomad.server.state.NomadServerState;
import com.terracottatech.nomad.server.state.NomadStateChange;
import com.terracottatech.persistence.sanskrit.Sanskrit;
import com.terracottatech.persistence.sanskrit.SanskritException;
import com.terracottatech.persistence.sanskrit.SanskritObject;
import com.terracottatech.persistence.sanskrit.change.SanskritChangeBuilder;

import java.util.UUID;

import static com.terracottatech.dynamic_config.nomad.persistence.NomadSanskritKeys.CHANGE_CREATION_HOST;
import static com.terracottatech.dynamic_config.nomad.persistence.NomadSanskritKeys.CHANGE_CREATION_USER;
import static com.terracottatech.dynamic_config.nomad.persistence.NomadSanskritKeys.CHANGE_OPERATION;
import static com.terracottatech.dynamic_config.nomad.persistence.NomadSanskritKeys.CHANGE_RESULT_HASH;
import static com.terracottatech.dynamic_config.nomad.persistence.NomadSanskritKeys.CHANGE_STATE;
import static com.terracottatech.dynamic_config.nomad.persistence.NomadSanskritKeys.CHANGE_VERSION;
import static com.terracottatech.dynamic_config.nomad.persistence.NomadSanskritKeys.CURRENT_VERSION;
import static com.terracottatech.dynamic_config.nomad.persistence.NomadSanskritKeys.HIGHEST_VERSION;
import static com.terracottatech.dynamic_config.nomad.persistence.NomadSanskritKeys.LAST_MUTATION_HOST;
import static com.terracottatech.dynamic_config.nomad.persistence.NomadSanskritKeys.LAST_MUTATION_USER;
import static com.terracottatech.dynamic_config.nomad.persistence.NomadSanskritKeys.LATEST_CHANGE_UUID;
import static com.terracottatech.dynamic_config.nomad.persistence.NomadSanskritKeys.MODE;
import static com.terracottatech.dynamic_config.nomad.persistence.NomadSanskritKeys.MUTATIVE_MESSAGE_COUNT;

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
  public UUID getLatestChangeUuid() {
    String uuidString = getString(LATEST_CHANGE_UUID);
    if (uuidString == null) {
      return null;
    }
    return UUID.fromString(uuidString);
  }

  @Override
  public long getCurrentVersion() {
    return getLong(CURRENT_VERSION);
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
      NomadChange change = child.getExternal(CHANGE_OPERATION, NomadChange.class);
      String foundChangeResultHash = child.getString(CHANGE_RESULT_HASH);
      String creationHost = child.getString(CHANGE_CREATION_HOST);
      String creationUser = child.getString(CHANGE_CREATION_USER);

      T newConfiguration = configStorage.getConfig(version);
      String newConfigurationhash = hashComputer.computeHash(newConfiguration);
      checkHash(changeUuid, newConfigurationhash, foundChangeResultHash);

      return new ChangeRequest<>(state, version, change, newConfiguration, creationHost, creationUser);
    } catch (ConfigStorageException e) {
      throw new NomadException("Failed to read configuration: " + changeUuid, e);
    }
  }

  private void checkHash(UUID changeUuid, String actualChangeResultHash, String foundChangeResultHash) throws NomadException {
    if (!actualChangeResultHash.equals(foundChangeResultHash)) {
      throw new NomadException("Bad hash for change: " + changeUuid + " found: " + foundChangeResultHash + " actual: " + actualChangeResultHash);
    }
  }

  @Override
  public NomadStateChange<T> newStateChange() {
    SanskritChangeBuilder changeBuilder = SanskritChangeBuilder.newChange();

    long newMutativeMessageCount = getNewMutativeMessageCount();
    changeBuilder.setLong(MUTATIVE_MESSAGE_COUNT, newMutativeMessageCount);

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
  public T getCurrentCommittedChangeResult() throws NomadException {
    long currentVersion = getCurrentVersion();

    try {
      return configStorage.getConfig(currentVersion);
    } catch (ConfigStorageException e) {
      throw new NomadException("Failed to load current configuration", e);
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