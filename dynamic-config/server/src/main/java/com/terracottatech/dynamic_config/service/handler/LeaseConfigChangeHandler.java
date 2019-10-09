/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.service.handler;

import com.terracottatech.dynamic_config.handler.ConfigChangeHandler;
import com.terracottatech.dynamic_config.handler.InvalidConfigChangeException;
import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.dynamic_config.model.NodeContext;
import com.terracottatech.dynamic_config.nomad.SettingNomadChange;

/**
 * @author Mathieu Carbou
 */
public class LeaseConfigChangeHandler implements ConfigChangeHandler {

  @Override
  public Cluster tryApply(NodeContext baseConfig, SettingNomadChange configChange) throws InvalidConfigChangeException {

    // TODO [DYNAMIC-CONFIG]: TDB-4709: IMPLEMENT LEASE CHANGE: validate only and apply the change to the incoming cluster

    return baseConfig.getCluster();
  }

  @Override
  public void apply(SettingNomadChange configChange) {
    // do nothing: lease update will be applied on restart
  }
}
