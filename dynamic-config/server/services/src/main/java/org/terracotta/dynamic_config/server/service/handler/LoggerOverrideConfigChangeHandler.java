/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
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
import org.terracotta.dynamic_config.api.server.ConfigChangeHandler;
import org.terracotta.dynamic_config.api.server.InvalidConfigChangeException;

import java.util.Collections;

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
  public void validate(NodeContext nodeContext, Configuration changes) throws InvalidConfigChangeException {
    LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

    if (!changes.hasValue()) {
      if (changes.getKey() != null) {
        // removal of a specific logger ?
        Logger logbackLogger = loggerContext.getLogger(changes.getKey());

        // verify illegal op
        if (Logger.ROOT_LOGGER_NAME.equals(logbackLogger.getName())) {
          throw new InvalidConfigChangeException("Cannot remove the root logger");
        }
      }

    } else {
      for (Configuration change : changes.expand()) {
        String logger = change.getKey();
        String level = change.getValue().get(); // we have a value otherwise the config was not valid

        // verify enum
        try {
          Level.valueOf(level);
        } catch (RuntimeException e) {
          throw new InvalidConfigChangeException("Illegal level: " + level, e);
        }

        // verify we can access the logger
        loggerContext.getLogger(logger);
      }
    }
  }

  @Override
  public void apply(Configuration changes) {
    LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

    if (!changes.hasValue()) {

      if (changes.getKey() != null) {
        // removal of a specific logger ?
        loggerContext.getLogger(changes.getKey()).setLevel(null);

      } else {
        // remove all configured loggers ?
        topologyService.getRuntimeNodeContext().getNode().getLoggerOverrides()
            .orElse(Collections.emptyMap())
            .keySet()
            .forEach(logger -> loggerContext.getLogger(logger).setLevel(null));
      }

    } else {
      for (Configuration change : changes.expand()) {
        String logger = change.getKey();
        String level = change.getValue().get();
        // setting the level to null will inherit from the parent
        loggerContext.getLogger(logger).setLevel(Level.valueOf(level));
      }
    }
  }

  public void init() {
    LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    topologyService.getUpcomingNodeContext().getNode().getLoggerOverrides().orDefault()
        .forEach((name, level) -> loggerContext.getLogger(name).setLevel(Level.valueOf(level)));
  }
}
