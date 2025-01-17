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
import org.terracotta.nomad.server.NomadException;
import org.terracotta.nomad.server.NomadServerMode;
import org.terracotta.nomad.server.NomadServerRequest;

import java.time.Instant;
import java.util.UUID;

public interface NomadStateChange<T> {
  NomadStateChange<T> setInitialized() throws NomadException;

  NomadStateChange<T> setMode(NomadServerMode mode) throws NomadException;

  NomadStateChange<T> setRequest(NomadServerRequest request) throws NomadException;

  NomadStateChange<T> setLatestChangeUuid(UUID changeUuid) throws NomadException;

  NomadStateChange<T> setCurrentVersion(long versionNumber) throws NomadException;

  NomadStateChange<T> setHighestVersion(long versionNumber) throws NomadException;

  NomadStateChange<T> setLastMutationHost(String lastMutationHost) throws NomadException;

  NomadStateChange<T> setLastMutationUser(String lastMutationUser) throws NomadException;

  NomadStateChange<T> setLastMutationTimestamp(Instant timestamp) throws NomadException;

  NomadStateChange<T> createChange(UUID changeUuid, ChangeRequest<T> changeRequest) throws NomadException;

  NomadStateChange<T> updateChangeRequestState(UUID changeUuid, ChangeRequestState newState) throws NomadException;
}
