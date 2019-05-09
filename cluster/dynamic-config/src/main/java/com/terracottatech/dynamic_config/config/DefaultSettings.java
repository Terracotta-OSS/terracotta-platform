/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.config;

import java.util.HashMap;
import java.util.Map;

import static com.terracottatech.dynamic_config.config.CommonOptions.CLIENT_LEASE_DURATION;
import static com.terracottatech.dynamic_config.config.CommonOptions.CLIENT_RECONNECT_WINDOW;
import static com.terracottatech.dynamic_config.config.CommonOptions.DATA_DIRS;
import static com.terracottatech.dynamic_config.config.CommonOptions.FAILOVER_PRIORITY;
import static com.terracottatech.dynamic_config.config.CommonOptions.NODE_BIND_ADDRESS;
import static com.terracottatech.dynamic_config.config.CommonOptions.NODE_CONFIG_DIR;
import static com.terracottatech.dynamic_config.config.CommonOptions.NODE_GROUP_BIND_ADDRESS;
import static com.terracottatech.dynamic_config.config.CommonOptions.NODE_GROUP_PORT;
import static com.terracottatech.dynamic_config.config.CommonOptions.NODE_HOSTNAME;
import static com.terracottatech.dynamic_config.config.CommonOptions.NODE_LOG_DIR;
import static com.terracottatech.dynamic_config.config.CommonOptions.NODE_METADATA_DIR;
import static com.terracottatech.dynamic_config.config.CommonOptions.NODE_PORT;
import static com.terracottatech.dynamic_config.config.CommonOptions.OFFHEAP_RESOURCES;
import static com.terracottatech.dynamic_config.Constants.DEFAULT_BIND_ADDRESS;
import static com.terracottatech.dynamic_config.Constants.DEFAULT_CLIENT_LEASE_DURATION;
import static com.terracottatech.dynamic_config.Constants.DEFAULT_CLIENT_RECONNECT_WINDOW;
import static com.terracottatech.dynamic_config.Constants.DEFAULT_CONFIG_DIR;
import static com.terracottatech.dynamic_config.Constants.DEFAULT_DATA_DIR;
import static com.terracottatech.dynamic_config.Constants.DEFAULT_FAILOVER_PRIORITY;
import static com.terracottatech.dynamic_config.Constants.DEFAULT_GROUP_BIND_ADDRESS;
import static com.terracottatech.dynamic_config.Constants.DEFAULT_GROUP_PORT;
import static com.terracottatech.dynamic_config.Constants.DEFAULT_HOSTNAME;
import static com.terracottatech.dynamic_config.Constants.DEFAULT_LOG_DIR;
import static com.terracottatech.dynamic_config.Constants.DEFAULT_METADATA_DIR;
import static com.terracottatech.dynamic_config.Constants.DEFAULT_OFFHEAP_RESOURCE;
import static com.terracottatech.dynamic_config.Constants.DEFAULT_PORT;

public class DefaultSettings {
  private static final Map<String, Object> DEFAULT_SETTING_VALUES = new HashMap<>();

  static {
    DEFAULT_SETTING_VALUES.put(NODE_HOSTNAME, DEFAULT_HOSTNAME);
    DEFAULT_SETTING_VALUES.put(NODE_PORT, String.valueOf(DEFAULT_PORT));
    DEFAULT_SETTING_VALUES.put(NODE_GROUP_PORT, String.valueOf(DEFAULT_GROUP_PORT));
    DEFAULT_SETTING_VALUES.put(NODE_BIND_ADDRESS, DEFAULT_BIND_ADDRESS);
    DEFAULT_SETTING_VALUES.put(NODE_GROUP_BIND_ADDRESS, DEFAULT_GROUP_BIND_ADDRESS);
    DEFAULT_SETTING_VALUES.put(NODE_CONFIG_DIR, DEFAULT_CONFIG_DIR);
    DEFAULT_SETTING_VALUES.put(NODE_METADATA_DIR, DEFAULT_METADATA_DIR);
    DEFAULT_SETTING_VALUES.put(NODE_LOG_DIR, DEFAULT_LOG_DIR);
    DEFAULT_SETTING_VALUES.put(FAILOVER_PRIORITY, DEFAULT_FAILOVER_PRIORITY);
    DEFAULT_SETTING_VALUES.put(CLIENT_RECONNECT_WINDOW, DEFAULT_CLIENT_RECONNECT_WINDOW);
    DEFAULT_SETTING_VALUES.put(CLIENT_LEASE_DURATION, DEFAULT_CLIENT_LEASE_DURATION);
    DEFAULT_SETTING_VALUES.put(OFFHEAP_RESOURCES, DEFAULT_OFFHEAP_RESOURCE);
    DEFAULT_SETTING_VALUES.put(DATA_DIRS, DEFAULT_DATA_DIR);
  }

  public static String getDefaultValueFor(String setting) {
    Object settingValue = DEFAULT_SETTING_VALUES.get(setting);
    return settingValue == null ? null : settingValue.toString();
  }
}
