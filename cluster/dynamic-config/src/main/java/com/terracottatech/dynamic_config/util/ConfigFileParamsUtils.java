/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.util;


public class ConfigFileParamsUtils {
  public static String getStripe(String key) {
    return splitKey(key)[0] + splitKey(key)[1];
  }

  public static String getNode(String key) {
    return splitKey(key)[2] + splitKey(key)[3];
  }

  public static String getProperty(String key) {
    return splitKey(key)[4];
  }

  public static String[] splitKey(String key) {
    return key.split("\\.");
  }
}
