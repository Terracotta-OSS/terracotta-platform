/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.nomad.messages;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

import static java.util.Objects.requireNonNull;

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
