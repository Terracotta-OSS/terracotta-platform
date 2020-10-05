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

import org.terracotta.nomad.server.ChangeState;
import org.terracotta.nomad.server.NomadException;
import org.terracotta.nomad.server.NomadServerMode;

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

  ChangeState<T> getChangeState(UUID changeUuid) throws NomadException;

  NomadStateChange<T> newStateChange();

  void applyStateChange(NomadStateChange<T> change) throws NomadException;

  Optional<T> getCurrentCommittedConfig() throws NomadException;

  void reset() throws NomadException;
}
