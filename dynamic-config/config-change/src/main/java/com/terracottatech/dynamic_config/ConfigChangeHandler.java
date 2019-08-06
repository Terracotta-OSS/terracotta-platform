/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config;

import org.terracotta.entity.PlatformConfiguration;

/**
 * Handles config changes on the server side
 */
public interface ConfigChangeHandler {

  enum Type {
    OFFHEAP("offheap-resources"),
    DATA_ROOT("data-roots");

    String name;

    Type(String name) {
      this.name = name;
    }

    @Override
    public String toString() {
      return name;
    }
  }

  Type getType();

  void initialize(PlatformConfiguration platformConfiguration);

  String tryApply(String baseConfig, String change) throws InvalidConfigChangeException;

  void apply(String change);
}
