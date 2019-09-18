/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.model;

/**
 * @author Mathieu Carbou
 */
public enum Operation {
  GET(false),
  SET(true),
  UNSET(false);

  private final boolean valueRequired;

  Operation(boolean valueRequired) {
    this.valueRequired = valueRequired;
  }

  public boolean isValueRequired() {
    return valueRequired;
  }

  @Override
  public String toString() {
    return name().toLowerCase();
  }
}
