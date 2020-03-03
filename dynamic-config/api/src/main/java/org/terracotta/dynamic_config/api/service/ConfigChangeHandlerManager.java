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
package org.terracotta.dynamic_config.api.service;

import org.terracotta.dynamic_config.api.model.Setting;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Class responsible to hold a reference to all the {@code org.terracotta.dynamic_config.handler.ConfigChangeHandler} on the classpath.
 * This is also a service accessible through the diagnostic port to forward to them any Nomad request
 *
 * @author Mathieu Carbou
 */
public interface ConfigChangeHandlerManager {
  /**
   * Register a {@code org.terracotta.dynamic_config.handler.ConfigChangeHandler} for a
   * {@code org.terracotta.dynamic_config.model.Setting}.
   *
   * @return false if the addition was not possible because the
   * {@code org.terracotta.dynamic_config.model.Setting} si already associated
   */
  boolean add(Setting setting, ConfigChangeHandler configChangeHandler);

  /**
   * Removes any {@code org.terracotta.dynamic_config.handler.ConfigChangeHandler} associated
   * to a {@code org.terracotta.dynamic_config.model.Setting}
   */
  void remove(Setting setting);

  /**
   * Find an associated {@code org.terracotta.dynamic_config.handler.ConfigChangeHandler}
   * to a {@code org.terracotta.dynamic_config.model.Setting}
   */
  Optional<ConfigChangeHandler> findConfigChangeHandler(Setting setting);

  /**
   * Register a {@code org.terracotta.dynamic_config.handler.ConfigChangeHandler} for a
   * {@code org.terracotta.dynamic_config.model.Setting}.
   * * {@code java.util.function.Supplier} provides the corresponding {@code org.terracotta.dynamic_config.handler.ConfigChangeHandler}
   *
   * @return false if the addition was not possible because the
   * {@code org.terracotta.dynamic_config.model.Setting} si already associated
   */
  boolean compute(Setting setting, Supplier<ConfigChangeHandler> supplier);
}
