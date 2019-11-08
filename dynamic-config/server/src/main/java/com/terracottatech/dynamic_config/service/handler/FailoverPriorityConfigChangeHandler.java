/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.service.handler;

import com.terracottatech.dynamic_config.handler.ConfigChangeHandler;
import com.terracottatech.dynamic_config.handler.InvalidConfigChangeException;
import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.dynamic_config.model.Configuration;
import com.terracottatech.dynamic_config.model.FailoverPriority;
import com.terracottatech.dynamic_config.model.NodeContext;
import com.terracottatech.dynamic_config.util.IParameterSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FailoverPriorityConfigChangeHandler implements ConfigChangeHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(FailoverPriorityConfigChangeHandler.class);

  private final IParameterSubstitutor parameterSubstitutor;

  public FailoverPriorityConfigChangeHandler(IParameterSubstitutor parameterSubstitutor) {
    this.parameterSubstitutor = parameterSubstitutor;
  }

  @Override
  public Cluster tryApply(final NodeContext nodeContext, final Configuration change) throws InvalidConfigChangeException {
    if (change.getValue() == null) {
      throw new InvalidConfigChangeException("Invalid change: " + change);
    }
    Cluster updatedCluster = nodeContext.getCluster();
    try {
      FailoverPriority.valueOf(change.getValue());
      change.apply(updatedCluster, parameterSubstitutor);
    } catch (Exception e) {
      throw new InvalidConfigChangeException(e.getMessage(), e);
    }
    return updatedCluster;
  }

  @Override
  public boolean apply(Configuration change) {
    LOGGER.info("Set failover-priority to: {}", change.getValue());
    return false;
  }
}
