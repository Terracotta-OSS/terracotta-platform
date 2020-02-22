/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.test_support.handler;

import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Configuration;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.service.ConfigChangeHandler;
import org.terracotta.dynamic_config.api.service.InvalidConfigChangeException;

public class GroupPortSimulateHandler implements ConfigChangeHandler {
  @Override
  public void validate(NodeContext nodeContext, Configuration change) throws InvalidConfigChangeException {
    Cluster cluster = nodeContext.getCluster();
    String configVal = change.getValue();
    String tmpConfig[] = configVal.split("#");
    String serverName = tmpConfig[0];
    String groupPort = tmpConfig[1];
    Node node = cluster.getNode(nodeContext.getStripeId(), serverName).get();
    node.setNodeGroupPort(Integer.parseInt(groupPort));
    //TODO [DYNAMIC-CONFIG]: no return anymore. So find another way to hack the config repo xml file to update the bind port. Idea: directly update the xml file written on disk (server section only)
  }
}
