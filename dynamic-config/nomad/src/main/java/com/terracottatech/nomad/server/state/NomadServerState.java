/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.nomad.server.state;

import com.terracottatech.nomad.server.ChangeRequest;
import com.terracottatech.nomad.server.NomadException;
import com.terracottatech.nomad.server.NomadServerMode;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface NomadServerState<T> {
  boolean isInitialized();

  NomadServerMode getMode();

  long getMutativeMessageCount();

  String getLastMutationHost();

  String getLastMutationUser();

  Instant getLastMutationTimestamp();

  UUID getLatestChangeUuid();

  long getCurrentVersion();

  long getHighestVersion();

  ChangeRequest<T> getChangeRequest(UUID changeUuid) throws NomadException;

  NomadStateChange<T> newStateChange();

  void applyStateChange(NomadStateChange<T> change) throws NomadException;

  Optional<T> getCurrentCommittedChangeResult() throws NomadException;
}
