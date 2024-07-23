/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
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

import static java.util.Objects.requireNonNull;

public abstract class MutativeMessage {
  private final long expectedMutativeMessageCount;
  private final String mutationHost;
  private final String mutationUser;
  private final Instant mutationTimestamp;

  // For Json
  MutativeMessage() {
    expectedMutativeMessageCount = 0;
    mutationHost = null;
    mutationUser = null;
    mutationTimestamp = null;
  }

  protected MutativeMessage(long expectedMutativeMessageCount,
                            String mutationHost,
                            String mutationUser,
                            Instant mutationTimestamp) {
    this.expectedMutativeMessageCount = expectedMutativeMessageCount;
    this.mutationHost = requireNonNull(mutationHost);
    this.mutationUser = requireNonNull(mutationUser);
    this.mutationTimestamp = mutationTimestamp;
  }

  public long getExpectedMutativeMessageCount() {
    return expectedMutativeMessageCount;
  }

  public String getMutationHost() {
    return mutationHost;
  }

  public String getMutationUser() {
    return mutationUser;
  }

  public Instant getMutationTimestamp() {
    return mutationTimestamp;
  }
}
