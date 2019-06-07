/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.nomad.server;

public class PotentialApplicationResult {
  private final boolean allowed;
  private final String newConfiguration;
  private final String rejectionReason;

  public static PotentialApplicationResult allow(String newConfiguration) {
    return new PotentialApplicationResult(true, newConfiguration, null);
  }

  public static PotentialApplicationResult reject(String reason) {
    return new PotentialApplicationResult(false, null, reason);
  }

  private PotentialApplicationResult(boolean allowed, String newConfiguration, String rejectionReason) {
    this.allowed = allowed;
    this.newConfiguration = newConfiguration;
    this.rejectionReason = rejectionReason;
  }

  public boolean isAllowed() {
    return allowed;
  }

  public String getNewConfiguration() {
    return newConfiguration;
  }

  public String getRejectionReason() {
    return rejectionReason;
  }
}
