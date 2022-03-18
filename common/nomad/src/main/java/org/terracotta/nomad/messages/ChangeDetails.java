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
package org.terracotta.nomad.messages;

import org.terracotta.nomad.client.change.NomadChange;
import org.terracotta.nomad.server.ChangeRequestState;

import java.time.Instant;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

public class ChangeDetails<T> {
  private final UUID changeUuid;
  private final ChangeRequestState state;
  private final long version;
  private final NomadChange operation;
  private final T result;
  private final String creationHost;
  private final String creationUser;
  private final Instant creationTimestamp;

  public ChangeDetails(UUID changeUuid,
                       ChangeRequestState state,
                       long version,
                       NomadChange operation,
                       T result,
                       String creationHost,
                       String creationUser,
                       Instant creationTimestamp) {
    this.changeUuid = requireNonNull(changeUuid);
    this.state = requireNonNull(state);
    this.version = version;
    this.operation = requireNonNull(operation);
    this.result = result;
    this.creationHost = requireNonNull(creationHost);
    this.creationUser = requireNonNull(creationUser);
    this.creationTimestamp = creationTimestamp;
  }

  public Instant getCreationTimestamp() {
    return creationTimestamp;
  }

  public UUID getChangeUuid() {
    return changeUuid;
  }

  public ChangeRequestState getState() {
    return state;
  }

  public long getVersion() {
    return version;
  }

  public NomadChange getOperation() {
    return operation;
  }

  public T getResult() {
    return result;
  }

  public String getCreationHost() {
    return creationHost;
  }

  public String getCreationUser() {
    return creationUser;
  }
}
