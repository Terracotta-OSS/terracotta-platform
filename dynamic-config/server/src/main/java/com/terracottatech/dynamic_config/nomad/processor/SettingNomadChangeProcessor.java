/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.nomad.processor;

import com.terracottatech.dynamic_config.ConfigChangeHandler;
import com.terracottatech.dynamic_config.InvalidConfigChangeException;
import com.terracottatech.dynamic_config.model.NodeContext;
import com.terracottatech.dynamic_config.model.Setting;
import com.terracottatech.dynamic_config.nomad.SettingNomadChange;
import com.terracottatech.nomad.server.NomadException;
import org.terracotta.entity.PlatformConfiguration;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * Supports the processing of {@link SettingNomadChange} for dynamic configuration
 */
public class SettingNomadChangeProcessor implements NomadChangeProcessor<SettingNomadChange> {
  private static final Map<Setting, ConfigChangeHandler> CHANGE_HANDLERS = Collections.synchronizedMap(new EnumMap<>(Setting.class));

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
  public NodeContext tryApply(NodeContext baseConfig, SettingNomadChange change) throws NomadException {
    try {
      return getHandler(change.getSetting()).tryApply(baseConfig, change);
    } catch (InvalidConfigChangeException e) {
      throw new NomadException(e);
    }
  }

  @Override
  public void apply(SettingNomadChange change) throws NomadException {
    getHandler(change.getSetting()).apply(change);
  }

  private ConfigChangeHandler getHandler(Setting setting) throws NomadException {
    checkInitialized();

    ConfigChangeHandler configChangeHandler = CHANGE_HANDLERS.get(setting);
    if (configChangeHandler == null) {
      throw new NomadException("Unknown ConfigChangeHandler type: " + setting);
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
      if (CHANGE_HANDLERS.putIfAbsent(configChangeHandler.getSetting(), configChangeHandler) != null) {
        throw new RuntimeException("Found multiple ConfigChangeHandlers of type: " + configChangeHandler.getSetting());
      }
    }
  }
}
