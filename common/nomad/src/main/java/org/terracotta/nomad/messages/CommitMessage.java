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

import java.time.Instant;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

public class CommitMessage extends MutativeMessage {
  private final UUID changeUuid;

  // For Json
  CommitMessage() {
    changeUuid = null;
  }

  public CommitMessage(long expectedMutativeMessageCount,
                       String mutationHost,
                       String mutationUser,
                       Instant mutationTimestamp,
                       UUID changeUuid) {
    super(expectedMutativeMessageCount, mutationHost, mutationUser, mutationTimestamp);
    this.changeUuid = requireNonNull(changeUuid);
  }

  public UUID getChangeUuid() {
    return changeUuid;
  }

  @Override
  public String toString() {
    return "CommitMessage{" +
        "changeUuid=" + changeUuid +
        ", expectedMutativeMessageCount=" + getExpectedMutativeMessageCount() +
        ", mutationHost='" + getMutationHost() + '\'' +
        ", mutationUser='" + getMutationUser() + '\'' +
        ", mutationTimestamp='" + getMutationTimestamp() + '\'' +
        '}';
  }
}
