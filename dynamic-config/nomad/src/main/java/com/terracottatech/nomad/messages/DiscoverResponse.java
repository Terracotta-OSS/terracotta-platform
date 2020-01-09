/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.nomad.messages;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.terracottatech.nomad.server.NomadServerMode;

import java.time.Instant;

import static java.util.Objects.requireNonNull;

public class DiscoverResponse<T> {
  private final NomadServerMode mode;
  private final long mutativeMessageCount;
  private final String lastMutationHost;
  private final String lastMutationUser;
  private final Instant lastMutationTimestamp;
  private final long currentVersion;
  private final long highestVersion;
  private ChangeDetails<T> latestChange;

  @JsonCreator
  public DiscoverResponse(@JsonProperty(value = "mode", required = true) NomadServerMode mode,
                          @JsonProperty(value = "mutativeMessageCount", required = true) long mutativeMessageCount,
                          @JsonProperty(value = "lastMutationHost") String lastMutationHost,
                          @JsonProperty(value = "lastMutationUser") String lastMutationUser,
                          @JsonProperty(value = "lastMutationTimestamp") Instant lastMutationTimestamp,
                          @JsonProperty(value = "currentVersion", required = true) long currentVersion,
                          @JsonProperty(value = "highestVersion", required = true) long highestVersion,
                          @JsonProperty(value = "latestChange") ChangeDetails<T> latestChange) {
    this.mode = requireNonNull(mode);
    this.mutativeMessageCount = mutativeMessageCount;
    this.lastMutationHost = lastMutationHost;
    this.lastMutationUser = lastMutationUser;
    this.lastMutationTimestamp = lastMutationTimestamp;
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

  public Instant getLastMutationTimestamp() {
    return lastMutationTimestamp;
  }

  public long getCurrentVersion() {
    return currentVersion;
  }

  public long getHighestVersion() {
    return highestVersion;
  }

  public ChangeDetails<T> getLatestChange() {
    return latestChange;
  }
}
