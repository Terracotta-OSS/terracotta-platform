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
import org.terracotta.nomad.messages.CommitMessage;
import org.terracotta.nomad.messages.PrepareMessage;
import org.terracotta.nomad.messages.RollbackMessage;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class NomadChangeInfo {
  private final UUID changeUuid;
  private final NomadChange nomadChange;
  private final ChangeRequestState changeRequestState;
  private final long version;
  private final String creationHost;
  private final String creationUser;
  private final Instant creationTimestamp;

  public NomadChangeInfo(UUID changeUuid,
                         NomadChange nomadChange,
                         ChangeRequestState changeRequestState,
                         long version,
                         String creationHost,
                         String creationUser,
                         Instant creationTimestamp) {
    this.changeUuid = changeUuid;
    this.nomadChange = nomadChange;
    this.changeRequestState = changeRequestState;
    this.version = version;
    this.creationHost = creationHost;
    this.creationUser = creationUser;
    this.creationTimestamp = creationTimestamp;
  }

  public Instant getCreationTimestamp() {
    return creationTimestamp;
  }

  public UUID getChangeUuid() {
    return changeUuid;
  }

  public NomadChange getNomadChange() {
    return nomadChange;
  }

  public ChangeRequestState getChangeRequestState() {
    return changeRequestState;
  }

  public String getCreationHost() {
    return creationHost;
  }

  public String getCreationUser() {
    return creationUser;
  }

  public long getVersion() {
    return version;
  }

  // do not add "result": it might be different on each node
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    NomadChangeInfo that = (NomadChangeInfo) o;
    return version == that.version &&
        Objects.equals(changeUuid, that.changeUuid) &&
        Objects.equals(nomadChange, that.nomadChange) &&
        changeRequestState == that.changeRequestState &&
        Objects.equals(creationHost, that.creationHost) &&
        Objects.equals(creationUser, that.creationUser) &&
        Objects.equals(creationTimestamp, that.creationTimestamp);
  }

  // do not add "result": it might be different on each node
  @Override
  public int hashCode() {
    return Objects.hash(changeUuid, nomadChange, changeRequestState, version, creationHost, creationUser, creationTimestamp);
  }

  @Override
  public String toString() {
    return "NomadChangeInfo{" +
        "changeUuid=" + changeUuid +
        ", changeRequestState=" + changeRequestState +
        ", version=" + version +
        ", creationHost=" + creationHost +
        ", creationUser=" + creationUser +
        ", creationTimestamp=" + creationTimestamp +
        ", nomadChange=" + nomadChange.getSummary() +
        '}';
  }


  public PrepareMessage toPrepareMessage(long mutativeMessageCount) {
    return new PrepareMessage(
        mutativeMessageCount,
        getCreationHost(),
        getCreationUser(),
        getCreationTimestamp(),
        getChangeUuid(),
        getVersion(),
        getNomadChange());
  }

  public CommitMessage toCommitMessage(long mutativeMessageCount) {
    return new CommitMessage(
        mutativeMessageCount,
        getCreationHost(),
        getCreationUser(),
        getCreationTimestamp(),
        getChangeUuid());
  }

  public RollbackMessage toRollbackMessage(long mutativeMessageCount) {
    return new RollbackMessage(
        mutativeMessageCount,
        getCreationHost(),
        getCreationUser(),
        getCreationTimestamp(),
        getChangeUuid());
  }
}
