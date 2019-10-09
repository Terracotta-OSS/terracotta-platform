/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.handler;

import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.dynamic_config.model.NodeContext;
import com.terracottatech.dynamic_config.nomad.SettingNomadChange;

/**
 * Handles config changes on the server side
 */
public interface ConfigChangeHandler {
  Cluster tryApply(NodeContext baseConfig, SettingNomadChange change) throws InvalidConfigChangeException;

  void apply(SettingNomadChange change);

  /**
   * Handler that will return null to reject a change
   */
  static ConfigChangeHandler reject() {
    return new ConfigChangeHandler() {
      @Override
      public Cluster tryApply(NodeContext baseConfig, SettingNomadChange change) throws InvalidConfigChangeException {
        return null;
      }

      @Override
      public void apply(SettingNomadChange change) {

      }
    };
  }

  /**
   * Handler that will do nothing
   */
  static ConfigChangeHandler noop() {
    return new ConfigChangeHandler() {
      @Override
      public Cluster tryApply(NodeContext baseConfig, SettingNomadChange change) throws InvalidConfigChangeException {
        return baseConfig.getCluster();
      }

      @Override
      public void apply(SettingNomadChange change) {

      }
    };
  }
}
