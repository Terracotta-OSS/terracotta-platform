/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.config.data_roots;

public enum ValidationFailureId {

  MISMATCHED_DATA_DIR_RESOURCE_NUMBERS(800001),
  PLATFORM_DATA_ROOT_MISSING_IN_ONE(800002),
  DIFFERENT_PLATFORM_DATA_ROOTS(800003),
  MISMATCHED_DATA_DIR_NAMES(800004),
  MULTIPLE_PLATFORM_DATA_ROOTS(800005);

  private final long failureId;

  ValidationFailureId(long failureId) {
    this.failureId = failureId;
  }

  public long getFailureId() {
    return failureId;
  }
}