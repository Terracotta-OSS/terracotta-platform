/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config;

import com.terracottatech.dynamic_config.model.NodeContext;
import com.terracottatech.dynamic_config.nomad.SettingNomadChange;
import com.terracottatech.dynamic_config.nomad.SettingNomadChange.Type;
import org.terracotta.entity.PlatformConfiguration;

/**
 * Handles config changes on the server side
 */
public interface ConfigChangeHandler {

  Type getType();

  void initialize(PlatformConfiguration platformConfiguration);

  NodeContext tryApply(NodeContext baseConfig, SettingNomadChange change) throws InvalidConfigChangeException;

  void apply(SettingNomadChange change);
}
