/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.nomad.messages;

import com.terracottatech.nomad.server.NomadServerMode;

public class DiscoverResponse {
  private final NomadServerMode mode;
  private final long mutativeMessageCount;
  private final String lastMutationHost;
  private final String lastMutationUser;
  private final long currentVersion;
  private final long highestVersion;
  private ChangeDetails latestChange;

  public DiscoverResponse(
      NomadServerMode mode,
      long mutativeMessageCount,
      String lastMutationHost,
      String lastMutationUser,
      long currentVersion,
      long highestVersion,
      ChangeDetails latestChange
  ) {
    this.mode = mode;
    this.mutativeMessageCount = mutativeMessageCount;
    this.lastMutationHost = lastMutationHost;
    this.lastMutationUser = lastMutationUser;
    this.currentVersion = currentVersion;
    this.highestVersion = highestVersion;
    this.latestChange = latestChange;
  }

  public NomadServerMode getMode() {
    return mode;
  }

  public long getMutativeMessageCount() {
    return mutativeMessageCount;
  }

  public String getLastMutationHost() {
    return lastMutationHost;
  }

  public String getLastMutationUser() {
    return lastMutationUser;
  }

  public long getCurrentVersion() {
    return currentVersion;
  }

  public long getHighestVersion() {
    return highestVersion;
  }

  public ChangeDetails getLatestChange() {
    return latestChange;
  }
}
