/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.config;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.terracottatech.dynamic_config.config.CommonOptions.FAILOVER_PRIORITY;
import static com.terracottatech.dynamic_config.config.CommonOptions.SECURITY_AUTHC;
import static com.terracottatech.dynamic_config.config.CommonOptions.SECURITY_SSL_TLS;
import static com.terracottatech.dynamic_config.config.CommonOptions.SECURITY_WHITELIST;

public class AcceptableSettingValues {
  private static final Map<String, Set<String>> ACCEPTABLE_SETTING_VALUES = new HashMap<>();

  static {
    ACCEPTABLE_SETTING_VALUES.put(FAILOVER_PRIORITY, new HashSet<>(Arrays.asList("availability", "consistency")));
    ACCEPTABLE_SETTING_VALUES.put(SECURITY_AUTHC, new HashSet<>(Arrays.asList("file", "certificate", "ldap")));
    ACCEPTABLE_SETTING_VALUES.put(SECURITY_SSL_TLS, new HashSet<>(Arrays.asList("true", "false")));
    ACCEPTABLE_SETTING_VALUES.put(SECURITY_WHITELIST, new HashSet<>(Arrays.asList("true", "false")));
  }

  public static Set<String> get(String setting) {
    return ACCEPTABLE_SETTING_VALUES.get(setting);
  }
}
