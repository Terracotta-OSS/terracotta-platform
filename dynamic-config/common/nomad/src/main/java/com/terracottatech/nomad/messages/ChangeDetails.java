/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.nomad.messages;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.terracottatech.nomad.client.change.NomadChange;
import com.terracottatech.nomad.server.ChangeRequestState;

import java.time.Instant;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

public class ChangeDetails<T> {
  private final UUID changeUuid;
  private final ChangeRequestState state;
  private final long version;
  private final NomadChange operation;
  @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
  private final T result;
  private final String creationHost;
  private final String creationUser;
  private final Instant creationTimestamp;

  @JsonCreator
  public ChangeDetails(@JsonProperty(value = "changeUuid", required = true) UUID changeUuid,
                       @JsonProperty(value = "state", required = true) ChangeRequestState state,
                       @JsonProperty(value = "version", required = true) long version,
                       @JsonProperty(value = "operation", required = true) NomadChange operation,
                       @JsonProperty(value = "result") T result,
                       @JsonProperty(value = "creationHost", required = true) String creationHost,
                       @JsonProperty(value = "creationUser", required = true) String creationUser,
                       @JsonProperty(value = "creationTimestamp", required = true) Instant creationTimestamp) {
    this.changeUuid = requireNonNull(changeUuid);
    this.state = requireNonNull(state);
    this.version = version;
    this.operation = requireNonNull(operation);
    this.result = result;
    this.creationHost = requireNonNull(creationHost);
    this.creationUser = requireNonNull(creationUser);
    this.creationTimestamp = creationTimestamp;
  }

  public Instant getCreationTimestamp() {
    return creationTimestamp;
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

  public T getResult() {
    return result;
  }

  public String getCreationHost() {
    return creationHost;
  }

  public String getCreationUser() {
    return creationUser;
  }
}
