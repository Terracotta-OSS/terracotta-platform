/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.test_support.handler;

import com.terracottatech.dynamic_config.api.model.Cluster;
import com.terracottatech.dynamic_config.api.model.Configuration;
import com.terracottatech.dynamic_config.api.model.Node;
import com.terracottatech.dynamic_config.api.model.NodeContext;
import com.terracottatech.dynamic_config.api.service.ConfigChangeHandler;
import com.terracottatech.dynamic_config.api.service.InvalidConfigChangeException;

public class GroupPortSimulateHandler implements ConfigChangeHandler {
  @Override
  public Cluster tryApply(NodeContext nodeContext, Configuration change) throws InvalidConfigChangeException {
    Cluster cluster = nodeContext.getCluster();
    String configVal = change.getValue();
    String tmpConfig[] = configVal.split("#");
    String serverName = tmpConfig[0];
    String groupPort = tmpConfig[1];
    Node node = cluster.getNode(nodeContext.getStripeId(), serverName).get();
    node.setNodeGroupPort(Integer.parseInt(groupPort));
    return cluster;
  }
}
