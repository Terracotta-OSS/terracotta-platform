/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.nomad.server.state;

import com.terracottatech.nomad.server.ChangeRequest;
import com.terracottatech.nomad.server.ChangeRequestState;
import com.terracottatech.nomad.server.NomadServerMode;

import java.util.UUID;

public interface NomadStateChange {
  NomadStateChange setInitialized();

  NomadStateChange setMode(NomadServerMode mode);

  NomadStateChange setLatestChangeUuid(UUID changeUuid);

  NomadStateChange setCurrentVersion(long versionNumber);

  NomadStateChange setHighestVersion(long versionNumber);

  NomadStateChange setLastMutationHost(String lastMutationHost);

  NomadStateChange setLastMutationUser(String lastMutationUser);

  NomadStateChange createChange(UUID changeUuid, ChangeRequest changeRequest);

  NomadStateChange updateChangeRequestState(UUID changeUuid, ChangeRequestState newState);
}
