/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.nomad.server.state;

import org.terracotta.nomad.server.ChangeRequest;
import org.terracotta.nomad.server.ChangeRequestState;
import org.terracotta.nomad.server.NomadServerMode;
import org.terracotta.nomad.server.NomadServerRequest;

import java.time.Instant;
import java.util.UUID;

public interface NomadStateChange<T> {
  NomadStateChange<T> setInitialized();

  NomadStateChange<T> setMode(NomadServerMode mode);

  NomadStateChange<T> setRequest(NomadServerRequest request);

  NomadStateChange<T> setLatestChangeUuid(UUID changeUuid);

  NomadStateChange<T> setCurrentVersion(long versionNumber);

  NomadStateChange<T> setHighestVersion(long versionNumber);

  NomadStateChange<T> setLastMutationHost(String lastMutationHost);

  NomadStateChange<T> setLastMutationUser(String lastMutationUser);

  NomadStateChange<T> setLastMutationTimestamp(Instant timestamp);

  NomadStateChange<T> createChange(UUID changeUuid, ChangeRequest<T> changeRequest);

  NomadStateChange<T> updateChangeRequestState(UUID changeUuid, ChangeRequestState newState);
}
