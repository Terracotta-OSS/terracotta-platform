/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
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
package org.terracotta.dynamic_config.server.configuration.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.dynamic_config.api.model.Setting;
import org.terracotta.dynamic_config.server.api.ConfigChangeHandler;
import org.terracotta.dynamic_config.server.api.ConfigChangeHandlerManager;
import org.terracotta.entity.StateDumpCollector;
import org.terracotta.entity.StateDumpable;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Mathieu Carbou
 */
public class ConfigChangeHandlerManagerImpl implements ConfigChangeHandlerManager, StateDumpable {
  private static final Logger LOGGER = LoggerFactory.getLogger(ConfigChangeHandlerManagerImpl.class);

  private final Map<Setting, ConfigChangeHandler> changeHandlers = new ConcurrentHashMap<>();

  @Override
  public ConfigChangeHandler set(Setting setting, ConfigChangeHandler configChangeHandler) {
    LOGGER.info("Registered dynamic configuration change handler for setting {}: {}", setting, configChangeHandler);
    return changeHandlers.put(setting, configChangeHandler);
  }

  @Override
  public void clear(Setting setting) {
    LOGGER.info("Removing dynamic configuration change handler for setting {}", setting);
    changeHandlers.remove(setting);
  }

  @Override
  public Optional<ConfigChangeHandler> findConfigChangeHandler(Setting setting) {
    return Optional.ofNullable(changeHandlers.get(setting));
  }

  @Override
  public void addStateTo(StateDumpCollector stateDumpCollector) {
    StateDumpCollector configChangeHandlers = stateDumpCollector.subStateDumpCollector("configChangeHandlers");
    this.changeHandlers.entrySet()
        .stream()
        .sorted(Comparator.comparing(e -> e.getKey().toString()))
        .forEach(e -> {
          if (e.getValue() instanceof StateDumpable) {
            ((StateDumpable) e.getValue()).addStateTo(configChangeHandlers.subStateDumpCollector(e.getKey().toString()));
          } else {
            configChangeHandlers.addState(e.getKey().toString(), e.getValue().toString());
          }
        });
  }
}
