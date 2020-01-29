/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.server.service.handler;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Configuration;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.service.ConfigChangeHandler;
import org.terracotta.dynamic_config.api.service.InvalidConfigChangeException;
import org.terracotta.dynamic_config.api.service.TopologyService;
import org.slf4j.LoggerFactory;

import static java.util.Objects.requireNonNull;

/**
 * @author Mathieu Carbou
 */
public class LoggerOverrideConfigChangeHandler implements ConfigChangeHandler {

  private final TopologyService topologyService;

  public LoggerOverrideConfigChangeHandler(TopologyService topologyService) {
    this.topologyService = requireNonNull(topologyService);
  }

  @Override
  public Cluster tryApply(NodeContext nodeContext, Configuration change) throws InvalidConfigChangeException {
    String logger = change.getKey();
    String level = change.getValue();

    // verify enum
    if (level != null) {
      try {
        Level.valueOf(level);
      } catch (RuntimeException e) {
        throw new InvalidConfigChangeException("Illegal level: " + level, e);
      }
    }

    // verify we can access the logger
    LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    Logger logbackLogger = loggerContext.getLogger(logger);

    // verify illegal op
    if (Logger.ROOT_LOGGER_NAME.equals(logbackLogger.getName()) && level == null) {
      throw new InvalidConfigChangeException("Cannot remove the root logger");
    }

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
    LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    String logger = change.getKey();
    String level = change.getValue();
    // setting the level to null will inherit from the parent
    loggerContext.getLogger(logger).setLevel(level == null ? null : Level.valueOf(level));
    return true;
  }

  public void init() {
    LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    topologyService.getUpcomingNodeContext().getNode().getNodeLoggerOverrides()
        .forEach((name, level) -> loggerContext.getLogger(name).setLevel(Level.valueOf(level.name())));
  }
}
