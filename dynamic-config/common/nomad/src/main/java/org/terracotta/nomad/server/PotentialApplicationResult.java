/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.nomad.server;

public class PotentialApplicationResult<T> {
  private final boolean allowed;
  private final T newConfiguration;
  private final String rejectionReason;

  public static <T> PotentialApplicationResult<T> allow(T newConfiguration) {
    return new PotentialApplicationResult<>(true, newConfiguration, null);
  }

  public static <T> PotentialApplicationResult<T> reject(String reason) {
    return new PotentialApplicationResult<>(false, null, reason);
  }

  private PotentialApplicationResult(boolean allowed, T newConfiguration, String rejectionReason) {
    this.allowed = allowed;
    this.newConfiguration = newConfiguration;
    this.rejectionReason = rejectionReason;
  }

  public boolean isAllowed() {
    return allowed;
  }

  public T getNewConfiguration() {
    return newConfiguration;
  }

  public String getRejectionReason() {
    return rejectionReason;
  }
}
