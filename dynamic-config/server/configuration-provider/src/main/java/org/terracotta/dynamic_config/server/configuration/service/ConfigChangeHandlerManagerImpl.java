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
package org.terracotta.dynamic_config.server.configuration.service;

import org.terracotta.dynamic_config.api.model.Setting;
import org.terracotta.dynamic_config.server.api.ConfigChangeHandler;
import org.terracotta.dynamic_config.server.api.ConfigChangeHandlerManager;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * @author Mathieu Carbou
 */
public class ConfigChangeHandlerManagerImpl implements ConfigChangeHandlerManager {

  private final Map<Setting, ConfigChangeHandler> changeHandlers = new ConcurrentHashMap<>();

  @Override
  public boolean add(Setting setting, ConfigChangeHandler configChangeHandler) {
    return changeHandlers.putIfAbsent(setting, configChangeHandler) == null;
  }

  @Override
  public void remove(Setting setting) {
    changeHandlers.remove(setting);
  }

  @Override
  public Optional<ConfigChangeHandler> findConfigChangeHandler(Setting setting) {
    return Optional.ofNullable(changeHandlers.get(setting));
  }

  @Override
  public boolean compute(Setting setting, Supplier<ConfigChangeHandler> supplier) {
    return changeHandlers.computeIfAbsent(setting, setting1 -> supplier.get()) == null;
  }
}
