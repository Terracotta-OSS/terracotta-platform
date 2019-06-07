/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.nomad.messages;

public abstract class MutativeMessage {
  private final long expectedMutativeMessageCount;
  private final String mutationHost;
  private final String mutationUser;

  MutativeMessage(long expectedMutativeMessageCount, String mutationHost, String mutationUser) {
    this.expectedMutativeMessageCount = expectedMutativeMessageCount;
    this.mutationHost = mutationHost;
    this.mutationUser = mutationUser;
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
}
