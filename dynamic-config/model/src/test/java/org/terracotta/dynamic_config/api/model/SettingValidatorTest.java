/*
 * Copyright Terracotta, Inc.
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

import org.junit.Test;

import java.nio.file.Paths;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.terracotta.dynamic_config.api.model.Setting.CLIENT_LEASE_DURATION;
import static org.terracotta.dynamic_config.api.model.Setting.CLIENT_RECONNECT_WINDOW;
import static org.terracotta.dynamic_config.api.model.Setting.CLUSTER_NAME;
import static org.terracotta.dynamic_config.api.model.Setting.DATA_DIRS;
import static org.terracotta.dynamic_config.api.model.Setting.FAILOVER_PRIORITY;
import static org.terracotta.dynamic_config.api.model.Setting.NODE_BACKUP_DIR;
import static org.terracotta.dynamic_config.api.model.Setting.NODE_BIND_ADDRESS;
import static org.terracotta.dynamic_config.api.model.Setting.NODE_GROUP_BIND_ADDRESS;
import static org.terracotta.dynamic_config.api.model.Setting.NODE_GROUP_PORT;
import static org.terracotta.dynamic_config.api.model.Setting.NODE_HOSTNAME;
import static org.terracotta.dynamic_config.api.model.Setting.NODE_LOGGER_OVERRIDES;
import static org.terracotta.dynamic_config.api.model.Setting.NODE_LOG_DIR;
import static org.terracotta.dynamic_config.api.model.Setting.NODE_METADATA_DIR;
import static org.terracotta.dynamic_config.api.model.Setting.NODE_NAME;
import static org.terracotta.dynamic_config.api.model.Setting.NODE_PORT;
import static org.terracotta.dynamic_config.api.model.Setting.OFFHEAP_RESOURCES;
import static org.terracotta.dynamic_config.api.model.Setting.SECURITY_AUDIT_LOG_DIR;
import static org.terracotta.dynamic_config.api.model.Setting.SECURITY_AUTHC;
import static org.terracotta.dynamic_config.api.model.Setting.SECURITY_DIR;
import static org.terracotta.dynamic_config.api.model.Setting.SECURITY_SSL_TLS;
import static org.terracotta.dynamic_config.api.model.Setting.SECURITY_WHITELIST;
import static org.terracotta.dynamic_config.api.model.Setting.TC_PROPERTIES;
import static org.terracotta.testing.ExceptionMatcher.throwing;

/**
 * @author Mathieu Carbou
 */
public class SettingValidatorTest {

  @Test
  public void test_CLUSTER_NAME() {
    validateOptional(CLUSTER_NAME);
    CLUSTER_NAME.validate(null); // cluster name can be set to null when loading config file
    CLUSTER_NAME.validate(""); // cluster name can be set to null when loading config file
    CLUSTER_NAME.validate("foo");
  }

  @Test
  public void test_NODE_NAME() {
    validateRequired(NODE_NAME);
    assertThat(
        () -> NODE_NAME.validate(""),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Setting '" + NODE_NAME + "' requires a value")))));
    Stream.of("d", "D", "h", "c", "i", "H", "n", "o", "a", "v", "t", "(").forEach(c -> {
      assertThat(
          () -> NODE_NAME.validate("%" + c),
          is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo(NODE_NAME + " cannot contain substitution parameters")))));
    });
    NODE_NAME.validate("foo");
  }

  @Test
  public void test_NODE_HOSTNAME() {
    validateRequired(NODE_HOSTNAME);
    assertThat(
        () -> NODE_HOSTNAME.validate(".."),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("<address> specified in hostname=<address> must be a valid hostname or IP address")))));
    assertThat(
        () -> NODE_HOSTNAME.validate(""),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Setting '" + NODE_HOSTNAME + "' requires a value")))));
    NODE_HOSTNAME.validate("foo");
  }

  @Test
  public void test_NODE_PORT() {
    Stream.of(NODE_PORT).forEach(setting -> {
      validateRequired(setting);
      assertThat(
          () -> setting.validate("foo"),
          is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("<port> specified in " + setting + "=<port> must be an integer between 1 and 65535")))));
      assertThat(
          () -> setting.validate("0"),
          is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("<port> specified in " + setting + "=<port> must be an integer between 1 and 65535")))));
      assertThat(
          () -> setting.validate("65536"),
          is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("<port> specified in " + setting + "=<port> must be an integer between 1 and 65535")))));
      assertThat(
          () -> setting.validate(""),
          is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Setting '" + setting + "' requires a value")))));
      setting.validate("9410");
    });
  }

  @Test
  public void test_NODE_GROUP_PORT() {
    Stream.of(NODE_GROUP_PORT).forEach(setting -> {
      validateRequired(setting);
      assertThat(
          () -> setting.validate("foo"),
          is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("<port> specified in " + setting + "=<port> must be an integer between 1 and 65535")))));
      assertThat(
          () -> setting.validate("0"),
          is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("<port> specified in " + setting + "=<port> must be an integer between 1 and 65535")))));
      assertThat(
          () -> setting.validate("65536"),
          is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("<port> specified in " + setting + "=<port> must be an integer between 1 and 65535")))));
      assertThat(
          () -> setting.validate(""),
          is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Setting '" + setting + "' requires a value")))));
      setting.validate("9410");
      setting.validate(null); // unset - switch back to default value
    });
  }

  @Test
  public void test_bind_addresses() {
    Stream.of(NODE_BIND_ADDRESS, NODE_GROUP_BIND_ADDRESS).forEach(setting -> {
      validateRequired(setting);
      assertThat(
          () -> setting.validate("my-hostname"),
          is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("<address> specified in " + setting + "=<address> must be a valid IP address")))));
      assertThat(
          () -> setting.validate(""),
          is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Setting '" + setting + "' requires a value")))));
      setting.validate(null); // unset - switch back to default value
      setting.validate("0.0.0.0");
    });
  }

  @Test
  public void test_paths() {
    Stream.of(NODE_LOG_DIR, NODE_METADATA_DIR).forEach(setting -> {
      validateRequired(setting);
      assertThat(
          () -> setting.validate("/\u0000/"),
          is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Invalid path specified for setting " + setting + ": /\u0000/")))));
      assertThat(
          () -> setting.validate(""),
          is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Setting '" + setting + "' requires a value")))));
      setting.validate(".");
      setting.validate(null); // unset - switch back to default value
    });

    Stream.of(NODE_BACKUP_DIR, SECURITY_DIR, SECURITY_AUDIT_LOG_DIR).forEach(setting -> {
      validateOptional(setting);
      assertThat(
          () -> setting.validate("/\u0000/"),
          is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Invalid path specified for setting " + setting + ": /\u0000/")))));
      setting.validate(null); // ok
      setting.validate(".");
    });
  }

  @Test
  public void test_TIMES() {
    Stream.of(CLIENT_RECONNECT_WINDOW, CLIENT_LEASE_DURATION).forEach(setting -> {
      validateRequired(setting);
      setting.validate("0s"); // ok - 0 allowed
      setting.validate(null); // unset - switch back to default value
      assertThat(
          () -> setting.validate("foo"),
          is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Invalid measure: 'foo'. <unit> is missing or not recognized. It must be one of " + setting.getAllowedUnits() + ".")))));
      assertThat(
          () -> setting.validate("1"),
          is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Invalid measure: '1'. <unit> is missing or not recognized. It must be one of " + setting.getAllowedUnits() + ".")))));
      assertThat(
          () -> setting.validate("1_"),
          is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Invalid measure: '1_'. <unit> is missing or not recognized. It must be one of " + setting.getAllowedUnits() + ".")))));
      assertThat(
          () -> setting.validate("1bad-unit"),
          is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Invalid measure: '1bad-unit'. <unit> is missing or not recognized. It must be one of " + setting.getAllowedUnits() + ".")))));
      assertThat(
          () -> setting.validate("-1s"),
          is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Quantity measure cannot be negative")))));
      assertThat(
          () -> setting.validate(""),
          is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Setting '" + setting + "' requires a value")))));
    });
  }

  @Test
  public void test_FAILOVER_PRIORITY() {
    validateOptional(FAILOVER_PRIORITY);
    Stream.of(
        "foo",
        "availability:8",
        "availability:foo",
        "consistency:-1",
        "consistency:foo",
        "consistency:",
        "1:consistency",
        "consistency:1:2",
        ":",
        ":::",
        "consistency-1",
        "??" // :D
    ).forEach(value -> assertThat(
        value,
        () -> FAILOVER_PRIORITY.validate(value),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("failover-priority should be either 'availability', 'consistency', or 'consistency:N' (where 'N' is the voter count expressed as a non-negative integer)")))))
    );
  }

  @Test
  public void test_SECURITY_AUTHC() {
    validateOptional(SECURITY_AUTHC);
    SECURITY_AUTHC.validate("ldap");
    SECURITY_AUTHC.validate("certificate");
    SECURITY_AUTHC.validate("file");
    assertThat(
        () -> SECURITY_AUTHC.validate("foo"),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("authc should be one of: [file, ldap, certificate]"))))
    );
    SECURITY_AUTHC.validate(null); // unset - switch back to default value
    SECURITY_AUTHC.validate("");
  }

  @Test
  public void test_WHITELIST_SSLTLS() {
    Stream.of(SECURITY_SSL_TLS, SECURITY_WHITELIST).forEach(setting -> {
      validateRequired(setting);
      setting.validate(null); // unset - switch back to default value
      setting.validate("true");
      setting.validate("false");
      setting.validate(null, null); // unset - switch back to default value
      setting.validate(null, "true");
      setting.validate(null, "false");

      assertThat(
          () -> setting.validate(""),
          is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Setting '" + setting + "' requires a value")))));
      assertThat(
          () -> setting.validate(null, ""),
          is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Setting '" + setting + "' requires a value")))));
      assertThat(
          () -> setting.validate("foo"),
          is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo(setting + " should be one of: [true, false]")))));
      assertThat(
          () -> setting.validate("FALSE"),
          is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo(setting + " should be one of: [true, false]")))));
      assertThat(
          () -> setting.validate("TRUE"),
          is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo(setting + " should be one of: [true, false]")))));
    });
  }

  @Test
  public void test_OFFHEAP_RESOURCES() {
    OFFHEAP_RESOURCES.validate(null); // unset - switch back to default value
    OFFHEAP_RESOURCES.validate(null, null);
    OFFHEAP_RESOURCES.validate("main", null);
    OFFHEAP_RESOURCES.validate("main", "1GB");
    OFFHEAP_RESOURCES.validate("main", "");
    OFFHEAP_RESOURCES.validate(null, "main:1GB");
    OFFHEAP_RESOURCES.validate(null, "main:1GB,second:2GB");
    OFFHEAP_RESOURCES.validate(null, "");
    OFFHEAP_RESOURCES.validate(""); // set to empty map

    assertThat(
        () -> OFFHEAP_RESOURCES.validate(null, "bar"),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("offheap-resources should be specified in the format <resource-name>:<quantity><unit>,<resource-name>:<quantity><unit>...")))));
    assertThat(
        () -> OFFHEAP_RESOURCES.validate(null, "bar:"),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("offheap-resources should be specified in the format <resource-name>:<quantity><unit>,<resource-name>:<quantity><unit>...")))));
    assertThat(
        () -> OFFHEAP_RESOURCES.validate(null, "bar: "),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("offheap-resources should be specified in the format <resource-name>:<quantity><unit>,<resource-name>:<quantity><unit>...")))));
    assertThat(
        () -> OFFHEAP_RESOURCES.validate(null, ":value"),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("offheap-resources should be specified in the format <resource-name>:<quantity><unit>,<resource-name>:<quantity><unit>...")))));
    assertThat(
        () -> OFFHEAP_RESOURCES.validate(null, " :value"),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("offheap-resources should be specified in the format <resource-name>:<quantity><unit>,<resource-name>:<quantity><unit>...")))));
    assertThat(
        () -> OFFHEAP_RESOURCES.validate(null, "bar:1,second:2GB"),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("offheap-resources.bar is invalid: Invalid measure: '1'. <unit> is missing or not recognized. It must be one of [B, KB, MB, GB, TB, PB].")))));
    assertThat(
        () -> OFFHEAP_RESOURCES.validate("foo", "bar"),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("offheap-resources.foo is invalid: Invalid measure: 'bar'. <unit> is missing or not recognized. It must be one of [B, KB, MB, GB, TB, PB].")))));
  }

  @Test
  public void test_DATA_DIRS() {
    DATA_DIRS.validate(null); // unset - switch back to default value
    DATA_DIRS.validate(null, null);
    DATA_DIRS.validate("main", null);
    DATA_DIRS.validate("main", "");
    DATA_DIRS.validate(null, "");
    DATA_DIRS.validate(""); // set to empty map

    // Valid Relative paths
    DATA_DIRS.validate("main", "foo/bar");
    DATA_DIRS.validate(null, "main:foo/bar");
    DATA_DIRS.validate(null, "main:foo/bar,second:foo/baz");

    // Valid Absolute paths
    String absPath1 = Paths.get("/foo/bar").toAbsolutePath().toString(); // resolves to C:\foo\bar on Windows
    String absPath2 = Paths.get("/foo/baz").toAbsolutePath().toString();
    DATA_DIRS.validate("main", absPath1);
    DATA_DIRS.validate(null, "main:" + absPath1);
    DATA_DIRS.validate(null, "main:" + absPath1 + ",second:" + absPath2);

    // Invalid paths
    assertThat(
        () -> DATA_DIRS.validate(null, "main:"),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("data-dirs should be specified in the format <resource-name>:<path>,<resource-name>:<path>...")))));
    assertThat(
        () -> DATA_DIRS.validate(null, "main: "),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("data-dirs should be specified in the format <resource-name>:<path>,<resource-name>:<path>...")))));
    assertThat(
        () -> DATA_DIRS.validate(null, ":value"),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("data-dirs should be specified in the format <resource-name>:<path>,<resource-name>:<path>...")))));
    assertThat(
        () -> DATA_DIRS.validate(null, " :value"),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("data-dirs should be specified in the format <resource-name>:<path>,<resource-name>:<path>...")))));
    assertThat(
        () -> DATA_DIRS.validate("foo", "/\u0000/"),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("data-dirs.foo is invalid: Bad path: /\u0000/")))));
  }

  @Test
  public void test_TC_PROPERTIES() {
    TC_PROPERTIES.validate(null); // unset - switch back to default value
    TC_PROPERTIES.validate(""); // set to empty map
    TC_PROPERTIES.validate(null, null);
    TC_PROPERTIES.validate(null, "");
    TC_PROPERTIES.validate("key", null);
    TC_PROPERTIES.validate("key", "value");
    TC_PROPERTIES.validate("key", "");
    TC_PROPERTIES.validate(null, "key:value");
    TC_PROPERTIES.validate(null, "key1:value,key2:value");

    assertThat(
        () -> TC_PROPERTIES.validate(null, "key:"),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("tc-properties should be specified in the format <key>:<value>,<key>:<value>...")))));
    assertThat(
        () -> TC_PROPERTIES.validate(null, "key: "),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("tc-properties should be specified in the format <key>:<value>,<key>:<value>...")))));
    assertThat(
        () -> TC_PROPERTIES.validate(null, ":value"),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("tc-properties should be specified in the format <key>:<value>,<key>:<value>...")))));
    assertThat(
        () -> TC_PROPERTIES.validate(null, " :value"),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("tc-properties should be specified in the format <key>:<value>,<key>:<value>...")))));
  }

  @Test
  public void test_NODE_LOGGER_OVERRIDES() {
    NODE_LOGGER_OVERRIDES.validate(null); // unset - switch back to default value
    NODE_LOGGER_OVERRIDES.validate(""); // set to empty map
    NODE_LOGGER_OVERRIDES.validate(null, null);
    NODE_LOGGER_OVERRIDES.validate(null, "");
    NODE_LOGGER_OVERRIDES.validate("key", null);
    NODE_LOGGER_OVERRIDES.validate("key", "");
    NODE_LOGGER_OVERRIDES.validate("key", "INFO");
    NODE_LOGGER_OVERRIDES.validate(null, "key:INFO");
    NODE_LOGGER_OVERRIDES.validate(null, "key1:INFO,key2:WARN");

    NODE_LOGGER_OVERRIDES.validate("com.foo", "TRACE");
    NODE_LOGGER_OVERRIDES.validate("com.foo", "DEBUG");
    NODE_LOGGER_OVERRIDES.validate("com.foo", "INFO");
    NODE_LOGGER_OVERRIDES.validate("com.foo", "WARN");
    NODE_LOGGER_OVERRIDES.validate("com.foo", "ERROR");

    assertThat(
        () -> NODE_LOGGER_OVERRIDES.validate(null, "key:"),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("logger-overrides should be specified in the format <logger>:<level>,<logger>:<level>...")))));
    assertThat(
        () -> NODE_LOGGER_OVERRIDES.validate(null, "key: "),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("logger-overrides should be specified in the format <logger>:<level>,<logger>:<level>...")))));
    assertThat(
        () -> NODE_LOGGER_OVERRIDES.validate(null, ":value"),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("logger-overrides should be specified in the format <logger>:<level>,<logger>:<level>...")))));
    assertThat(
        () -> NODE_LOGGER_OVERRIDES.validate(null, " :value"),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("logger-overrides should be specified in the format <logger>:<level>,<logger>:<level>...")))));

    assertThat(
        () -> NODE_LOGGER_OVERRIDES.validate("com.foo", "FATAL"),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("logger-overrides.com.foo is invalid: Bad level: FATAL")))));
  }

  private void validateOptional(Setting setting) {
    assertThat(
        () -> setting.validate("foo", "bar"),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo(setting + " is not a map")))));
    setting.validate("\u0000");
    setting.validate("  ");
  }

  private void validateRequired(Setting setting) {
    assertThat(
        () -> setting.validate("foo", "bar"),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo(setting + " is not a map")))));
    assertThat(
        () -> setting.validate("\u0000"),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Setting '" + setting + "' requires a value")))));
    assertThat(
        () -> setting.validate("  "),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Setting '" + setting + "' requires a value")))));
  }

}