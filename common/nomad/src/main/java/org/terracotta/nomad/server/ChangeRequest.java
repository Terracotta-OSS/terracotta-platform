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
package org.terracotta.nomad.server;

import org.terracotta.nomad.client.change.NomadChange;

import java.time.Instant;
import java.util.UUID;

public class ChangeRequest<T> {
  private final ChangeRequestState state;
  private final long version;
  private final UUID prevChangeId;
  private final NomadChange change;
  private final T changeResult;
  private final String creationHost;
  private final String creationUser;
  private final Instant creationTimestamp;

  public ChangeRequest(ChangeRequestState state, long version, UUID prevChangeId, NomadChange change, T changeResult, String creationHost, String creationUser, Instant creationTimestamp) {
    this.state = state;
    this.version = version;
    this.prevChangeId = prevChangeId;
    this.change = change;
    this.changeResult = changeResult;
    this.creationHost = creationHost;
    this.creationUser = creationUser;
    this.creationTimestamp = creationTimestamp;
  }

  public ChangeRequestState getState() {
    return state;
  }

  public long getVersion() {
    return version;
  }

  public UUID getPrevChangeId() {
    return prevChangeId;
  }

  public NomadChange getChange() {
    return change;
  }

  public T getChangeResult() {
    return changeResult;
  }

  public String getCreationHost() {
    return creationHost;
  }

  public String getCreationUser() {
    return creationUser;
  }

  public Instant getCreationTimestamp() {
    return creationTimestamp;
  }
}
