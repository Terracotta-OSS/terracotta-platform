/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.api.service;

import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Configuration;
import org.terracotta.dynamic_config.api.model.NodeContext;

/**
 * Handles config changes on the server side
 */
public interface ConfigChangeHandler {

  /**
   * Try to apply a change to the provided topology and returns the updated topology (or non updated).
   *
   * @return the (eventually) updated cluster object, or null if we reject the change
   */
  Cluster tryApply(NodeContext nodeContext, Configuration change) throws InvalidConfigChangeException;


  /**
   * Apply a change at runtime on the server
   *
   * @return true if the change was applied at runtime. Returning true means that the runtime topology will also need to be updated.
   */
  default boolean apply(Configuration change) {
    return false;
  }

  /**
   * Handler that will return null to reject a change
   */
  static ConfigChangeHandler reject() {
    return new ConfigChangeHandler() {
      @Override
      public Cluster tryApply(NodeContext nodeContext, Configuration change) {
        return null;
      }

      @Override
      public String toString() {
        return "ConfigChangeHandler#reject()";
      }
    };
  }

  /**
   * Handler that will do nothing
   */
  static ConfigChangeHandler noop() {
    return new ConfigChangeHandler() {
      @Override
      public Cluster tryApply(NodeContext nodeContext, Configuration change) {
        return nodeContext.getCluster();
      }

      @Override
      public String toString() {
        return "ConfigChangeHandler#noop()";
      }
    };
  }

  /**
   * Handler that will just apply the change after a restart
   */
  static ConfigChangeHandler applyAfterRestart() {
    return new ConfigChangeHandler() {
      @Override
      public Cluster tryApply(NodeContext nodeContext, Configuration change) throws InvalidConfigChangeException {
        try {
          Cluster updatedCluster = nodeContext.getCluster();
          change.apply(updatedCluster);
          return updatedCluster;
        } catch (RuntimeException e) {
          throw new InvalidConfigChangeException(e.getMessage(), e);
        }
      }

      @Override
      public String toString() {
        return "ConfigChangeHandler#applyAfterRestart()";
      }
    };
  }

  /**
   * Handler that will just apply the change at runtime
   */
  static ConfigChangeHandler applyAtRuntime() {

    return new ConfigChangeHandler() {
      @Override
      public Cluster tryApply(NodeContext nodeContext, Configuration change) throws InvalidConfigChangeException {
        try {
          Cluster updatedCluster = nodeContext.getCluster();
          change.apply(updatedCluster);
          return updatedCluster;
        } catch (RuntimeException e) {
          throw new InvalidConfigChangeException(e.getMessage(), e);
        }
      }

      @Override
      public boolean apply(Configuration change) {
        return true;
      }

      @Override
      public String toString() {
        return "ConfigChangeHandler#applyAtRuntime()";
      }
    };
  }
}