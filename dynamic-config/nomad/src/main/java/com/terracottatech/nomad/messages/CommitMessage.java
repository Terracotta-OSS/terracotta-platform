/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.nomad.messages;

import java.util.UUID;

public class CommitMessage extends MutativeMessage {
  private final UUID changeUuid;

  public CommitMessage(long expectedMutativeMessageCount, String mutationHost, String mutationUser, UUID changeUuid) {
    super(expectedMutativeMessageCount, mutationHost, mutationUser);
    this.changeUuid = changeUuid;
  }

  public UUID getChangeUuid() {
    return changeUuid;
  }
}
