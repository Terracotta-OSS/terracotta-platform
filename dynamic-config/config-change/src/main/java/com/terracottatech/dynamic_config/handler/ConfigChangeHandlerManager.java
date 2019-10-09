/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.handler;

import com.terracottatech.dynamic_config.model.Setting;

import java.util.Optional;

/**
 * Class responsible to hold a reference to all the {@code com.terracottatech.dynamic_config.handler.ConfigChangeHandler} on the classpath.
 * This is also a service accessible through the diagnostic port to forward to them any Nomad request
 *
 * @author Mathieu Carbou
 */
public interface ConfigChangeHandlerManager {
  /**
   * Register a {@code com.terracottatech.dynamic_config.handler.ConfigChangeHandler} for a
   * {@code com.terracottatech.dynamic_config.model.Setting}.
   *
   * @return false if the addition was not possible because the
   * {@code com.terracottatech.dynamic_config.model.Setting} si already associated
   */
  boolean add(Setting setting, ConfigChangeHandler configChangeHandler);

  /**
   * Removes any {@code com.terracottatech.dynamic_config.handler.ConfigChangeHandler} associated
   * to a {@code com.terracottatech.dynamic_config.model.Setting}
   */
  void remove(Setting setting);

  /**
   * Find an associated {@code com.terracottatech.dynamic_config.handler.ConfigChangeHandler}
   * to a {@code com.terracottatech.dynamic_config.model.Setting}
   */
  Optional<ConfigChangeHandler> findConfigChangeHandler(Setting setting);
}
