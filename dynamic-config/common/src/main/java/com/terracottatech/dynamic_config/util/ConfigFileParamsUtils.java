/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.util;


import com.terracottatech.dynamic_config.model.Setting;

public class ConfigFileParamsUtils {
  public static int getStripeId(String key) {
    if (!"stripe".equals(splitKey(key)[0])) {
      throw new IllegalArgumentException("Invalid key: " + key);
    }
    return Integer.parseInt(splitKey(key)[1]);
  }

  public static String getNodeId(String key) {
    if (!"node".equals(splitKey(key)[2])) {
      throw new IllegalArgumentException("Invalid key: " + key);
    }
    return "node-" + splitKey(key)[3];
  }

  public static Setting getSetting(String key) {
    return Setting.fromName(splitKey(key)[4]);
  }

  public static String[] splitKey(String key) {
    return key.split("\\.");
  }
}
