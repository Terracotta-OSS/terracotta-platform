/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.nomad.entity.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.terracotta.entity.EntityMessage;
import org.terracotta.nomad.messages.MutativeMessage;

/**
 * @author Mathieu Carbou
 */
public class NomadEntityMessage implements EntityMessage {

  private final MutativeMessage nomadMessage;

  @JsonCreator
  public NomadEntityMessage(@JsonProperty(value = "nomadMessage", required = true) MutativeMessage nomadMessage) {
    this.nomadMessage = nomadMessage;
  }

  public MutativeMessage getNomadMessage() {
    return nomadMessage;
  }
}
