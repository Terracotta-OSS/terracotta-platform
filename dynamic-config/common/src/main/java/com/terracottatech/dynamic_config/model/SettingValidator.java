/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.model;

import com.terracottatech.utilities.Measure;
import com.terracottatech.utilities.MemoryUnit;
import com.terracottatech.utilities.TimeUnit;
import com.terracottatech.utilities.Tuple2;

import java.util.function.BiConsumer;

import static com.terracottatech.utilities.HostAndIpValidator.isValidHost;
import static com.terracottatech.utilities.HostAndIpValidator.isValidIPv4;
import static com.terracottatech.utilities.HostAndIpValidator.isValidIPv6;
import static java.util.Objects.requireNonNull;

/**
 * This class purpose is to be used internally in {@link Setting}
 *
 * @author Mathieu Carbou
 */
class SettingValidator {

  private static final String MULTI_VALUE_SEP = ",";
  private static final String PARAM_INTERNAL_SEP = ":";

  static final BiConsumer<String, Tuple2<String, String>> DEFAULT = (setting, kv) -> {
    // default validator applied to all settings
    requireNonNull(kv);
    Setting s = Setting.fromName(setting);
    if (!s.allowsValue(kv.t2)) {
      throw new IllegalArgumentException(s + " should be one of: " + s.getAllowedValues());
    }
  };

  static final BiConsumer<String, Tuple2<String, String>> TIME_VALIDATOR = (setting, kv) -> {
    requireNonNull(kv.t2);
    Setting s = Setting.fromName(setting);
    Measure.parse(kv.t2, TimeUnit.class, null, s.getAllowedUnits());
  };

  static final BiConsumer<String, Tuple2<String, String>> PORT_VALIDATOR = (setting, kv) -> {
    requireNonNull(kv);
    int port;
    try {
      port = Integer.parseInt(kv.t2);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("<port> specified in " + setting + "=<port> must be an integer between 1 and 65535");
    }
    if (port < 1 || port > 65535) {
      throw new IllegalArgumentException("<port> specified in " + setting + "=<port> must be an integer between 1 and 65535");
    }
  };

  static final BiConsumer<String, Tuple2<String, String>> ADDRESS_VALIDATOR = (setting, kv) -> {
    requireNonNull(kv);
    if (!isValidIPv4(kv.t2) && !isValidIPv6(kv.t2)) {
      throw new IllegalArgumentException("<address> specified in " + setting + "=<address> must be a valid IP address");
    }
  };

  static final BiConsumer<String, Tuple2<String, String>> HOST_VALIDATOR = (setting, kv) -> {
    requireNonNull(kv);
    final String hostname = kv.t2;
    if (!isValidIPv4(hostname) && !isValidIPv6(hostname) && !isValidHost(hostname)) {
      throw new IllegalArgumentException("<address> specified in " + setting + "=<address> must be a valid hostname or IP address");
    }
  };

  static final BiConsumer<String, Tuple2<String, String>> OFFHEAP_VALIDATOR = (setting, kv) -> {
    requireNonNull(kv);
    String value = kv.t2;
    if (kv.t1 != null) {
      // case where we validate: offheap-resources.main=1GB
      value = kv.t1 + ":" + value;
    }
    final String[] offheapResources = value.split(MULTI_VALUE_SEP);
    for (String offheapResource : offheapResources) {
      final String[] split = offheapResource.split(PARAM_INTERNAL_SEP);
      if (split.length != 2 || split[0] == null || split[1] == null || split[0].trim().isEmpty() || split[1].trim().isEmpty()) {
        throw new IllegalArgumentException(setting + " should be specified in <resource-name>:<quantity><unit>,<resource-name>:<quantity><unit>... format");
      }
      Measure.parse(split[1], MemoryUnit.class, null, Setting.fromName(setting).getAllowedUnits());
    }
  };

  static final BiConsumer<String, Tuple2<String, String>> DATA_DIRS_VALIDATOR = (setting, kv) -> {
    requireNonNull(kv);
    String value = kv.t2;
    if (kv.t1 != null) {
      // case where we validate: data-dirs.main=foo/bar
      value = kv.t1 + ":" + value;
    }
    final String[] mappings = value.split(MULTI_VALUE_SEP);
    for (String mapping : mappings) {
      final String[] split = mapping.split(PARAM_INTERNAL_SEP);
      if (split.length != 2 || split[0] == null || split[1] == null || split[0].trim().isEmpty() || split[1].trim().isEmpty()) {
        throw new IllegalArgumentException(setting + " should be specified in <resource-name>:<path>,<resource-name>:<path>... format");
      }
    }
  };
}
