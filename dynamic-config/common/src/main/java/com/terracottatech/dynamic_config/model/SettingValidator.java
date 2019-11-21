/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.model;

import com.terracottatech.utilities.Measure;
import com.terracottatech.utilities.MemoryUnit;
import com.terracottatech.utilities.TimeUnit;
import com.terracottatech.utilities.Tuple2;

import java.nio.file.Paths;
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

  static final BiConsumer<String, Tuple2<String, String>> DEFAULT_VALIDATOR = (setting, kv) -> {
    // default validator applied to all settings
    Setting s = Setting.fromName(setting);
    requireNonNull(kv);
    requireNull(s, kv.t1);
    // prevent null if property is required
    if (s.isRequired() && kv.t2 == null) {
      throw new IllegalArgumentException(s + " cannot be null");
    }
    // whether required or optional, prevent empty strings
    if (kv.t2 != null && kv.t2.trim().isEmpty()) {
      throw new IllegalArgumentException(s + " cannot be empty");
    }
    if (!s.allowsValue(kv.t2)) {
      throw new IllegalArgumentException(s + " should be one of: " + s.getAllowedValues());
    }
  };

  static final BiConsumer<String, Tuple2<String, String>> TIME_VALIDATOR = (setting, kv) -> {
    DEFAULT_VALIDATOR.accept(setting, kv);
    Setting s = Setting.fromName(setting);
    Measure.parse(kv.t2, TimeUnit.class, null, s.getAllowedUnits());
  };

  static final BiConsumer<String, Tuple2<String, String>> PORT_VALIDATOR = (setting, kv) -> {
    DEFAULT_VALIDATOR.accept(setting, kv);
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

  static final BiConsumer<String, Tuple2<String, String>> PATH_VALIDATOR = (setting, kv) -> {
    DEFAULT_VALIDATOR.accept(setting, kv);
    try {
      Paths.get(kv.t2);
    } catch (RuntimeException e) {
      throw new IllegalArgumentException("Invalid path specified for setting " + setting + ": " + kv.t2);
    }
  };

  static final BiConsumer<String, Tuple2<String, String>> ADDRESS_VALIDATOR = (setting, kv) -> {
    DEFAULT_VALIDATOR.accept(setting, kv);
    if (!isValidIPv4(kv.t2) && !isValidIPv6(kv.t2)) {
      throw new IllegalArgumentException("<address> specified in " + setting + "=<address> must be a valid IP address");
    }
  };

  static final BiConsumer<String, Tuple2<String, String>> HOST_VALIDATOR = (setting, kv) -> {
    DEFAULT_VALIDATOR.accept(setting, kv);
    final String hostname = kv.t2;
    if (!isValidIPv4(hostname) && !isValidIPv6(hostname) && !isValidHost(hostname)) {
      throw new IllegalArgumentException("<address> specified in " + setting + "=<address> must be a valid hostname or IP address");
    }
  };

  static final BiConsumer<String, Tuple2<String, String>> OFFHEAP_VALIDATOR = (setting, kv) -> {
    if (kv.t2 != null) {
      validateMappings(kv, setting + " should be specified in the format <resource-name>:<quantity><unit>,<resource-name>:<quantity><unit>...", (k, v) -> {
        try {
          Measure.parse(v, MemoryUnit.class, null, Setting.fromName(setting).getAllowedUnits());
        } catch (RuntimeException e) {
          throw new IllegalArgumentException(setting + "." + k + " is invalid: " + e.getMessage());
        }
      });
    }
  };

  static final BiConsumer<String, Tuple2<String, String>> DATA_DIRS_VALIDATOR = (setting, kv) -> {
    if (kv.t2 != null) {
      // we have a value, we want to set:
      // - set data-dirs=main:foo/bar
      // - set data-dirs.main=foo/bar
      validateMappings(kv, setting + " should be specified in the format <resource-name>:<path>,<resource-name>:<path>...", (k, v) -> {
        try {
          Paths.get(v);
        } catch (RuntimeException e) {
          throw new IllegalArgumentException(setting + "." + k + " is invalid: Bad path: " + kv.t2);
        }
      });
    }
  };

  static final BiConsumer<String, Tuple2<String, String>> PROPS_VALIDATOR = (setting, kv) -> {
    if (kv.t2 != null) {
      // we have a value, we want to set:
      // - set tc-properties=key:value
      // - set tc-properties.key=value
      validateMappings(kv, setting + " should be specified in the format <key>:<value>,<key>:<value>...", (k, v) -> {
      });
    }
  };

  private static void validateMappings(Tuple2<String, String> kv, String err, BiConsumer<String, String> valueValidator) {
    // normalize to something like: main:foo/bar
    String value = kv.t1 == null ? kv.t2 : (kv.t1 + ":" + kv.t2);
    final String[] mappings = value.split(MULTI_VALUE_SEP);
    for (String mapping : mappings) {
      String[] split = mapping.split(PARAM_INTERNAL_SEP);
      if (split.length != 2 || split[0] == null || split[1] == null || split[0].trim().isEmpty() || split[1].trim().isEmpty()) {
        throw new IllegalArgumentException(err);
      }
      valueValidator.accept(split[0], split[1]);
    }
  }

  private static void requireNull(Setting setting, String key) {
    if (!setting.isMap() && key != null) {
      throw new IllegalArgumentException(setting + " is not a map");
    }
  }
}
