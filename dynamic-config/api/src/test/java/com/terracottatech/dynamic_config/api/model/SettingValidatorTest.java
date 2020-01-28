/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.api.model;

import org.junit.Test;

import java.util.stream.Stream;

import static com.terracottatech.dynamic_config.api.model.Setting.CLIENT_LEASE_DURATION;
import static com.terracottatech.dynamic_config.api.model.Setting.CLIENT_RECONNECT_WINDOW;
import static com.terracottatech.dynamic_config.api.model.Setting.CLUSTER_NAME;
import static com.terracottatech.dynamic_config.api.model.Setting.DATA_DIRS;
import static com.terracottatech.dynamic_config.api.model.Setting.FAILOVER_PRIORITY;
import static com.terracottatech.dynamic_config.api.model.Setting.LICENSE_FILE;
import static com.terracottatech.dynamic_config.api.model.Setting.NODE_BACKUP_DIR;
import static com.terracottatech.dynamic_config.api.model.Setting.NODE_BIND_ADDRESS;
import static com.terracottatech.dynamic_config.api.model.Setting.NODE_GROUP_BIND_ADDRESS;
import static com.terracottatech.dynamic_config.api.model.Setting.NODE_GROUP_PORT;
import static com.terracottatech.dynamic_config.api.model.Setting.NODE_HOSTNAME;
import static com.terracottatech.dynamic_config.api.model.Setting.NODE_LOGGER_OVERRIDES;
import static com.terracottatech.dynamic_config.api.model.Setting.NODE_LOG_DIR;
import static com.terracottatech.dynamic_config.api.model.Setting.NODE_METADATA_DIR;
import static com.terracottatech.dynamic_config.api.model.Setting.NODE_NAME;
import static com.terracottatech.dynamic_config.api.model.Setting.NODE_PORT;
import static com.terracottatech.dynamic_config.api.model.Setting.NODE_REPOSITORY_DIR;
import static com.terracottatech.dynamic_config.api.model.Setting.OFFHEAP_RESOURCES;
import static com.terracottatech.dynamic_config.api.model.Setting.SECURITY_AUDIT_LOG_DIR;
import static com.terracottatech.dynamic_config.api.model.Setting.SECURITY_AUTHC;
import static com.terracottatech.dynamic_config.api.model.Setting.SECURITY_DIR;
import static com.terracottatech.dynamic_config.api.model.Setting.SECURITY_SSL_TLS;
import static com.terracottatech.dynamic_config.api.model.Setting.SECURITY_WHITELIST;
import static com.terracottatech.dynamic_config.api.model.Setting.TC_PROPERTIES;
import static com.terracottatech.testing.ExceptionMatcher.throwing;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Mathieu Carbou
 */
public class SettingValidatorTest {

  @Test
  public void test_defaults() {
    validateDefaults(CLUSTER_NAME);
    CLUSTER_NAME.validate(null); // cluster name can be set to null when loading config file
    CLUSTER_NAME.validate("foo");
  }

  @Test
  public void test_NODE_NAME() {
    validateDefaults(NODE_NAME);
    assertThat(
        () -> NODE_NAME.validate(null),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo(NODE_NAME + " cannot be null")))));
    Stream.of("d", "D", "h", "c", "i", "H", "n", "o", "a", "v", "t", "(").forEach(c -> {
      assertThat(
          () -> NODE_NAME.validate("%" + c),
          is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo(NODE_NAME + " cannot contain substitution parameters")))));
    });
    NODE_NAME.validate("foo");
  }

  @Test
  public void test_NODE_HOSTNAME() {
    validateDefaults(NODE_HOSTNAME);
    assertThat(
        () -> NODE_HOSTNAME.validate(".."),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("<address> specified in node-hostname=<address> must be a valid hostname or IP address")))));
    assertThat(
        () -> NODE_HOSTNAME.validate(null),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo(NODE_HOSTNAME + " cannot be null")))));
    NODE_HOSTNAME.validate("foo");
  }

  @Test
  public void test_ports() {
    Stream.of(NODE_PORT, NODE_GROUP_PORT).forEach(setting -> {
      validateDefaults(setting);
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
          () -> setting.validate(null),
          is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo(setting + " cannot be null")))));
      setting.validate("9410");
    });
  }

  @Test
  public void test_bind_addresses() {
    Stream.of(NODE_BIND_ADDRESS, NODE_GROUP_BIND_ADDRESS).forEach(setting -> {
      validateDefaults(setting);
      assertThat(
          () -> setting.validate("my-hostname"),
          is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("<address> specified in " + setting + "=<address> must be a valid IP address")))));
      assertThat(
          () -> setting.validate(null),
          is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo(setting + " cannot be null")))));
      setting.validate("0.0.0.0");
    });
  }

  @Test
  public void test_paths() {
    Stream.of(NODE_REPOSITORY_DIR, NODE_METADATA_DIR, NODE_LOG_DIR, LICENSE_FILE).forEach(setting -> {
      validateDefaults(setting);
      assertThat(
          () -> setting.validate("/\u0000/"),
          is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Invalid path specified for setting " + setting + ": /\u0000/")))));
      assertThat(
          () -> setting.validate(null),
          is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo(setting + " cannot be null")))));
      setting.validate(".");
    });

    Stream.of(NODE_BACKUP_DIR, SECURITY_DIR, SECURITY_AUDIT_LOG_DIR).forEach(setting -> {
      validateDefaults(setting);
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
      validateDefaults(setting);
      setting.validate("0s"); // ok - 0 allowed
      assertThat(
          () -> setting.validate("foo"),
          is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Invalid measure: 'foo'. <quantity> is missing. Measure should be specified in <quantity><unit> format.")))));
      assertThat(
          () -> setting.validate("1"),
          is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Invalid measure: '1'. <unit> is missing. Measure should be specified in <quantity><unit> format.")))));
      assertThat(
          () -> setting.validate("1_"),
          is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Invalid measure: '1_'. <unit> must be one of " + setting.getAllowedUnits() + ".")))));
      assertThat(
          () -> setting.validate("1bad-unit"),
          is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Invalid measure: '1bad-unit'. <unit> must be one of " + setting.getAllowedUnits() + ".")))));
      assertThat(
          () -> setting.validate("-1s"),
          is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Quantity measure cannot be negative")))));
    });
  }

  @Test
  public void test_FAILOVER_PRIORITY() {
    validateDefaults(FAILOVER_PRIORITY);
    Stream.of(
        "consistency:0",
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
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("failover-priority should be either 'availability', 'consistency', or 'consistency:N' (where 'N' is the voter count expressed as a positive integer)")))))
    );
  }

  @Test
  public void test_SECURITY_AUTHC() {
    validateDefaults(SECURITY_AUTHC);
    SECURITY_AUTHC.validate("ldap");
    SECURITY_AUTHC.validate("certificate");
    SECURITY_AUTHC.validate("file");
    assertThat(
        () -> SECURITY_AUTHC.validate("foo"),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("security-authc should be one of: [file, ldap, certificate]"))))
    );
  }

  @Test
  public void test_booleans() {
    Stream.of(SECURITY_SSL_TLS, SECURITY_WHITELIST).forEach(setting -> {
      validateDefaults(setting);
      setting.validate("true");
      setting.validate("false");
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
    OFFHEAP_RESOURCES.validate(null);
    OFFHEAP_RESOURCES.validate(null, null);
    OFFHEAP_RESOURCES.validate("main", null);
    OFFHEAP_RESOURCES.validate("main", "1GB");
    OFFHEAP_RESOURCES.validate(null, "main:1GB");
    OFFHEAP_RESOURCES.validate(null, "main:1GB,second:2GB");

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
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("offheap-resources.bar is invalid: Invalid measure: '1'. <unit> is missing. Measure should be specified in <quantity><unit> format.")))));
    assertThat(
        () -> OFFHEAP_RESOURCES.validate("foo", "bar"),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("offheap-resources.foo is invalid: Invalid measure: 'bar'. <quantity> is missing. Measure should be specified in <quantity><unit> format.")))));
  }

  @Test
  public void test_DATA_DIRS() {
    DATA_DIRS.validate(null);
    DATA_DIRS.validate(null, null);
    DATA_DIRS.validate("main", null);
    DATA_DIRS.validate("main", "foo/bar");
    DATA_DIRS.validate(null, "main:foo/bar");
    DATA_DIRS.validate(null, "main:foo/bar,second:foo/baz");

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
    TC_PROPERTIES.validate(null);
    TC_PROPERTIES.validate(null, null);
    TC_PROPERTIES.validate("key", null);
    TC_PROPERTIES.validate("key", "value");
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
    NODE_LOGGER_OVERRIDES.validate(null);
    NODE_LOGGER_OVERRIDES.validate(null, null);
    NODE_LOGGER_OVERRIDES.validate("key", null);
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
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("node-logger-overrides should be specified in the format <logger>:<level>,<logger>:<level>...")))));
    assertThat(
        () -> NODE_LOGGER_OVERRIDES.validate(null, "key: "),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("node-logger-overrides should be specified in the format <logger>:<level>,<logger>:<level>...")))));
    assertThat(
        () -> NODE_LOGGER_OVERRIDES.validate(null, ":value"),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("node-logger-overrides should be specified in the format <logger>:<level>,<logger>:<level>...")))));
    assertThat(
        () -> NODE_LOGGER_OVERRIDES.validate(null, " :value"),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("node-logger-overrides should be specified in the format <logger>:<level>,<logger>:<level>...")))));

    assertThat(
        () -> NODE_LOGGER_OVERRIDES.validate("com.foo", "FATAL"),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("node-logger-overrides.com.foo is invalid: Bad level: FATAL")))));
    assertThat(
        () -> NODE_LOGGER_OVERRIDES.validate("com.foo", "OFF"),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("node-logger-overrides.com.foo is invalid: Bad level: OFF")))));
  }

  private void validateDefaults(Setting setting) {
    assertThat(
        () -> setting.validate("foo", "bar"),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo(setting + " is not a map")))));
    assertThat(
        () -> setting.validate(""),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo(setting + " cannot be empty")))));
    assertThat(
        () -> setting.validate("\u0000"),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo(setting + " cannot be empty")))));
    assertThat(
        () -> setting.validate("  "),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo(setting + " cannot be empty")))));
  }

}