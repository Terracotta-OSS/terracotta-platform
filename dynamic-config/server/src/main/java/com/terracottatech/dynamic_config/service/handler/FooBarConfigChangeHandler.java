/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.service.handler;

import com.terracottatech.dynamic_config.handler.ConfigChangeHandler;
import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.dynamic_config.model.Configuration;
import com.terracottatech.dynamic_config.model.NodeContext;

/**
 * @author Mathieu Carbou
 */
public class FooBarConfigChangeHandler implements ConfigChangeHandler {
  @Override
  public Cluster tryApply(NodeContext baseConfig, Configuration change) {

    // TODO [DYNAMIC-CONFIG]: TDB-4710: IMPLEMENT TC-PROPERTIES CHANGE

    //TODO here: validate and udpate the cluster model

    return baseConfig.getCluster();
  }

  @Override
  public boolean apply(Configuration change) {
    // TODO [DYNAMIC-CONFIG]: TDB-4710: IMPLEMENT TC-PROPERTIES CHANGE AT RUNTIME
    return true;
  }
}
