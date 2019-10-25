/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.nomad.server;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.terracottatech.nomad.client.change.NomadChange;

import java.util.Objects;
import java.util.UUID;

public class NomadChangeInfo {
  private UUID changeUuid;
  private NomadChange nomadChange;

  @JsonCreator
  public NomadChangeInfo(@JsonProperty(value = "changeUuid", required = true) UUID changeUuid,
                         @JsonProperty (value = "nomadChange", required = true) NomadChange nomadChange) {
    this.changeUuid = changeUuid;
    this.nomadChange = nomadChange;
  }

  public UUID getChangeUuid() {
    return changeUuid;
  }

  public NomadChange getNomadChange() {
    return nomadChange;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    final NomadChangeInfo that = (NomadChangeInfo)o;
    return Objects.equals(changeUuid, that.changeUuid) &&
           Objects.equals(nomadChange, that.nomadChange);
  }

  @Override
  public int hashCode() {
    return Objects.hash(changeUuid, nomadChange);
  }

  @Override
  public String toString() {
    return "NomadChangeInfo{" +
           "changeUuid=" + changeUuid +
           ", nomadChange=" + nomadChange +
           '}';
  }
}
