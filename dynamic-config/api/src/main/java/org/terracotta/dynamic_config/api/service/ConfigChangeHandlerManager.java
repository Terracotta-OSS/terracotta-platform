/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
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
