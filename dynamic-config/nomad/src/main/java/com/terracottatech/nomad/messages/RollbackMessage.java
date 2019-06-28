/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.nomad.messages;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public class RollbackMessage extends MutativeMessage {
  private final UUID changeUuid;

  @JsonCreator
  public RollbackMessage(@JsonProperty("expectedMutativeMessageCount") long expectedMutativeMessageCount,
                         @JsonProperty("mutationHost") String mutationHost,
                         @JsonProperty("mutationUser") String mutationUser,
                         @JsonProperty("changeUuid") UUID changeUuid) {
    super(expectedMutativeMessageCount, mutationHost, mutationUser);
    this.changeUuid = changeUuid;
  }

  public UUID getChangeUuid() {
    return changeUuid;
  }
}
