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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.Instant;

import static java.util.Objects.requireNonNull;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "messageType")
@JsonSubTypes({
    @JsonSubTypes.Type(name = "COMMIT", value = CommitMessage.class),
    @JsonSubTypes.Type(name = "PREPARE", value = PrepareMessage.class),
    @JsonSubTypes.Type(name = "TAKEOVER", value = TakeoverMessage.class),
    @JsonSubTypes.Type(name = "TAKEOVER", value = TakeoverMessage.class),
})
public abstract class MutativeMessage {
  private final long expectedMutativeMessageCount;
  private final String mutationHost;
  private final String mutationUser;
  private Instant mutationTimestamp;

  @JsonCreator
  MutativeMessage(@JsonProperty(value = "expectedMutativeMessageCount", required = true) long expectedMutativeMessageCount,
                  @JsonProperty(value = "mutationHost", required = true) String mutationHost,
                  @JsonProperty(value = "mutationUser", required = true) String mutationUser,
                  @JsonProperty(value = "mutationTimestamp", required = true) Instant mutationTimestamp) {
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
