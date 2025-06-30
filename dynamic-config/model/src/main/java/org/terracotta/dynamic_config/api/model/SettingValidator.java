/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.dynamic_config.api.model;

import org.terracotta.common.struct.Measure;
import org.terracotta.common.struct.MemoryUnit;
import org.terracotta.common.struct.TimeUnit;
import org.terracotta.common.struct.Tuple2;

import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.function.BiConsumer;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;
import static org.terracotta.inet.HostAndIpValidator.isValidHost;
import static org.terracotta.inet.HostAndIpValidator.isValidIPv4;
import static org.terracotta.inet.HostAndIpValidator.isValidIPv6;

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
    // prevent null or empty if property is required and unsetting it is not permitted
    // Note: the 0-length string is allowed: it means that the user has specifically asked for a reset

    if (s.mustBePresent() && !s.allows(Operation.UNSET) && (kv.t2 == null || kv.t2.trim().isEmpty())) {
      throw new IllegalArgumentException(s + " cannot be null or empty");
    }
    if (kv.t2 != null && !kv.t2.isEmpty() && kv.t2.trim().isEmpty()) {
      throw new IllegalArgumentException(s + " cannot be null or empty");
    }
    // if we have something set by user, then check with the allowed values
    if (kv.t2 != null && !kv.t2.isEmpty() && !s.allowsValue(kv.t2)) {
      throw new IllegalArgumentException(s + " should be one of: " + s.getAllowedValues());
    }
  };

  static final BiConsumer<String, Tuple2<String, String>> NAME_VALIDATOR = (setting, kv) -> {
    DEFAULT_VALIDATOR.accept(setting, kv);
    if (Substitutor.containsSubstitutionParams(kv.t2)) {
      throw new IllegalArgumentException(setting + " cannot contain substitution parameters");
    }
  };

  static final BiConsumer<String, Tuple2<String, String>> TIME_VALIDATOR = (setting, kv) -> {
    DEFAULT_VALIDATOR.accept(setting, kv);
    if (kv.t2 != null) {
      Setting s = Setting.fromName(setting);
      Measure.parse(kv.t2, TimeUnit.class, s.getAllowedUnits());
    }
  };

  static final BiConsumer<String, Tuple2<String, String>> PORT_VALIDATOR = (setting, kv) -> {
    DEFAULT_VALIDATOR.accept(setting, kv);
    if (kv.t2 != null && !Substitutor.containsSubstitutionParams(kv.t2)) {
      int port;
      try {
        port = Integer.parseInt(kv.t2);
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException("<port> specified in " + setting + "=<port> must be an integer between 1 and 65535");
      }
      if (port < 1 || port > 65535) {
        throw new IllegalArgumentException("<port> specified in " + setting + "=<port> must be an integer between 1 and 65535");
      }
    }
  };

  static final BiConsumer<String, Tuple2<String, String>> PATH_VALIDATOR = (setting, kv) -> {
    DEFAULT_VALIDATOR.accept(setting, kv);
    if (kv.t2 != null && !Substitutor.containsSubstitutionParams(kv.t2)) {
      try {
        Paths.get(kv.t2);
      } catch (RuntimeException e) {
        throw new IllegalArgumentException("Invalid path specified for setting " + setting + ": " + kv.t2);
      }
    }
  };

  static final BiConsumer<String, Tuple2<String, String>> ADDRESS_VALIDATOR = (setting, kv) -> {
    DEFAULT_VALIDATOR.accept(setting, kv);
    if (kv.t2 != null && !Substitutor.containsSubstitutionParams(kv.t2) && !isValidIPv4(kv.t2) && !isValidIPv6(kv.t2)) {
      throw new IllegalArgumentException("<address> specified in " + setting + "=<address> must be a valid IP address");
    }
  };

  static final BiConsumer<String, Tuple2<String, String>> HOST_VALIDATOR = (setting, kv) -> {
    DEFAULT_VALIDATOR.accept(setting, kv);
    final String hostname = kv.t2;
    if (kv.t2 != null && !Substitutor.containsSubstitutionParams(kv.t2) && !isValidIPv4(hostname) && !isValidIPv6(hostname) && !isValidHost(hostname)) {
      throw new IllegalArgumentException("<address> specified in " + setting + "=<address> must be a valid hostname or IP address");
    }
  };

  static final BiConsumer<String, Tuple2<String, String>> INET_SOCKET_ADDRESS_VALIDATOR = (setting, kv) -> {
    DEFAULT_VALIDATOR.accept(setting, kv);
    final String address = kv.t2;

    String[] parts = address.split(":");
    if (parts.length != 2) {
      throw new IllegalArgumentException("Invalid format for " + setting + ". Expected format: <host>:<port>");
    }

    String hostname = parts[0];
    String portStr = parts[1];

    if (!Substitutor.containsSubstitutionParams(address) && !isValidIPv4(hostname) && !isValidIPv6(hostname) && !isValidHost(hostname)) {
      throw new IllegalArgumentException("<address> specified in " + setting + "=<address> must be a valid hostname or IP address");
    }

    try {
      int port = Integer.parseInt(portStr);
      if (port < 0 || port > 65535) {
        throw new IllegalArgumentException("Invalid port in " + setting + ": " + port + " (must be 0-65535)");
      }
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Port must be a number in " + setting + ": " + portStr);
    }
  };


  static final BiConsumer<String, Tuple2<String, String>> OFFHEAP_VALIDATOR = (setting, kv) -> {
    if (kv.t2 != null && !kv.t2.isEmpty()) {
      validateMappings(kv, setting + " should be specified in the format <resource-name>:<quantity><unit>,<resource-name>:<quantity><unit>...", (k, v) -> {
        try {
          Measure.parse(v, MemoryUnit.class, Setting.fromName(setting).getAllowedUnits());
        } catch (RuntimeException e) {
          throw new IllegalArgumentException(setting + "." + k + " is invalid: " + e.getMessage());
        }
      });
    }
  };

  static final BiConsumer<String, Tuple2<String, String>> DATA_DIRS_VALIDATOR = (setting, kv) -> {
    if (kv.t2 != null && !kv.t2.isEmpty()) {
      // we have a value, we want to set:
      // - set data-dirs=main:foo/bar
      // - set data-dirs.main=foo/bar
      validatePathMappings(kv, setting + " should be specified in the format <resource-name>:<path>,<resource-name>:<path>...", (k, v) -> {
        if (!Substitutor.containsSubstitutionParams(v)) {
          try {
            Paths.get(v);
          } catch (RuntimeException e) {
            throw new IllegalArgumentException(setting + "." + k + " is invalid: Bad path: " + v);
          }
        }
      });
    }
  };

  private static final Set<String> LEGAL_LOGGER_LEVELS = unmodifiableSet(new HashSet<>(asList("ALL", "TRACE", "DEBUG", "INFO", "WARN", "ERROR", "OFF")));
  static final BiConsumer<String, Tuple2<String, String>> LOGGER_LEVEL_VALIDATOR = (setting, kv) -> {
    if (kv.t2 != null && !kv.t2.isEmpty()) {
      // we have a value, we want to set:
      // - set loggers=com.foo.bar:TRACE
      // - set loggers.com.foo.bar=TRACE
      validateMappings(kv, setting + " should be specified in the format <logger>:<level>,<logger>:<level>...", (k, v) -> {
        if (!LEGAL_LOGGER_LEVELS.contains(v.toUpperCase(Locale.ROOT))) {
          throw new IllegalArgumentException(setting + "." + k + " is invalid: Bad level: " + v);
        }
      });
    }
  };

  static final BiConsumer<String, Tuple2<String, String>> PROPS_VALIDATOR = (setting, kv) -> {
    if (kv.t2 != null && !kv.t2.isEmpty()) {
      // we have a value, we want to set:
      // - set tc-properties=key:value
      // - set tc-properties.key=value
      validateMappings(kv, setting + " should be specified in the format <key>:<value>,<key>:<value>...", (k, v) -> {
      });
    }
  };

  private static void validateMappings(Tuple2<String, String> kv, String err, BiConsumer<String, String> valueValidator) {
    // normalize to something like: main:128MB
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

  private static void validatePathMappings(Tuple2<String, String> kv, String err, BiConsumer<String, String> valueValidator) {
    // normalize to something like: main:foo/bar
    String value = kv.t1 == null ? kv.t2 : (kv.t1 + ":" + kv.t2);
    final String[] mappings = value.split(MULTI_VALUE_SEP);
    for (String mapping : mappings) {
      String[] split = mapping.split(PARAM_INTERNAL_SEP);
      // Split should contain at least two tokens. Paths can contain multiple colons both on Windows and *nixes
      if (split.length < 2 || split[0] == null || split[1] == null || split[0].trim().isEmpty() || split[1].trim().isEmpty()) {
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
