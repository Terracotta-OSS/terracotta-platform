/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.nomad.messages;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.terracottatech.nomad.client.change.NomadChange;
import com.terracottatech.nomad.server.ChangeRequestState;

import java.util.UUID;

public class ChangeDetails {
  private final UUID changeUuid;
  private final ChangeRequestState state;
  private final long version;
  private final NomadChange operation;
  private final String result;
  private final String creationHost;
  private final String creationUser;

  @JsonCreator
  public ChangeDetails(@JsonProperty("changeUuid") UUID changeUuid,
                       @JsonProperty("state") ChangeRequestState state,
                       @JsonProperty("version") long version,
                       @JsonProperty("operation") NomadChange operation,
                       @JsonProperty("result") String result,
                       @JsonProperty("creationHost") String creationHost,
                       @JsonProperty("creationUser") String creationUser) {
    this.changeUuid = changeUuid;
    this.state = state;
    this.version = version;
    this.operation = operation;
    this.result = result;
    this.creationHost = creationHost;
    this.creationUser = creationUser;
  }

  public UUID getChangeUuid() {
    return changeUuid;
  }

  public ChangeRequestState getState() {
    return state;
  }

  public long getVersion() {
    return version;
  }

  public NomadChange getOperation() {
    return operation;
  }

  public String getResult() {
    return result;
  }

  public String getCreationHost() {
    return creationHost;
  }

  public String getCreationUser() {
    return creationUser;
  }
}
