/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.nomad.server;

import com.terracottatech.nomad.client.change.NomadChange;

import java.util.UUID;

public class NomadChangeHolder {
  private UUID changeUuid;
  private NomadChange nomadChange;

  public NomadChangeHolder(UUID changeUuid, NomadChange nomadChange) {
    this.changeUuid = changeUuid;
    this.nomadChange = nomadChange;
  }

  public UUID getChangeUuid() {
    return changeUuid;
  }

  public NomadChange getNomadChange() {
    return nomadChange;
  }
}
