/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.nomad.server;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.terracottatech.nomad.client.change.NomadChange;

import java.util.Objects;
import java.util.UUID;

public class NomadChangeInfo {
  private final UUID changeUuid;
  private final NomadChange nomadChange;
  private final ChangeRequestState changeRequestState;
  private final long version;
  private final String creationHost;
  private final String creationUser;

  @JsonCreator
  public NomadChangeInfo(@JsonProperty(value = "changeUuid", required = true) UUID changeUuid,
                         @JsonProperty(value = "nomadChange", required = true) NomadChange nomadChange,
                         @JsonProperty(value = "changeRequestState", required = true) ChangeRequestState changeRequestState,
                         @JsonProperty(value = "version", required = true) long version,
                         @JsonProperty(value = "creationHost", required = true) String creationHost,
                         @JsonProperty(value = "creationUser", required = true) String creationUser) {
    this.changeUuid = changeUuid;
    this.nomadChange = nomadChange;
    this.changeRequestState = changeRequestState;
    this.version = version;
    this.creationHost = creationHost;
    this.creationUser = creationUser;
  }

  public UUID getChangeUuid() {
    return changeUuid;
  }

  public NomadChange getNomadChange() {
    return nomadChange;
  }

  public ChangeRequestState getChangeRequestState() {
    return changeRequestState;
  }

  public String getCreationHost() {
    return creationHost;
  }

  public String getCreationUser() {
    return creationUser;
  }

  public long getVersion() {
    return version;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    NomadChangeInfo that = (NomadChangeInfo) o;
    return version == that.version &&
        Objects.equals(changeUuid, that.changeUuid) &&
        Objects.equals(nomadChange, that.nomadChange) &&
        changeRequestState == that.changeRequestState &&
        Objects.equals(creationHost, that.creationHost) &&
        Objects.equals(creationUser, that.creationUser);
  }

  @Override
  public int hashCode() {
    return Objects.hash(changeUuid, nomadChange, changeRequestState, version, creationHost, creationUser);
  }

  @Override
  public String toString() {
    return "NomadChangeInfo{" +
        "changeUuid=" + changeUuid +
        ", nomadChange=" + nomadChange +
        ", changeRequestState=" + changeRequestState +
        ", version=" + version +
        ", creationHost=" + creationHost +
        ", creationUser=" + creationUser +
        '}';
  }
}
