/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.nomad.server;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.terracotta.nomad.client.change.NomadChange;

import java.time.Instant;

public class ChangeRequest<T> {
  private final ChangeRequestState state;
  private final long version;
  private final String prevChangeId;
  private final NomadChange change;
  @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
  private final T changeResult;
  private final String creationHost;
  private final String creationUser;
  private final Instant creationTimestamp;

  public ChangeRequest(ChangeRequestState state, long version, String prevChangeId, NomadChange change, T changeResult, String creationHost, String creationUser, Instant creationTimestamp) {
    this.state = state;
    this.version = version;
    this.prevChangeId = prevChangeId;
    this.change = change;
    this.changeResult = changeResult;
    this.creationHost = creationHost;
    this.creationUser = creationUser;
    this.creationTimestamp = creationTimestamp;
  }

  public ChangeRequestState getState() {
    return state;
  }

  public long getVersion() {
    return version;
  }

  public String getPrevChangeId() {
    return prevChangeId;
  }

  public NomadChange getChange() {
    return change;
  }

  public T getChangeResult() {
    return changeResult;
  }

  public String getCreationHost() {
    return creationHost;
  }

  public String getCreationUser() {
    return creationUser;
  }

  public Instant getCreationTimestamp() {
    return creationTimestamp;
  }
}