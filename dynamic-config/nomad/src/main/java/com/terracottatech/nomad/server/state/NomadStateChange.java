/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.nomad.server.state;

import com.terracottatech.nomad.server.ChangeRequest;
import com.terracottatech.nomad.server.ChangeRequestState;
import com.terracottatech.nomad.server.NomadServerMode;

import java.util.UUID;

public interface NomadStateChange<T> {
  NomadStateChange<T> setInitialized();

  NomadStateChange<T> setMode(NomadServerMode mode);

  NomadStateChange<T> setLatestChangeUuid(UUID changeUuid);

  NomadStateChange<T> setCurrentVersion(long versionNumber);

  NomadStateChange<T> setHighestVersion(long versionNumber);

  NomadStateChange<T> setLastMutationHost(String lastMutationHost);

  NomadStateChange<T> setLastMutationUser(String lastMutationUser);

  NomadStateChange<T> createChange(UUID changeUuid, ChangeRequest<T> changeRequest);

  NomadStateChange<T> updateChangeRequestState(UUID changeUuid, ChangeRequestState newState);
}
