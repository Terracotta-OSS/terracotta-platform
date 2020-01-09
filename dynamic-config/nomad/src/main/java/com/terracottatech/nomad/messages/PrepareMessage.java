/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.nomad.messages;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.terracottatech.nomad.client.change.NomadChange;

import java.time.Instant;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

public class PrepareMessage extends MutativeMessage {
  private final UUID changeUuid;
  private final long versionNumber;
  private final NomadChange change;

  @JsonCreator
  public PrepareMessage(@JsonProperty(value = "expectedMutativeMessageCount", required = true) long expectedMutativeMessageCount,
                        @JsonProperty(value = "mutationHost", required = true) String mutationHost,
                        @JsonProperty(value = "mutationUser", required = true) String mutationUser,
                        @JsonProperty(value = "mutationTimestamp", required = true) Instant mutationTimestamp,
                        @JsonProperty(value = "changeUuid", required = true) UUID changeUuid,
                        @JsonProperty(value = "versionNumber", required = true) long versionNumber,
                        @JsonProperty(value = "change") NomadChange change) {
    super(expectedMutativeMessageCount, mutationHost, mutationUser, mutationTimestamp);
    this.changeUuid = requireNonNull(changeUuid);
    this.versionNumber = versionNumber;
    this.change = change;
  }

  public UUID getChangeUuid() {
    return changeUuid;
  }

  public long getVersionNumber() {
    return versionNumber;
  }

  public NomadChange getChange() {
    return change;
  }

  @Override
  public String toString() {
    return "PrepareMessage{" +
        "changeUuid=" + changeUuid +
        ", versionNumber=" + versionNumber +
        ", change=" + change +
        ", expectedMutativeMessageCount=" + getExpectedMutativeMessageCount() +
        ", mutationHost='" + getMutationHost() + '\'' +
        ", mutationUser='" + getMutationUser() + '\'' +
        ", mutationTimestamp='" + getMutationTimestamp() + '\'' +
        '}';
  }
}
