/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.nomad.processor;

import com.terracottatech.dynamic_config.ConfigChangeHandler;
import com.terracottatech.dynamic_config.ConfigChangeHandler.Type;
import com.terracottatech.dynamic_config.InvalidConfigChangeException;
import com.terracottatech.dynamic_config.nomad.SettingNomadChange;
import com.terracottatech.nomad.server.NomadException;

import org.terracotta.entity.PlatformConfiguration;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.ServiceLoader;

import static java.util.Objects.requireNonNull;

/**
 * Supports the processing of {@link SettingNomadChange} for dynamic configuration
 */
public class SettingNomadChangeProcessor implements NomadChangeProcessor<SettingNomadChange> {
  private static final Map<Type, ConfigChangeHandler> CHANGE_HANDLERS = Collections.synchronizedMap(new EnumMap<>(Type.class));

  private static SettingNomadChangeProcessor INSTANCE = new SettingNomadChangeProcessor();

  public static SettingNomadChangeProcessor get() {
    return INSTANCE;
  }

  private SettingNomadChangeProcessor() {

  }

  private volatile boolean initialized;

  public void setPlatformConfiguration(PlatformConfiguration platformConfiguration) {
    initializeChangeHandlers(platformConfiguration);
    initialized = true;
  }

  @Override
  public String getConfigWithChange(String baseConfig, SettingNomadChange change) throws NomadException {
    try {
      return getHandler(change.getConfigType()).getConfigWithChange(baseConfig, change.getChange());
    } catch (InvalidConfigChangeException e) {
      throw new NomadException(e);
    }
  }

  @Override
  public void applyChange(SettingNomadChange change) throws NomadException {
    getHandler(change.getConfigType()).applyChange(change.getChange());
  }

  private ConfigChangeHandler getHandler(ConfigChangeHandler.Type type) throws NomadException {
    checkInitialized();

    ConfigChangeHandler configChangeHandler = CHANGE_HANDLERS.get(type);
    if (configChangeHandler == null) {
      throw new NomadException("Unknown ConfigChangeHandler type: " + type);
    }
    return configChangeHandler;
  }

  private void checkInitialized() {
    if (!initialized) {
      throw new RuntimeException("SettingNomadChangeProcessor is not initialized");
    }
  }

  private void initializeChangeHandlers(PlatformConfiguration platformConfiguration) {
    for (ConfigChangeHandler configChangeHandler : ServiceLoader.load(ConfigChangeHandler.class)) {
      configChangeHandler.initialize(platformConfiguration);
      if (CHANGE_HANDLERS.putIfAbsent(configChangeHandler.getType(), configChangeHandler) != null) {
        throw new RuntimeException("Found multiple ConfigChangeHandlers of type: " + configChangeHandler.getType());
      }
    }
  }
}