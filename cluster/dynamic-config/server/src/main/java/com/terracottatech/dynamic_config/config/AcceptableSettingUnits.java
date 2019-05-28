/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.config;

import com.terracottatech.utilities.MemoryUnit;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.terracottatech.dynamic_config.config.CommonOptions.CLIENT_LEASE_DURATION;
import static com.terracottatech.dynamic_config.config.CommonOptions.CLIENT_RECONNECT_WINDOW;
import static com.terracottatech.dynamic_config.config.CommonOptions.OFFHEAP_RESOURCES;

public class AcceptableSettingUnits {
  private static final Map<String, Set<String>> ACCEPTABLE_SETTING_UNITS = new HashMap<>();

  static {
    ACCEPTABLE_SETTING_UNITS.put(OFFHEAP_RESOURCES, Arrays.stream(MemoryUnit.values()).map(memoryUnit -> memoryUnit.name()).collect(Collectors.toSet()));
    ACCEPTABLE_SETTING_UNITS.put(CLIENT_LEASE_DURATION, new HashSet<>(Arrays.asList("ms", "s", "m", "h")));
    ACCEPTABLE_SETTING_UNITS.put(CLIENT_RECONNECT_WINDOW, new HashSet<>(Arrays.asList("s", "m", "h")));
  }

  public static Set<String> get(String setting) {
    return ACCEPTABLE_SETTING_UNITS.get(setting);
  }
}
