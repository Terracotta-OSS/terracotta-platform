/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.nomad.messages;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class TakeoverMessage extends MutativeMessage {
  @JsonCreator
  public TakeoverMessage(@JsonProperty("expectedMutativeMessageCount") long expectedMutativeMessageCount,
                         @JsonProperty("mutationHost") String mutationHost,
                         @JsonProperty("mutationUser") String mutationUser) {
    super(expectedMutativeMessageCount, mutationHost, mutationUser);
  }
}
