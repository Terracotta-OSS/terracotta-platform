/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.nomad.messages;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.terracottatech.nomad.client.change.NomadChange;

import java.util.UUID;

public class PrepareMessage extends MutativeMessage {
  private final UUID changeUuid;
  private final long versionNumber;
  private final NomadChange change;

  @JsonCreator
  public PrepareMessage(@JsonProperty("expectedMutativeMessageCount") long expectedMutativeMessageCount,
                        @JsonProperty("mutationHost") String mutationHost,
                        @JsonProperty("mutationUser") String mutationUser,
                        @JsonProperty("changeUuid") UUID changeUuid,
                        @JsonProperty("versionNumber") long versionNumber,
                        @JsonProperty("change") NomadChange change) {
    super(expectedMutativeMessageCount, mutationHost, mutationUser);
    this.changeUuid = changeUuid;
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
}
