/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.config;

import com.terracottatech.dynamic_config.Constants;
import com.terracottatech.utilities.Measure;
import com.terracottatech.dynamic_config.model.Node;
import com.terracottatech.dynamic_config.util.ConfigUtils;
import com.terracottatech.utilities.MemoryUnit;
import com.terracottatech.utilities.TimeUnit;

import java.nio.file.Paths;
import java.util.Collections;
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

public class DefaultSettings {
  private static final Map<String, Object> DEFAULT_SETTING_VALUES = new HashMap<>();

  static {
    DEFAULT_SETTING_VALUES.put(NODE_HOSTNAME, Constants.DEFAULT_HOSTNAME);
    DEFAULT_SETTING_VALUES.put(NODE_PORT, Constants.DEFAULT_PORT);
    DEFAULT_SETTING_VALUES.put(NODE_GROUP_PORT, Constants.DEFAULT_GROUP_PORT);
    DEFAULT_SETTING_VALUES.put(NODE_BIND_ADDRESS, Constants.DEFAULT_BIND_ADDRESS);
    DEFAULT_SETTING_VALUES.put(NODE_GROUP_BIND_ADDRESS, Constants.DEFAULT_GROUP_BIND_ADDRESS);
    DEFAULT_SETTING_VALUES.put(NODE_CONFIG_DIR, Constants.DEFAULT_CONFIG_DIR);
    DEFAULT_SETTING_VALUES.put(NODE_METADATA_DIR, Constants.DEFAULT_METADATA_DIR);
    DEFAULT_SETTING_VALUES.put(NODE_LOG_DIR, Constants.DEFAULT_LOG_DIR);
    DEFAULT_SETTING_VALUES.put(FAILOVER_PRIORITY, Constants.DEFAULT_FAILOVER_PRIORITY);
    DEFAULT_SETTING_VALUES.put(CLIENT_RECONNECT_WINDOW, Constants.DEFAULT_CLIENT_RECONNECT_WINDOW);
    DEFAULT_SETTING_VALUES.put(CLIENT_LEASE_DURATION, Constants.DEFAULT_CLIENT_LEASE_DURATION);
    DEFAULT_SETTING_VALUES.put(OFFHEAP_RESOURCES, Constants.DEFAULT_OFFHEAP_RESOURCE);
    DEFAULT_SETTING_VALUES.put(DATA_DIRS, Constants.DEFAULT_DATA_DIR);
  }

  public static Map<String, Object> getAll() {
    return Collections.unmodifiableMap(DEFAULT_SETTING_VALUES);
  }

  public static String getDefaultValueFor(String setting) {
    Object settingValue = DEFAULT_SETTING_VALUES.get(setting);
    return settingValue == null ? null : settingValue.toString();
  }

  public static Map<String, String> fillDefaultsIfNeeded(Node node) {
    Map<String, String> defaultOptions = new HashMap<>();
    if (node.getNodeName() == null) {
      String generateNodeName = ConfigUtils.generateNodeName();
      node.setNodeName(generateNodeName);
      defaultOptions.put(CommonOptions.NODE_NAME, generateNodeName);
    }

    if (node.getNodeHostname() == null) {
      node.setNodeHostname(Constants.DEFAULT_HOSTNAME);
      defaultOptions.put(CommonOptions.NODE_HOSTNAME, Constants.DEFAULT_HOSTNAME);
    }

    if (node.getNodePort() == 0) {
      node.setNodePort(Integer.parseInt(Constants.DEFAULT_PORT));
      defaultOptions.put(CommonOptions.NODE_PORT, Constants.DEFAULT_PORT);
    }

    if (node.getNodeGroupPort() == 0) {
      node.setNodeGroupPort(Integer.parseInt(Constants.DEFAULT_GROUP_PORT));
      defaultOptions.put(CommonOptions.NODE_GROUP_PORT, Constants.DEFAULT_GROUP_PORT);
    }

    if (node.getOffheapResources().isEmpty()) {
      String[] split = Constants.DEFAULT_OFFHEAP_RESOURCE.split(Constants.PARAM_INTERNAL_SEP);
      node.setOffheapResource(split[0], Measure.parse(split[1], MemoryUnit.class));
      defaultOptions.put(CommonOptions.OFFHEAP_RESOURCES, Constants.DEFAULT_OFFHEAP_RESOURCE);
    }

    if (node.getDataDirs().isEmpty()) {
      String[] split = Constants.DEFAULT_DATA_DIR.split(Constants.PARAM_INTERNAL_SEP);
      node.setDataDir(split[0], Paths.get(split[1]));
      defaultOptions.put(CommonOptions.DATA_DIRS, Constants.DEFAULT_DATA_DIR);
    }

    if (node.getNodeBindAddress() == null) {
      node.setNodeBindAddress(Constants.DEFAULT_BIND_ADDRESS);
      defaultOptions.put(CommonOptions.NODE_BIND_ADDRESS, Constants.DEFAULT_BIND_ADDRESS);
    }

    if (node.getNodeGroupBindAddress() == null) {
      node.setNodeGroupBindAddress(Constants.DEFAULT_GROUP_BIND_ADDRESS);
      defaultOptions.put(CommonOptions.NODE_GROUP_BIND_ADDRESS, Constants.DEFAULT_GROUP_BIND_ADDRESS);
    }

    if (node.getNodeConfigDir() == null) {
      node.setNodeConfigDir(Paths.get(Constants.DEFAULT_CONFIG_DIR));
      defaultOptions.put(CommonOptions.NODE_CONFIG_DIR, Constants.DEFAULT_CONFIG_DIR);
    }

    if (node.getNodeLogDir() == null) {
      node.setNodeLogDir(Paths.get(Constants.DEFAULT_LOG_DIR));
      defaultOptions.put(CommonOptions.NODE_LOG_DIR, Constants.DEFAULT_LOG_DIR);
    }

    if (node.getNodeMetadataDir() == null) {
      node.setNodeMetadataDir(Paths.get(Constants.DEFAULT_METADATA_DIR));
      defaultOptions.put(CommonOptions.NODE_METADATA_DIR, Constants.DEFAULT_METADATA_DIR);
    }

    if (node.getFailoverPriority() == null) {
      node.setFailoverPriority(Constants.DEFAULT_FAILOVER_PRIORITY);
      defaultOptions.put(CommonOptions.FAILOVER_PRIORITY, Constants.DEFAULT_FAILOVER_PRIORITY);
    }

    if (node.getClientReconnectWindow() == null) {
      node.setClientReconnectWindow(Measure.parse(Constants.DEFAULT_CLIENT_RECONNECT_WINDOW, TimeUnit.class));
      defaultOptions.put(CommonOptions.CLIENT_RECONNECT_WINDOW, Constants.DEFAULT_CLIENT_RECONNECT_WINDOW);
    }

    if (node.getClientLeaseDuration() == null) {
      node.setClientLeaseDuration(Measure.parse(Constants.DEFAULT_CLIENT_LEASE_DURATION, TimeUnit.class));
      defaultOptions.put(CommonOptions.CLIENT_LEASE_DURATION, Constants.DEFAULT_CLIENT_LEASE_DURATION);
    }

    return defaultOptions;
  }

}
