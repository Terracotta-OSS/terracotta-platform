/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.model;

import org.junit.Test;

import java.util.stream.Stream;

import static com.terracottatech.dynamic_config.model.Operation.GET;
import static com.terracottatech.dynamic_config.model.Operation.SET;
import static com.terracottatech.dynamic_config.model.Operation.UNSET;
import static com.terracottatech.dynamic_config.model.Scope.CLUSTER;
import static com.terracottatech.dynamic_config.model.Scope.NODE;
import static com.terracottatech.dynamic_config.model.Scope.STRIPE;
import static com.terracottatech.dynamic_config.model.Setting.CLIENT_LEASE_DURATION;
import static com.terracottatech.dynamic_config.model.Setting.CLIENT_RECONNECT_WINDOW;
import static com.terracottatech.dynamic_config.model.Setting.FAILOVER_PRIORITY;
import static com.terracottatech.dynamic_config.model.Setting.LICENSE_FILE;
import static com.terracottatech.dynamic_config.model.Setting.NODE_BIND_ADDRESS;
import static com.terracottatech.dynamic_config.model.Setting.NODE_GROUP_BIND_ADDRESS;
import static com.terracottatech.dynamic_config.model.Setting.NODE_GROUP_PORT;
import static com.terracottatech.dynamic_config.model.Setting.NODE_LOG_DIR;
import static com.terracottatech.dynamic_config.model.Setting.NODE_METADATA_DIR;
import static com.terracottatech.dynamic_config.model.Setting.NODE_NAME;
import static com.terracottatech.dynamic_config.model.Setting.NODE_PORT;
import static com.terracottatech.dynamic_config.model.Setting.NODE_REPOSITORY_DIR;
import static com.terracottatech.dynamic_config.model.Setting.SECURITY_SSL_TLS;
import static com.terracottatech.dynamic_config.model.Setting.SECURITY_WHITELIST;
import static com.terracottatech.dynamic_config.util.IParameterSubstitutor.identity;
import static com.terracottatech.utilities.Tuple2.tuple2;
import static com.terracottatech.utilities.hamcrest.ExceptionMatcher.throwing;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * @author Mathieu Carbou
 */
public class ConfigurationTest {

  private static final String ERROR_SETTING = " Reason: Illegal setting name: ";
  private static final String ERROR_STRIPE_ID = " Reason: Expected stripe id to be greater than 0";
  private static final String ERROR_ADDRESS = " Reason: <address> specified in node-hostname=<address> must be a valid hostname or IP address";
  private static final String ERROR_PORT = " Reason: <port> specified in node-port=<port> must be an integer between 1 and 65535";
  private static final String ERROR_AUTHC = " Reason: security-authc should be one of: [file, ldap, certificate]";
  private static final String ERROR_AUTHC_SCOPE = " Reason: Setting security-authc does not allow scope NODE";
  private static final String ERROR_FAILOVER = " Reason: failover-priority should be either 'availability', 'consistency', or 'consistency:N' (where 'N' is the voter count expressed as a positive integer)";
  private static final String ERROR_FAILOVER_SCOPE = " Reason: Setting failover-priority does not allow scope NODE";
  private static final String ERROR_SSL = " Reason: security-ssl-tls should be one of: [true, false]";
  private static final String ERROR_SSL_SCOPE = " Reason: Setting security-ssl-tls does not allow scope NODE";
  private static final String ERROR_WHITELIST = " Reason: security-whitelist should be one of: [true, false]";
  private static final String ERROR_WHITELIST_SCOPE = " Reason: Setting security-whitelist does not allow scope NODE";
  private static final String ERROR_OFFHEAP = " Reason: offheap-resources should be specified in <resource-name>:<quantity><unit>,<resource-name>:<quantity><unit>... format";
  private static final String ERROR_DIRS = " Reason: data-dirs should be specified in <resource-name>:<path>,<resource-name>:<path>... format";
  private static final String ERROR_OFFHEAP_UNIT = " Reason: Invalid measure: '1R'. <unit> must be one of [B, KB, MB, GB, TB, PB].";
  private static final String ERROR_SCOPE = " Reason: Setting offheap-resources does not allow scope NODE";
  private static final String ERROR_MEASURE = " Reason: Invalid measure: 'blah'. <quantity> is missing. Measure should be specified in <quantity><unit> format.";
  private static final String ERROR_LEASE_SCOPE = " Reason: Setting client-lease-duration does not allow scope NODE";
  private static final String ERROR_RECONNECT_SCOPE = " Reason: Setting client-reconnect-window does not allow scope NODE";
  private static final String ERROR_NODE_ID = " Reason: Expected node id to be greater than 0";
  private static final String ERROR_MAP = " Reason: Setting client-reconnect-window is not a map and must not have a key name";
  private static final String ERROR_FORMAT = "";

  @SuppressWarnings("OptionalGetWithoutIsPresent")
  @Test
  public void test_valueOf() {
    Stream.of(".", ":").forEach(nsSeparator -> Stream.of(Setting.values()).forEach(setting -> {

      if (setting.isMap()) {

        if (setting.allowsOperationsWithScope(NODE)) {
          assertThat(Configuration.valueOf("stripe.1.node.2" + nsSeparator + setting + ".key=" + value(setting, true)).getKey(), is(equalTo("key")));
          assertThat(Configuration.valueOf("stripe.1.node.2" + nsSeparator + setting + ".key=" + value(setting, true)).getValue(), is(equalTo(value(setting, true))));
          assertThat(Configuration.valueOf("stripe.1.node.2" + nsSeparator + setting + ".key=" + value(setting, true)).getNodeId(), is(equalTo(2)));
          assertThat(Configuration.valueOf("stripe.1.node.2" + nsSeparator + setting + ".key=" + value(setting, true)).getStripeId(), is(equalTo(1)));
          assertThat(Configuration.valueOf("stripe.1.node.2" + nsSeparator + setting + ".key=" + value(setting, true)).getScope(), is(equalTo(NODE)));
          assertThat(Configuration.valueOf("stripe.1.node.2" + nsSeparator + setting + ".key=" + value(setting, true)).getSetting(), is(equalTo(setting)));

          assertThat(Configuration.valueOf("stripe.1.node.2" + nsSeparator + setting + ".key").getKey(), is(equalTo("key")));
          assertThat(Configuration.valueOf("stripe.1.node.2" + nsSeparator + setting + ".key").getValue(), is(nullValue()));
          assertThat(Configuration.valueOf("stripe.1.node.2" + nsSeparator + setting + ".key").getNodeId(), is(equalTo(2)));
          assertThat(Configuration.valueOf("stripe.1.node.2" + nsSeparator + setting + ".key").getStripeId(), is(equalTo(1)));
          assertThat(Configuration.valueOf("stripe.1.node.2" + nsSeparator + setting + ".key").getScope(), is(equalTo(NODE)));
          assertThat(Configuration.valueOf("stripe.1.node.2" + nsSeparator + setting + ".key").getSetting(), is(equalTo(setting)));
        }

        if (setting.allowsOperationsWithScope(STRIPE)) {
          assertThat(Configuration.valueOf("stripe.1" + nsSeparator + setting + ".key=" + value(setting, true)).getKey(), is(equalTo("key")));
          assertThat(Configuration.valueOf("stripe.1" + nsSeparator + setting + ".key=" + value(setting, true)).getValue(), is(equalTo(value(setting, true))));
          assertThat(Configuration.valueOf("stripe.1" + nsSeparator + setting + ".key=" + value(setting, true)).getStripeId(), is(equalTo(1)));
          assertThat(Configuration.valueOf("stripe.1" + nsSeparator + setting + ".key=" + value(setting, true)).getScope(), is(equalTo(STRIPE)));
          assertThat(Configuration.valueOf("stripe.1" + nsSeparator + setting + ".key=" + value(setting, true)).getSetting(), is(equalTo(setting)));

          assertThat(Configuration.valueOf("stripe.1" + nsSeparator + setting + ".key").getKey(), is(equalTo("key")));
          assertThat(Configuration.valueOf("stripe.1" + nsSeparator + setting + ".key").getValue(), is(nullValue()));
          assertThat(Configuration.valueOf("stripe.1" + nsSeparator + setting + ".key").getStripeId(), is(equalTo(1)));
          assertThat(Configuration.valueOf("stripe.1" + nsSeparator + setting + ".key").getScope(), is(equalTo(STRIPE)));
          assertThat(Configuration.valueOf("stripe.1" + nsSeparator + setting + ".key").getSetting(), is(equalTo(setting)));
        }

        if (setting.allowsOperationsWithScope(CLUSTER)) {
          assertThat(Configuration.valueOf(setting + ".key=" + value(setting, true)).getKey(), is(equalTo("key")));
          assertThat(Configuration.valueOf(setting + ".key=" + value(setting, true)).getValue(), is(equalTo(value(setting, true))));
          assertThat(Configuration.valueOf(setting + ".key=" + value(setting, true)).getScope(), is(equalTo(CLUSTER)));
          assertThat(Configuration.valueOf(setting + ".key=" + value(setting, true)).getSetting(), is(equalTo(setting)));

          assertThat(Configuration.valueOf(setting + ".key").getKey(), is(equalTo("key")));
          assertThat(Configuration.valueOf(setting + ".key").getValue(), is(nullValue()));
          assertThat(Configuration.valueOf(setting + ".key").getScope(), is(equalTo(CLUSTER)));
          assertThat(Configuration.valueOf(setting + ".key").getSetting(), is(equalTo(setting)));
        }

      }

      if (setting.allowsOperationsWithScope(NODE)) {
        assertThat(Configuration.valueOf("stripe.1.node.2" + nsSeparator + setting + "=" + value(setting)).getKey(), is(nullValue()));
        assertThat(Configuration.valueOf("stripe.1.node.2" + nsSeparator + setting + "=" + value(setting)).getValue(), is(equalTo(value(setting))));
        assertThat(Configuration.valueOf("stripe.1.node.2" + nsSeparator + setting + "=" + value(setting)).getNodeId(), is(equalTo(2)));
        assertThat(Configuration.valueOf("stripe.1.node.2" + nsSeparator + setting + "=" + value(setting)).getStripeId(), is(equalTo(1)));
        assertThat(Configuration.valueOf("stripe.1.node.2" + nsSeparator + setting + "=" + value(setting)).getScope(), is(equalTo(NODE)));
        assertThat(Configuration.valueOf("stripe.1.node.2" + nsSeparator + setting + "=" + value(setting)).getSetting(), is(equalTo(setting)));

        assertThat(Configuration.valueOf("stripe.1.node.2" + nsSeparator + setting).getKey(), is(nullValue()));
        assertThat(Configuration.valueOf("stripe.1.node.2" + nsSeparator + setting).getValue(), is(nullValue()));
        assertThat(Configuration.valueOf("stripe.1.node.2" + nsSeparator + setting).getNodeId(), is(equalTo(2)));
        assertThat(Configuration.valueOf("stripe.1.node.2" + nsSeparator + setting).getStripeId(), is(equalTo(1)));
        assertThat(Configuration.valueOf("stripe.1.node.2" + nsSeparator + setting).getScope(), is(equalTo(NODE)));
        assertThat(Configuration.valueOf("stripe.1.node.2" + nsSeparator + setting).getSetting(), is(equalTo(setting)));
      }

      if (setting.allowsOperationsWithScope(STRIPE)) {
        assertThat(Configuration.valueOf("stripe.1" + nsSeparator + setting + "=" + value(setting)).getKey(), is(nullValue()));
        assertThat(Configuration.valueOf("stripe.1" + nsSeparator + setting + "=" + value(setting)).getValue(), is(equalTo(value(setting))));
        assertThat(Configuration.valueOf("stripe.1" + nsSeparator + setting + "=" + value(setting)).getStripeId(), is(equalTo(1)));
        assertThat(Configuration.valueOf("stripe.1" + nsSeparator + setting + "=" + value(setting)).getScope(), is(equalTo(STRIPE)));
        assertThat(Configuration.valueOf("stripe.1" + nsSeparator + setting + "=" + value(setting)).getSetting(), is(equalTo(setting)));

        assertThat(Configuration.valueOf("stripe.1" + nsSeparator + setting).getKey(), is(nullValue()));
        assertThat(Configuration.valueOf("stripe.1" + nsSeparator + setting).getValue(), is(nullValue()));
        assertThat(Configuration.valueOf("stripe.1" + nsSeparator + setting).getStripeId(), is(equalTo(1)));
        assertThat(Configuration.valueOf("stripe.1" + nsSeparator + setting).getScope(), is(equalTo(STRIPE)));
        assertThat(Configuration.valueOf("stripe.1" + nsSeparator + setting).getSetting(), is(equalTo(setting)));
      }

      if (setting.allowsOperationsWithScope(CLUSTER)) {
        assertThat(Configuration.valueOf(setting + "=" + value(setting)).getKey(), is(nullValue()));
        assertThat(Configuration.valueOf(setting + "=" + value(setting)).getValue(), is(equalTo(value(setting))));
        assertThat(Configuration.valueOf(setting + "=" + value(setting)).getScope(), is(equalTo(CLUSTER)));
        assertThat(Configuration.valueOf(setting + "=" + value(setting)).getSetting(), is(equalTo(setting)));

        assertThat(Configuration.valueOf(setting.toString()).getKey(), is(nullValue()));
        assertThat(Configuration.valueOf(setting.toString()).getValue(), is(nullValue()));
        assertThat(Configuration.valueOf(setting.toString()).getScope(), is(equalTo(CLUSTER)));
        assertThat(Configuration.valueOf(setting.toString()).getSetting(), is(equalTo(setting)));
      }
    }));

  }

  @Test
  public void test_invalid_inputs() {
    Stream.of(".", ":").forEach(nsSeparator -> {
      Stream.of(
          tuple2("", ERROR_FORMAT),
          tuple2("blah.1.node.1" + nsSeparator + "offheap-resources.main", ERROR_FORMAT),
          tuple2("stripe.1.blah.1" + nsSeparator + "offheap-resources.main", ERROR_FORMAT),
          tuple2("stripe.0.node.1" + nsSeparator + "node-backup-dir", ERROR_STRIPE_ID),
          tuple2("stripe.1.node.0" + nsSeparator + "node-backup-dir", ERROR_NODE_ID),
          tuple2("stripe.1.node.1.blah.1" + nsSeparator + "offheap-resources.main", ERROR_FORMAT),
          tuple2("stripe-1.node-1" + nsSeparator + "offheap-resources.main", ERROR_FORMAT),
          tuple2("stripe.1.node-1" + nsSeparator + "offheap-resources:main", ERROR_FORMAT),
          tuple2("stripe.1.node.1" + nsSeparator + "blah.main", ERROR_SETTING + "blah"),
          tuple2("data-dirs.main.foo", ERROR_FORMAT),
          tuple2("blah.1" + nsSeparator + "node-backup-dir", ERROR_FORMAT),
          tuple2("stripe.0" + nsSeparator + "node-backup-dir", ERROR_STRIPE_ID),
          tuple2("stripe-1" + nsSeparator + "node-backup-dir", ERROR_FORMAT),
          tuple2("stripe.1" + nsSeparator + "blah.main", ERROR_SETTING + "blah"),
          tuple2("offheap-resources:main", ERROR_FORMAT),
          tuple2("blah.main", ERROR_SETTING + "blah"),
          tuple2("stripe.1.node.1" + nsSeparator + "data-dirs.main.foo", ERROR_FORMAT)
      ).forEach(tupe -> assertThat(
          tupe.t1,
          () -> Configuration.valueOf(tupe.t1).validate(GET, identity()),
          is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Invalid input: '" + tupe.t1 + "'." + tupe.t2))))));

      Stream.of(
          tuple2("blah.1.node.1" + nsSeparator + "offheap-resources.main=512MB", ERROR_FORMAT),
          tuple2("stripe.1.blah.1" + nsSeparator + "offheap-resources.main=512MB", ERROR_FORMAT),
          tuple2("stripe.0.node.1" + nsSeparator + "node-backup-dir=foo", ERROR_STRIPE_ID),
          tuple2("stripe.1.node.0" + nsSeparator + "node-backup-dir=foo", ERROR_NODE_ID),
          tuple2("stripe.1.node.1.blah.1" + nsSeparator + "offheap-resources.main=512MB", ERROR_FORMAT),
          tuple2("stripe-1.node-1" + nsSeparator + "offheap-resources.main=512MB", ERROR_FORMAT),
          tuple2("stripe-1.node-1" + nsSeparator + "offheap-resources:main=512MB", ERROR_FORMAT),
          tuple2("stripe.1.node.1" + nsSeparator + "blah.main=512MB", ERROR_SETTING + "blah"),
          tuple2("stripe.1.node.1" + nsSeparator + "data-dirs.main.foo=512MB", ERROR_FORMAT),
          tuple2("stripe.1.node.1" + nsSeparator + "data-dirs.main.foo=", ERROR_FORMAT),
          tuple2("stripe.1.node.1" + nsSeparator + "node-port=-100", ERROR_PORT),
          tuple2("stripe.1.node.1" + nsSeparator + "node-hostname=3:3:3", ERROR_ADDRESS),
          tuple2("security-authc=blah", ERROR_AUTHC),
          tuple2("stripe.1.node.1" + nsSeparator + "security-authc=file", ERROR_AUTHC_SCOPE),
          tuple2("failover-priority=blah", ERROR_FAILOVER),
          tuple2("stripe.1.node.1" + nsSeparator + "failover-priority=availability", ERROR_FAILOVER_SCOPE),
          tuple2("security-ssl-tls=blah", ERROR_SSL),
          tuple2("stripe.1.node.1" + nsSeparator + "security-ssl-tls=true", ERROR_SSL_SCOPE),
          tuple2("security-whitelist=blah", ERROR_WHITELIST),
          tuple2("stripe.1.node.1" + nsSeparator + "security-whitelist=true", ERROR_WHITELIST_SCOPE),
          tuple2("offheap-resources.main=blah", ERROR_MEASURE),
          tuple2("stripe.1.node.1" + nsSeparator + "offheap-resources.main=1G", ERROR_SCOPE),
          tuple2("offheap-resources.main=1R", ERROR_OFFHEAP_UNIT),
          tuple2("client-lease-duration=blah", ERROR_MEASURE),
          tuple2("stripe.1.node.1" + nsSeparator + "client-lease-duration=1m", ERROR_LEASE_SCOPE),
          tuple2("client-reconnect-window=blah", ERROR_MEASURE),
          tuple2("stripe.1.node.1" + nsSeparator + "client-reconnect-window=1m", ERROR_RECONNECT_SCOPE),
          tuple2("blah.1" + nsSeparator + "offheap-resources.main=512MB", ERROR_FORMAT),
          tuple2("stripe.0" + nsSeparator + "offheap-resources.main=512MB", ERROR_STRIPE_ID),
          tuple2("stripe-1" + nsSeparator + "offheap-resources.main=512MB", ERROR_FORMAT),
          tuple2("stripe.1" + nsSeparator + "blah.main=512MB", ERROR_SETTING + "blah"),
          tuple2("offheap-resources:main=512MB", ERROR_FORMAT),
          tuple2("offheap-resources.main=main:512MB", ERROR_OFFHEAP),
          tuple2("data-dirs:main=foo/bar", ERROR_FORMAT),
          tuple2("data-dirs.main=main:foo/bar", ERROR_DIRS),
          tuple2("client-reconnect-window.main=512MB", ERROR_MAP),
          tuple2("blah.main=512MB", ERROR_SETTING + "blah")
      ).forEach(tupe -> assertThat(
          tupe.t1,
          () -> Configuration.valueOf(tupe.t1).validate(SET, identity()),
          is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Invalid input: '" + tupe.t1 + "'." + tupe.t2))))));
    });
  }

  @Test
  public void test_match() {
    Stream.of(".", ":").forEach(nsSeparator -> {
      Stream.of(
          tuple2("offheap-resources", "offheap-resources=main:1GB,second:2GB"),
          tuple2("offheap-resources.main", "offheap-resources.main=1GB")
      ).forEach(tupe -> assertThat(tupe.t1 + " matches " + tupe.t2, Configuration.valueOf(tupe.t1).matchConfigPropertyKey(tupe.t2), is(true)));

      Stream.of(
          tuple2("offheap-resources", "offheap-resources.main=1GB"),
          tuple2("offheap-resources", "offheap-resources.second=2GB"),
          tuple2("offheap-resources.main", "offheap-resources.second=1GB"),
          tuple2("offheap-resources.main", "offheap-resources=main:1GB")
      ).forEach(tupe -> assertThat(tupe.t1 + " matches " + tupe.t2, Configuration.valueOf(tupe.t1).matchConfigPropertyKey(tupe.t2), is(false)));
    });
  }

  @Test
  public void test_validate() {
    Stream.of(NODE_NAME, NODE_REPOSITORY_DIR)
        .forEach(s -> assertThat(
            s.toString(),
            () -> Configuration.valueOf(s + "=foo").validate(SET, identity()),
            is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(containsString("Setting " + s + " does not allow operation set"))))));

    Stream.of(NODE_NAME, NODE_REPOSITORY_DIR, NODE_PORT, NODE_GROUP_PORT, NODE_BIND_ADDRESS, NODE_GROUP_BIND_ADDRESS, NODE_METADATA_DIR, NODE_LOG_DIR)
        .forEach(s -> assertThat(
            s.toString(),
            () -> Configuration.valueOf("stripe.1.node.1." + s).validate(UNSET, identity()),
            is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(containsString("Setting " + s + " does not allow operation unset"))))));

    Stream.of(CLIENT_RECONNECT_WINDOW, FAILOVER_PRIORITY, CLIENT_LEASE_DURATION, LICENSE_FILE, SECURITY_SSL_TLS, SECURITY_WHITELIST)
        .forEach(s -> assertThat(
            s.toString(),
            () -> Configuration.valueOf(s.toString()).validate(UNSET, identity()),
            is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Invalid input: '" + s + "'. Reason: Setting " + s + " does not allow operation unset"))))));

    Configuration.valueOf("offheap-resources").validate(GET, identity());
    Configuration.valueOf("offheap-resources.main").validate(GET, identity());
    Configuration.valueOf("offheap-resources").validate(UNSET, identity());
    Configuration.valueOf("offheap-resources.main").validate(UNSET, identity());
    Configuration.valueOf("offheap-resources=main:1GB").validate(SET, identity());
    Configuration.valueOf("offheap-resources.main=1GB").validate(SET, identity());
    assertThat(
        () -> Configuration.valueOf("offheap-resources").validate(SET, identity()),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Invalid input: 'offheap-resources'. Reason: Operation set requires a value")))));
    assertThat(
        () -> Configuration.valueOf("offheap-resources=main:1GB").validate(GET, identity()),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Invalid input: 'offheap-resources=main:1GB'. Reason: Operation get must not have a value")))));
    assertThat(
        () -> Configuration.valueOf("offheap-resources=main:1GB").validate(UNSET, identity()),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Invalid input: 'offheap-resources=main:1GB'. Reason: Operation unset must not have a value")))));

    assertThat(
        () -> Configuration.valueOf("offheap-resources.main").validate(SET, identity()),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Invalid input: 'offheap-resources.main'. Reason: Operation set requires a value")))));
    assertThat(
        () -> Configuration.valueOf("offheap-resources.main=1GB").validate(GET, identity()),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Invalid input: 'offheap-resources.main=1GB'. Reason: Operation get must not have a value")))));
    assertThat(
        () -> Configuration.valueOf("offheap-resources.main=1GB").validate(UNSET, identity()),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Invalid input: 'offheap-resources.main=1GB'. Reason: Operation unset must not have a value")))));

    Configuration.valueOf("data-dirs").validate(GET, identity());
    Configuration.valueOf("data-dirs.main").validate(GET, identity());
    Configuration.valueOf("data-dirs").validate(UNSET, identity());
    Configuration.valueOf("data-dirs.main").validate(UNSET, identity());
    Configuration.valueOf("data-dirs=main:foo/bar").validate(SET, identity());
    Configuration.valueOf("data-dirs.main=foo/bar").validate(SET, identity());
    assertThat(
        () -> Configuration.valueOf("data-dirs").validate(SET, identity()),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Invalid input: 'data-dirs'. Reason: Operation set requires a value")))));
    assertThat(
        () -> Configuration.valueOf("data-dirs=main:foo/bar").validate(GET, identity()),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Invalid input: 'data-dirs=main:foo/bar'. Reason: Operation get must not have a value")))));
    assertThat(
        () -> Configuration.valueOf("data-dirs=main:foo/bar").validate(UNSET, identity()),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Invalid input: 'data-dirs=main:foo/bar'. Reason: Operation unset must not have a value")))));
    assertThat(
        () -> Configuration.valueOf("data-dirs.main").validate(SET, identity()),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Invalid input: 'data-dirs.main'. Reason: Operation set requires a value")))));
    assertThat(
        () -> Configuration.valueOf("data-dirs.main=foo/bar").validate(GET, identity()),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Invalid input: 'data-dirs.main=foo/bar'. Reason: Operation get must not have a value")))));
    assertThat(
        () -> Configuration.valueOf("data-dirs.main=foo/bar").validate(UNSET, identity()),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Invalid input: 'data-dirs.main=foo/bar'. Reason: Operation unset must not have a value")))));
  }

  private static String value(Setting setting) {
    return value(setting, false);
  }

  private static String value(Setting setting, boolean stripKey) {
    if (setting == NODE_NAME) {
      return "node-1234";
    }
    String kv = setting.getDefaultValue();
    if (kv == null) {
      kv = setting.getAllowedValues().isEmpty() ? "val" : setting.getAllowedValues().iterator().next();
    }
    return stripKey && kv.contains(":") ? kv.split(":")[1] : kv;
  }

}