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
