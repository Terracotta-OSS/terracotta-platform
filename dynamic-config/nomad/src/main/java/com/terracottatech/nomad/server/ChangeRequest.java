/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.nomad.server;

import com.terracottatech.nomad.client.change.NomadChange;

public class ChangeRequest {
  private final ChangeRequestState state;
  private final long version;
  private final NomadChange change;
  private final String changeResult;
  private final String creationHost;
  private final String creationUser;

  public ChangeRequest(ChangeRequestState state, long version, NomadChange change, String changeResult, String creationHost, String creationUser) {
    this.state = state;
    this.version = version;
    this.change = change;
    this.changeResult = changeResult;
    this.creationHost = creationHost;
    this.creationUser = creationUser;
  }

  public ChangeRequestState getState() {
    return state;
  }

  public long getVersion() {
    return version;
  }

  public NomadChange getChange() {
    return change;
  }

  public String getChangeResult() {
    return changeResult;
  }

  public String getCreationHost() {
    return creationHost;
  }

  public String getCreationUser() {
    return creationUser;
  }
}
