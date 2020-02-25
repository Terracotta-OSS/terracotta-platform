/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.api.service;

import org.terracotta.dynamic_config.api.model.Configuration;
import org.terracotta.dynamic_config.api.model.NodeContext;

/**
 * Handles config changes on the server side
 */
public interface ConfigChangeHandler {

  /**
   * Validate a change a change and throw if invalid
   */
  default void validate(NodeContext nodeContext, Configuration change) throws InvalidConfigChangeException {}


  /**
   * Apply a change at runtime on the server
   */
  default void apply(Configuration change) {
  }

  /**
   * Handler that will return null to reject a change
   */
  static ConfigChangeHandler reject() {
    return new ConfigChangeHandler() {
      @Override
      public void validate(NodeContext nodeContext, Configuration change) throws InvalidConfigChangeException {
        throw new InvalidConfigChangeException("Unable to apply this change: " + change);
      }

      @Override
      public String toString() {
        return "ConfigChangeHandler#reject()";
      }
    };
  }

  /**
   * Handler that will just apply the change after a restart
   */
  static ConfigChangeHandler accept() {
    return new ConfigChangeHandler() {
      @Override
      public String toString() {
        return "ConfigChangeHandler#accept()";
      }
    };
  }
}
