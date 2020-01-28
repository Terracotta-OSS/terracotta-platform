/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.server.service;

import com.terracottatech.dynamic_config.api.model.Setting;
import com.terracottatech.dynamic_config.api.service.ConfigChangeHandler;
import com.terracottatech.dynamic_config.api.service.ConfigChangeHandlerManager;

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
