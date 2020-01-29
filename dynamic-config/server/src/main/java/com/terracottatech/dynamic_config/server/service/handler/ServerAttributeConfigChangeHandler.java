/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.server.service.handler;

import com.terracottatech.dynamic_config.api.model.Cluster;
import com.terracottatech.dynamic_config.api.model.Configuration;
import com.terracottatech.dynamic_config.api.model.NodeContext;
import com.terracottatech.dynamic_config.api.service.ConfigChangeHandler;
import com.terracottatech.dynamic_config.api.service.InvalidConfigChangeException;

import static com.terracottatech.dynamic_config.api.model.Setting.NODE_BIND_ADDRESS;
import static com.terracottatech.dynamic_config.api.model.Setting.NODE_GROUP_BIND_ADDRESS;
import static com.terracottatech.inet.HostAndIpValidator.isValidHost;
import static com.terracottatech.inet.HostAndIpValidator.isValidIPv4;
import static com.terracottatech.inet.HostAndIpValidator.isValidIPv6;

public class ServerAttributeConfigChangeHandler implements ConfigChangeHandler {

  @Override
  public Cluster tryApply(NodeContext nodeContext, Configuration change) throws InvalidConfigChangeException {
    if (change.getValue() == null) {
      throw new InvalidConfigChangeException("Invalid change: " + change);
    }

    try {
      if (change.getSetting() == NODE_GROUP_BIND_ADDRESS || change.getSetting() == NODE_BIND_ADDRESS) {
        validateHostOrIp(change.getValue());
      }

      if (change.getSetting() == NODE_BIND_ADDRESS) {
        // When node-bind-address is set, set the node-group-bind-address to the same value because platform does it
        nodeContext.getNode().setNodeGroupBindAddress(change.getValue());
      }

      Cluster updatedCluster = nodeContext.getCluster();
      change.apply(updatedCluster);
      return updatedCluster;
    } catch (RuntimeException e) {
      throw new InvalidConfigChangeException(e.getMessage(), e);
    }
  }

  private void validateHostOrIp(String hostOrIp) throws InvalidConfigChangeException {
    if (!isValidHost(hostOrIp) && !isValidIPv4(hostOrIp) && !isValidIPv6(hostOrIp)) {
      throw new InvalidConfigChangeException("bind address should be a valid hostname or IP");
    }
  }
}
