/*
 * Copyright Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.dynamic_config.server.service.handler;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import org.slf4j.LoggerFactory;
import org.terracotta.dynamic_config.api.model.Configuration;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.service.TopologyService;
import org.terracotta.dynamic_config.server.api.ConfigChangeHandler;
import org.terracotta.dynamic_config.server.api.InvalidConfigChangeException;

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
  public void validate(NodeContext nodeContext, Configuration change) throws InvalidConfigChangeException {
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
  }

  @Override
  public void apply(Configuration change) {
    LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    String logger = change.getKey();
    String level = change.getValue();
    // setting the level to null will inherit from the parent
    loggerContext.getLogger(logger).setLevel(level == null ? null : Level.valueOf(level));
  }

  public void init() {
    LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    topologyService.getUpcomingNodeContext().getNode().getLoggerOverrides()
        .forEach((name, level) -> loggerContext.getLogger(name).setLevel(Level.valueOf(level)));
  }
}
