/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.service.handler;

import com.terracottatech.dynamic_config.handler.ConfigChangeHandler;
import com.terracottatech.dynamic_config.handler.InvalidConfigChangeException;
import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.dynamic_config.model.Configuration;
import com.terracottatech.dynamic_config.model.NodeContext;
import com.terracottatech.dynamic_config.util.IParameterSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.terracottatech.dynamic_config.model.Setting.NODE_BIND_ADDRESS;
import static com.terracottatech.dynamic_config.model.Setting.NODE_GROUP_BIND_ADDRESS;
import static com.terracottatech.utilities.HostAndIpValidator.isValidHost;
import static com.terracottatech.utilities.HostAndIpValidator.isValidIPv4;
import static com.terracottatech.utilities.HostAndIpValidator.isValidIPv6;

public class ServerAttributeConfigChangeHandler implements ConfigChangeHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(ServerAttributeConfigChangeHandler.class);

  private final IParameterSubstitutor parameterSubstitutor;

  public ServerAttributeConfigChangeHandler(IParameterSubstitutor parameterSubstitutor) {
    this.parameterSubstitutor = parameterSubstitutor;
  }

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
      change.apply(updatedCluster, parameterSubstitutor);
      return updatedCluster;
    } catch (Exception e) {
      throw new InvalidConfigChangeException(e.getMessage(), e);
    }
  }

  @Override
  public boolean apply(Configuration change) {
    LOGGER.info("Set {} to: {}", change.getSetting().toString(), change.getValue());
    return false;
  }

  private void validateHostOrIp(String hostOrIp) throws InvalidConfigChangeException {
    if (!isValidHost(hostOrIp) && !isValidIPv4(hostOrIp) && !isValidIPv6(hostOrIp)) {
      throw new InvalidConfigChangeException("bind address should be a valid hostname or IP");
    }
  }
}
