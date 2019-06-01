/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.config;

import com.terracottatech.utilities.MemoryUnit;
import com.terracottatech.utilities.Unit;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static com.terracottatech.dynamic_config.config.CommonOptions.CLIENT_LEASE_DURATION;
import static com.terracottatech.dynamic_config.config.CommonOptions.CLIENT_RECONNECT_WINDOW;
import static com.terracottatech.dynamic_config.config.CommonOptions.OFFHEAP_RESOURCES;
import static com.terracottatech.utilities.TimeUnit.HOURS;
import static com.terracottatech.utilities.TimeUnit.MILLISECONDS;
import static com.terracottatech.utilities.TimeUnit.MINUTES;
import static com.terracottatech.utilities.TimeUnit.SECONDS;

public class AcceptableSettingUnits {
  private static final Map<String, Set<?>> ACCEPTABLE_SETTING_UNITS = new HashMap<>();

  static {
    ACCEPTABLE_SETTING_UNITS.put(OFFHEAP_RESOURCES, new LinkedHashSet<>(Arrays.asList(MemoryUnit.values())));
    ACCEPTABLE_SETTING_UNITS.put(CLIENT_LEASE_DURATION, new LinkedHashSet<>(Arrays.asList(MILLISECONDS, SECONDS, MINUTES, HOURS)));
    ACCEPTABLE_SETTING_UNITS.put(CLIENT_RECONNECT_WINDOW, new LinkedHashSet<>(Arrays.asList(SECONDS, MINUTES, HOURS)));
  }

  @SuppressWarnings("unchecked")
  public static <U extends Enum<U> & Unit<U>> Set<U> get(String setting) {
    return (Set<U>) ACCEPTABLE_SETTING_UNITS.get(setting);
  }
}
