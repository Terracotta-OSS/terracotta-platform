/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.api.model;

import org.hamcrest.CustomTypeSafeMatcher;
import org.hamcrest.Matcher;
import org.junit.Test;

import java.io.File;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static com.terracottatech.dynamic_config.api.model.Operation.CONFIG;
import static com.terracottatech.dynamic_config.api.model.Operation.GET;
import static com.terracottatech.dynamic_config.api.model.Operation.SET;
import static com.terracottatech.dynamic_config.api.model.Operation.UNSET;
import static com.terracottatech.dynamic_config.api.model.Scope.CLUSTER;
import static com.terracottatech.dynamic_config.api.model.Scope.NODE;
import static com.terracottatech.dynamic_config.api.model.Scope.STRIPE;
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
import static com.terracottatech.struct.tuple.Tuple2.tuple2;
import static com.terracottatech.struct.tuple.Tuple3.tuple3;
import static com.terracottatech.testing.ExceptionMatcher.throwing;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author Mathieu Carbou
 */
public class ConfigurationTest {

  @Test
  public void test_valueOf_settings_cluster_level() {
    Stream.of(NODE_REPOSITORY_DIR).forEach(setting -> {
      String err = "Invalid input: 'node-repository-dir=%H/terracotta/repository'. Reason: node-repository-dir does not allow any operation at cluster level".replace("/", File.separator); // unix/win compat'
      assertThat(
          () -> Configuration.valueOf(setting),
          is(throwing(instanceOf(IllegalArgumentException.class))
              .andMessage(is(equalTo(err)))));
    });

    Stream.of(
        LICENSE_FILE
    ).forEach(setting -> assertThat(
        () -> Configuration.valueOf(setting),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Invalid input: 'license-file='. Reason: license-file requires a value"))))));

    Stream.of(
        NODE_HOSTNAME,
        NODE_NAME,
        NODE_PORT
    ).forEach(setting -> assertThat(
        () -> Configuration.valueOf(setting),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(endsWith("Reason: " + setting + " cannot be set at cluster level"))))));

    Stream.of(
        CLIENT_LEASE_DURATION,
        CLIENT_RECONNECT_WINDOW,
        CLUSTER_NAME,
        DATA_DIRS,
        FAILOVER_PRIORITY,
        NODE_BACKUP_DIR,
        NODE_BIND_ADDRESS,
        NODE_GROUP_BIND_ADDRESS,
        NODE_GROUP_PORT,
        NODE_LOG_DIR,
        NODE_METADATA_DIR,
        OFFHEAP_RESOURCES,
        SECURITY_AUDIT_LOG_DIR,
        SECURITY_AUTHC,
        SECURITY_DIR,
        SECURITY_SSL_TLS,
        SECURITY_WHITELIST,
        TC_PROPERTIES
    ).forEach(setting -> {
      Configuration configuration = Configuration.valueOf(setting);
      String rawInput = configuration.toString();

      // verify that we can parse back the generated string
      assertThat(rawInput, configuration, is(equalTo(Configuration.valueOf(rawInput))));

      // verify parsed attributes
      assertThat(rawInput, configuration.getScope(), is(equalTo(CLUSTER)));
      assertThat(rawInput, configuration.getSetting(), is(setting));
      assertThat(rawInput, configuration.getKey(), is(nullValue()));

      // verify the generated string
      if (setting == NODE_NAME) {
        // node name always generates a random default value
        assertThat(rawInput, rawInput, startsWith("node-name=node-"));
      } else {
        String defaultValue = setting.getDefaultValue();
        assertThat(rawInput, configuration.getValue(), is(equalTo(defaultValue)));
        assertThat(rawInput, rawInput, is(equalTo(setting + "=" + (defaultValue == null ? "" : defaultValue))));
      }
    });
  }

  @Test
  public void test_valueOf_settings_stripe_level() {
    Stream.of(
        CLIENT_LEASE_DURATION,
        CLIENT_RECONNECT_WINDOW,
        CLUSTER_NAME,
        FAILOVER_PRIORITY,
        LICENSE_FILE,
        NODE_REPOSITORY_DIR,
        OFFHEAP_RESOURCES,
        SECURITY_AUTHC,
        SECURITY_SSL_TLS,
        SECURITY_WHITELIST
    ).forEach(setting -> assertThat(
        setting.toString(),
        () -> Configuration.valueOf(setting, 1),
        is(throwing(instanceOf(IllegalArgumentException.class))
            .andMessage(both(
                startsWith("Invalid input: 'stripe.1." + setting + "="))
                .and(endsWith("'. Reason: " + setting + " does not allow any operation at stripe level"))))));


    Stream.of(
        NODE_HOSTNAME,
        NODE_NAME,
        NODE_PORT
    ).forEach(setting -> assertThat(
        () -> Configuration.valueOf(setting, 1),
        is(throwing(instanceOf(IllegalArgumentException.class))
            .andMessage(both(
                startsWith("Invalid input: 'stripe.1." + setting + "="))
                .and(endsWith("'. Reason: " + setting + " cannot be set at stripe level"))))));

    Stream.of(
        DATA_DIRS,
        NODE_BACKUP_DIR,
        NODE_BIND_ADDRESS,
        NODE_GROUP_BIND_ADDRESS,
        NODE_GROUP_PORT,
        NODE_LOG_DIR,
        NODE_METADATA_DIR,
        SECURITY_AUDIT_LOG_DIR,
        SECURITY_DIR,
        TC_PROPERTIES
    ).forEach(setting -> {
      Configuration configuration = Configuration.valueOf(setting, 1);
      String rawInput = configuration.toString();

      // verify that we can parse back the generated string
      assertThat(rawInput, configuration, is(equalTo(Configuration.valueOf(rawInput))));

      // verify parsed attributes
      assertThat(rawInput, configuration.getScope(), is(equalTo(STRIPE)));
      assertThat(rawInput, configuration.getSetting(), is(setting));
      assertThat(rawInput, configuration.getKey(), is(nullValue()));

      // verify the generated string
      if (setting == NODE_NAME) {
        // node name always generates a random default value
        assertThat(rawInput, rawInput, startsWith("stripe.1.node-name=node-"));
      } else {
        String defaultValue = setting.getDefaultValue();
        assertThat(rawInput, configuration.getValue(), is(equalTo(defaultValue)));
        assertThat(rawInput, rawInput, is(equalTo("stripe.1." + setting + "=" + (defaultValue == null ? "" : defaultValue))));
      }
    });
  }

  @Test
  public void test_valueOf_settings_node_level() {
    Stream.of(
        CLIENT_LEASE_DURATION,
        CLIENT_RECONNECT_WINDOW,
        CLUSTER_NAME,
        FAILOVER_PRIORITY,
        LICENSE_FILE,
        NODE_REPOSITORY_DIR,
        OFFHEAP_RESOURCES,
        SECURITY_AUTHC,
        SECURITY_SSL_TLS,
        SECURITY_WHITELIST
    ).forEach(setting -> assertThat(
        setting.toString(),
        () -> Configuration.valueOf(setting, 1, 1),
        is(throwing(instanceOf(IllegalArgumentException.class))
            .andMessage(both(
                startsWith("Invalid input: 'stripe.1.node.1." + setting + "="))
                .and(endsWith("'. Reason: " + setting + " does not allow any operation at node level"))))));

    Stream.of(
        NODE_NAME,
        NODE_HOSTNAME,
        NODE_PORT,
        DATA_DIRS,
        NODE_BACKUP_DIR,
        NODE_BIND_ADDRESS,
        NODE_GROUP_BIND_ADDRESS,
        NODE_GROUP_PORT,
        NODE_LOG_DIR,
        NODE_METADATA_DIR,
        SECURITY_AUDIT_LOG_DIR,
        SECURITY_DIR,
        TC_PROPERTIES
    ).forEach(setting -> {
      Configuration configuration = Configuration.valueOf(setting, 1, 1);
      String rawInput = configuration.toString();

      // verify that we can parse back the generated string
      assertThat(rawInput, configuration, is(equalTo(Configuration.valueOf(rawInput))));

      // verify parsed attributes
      assertThat(rawInput, configuration.getScope(), is(equalTo(NODE)));
      assertThat(rawInput, configuration.getSetting(), is(setting));
      assertThat(rawInput, configuration.getKey(), is(nullValue()));

      // verify the generated string
      if (setting == NODE_NAME) {
        // node name always generates a random default value
        assertThat(rawInput, rawInput, startsWith("stripe.1.node.1.node-name=node-"));
      } else {
        String defaultValue = setting.getDefaultValue();
        assertThat(rawInput, configuration.getValue(), is(equalTo(defaultValue)));
        assertThat(rawInput, rawInput, is(equalTo("stripe.1.node.1." + setting + "=" + (defaultValue == null ? "" : defaultValue))));
      }
    });
  }

  @Test
  public void test_valueOf_with_valid_string() {
    // test for each supported namespace separation . and : (i.e. stripe.1.node-backup-dir=foo/bar and stripe.1:node-backup-dir=foo/bar)
    Stream.of(".", ":").forEach(ns -> {

      // get allowed for all scopes
      // empty value disallowed
      // set allowed for scope node
      Stream.of(
          tuple2(NODE_NAME, "foo"),
          tuple2(NODE_HOSTNAME, "foo"),
          tuple2(NODE_PORT, "9410")
      ).forEach(tuple -> {
        allowInput(tuple.t1.toString(), tuple.t1, CLUSTER, null, null, null, null);
        rejectInput(tuple.t1 + "=", "Invalid input: '" + tuple.t1 + "='. Reason: " + tuple.t1 + " requires a value");
        rejectInput(tuple.t1 + "=" + tuple.t2, "Invalid input: '" + tuple.t1 + "=" + tuple.t2 + "'. Reason: " + tuple.t1 + " cannot be set at cluster level");

        allowInput("stripe.1" + ns + tuple.t1, tuple.t1, STRIPE, 1, null, null, null);
        rejectInput("stripe.1" + ns + tuple.t1 + "=", "Invalid input: 'stripe.1" + ns + tuple.t1 + "='. Reason: " + tuple.t1 + " requires a value");
        rejectInput("stripe.1" + ns + tuple.t1 + "=" + tuple.t2, "Invalid input: 'stripe.1" + ns + tuple.t1 + "=" + tuple.t2 + "'. Reason: " + tuple.t1 + " cannot be set at stripe level");

        allowInput("stripe.1.node.1" + ns + tuple.t1, tuple.t1, NODE, 1, 1, null, null);
        rejectInput("stripe.1.node.1" + ns + tuple.t1 + "=", "Invalid input: 'stripe.1.node.1" + ns + tuple.t1 + "='. Reason: " + tuple.t1 + " requires a value");
        allowInput("stripe.1.node.1" + ns + tuple.t1 + "=" + tuple.t2, tuple.t1, NODE, 1, 1, null, tuple.t2);
      });

      // get allowed for all scopes
      // empty value disallowed
      // set allowed for all scopes
      Stream.of(
          tuple2(NODE_GROUP_PORT, "9410"),
          tuple2(NODE_BIND_ADDRESS, "0.0.0.0"),
          tuple2(NODE_GROUP_BIND_ADDRESS, "0.0.0.0"),
          tuple2(NODE_METADATA_DIR, "foo/bar"),
          tuple2(NODE_LOG_DIR, "foo/bar")
      ).forEach(tuple -> {
        allowInput(tuple.t1.toString(), tuple.t1, CLUSTER, null, null, null, null);
        rejectInput(tuple.t1 + "=", "Invalid input: '" + tuple.t1 + "='. Reason: " + tuple.t1 + " requires a value");
        allowInput(tuple.t1 + "=" + tuple.t2, tuple.t1, CLUSTER, null, null, null, tuple.t2);

        allowInput("stripe.1" + ns + tuple.t1, tuple.t1, STRIPE, 1, null, null, null);
        rejectInput("stripe.1" + ns + tuple.t1 + "=", "Invalid input: 'stripe.1" + ns + tuple.t1 + "='. Reason: " + tuple.t1 + " requires a value");
        allowInput("stripe.1" + ns + tuple.t1 + "=" + tuple.t2, tuple.t1, STRIPE, null, null, null, tuple.t2);

        allowInput("stripe.1.node.1" + ns + tuple.t1, tuple.t1, NODE, 1, 1, null, null);
        rejectInput("stripe.1.node.1" + ns + tuple.t1 + "=", "Invalid input: 'stripe.1.node.1" + ns + tuple.t1 + "='. Reason: " + tuple.t1 + " requires a value");
        allowInput("stripe.1.node.1" + ns + tuple.t1 + "=" + tuple.t2, tuple.t1, NODE, 1, 1, null, tuple.t2);
      });

      // get allowed for all scopes
      // empty value allowed for all scopes
      // set allowed for all scopes
      Stream.of(
          tuple2(NODE_BACKUP_DIR, "foo/bar"),
          tuple2(SECURITY_DIR, "foo/bar"),
          tuple2(SECURITY_AUDIT_LOG_DIR, "foo/bar")
      ).forEach(tuple -> {
        allowInput(tuple.t1.toString(), tuple.t1, CLUSTER, null, null, null, null);
        allowInput(tuple.t1 + "=", tuple.t1, CLUSTER, null, null, null, null);
        allowInput(tuple.t1 + "=" + tuple.t2, tuple.t1, CLUSTER, null, null, null, tuple.t2);

        allowInput("stripe.1" + ns + tuple.t1, tuple.t1, STRIPE, 1, null, null, null);
        allowInput("stripe.1" + ns + tuple.t1 + "=", tuple.t1, STRIPE, null, null, null, null);
        allowInput("stripe.1" + ns + tuple.t1 + "=" + tuple.t2, tuple.t1, STRIPE, null, null, null, tuple.t2);

        allowInput("stripe.1.node.1" + ns + tuple.t1, tuple.t1, NODE, 1, 1, null, null);
        allowInput("stripe.1.node.1" + ns + tuple.t1 + "=", tuple.t1, NODE, 1, 1, null, null);
        allowInput("stripe.1.node.1" + ns + tuple.t1 + "=" + tuple.t2, tuple.t1, NODE, 1, 1, null, tuple.t2);
      });

      // get allowed for scope cluster
      // empty value not allowed
      // set allowed for scope cluster
      Stream.of(
          tuple2(CLIENT_RECONNECT_WINDOW, "20s"),
          tuple2(FAILOVER_PRIORITY, "availability"),
          tuple2(CLIENT_LEASE_DURATION, "20s"),
          tuple2(SECURITY_SSL_TLS, "true"),
          tuple2(SECURITY_WHITELIST, "true")
      ).forEach(tuple -> {
        allowInput(tuple.t1.toString(), tuple.t1, CLUSTER, null, null, null, null);
        rejectInput(tuple.t1 + "=", "Invalid input: '" + tuple.t1 + "='. Reason: " + tuple.t1 + " requires a value");
        allowInput(tuple.t1 + "=" + tuple.t2, tuple.t1, CLUSTER, null, null, null, tuple.t2);

        rejectInput("stripe.1" + ns + tuple.t1, "Invalid input: 'stripe.1" + ns + tuple.t1 + "'. Reason: " + tuple.t1 + " does not allow any operation at stripe level");
        rejectInput("stripe.1" + ns + tuple.t1 + "=", "Invalid input: 'stripe.1" + ns + tuple.t1 + "='. Reason: " + tuple.t1 + " does not allow any operation at stripe level");
        rejectInput("stripe.1" + ns + tuple.t1 + "=" + tuple.t2, "Invalid input: 'stripe.1" + ns + tuple.t1 + "=" + tuple.t2 + "'. Reason: " + tuple.t1 + " does not allow any operation at stripe level");

        rejectInput("stripe.1.node.1" + ns + tuple.t1, "Invalid input: 'stripe.1.node.1" + ns + tuple.t1 + "'. Reason: " + tuple.t1 + " does not allow any operation at node level");
        rejectInput("stripe.1.node.1" + ns + tuple.t1 + "=", "Invalid input: 'stripe.1.node.1" + ns + tuple.t1 + "='. Reason: " + tuple.t1 + " does not allow any operation at node level");
        rejectInput("stripe.1.node.1" + ns + tuple.t1 + "=" + tuple.t2, "Invalid input: 'stripe.1.node.1" + ns + tuple.t1 + "=" + tuple.t2 + "'. Reason: " + tuple.t1 + " does not allow any operation at node level");
      });

      // get allowed for scope cluster
      // empty value allowed for scope cluster
      // set allowed for scope cluster
      Stream.of(
          tuple2(CLUSTER_NAME, "foo"),
          tuple2(SECURITY_AUTHC, "certificate")
      ).forEach(tuple -> {
        allowInput(tuple.t1.toString(), tuple.t1, CLUSTER, null, null, null, null);
        allowInput(tuple.t1 + "=", tuple.t1, CLUSTER, null, null, null, null);
        allowInput(tuple.t1 + "=" + tuple.t2, tuple.t1, CLUSTER, null, null, null, tuple.t2);

        rejectInput("stripe.1" + ns + tuple.t1, "Invalid input: 'stripe.1" + ns + tuple.t1 + "'. Reason: " + tuple.t1 + " does not allow any operation at stripe level");
        rejectInput("stripe.1" + ns + tuple.t1 + "=", "Invalid input: 'stripe.1" + ns + tuple.t1 + "='. Reason: " + tuple.t1 + " does not allow any operation at stripe level");
        rejectInput("stripe.1" + ns + tuple.t1 + "=" + tuple.t2, "Invalid input: 'stripe.1" + ns + tuple.t1 + "=" + tuple.t2 + "'. Reason: " + tuple.t1 + " does not allow any operation at stripe level");

        rejectInput("stripe.1.node.1" + ns + tuple.t1, "Invalid input: 'stripe.1.node.1" + ns + tuple.t1 + "'. Reason: " + tuple.t1 + " does not allow any operation at node level");
        rejectInput("stripe.1.node.1" + ns + tuple.t1 + "=", "Invalid input: 'stripe.1.node.1" + ns + tuple.t1 + "='. Reason: " + tuple.t1 + " does not allow any operation at node level");
        rejectInput("stripe.1.node.1" + ns + tuple.t1 + "=" + tuple.t2, "Invalid input: 'stripe.1.node.1" + ns + tuple.t1 + "=" + tuple.t2 + "'. Reason: " + tuple.t1 + " does not allow any operation at node level");
      });

      // get not allowed
      // empty value not allowed
      // set not allowed
      Stream.of(
          tuple2(NODE_REPOSITORY_DIR, "foo/bar")
      ).forEach(tuple -> {
        rejectInput(tuple.t1.toString(), "Invalid input: '" + tuple.t1 + "'. Reason: " + tuple.t1 + " does not allow any operation at cluster level");
        rejectInput(tuple.t1 + "=", "Invalid input: '" + tuple.t1 + "='. Reason: " + tuple.t1 + " does not allow any operation at cluster level");
        rejectInput(tuple.t1 + "=" + tuple.t2, "Invalid input: '" + tuple.t1 + "=" + tuple.t2 + "'. Reason: " + tuple.t1 + " does not allow any operation at cluster level");

        rejectInput("stripe.1" + ns + tuple.t1, "Invalid input: 'stripe.1" + ns + tuple.t1 + "'. Reason: " + tuple.t1 + " does not allow any operation at stripe level");
        rejectInput("stripe.1" + ns + tuple.t1 + "=", "Invalid input: 'stripe.1" + ns + tuple.t1 + "='. Reason: " + tuple.t1 + " does not allow any operation at stripe level");
        rejectInput("stripe.1" + ns + tuple.t1 + "=" + tuple.t2, "Invalid input: 'stripe.1" + ns + tuple.t1 + "=" + tuple.t2 + "'. Reason: " + tuple.t1 + " does not allow any operation at stripe level");

        rejectInput("stripe.1.node.1" + ns + tuple.t1, "Invalid input: 'stripe.1.node.1" + ns + tuple.t1 + "'. Reason: " + tuple.t1 + " does not allow any operation at node level");
        rejectInput("stripe.1.node.1" + ns + tuple.t1 + "=", "Invalid input: 'stripe.1.node.1" + ns + tuple.t1 + "='. Reason: " + tuple.t1 + " does not allow any operation at node level");
        rejectInput("stripe.1.node.1" + ns + tuple.t1 + "=" + tuple.t2, "Invalid input: 'stripe.1.node.1" + ns + tuple.t1 + "=" + tuple.t2 + "'. Reason: " + tuple.t1 + " does not allow any operation at node level");
      });

      // get not allowed
      // empty value not allowed
      // set allowed for scope cluster
      Stream.of(
          tuple2(LICENSE_FILE, "/path/to/license.xml")
      ).forEach(tuple -> {
        rejectInput(tuple.t1.toString(), "Invalid input: '" + tuple.t1 + "'. Reason: " + tuple.t1 + " cannot be read or cleared");
        rejectInput(tuple.t1 + "=", "Invalid input: '" + tuple.t1 + "='. Reason: " + tuple.t1 + " requires a value");
        allowInput(tuple.t1 + "=" + tuple.t2, tuple.t1, CLUSTER, null, null, null, tuple.t2);

        rejectInput("stripe.1" + ns + tuple.t1, "Invalid input: 'stripe.1" + ns + tuple.t1 + "'. Reason: " + tuple.t1 + " does not allow any operation at stripe level");
        rejectInput("stripe.1" + ns + tuple.t1 + "=", "Invalid input: 'stripe.1" + ns + tuple.t1 + "='. Reason: " + tuple.t1 + " does not allow any operation at stripe level");
        rejectInput("stripe.1" + ns + tuple.t1 + "=" + tuple.t2, "Invalid input: 'stripe.1" + ns + tuple.t1 + "=" + tuple.t2 + "'. Reason: " + tuple.t1 + " does not allow any operation at stripe level");

        rejectInput("stripe.1.node.1" + ns + tuple.t1, "Invalid input: 'stripe.1.node.1" + ns + tuple.t1 + "'. Reason: " + tuple.t1 + " does not allow any operation at node level");
        rejectInput("stripe.1.node.1" + ns + tuple.t1 + "=", "Invalid input: 'stripe.1.node.1" + ns + tuple.t1 + "='. Reason: " + tuple.t1 + " does not allow any operation at node level");
        rejectInput("stripe.1.node.1" + ns + tuple.t1 + "=" + tuple.t2, "Invalid input: 'stripe.1.node.1" + ns + tuple.t1 + "=" + tuple.t2 + "'. Reason: " + tuple.t1 + " does not allow any operation at node level");
      });

      // get allowed for all scopes
      // empty value allowed for all scopes
      // set allowed for all scopes
      Stream.of(
          tuple3(TC_PROPERTIES, "a.b.c", "d.e.f"),
          tuple3(DATA_DIRS, "a.b.c", "foo/bar")
      ).forEach(tuple -> {
        allowInput(tuple.t1.toString(), tuple.t1, CLUSTER, null, null, null, null);
        allowInput(tuple.t1 + "=", tuple.t1, CLUSTER, null, null, null, null);
        allowInput(tuple.t1 + "=" + tuple.t2 + ":" + tuple.t3, tuple.t1, CLUSTER, null, null, null, tuple.t2 + ":" + tuple.t3);

        allowInput(tuple.t1 + "." + tuple.t2, tuple.t1, CLUSTER, null, null, tuple.t2, null);
        allowInput(tuple.t1 + "." + tuple.t2 + "=", tuple.t1, CLUSTER, null, null, tuple.t2, null);
        allowInput(tuple.t1 + "." + tuple.t2 + "=" + tuple.t3, tuple.t1, CLUSTER, null, null, tuple.t2, tuple.t3);

        allowInput("stripe.1" + ns + tuple.t1, tuple.t1, STRIPE, 1, null, null, null);
        allowInput("stripe.1" + ns + tuple.t1 + "=", tuple.t1, STRIPE, null, null, null, null);
        allowInput("stripe.1" + ns + tuple.t1 + "=" + tuple.t2 + ":" + tuple.t3, tuple.t1, STRIPE, null, null, null, tuple.t2 + ":" + tuple.t3);

        allowInput("stripe.1" + ns + tuple.t1 + "." + tuple.t2, tuple.t1, STRIPE, 1, null, tuple.t2, null);
        allowInput("stripe.1" + ns + tuple.t1 + "." + tuple.t2 + "=", tuple.t1, STRIPE, null, null, tuple.t2, null);
        allowInput("stripe.1" + ns + tuple.t1 + "." + tuple.t2 + "=" + tuple.t3, tuple.t1, STRIPE, null, null, tuple.t2, tuple.t3);

        allowInput("stripe.1.node.1" + ns + tuple.t1, tuple.t1, NODE, 1, 1, null, null);
        allowInput("stripe.1.node.1" + ns + tuple.t1 + "=", tuple.t1, NODE, 1, 1, null, null);
        allowInput("stripe.1.node.1" + ns + tuple.t1 + "=" + tuple.t2 + ":" + tuple.t3, tuple.t1, NODE, 1, 1, null, tuple.t2 + ":" + tuple.t3);

        allowInput("stripe.1.node.1" + ns + tuple.t1 + "." + tuple.t2, tuple.t1, NODE, 1, 1, tuple.t2, null);
        allowInput("stripe.1.node.1" + ns + tuple.t1 + "." + tuple.t2 + "=", tuple.t1, NODE, 1, 1, tuple.t2, null);
        allowInput("stripe.1.node.1" + ns + tuple.t1 + "." + tuple.t2 + "=" + tuple.t3, tuple.t1, NODE, 1, 1, tuple.t2, tuple.t3);
      });

      // get allowed for scope cluster
      // empty value allowed for scope cluster
      // set allowed for scope cluster
      Stream.of(
          tuple3(OFFHEAP_RESOURCES, "a.b.c", "1GB")
      ).forEach(tuple -> {
        allowInput(tuple.t1.toString(), tuple.t1, CLUSTER, null, null, null, null);
        allowInput(tuple.t1 + "=", tuple.t1, CLUSTER, null, null, null, null);
        allowInput(tuple.t1 + "=" + tuple.t2 + ":" + tuple.t3, tuple.t1, CLUSTER, null, null, null, tuple.t2 + ":" + tuple.t3);

        allowInput(tuple.t1 + "." + tuple.t2, tuple.t1, CLUSTER, null, null, tuple.t2, null);
        allowInput(tuple.t1 + "." + tuple.t2 + "=", tuple.t1, CLUSTER, null, null, tuple.t2, null);
        allowInput(tuple.t1 + "." + tuple.t2 + "=" + tuple.t3, tuple.t1, CLUSTER, null, null, tuple.t2, tuple.t3);

        rejectInput("stripe.1" + ns + tuple.t1, "Invalid input: 'stripe.1" + ns + tuple.t1 + "'. Reason: " + tuple.t1 + " does not allow any operation at stripe level");
        rejectInput("stripe.1" + ns + tuple.t1 + "=", "Invalid input: 'stripe.1" + ns + tuple.t1 + "='. Reason: " + tuple.t1 + " does not allow any operation at stripe level");
        rejectInput("stripe.1" + ns + tuple.t1 + "=" + tuple.t2 + ":" + tuple.t3, "Invalid input: 'stripe.1" + ns + tuple.t1 + "=" + tuple.t2 + ":" + tuple.t3 + "'. Reason: " + tuple.t1 + " does not allow any operation at stripe level");

        rejectInput("stripe.1" + ns + tuple.t1 + "." + tuple.t2, "Invalid input: 'stripe.1" + ns + tuple.t1 + "." + tuple.t2 + "'. Reason: " + tuple.t1 + " does not allow any operation at stripe level");
        rejectInput("stripe.1" + ns + tuple.t1 + "." + tuple.t2 + "=", "Invalid input: 'stripe.1" + ns + tuple.t1 + "." + tuple.t2 + "='. Reason: " + tuple.t1 + " does not allow any operation at stripe level");
        rejectInput("stripe.1" + ns + tuple.t1 + "." + tuple.t2 + "=" + tuple.t3, "Invalid input: 'stripe.1" + ns + tuple.t1 + "." + tuple.t2 + "=" + tuple.t3 + "'. Reason: " + tuple.t1 + " does not allow any operation at stripe level");

        rejectInput("stripe.1.node.1" + ns + tuple.t1, "Invalid input: 'stripe.1.node.1" + ns + tuple.t1 + "'. Reason: " + tuple.t1 + " does not allow any operation at node level");
        rejectInput("stripe.1.node.1" + ns + tuple.t1 + "=", "Invalid input: 'stripe.1.node.1" + ns + tuple.t1 + "='. Reason: " + tuple.t1 + " does not allow any operation at node level");
        rejectInput("stripe.1.node.1" + ns + tuple.t1 + "=" + tuple.t2 + ":" + tuple.t3, "Invalid input: 'stripe.1.node.1" + ns + tuple.t1 + "=" + tuple.t2 + ":" + tuple.t3 + "'. Reason: " + tuple.t1 + " does not allow any operation at node level");

        rejectInput("stripe.1.node.1" + ns + tuple.t1 + "." + tuple.t2, "Invalid input: 'stripe.1.node.1" + ns + tuple.t1 + "." + tuple.t2 + "'. Reason: " + tuple.t1 + " does not allow any operation at node level");
        rejectInput("stripe.1.node.1" + ns + tuple.t1 + "." + tuple.t2 + "=", "Invalid input: 'stripe.1.node.1" + ns + tuple.t1 + "." + tuple.t2 + "='. Reason: " + tuple.t1 + " does not allow any operation at node level");
        rejectInput("stripe.1.node.1" + ns + tuple.t1 + "." + tuple.t2 + "=" + tuple.t3, "Invalid input: 'stripe.1.node.1" + ns + tuple.t1 + "." + tuple.t2 + "=" + tuple.t3 + "'. Reason: " + tuple.t1 + " does not allow any operation at node level");
      });
    });
  }

  @Test
  public void test_valueOf_with_invalid_string() {
    assertThat(() -> Configuration.valueOf((String) null), is(throwing(instanceOf(NullPointerException.class))));

    // test for each supported namespace separation . and : (i.e. stripe.1.node-backup-dir=foo/bar and stripe.1:node-backup-dir=foo/bar)
    Stream.of(".", ":").forEach(ns -> {

      // missing setting name
      rejectInput("", "Invalid input: ''. Reason: valid setting name not found");
      rejectInput("stripe.1", "Invalid input: 'stripe.1'. Reason: valid setting name not found");
      rejectInput("stripe.1.node.1", "Invalid input: 'stripe.1.node.1'. Reason: valid setting name not found");

      // bad setting name
      rejectInput("foo", "Invalid input: 'foo'. Reason: Invalid setting name: 'foo'");
      rejectInput("stripe.1" + ns + "foo", "Invalid input: 'stripe.1" + ns + "foo'. Reason: Invalid setting name: 'foo'");
      rejectInput("stripe.1.node.1" + ns + "foo", "Invalid input: 'stripe.1.node.1" + ns + "foo'. Reason: Invalid setting name: 'foo'");
      rejectInput("foo.stripe.1.node.1" + ns + "foo", "Invalid input: 'foo.stripe.1.node.1" + ns + "foo'. Reason: Invalid setting name: 'foo'");
      rejectInput("stripe.1.foo.node.1" + ns + "foo", "Invalid input: 'stripe.1.foo.node.1" + ns + "foo'. Reason: Invalid setting name: 'foo'");

      // bad ids
      rejectInput("stripe.0" + ns + "node-backup-dir", "Invalid input: 'stripe.0" + ns + "node-backup-dir'. Reason: Expected stripe ID to be greater than 0");
      rejectInput("stripe.-1" + ns + "node-backup-dir", "Invalid input: 'stripe.-1" + ns + "node-backup-dir'");
      rejectInput("stripe.foo" + ns + "node-backup-dir", "Invalid input: 'stripe.foo" + ns + "node-backup-dir'");
      rejectInput("stripe.1.node.0" + ns + "node-backup-dir", "Invalid input: 'stripe.1.node.0" + ns + "node-backup-dir'. Reason: Expected node ID to be greater than 0");
      rejectInput("stripe.1.node.-1" + ns + "node-backup-dir", "Invalid input: 'stripe.1.node.-1" + ns + "node-backup-dir'");
      rejectInput("stripe.1.node.foo" + ns + "node-backup-dir", "Invalid input: 'stripe.1.node.foo" + ns + "node-backup-dir'");

      // bad formats
      rejectInput("node.1.stripe.1" + ns + "node-backup-dir", "Invalid input: 'node.1.stripe.1" + ns + "node-backup-dir'");
      rejectInput("stripe.1.stripe.1" + ns + "node-backup-dir", "Invalid input: 'stripe.1.stripe.1" + ns + "node-backup-dir'");
      rejectInput("stripe.1.node.1.stripe.1" + ns + "node-backup-dir", "Invalid input: 'stripe.1.node.1.stripe.1" + ns + "node-backup-dir'");
      rejectInput("stripe.1.node.1.node.1" + ns + "node-backup-dir", "Invalid input: 'stripe.1.node.1.node.1" + ns + "node-backup-dir'");
      rejectInput("stripe" + ns + "node-backup-dir", "Invalid input: 'stripe" + ns + "node-backup-dir'");

      // bad settings combinations
      rejectInput("node-backup-dir.key", "Invalid input: 'node-backup-dir.key'. Reason: node-backup-dir is not a map and must not have a key");
      rejectInput("stripe.1.node.1.failover-priority", "Invalid input: 'stripe.1.node.1.failover-priority'. Reason: failover-priority does not allow any operation at node level");
    });
  }

  @Test
  public void test_validate() {
    assertThat(
        () -> Configuration.valueOf("failover-priority=availability").validate(GET),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Invalid input: 'failover-priority=availability'. Reason: Operation get must not have a value")))));
    assertThat(
        () -> Configuration.valueOf("offheap-resources=main:1GB").validate(UNSET),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Invalid input: 'offheap-resources=main:1GB'. Reason: Operation unset must not have a value")))));
    assertThat(
        () -> Configuration.valueOf("failover-priority").validate(SET),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Invalid input: 'failover-priority'. Reason: Operation set requires a value")))));
    assertThat(
        () -> Configuration.valueOf("failover-priority").validate(CONFIG),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Invalid input: 'failover-priority'. Reason: Operation config requires a value")))));

    // get allowed for all scopes
    // unset not allowed for all scopes
    // set not allowed for all scopes
    // config allowed for scope node
    Stream.of(
        tuple2(NODE_NAME, "foo"),
        tuple2(NODE_HOSTNAME, "foo"),
        tuple2(NODE_PORT, "9410")
    ).forEach(tuple -> {
      allow(GET, tuple.t1.toString());
      reject(UNSET, tuple.t1.toString(), "Invalid input: '" + tuple.t1 + "'. Reason: " + tuple.t1 + " does not allow operation unset at cluster level");
      reject(SET, tuple.t1 + "=" + tuple.t2, "Invalid input: '" + tuple.t1 + "=" + tuple.t2 + "'. Reason: " + tuple.t1 + " cannot be set at cluster level");
      reject(SET, tuple.t1 + "=", "Invalid input: '" + tuple.t1 + "=" + "'. Reason: " + tuple.t1 + " requires a value");
      reject(CONFIG, tuple.t1 + "=" + tuple.t2, "Invalid input: '" + tuple.t1 + "=" + tuple.t2 + "'. Reason: " + tuple.t1 + " cannot be set at cluster level");
      reject(CONFIG, tuple.t1 + "=", "Invalid input: '" + tuple.t1 + "=" + "'. Reason: " + tuple.t1 + " requires a value");

      allow(GET, "stripe.1." + tuple.t1);
      reject(UNSET, "stripe.1." + tuple.t1, "Invalid input: 'stripe.1." + tuple.t1 + "'. Reason: " + tuple.t1 + " does not allow operation unset at stripe level");
      reject(SET, "stripe.1." + tuple.t1 + "=" + tuple.t2, "Invalid input: 'stripe.1." + tuple.t1 + "=" + tuple.t2 + "'. Reason: " + tuple.t1 + " cannot be set at stripe level");
      reject(SET, "stripe.1." + tuple.t1 + "=", "Invalid input: 'stripe.1." + tuple.t1 + "=" + "'. Reason: " + tuple.t1 + " requires a value");
      reject(CONFIG, "stripe.1." + tuple.t1 + "=" + tuple.t2, "Invalid input: 'stripe.1." + tuple.t1 + "=" + tuple.t2 + "'. Reason: " + tuple.t1 + " cannot be set at stripe level");
      reject(CONFIG, "stripe.1." + tuple.t1 + "=", "Invalid input: 'stripe.1." + tuple.t1 + "=" + "'. Reason: " + tuple.t1 + " requires a value");

      allow(GET, "stripe.1.node.1." + tuple.t1);
      reject(UNSET, "stripe.1.node.1." + tuple.t1, "Invalid input: 'stripe.1.node.1." + tuple.t1 + "'. Reason: " + tuple.t1 + " does not allow operation unset at node level");
      reject(SET, "stripe.1.node.1." + tuple.t1 + "=" + tuple.t2, "Invalid input: 'stripe.1.node.1." + tuple.t1 + "=" + tuple.t2 + "'. Reason: " + tuple.t1 + " does not allow operation set at node level");
      reject(SET, "stripe.1.node.1." + tuple.t1 + "=", "Invalid input: 'stripe.1.node.1." + tuple.t1 + "=" + "'. Reason: " + tuple.t1 + " requires a value");
      allow(CONFIG, "stripe.1.node.1." + tuple.t1 + "=" + tuple.t2);
      reject(CONFIG, "stripe.1.node.1." + tuple.t1 + "=", "Invalid input: 'stripe.1.node.1." + tuple.t1 + "=" + "'. Reason: " + tuple.t1 + " requires a value");
    });

    // get allowed for all scopes
    // unset not allowed for all scopes
    // set allowed for all scopes
    // config allowed for scope node
    Stream.of(
        tuple2(NODE_GROUP_PORT, "9410"),
        tuple2(NODE_BIND_ADDRESS, "0.0.0.0"),
        tuple2(NODE_GROUP_BIND_ADDRESS, "0.0.0.0"),
        tuple2(NODE_METADATA_DIR, "foo/bar"),
        tuple2(NODE_LOG_DIR, "foo/bar")
    ).forEach(tuple -> {
      allow(GET, tuple.t1.toString());
      reject(UNSET, tuple.t1.toString(), "Invalid input: '" + tuple.t1 + "'. Reason: " + tuple.t1 + " does not allow operation unset at cluster level");
      allow(SET, tuple.t1 + "=" + tuple.t2);
      reject(SET, tuple.t1 + "=", "Invalid input: '" + tuple.t1 + "=" + "'. Reason: " + tuple.t1 + " requires a value");
      reject(CONFIG, tuple.t1 + "=" + tuple.t2, "Invalid input: '" + tuple.t1 + "=" + tuple.t2 + "'. Reason: " + tuple.t1 + " does not allow operation config at cluster level");
      reject(CONFIG, tuple.t1 + "=", "Invalid input: '" + tuple.t1 + "=" + "'. Reason: " + tuple.t1 + " requires a value");

      allow(GET, "stripe.1." + tuple.t1);
      reject(UNSET, "stripe.1." + tuple.t1, "Invalid input: 'stripe.1." + tuple.t1 + "'. Reason: " + tuple.t1 + " does not allow operation unset at stripe level");
      allow(SET, "stripe.1." + tuple.t1 + "=" + tuple.t2);
      reject(SET, "stripe.1." + tuple.t1 + "=", "Invalid input: 'stripe.1." + tuple.t1 + "=" + "'. Reason: " + tuple.t1 + " requires a value");
      reject(CONFIG, "stripe.1." + tuple.t1 + "=" + tuple.t2, "Invalid input: 'stripe.1." + tuple.t1 + "=" + tuple.t2 + "'. Reason: " + tuple.t1 + " does not allow operation config at stripe level");
      reject(CONFIG, "stripe.1." + tuple.t1 + "=", "Invalid input: 'stripe.1." + tuple.t1 + "=" + "'. Reason: " + tuple.t1 + " requires a value");

      allow(GET, "stripe.1.node.1." + tuple.t1);
      reject(UNSET, "stripe.1.node.1." + tuple.t1, "Invalid input: 'stripe.1.node.1." + tuple.t1 + "'. Reason: " + tuple.t1 + " does not allow operation unset at node level");
      allow(SET, "stripe.1.node.1." + tuple.t1 + "=" + tuple.t2);
      reject(SET, "stripe.1.node.1." + tuple.t1 + "=", "Invalid input: 'stripe.1.node.1." + tuple.t1 + "=" + "'. Reason: " + tuple.t1 + " requires a value");
      allow(CONFIG, "stripe.1.node.1." + tuple.t1 + "=" + tuple.t2);
      reject(CONFIG, "stripe.1.node.1." + tuple.t1 + "=", "Invalid input: 'stripe.1.node.1." + tuple.t1 + "=" + "'. Reason: " + tuple.t1 + " requires a value");
    });

    // get allowed for all scopes
    // unset allowed for all scopes
    // set allowed for all scopes
    // config allowed for scope node
    Stream.of(
        tuple2(NODE_BACKUP_DIR, "foo/bar"),
        tuple2(SECURITY_DIR, "foo/bar"),
        tuple2(SECURITY_AUDIT_LOG_DIR, "foo/bar")
    ).forEach(tuple -> {
      allow(GET, tuple.t1.toString());
      allow(UNSET, tuple.t1.toString());
      allow(SET, tuple.t1 + "=" + tuple.t2);
      reject(SET, tuple.t1 + "=", "Invalid input: '" + tuple.t1 + "=" + "'. Reason: Operation set requires a value");
      reject(CONFIG, tuple.t1 + "=" + tuple.t2, "Invalid input: '" + tuple.t1 + "=" + tuple.t2 + "'. Reason: " + tuple.t1 + " does not allow operation config at cluster level");
      reject(CONFIG, tuple.t1 + "=", "Invalid input: '" + tuple.t1 + "=" + "'. Reason: " + tuple.t1 + " does not allow operation config at cluster level");

      allow(GET, "stripe.1." + tuple.t1);
      allow(UNSET, "stripe.1." + tuple.t1);
      allow(SET, "stripe.1." + tuple.t1 + "=" + tuple.t2);
      reject(SET, "stripe.1." + tuple.t1 + "=", "Invalid input: 'stripe.1." + tuple.t1 + "=" + "'. Reason: Operation set requires a value");
      reject(CONFIG, "stripe.1." + tuple.t1 + "=" + tuple.t2, "Invalid input: 'stripe.1." + tuple.t1 + "=" + tuple.t2 + "'. Reason: " + tuple.t1 + " does not allow operation config at stripe level");
      reject(CONFIG, "stripe.1." + tuple.t1 + "=", "Invalid input: 'stripe.1." + tuple.t1 + "=" + "'. Reason: " + tuple.t1 + " does not allow operation config at stripe level");

      allow(GET, "stripe.1.node.1." + tuple.t1);
      allow(UNSET, "stripe.1.node.1." + tuple.t1);
      allow(SET, "stripe.1.node.1." + tuple.t1 + "=" + tuple.t2);
      reject(SET, "stripe.1.node.1." + tuple.t1 + "=", "Invalid input: 'stripe.1.node.1." + tuple.t1 + "=" + "'. Reason: Operation set requires a value");
      allow(CONFIG, "stripe.1.node.1." + tuple.t1 + "=" + tuple.t2);
      allow(CONFIG, "stripe.1.node.1." + tuple.t1 + "=");
    });

    // get allowed for scope cluster
    // unset not allowed for all scopes
    // set allowed for scope cluster
    // config allowed for scope cluster
    Stream.of(
        tuple2(CLIENT_RECONNECT_WINDOW, "20s"),
        tuple2(FAILOVER_PRIORITY, "availability"),
        tuple2(CLIENT_LEASE_DURATION, "20s"),
        tuple2(SECURITY_SSL_TLS, "true"),
        tuple2(SECURITY_WHITELIST, "true")
    ).forEach(tuple -> {
      allow(GET, tuple.t1.toString());
      reject(UNSET, tuple.t1.toString(), "Invalid input: '" + tuple.t1 + "'. Reason: " + tuple.t1 + " does not allow operation unset at cluster level");
      allow(SET, tuple.t1 + "=" + tuple.t2);
      reject(SET, tuple.t1 + "=", "Invalid input: '" + tuple.t1 + "=" + "'. Reason: " + tuple.t1 + " requires a value");
      allow(CONFIG, tuple.t1 + "=" + tuple.t2);
      reject(CONFIG, tuple.t1 + "=", "Invalid input: '" + tuple.t1 + "=" + "'. Reason: " + tuple.t1 + " requires a value");

      reject(GET, "stripe.1." + tuple.t1, "Invalid input: 'stripe.1." + tuple.t1 + "'. Reason: " + tuple.t1 + " does not allow any operation at stripe level");
      reject(UNSET, "stripe.1." + tuple.t1, "Invalid input: 'stripe.1." + tuple.t1 + "'. Reason: " + tuple.t1 + " does not allow any operation at stripe level");
      reject(SET, "stripe.1." + tuple.t1 + "=" + tuple.t2, "Invalid input: 'stripe.1." + tuple.t1 + "=" + tuple.t2 + "'. Reason: " + tuple.t1 + " does not allow any operation at stripe level");
      reject(SET, "stripe.1." + tuple.t1 + "=", "Invalid input: 'stripe.1." + tuple.t1 + "=" + "'. Reason: " + tuple.t1 + " does not allow any operation at stripe level");
      reject(CONFIG, "stripe.1." + tuple.t1 + "=" + tuple.t2, "Invalid input: 'stripe.1." + tuple.t1 + "=" + tuple.t2 + "'. Reason: " + tuple.t1 + " does not allow any operation at stripe level");
      reject(CONFIG, "stripe.1." + tuple.t1 + "=", "Invalid input: 'stripe.1." + tuple.t1 + "=" + "'. Reason: " + tuple.t1 + " does not allow any operation at stripe level");

      reject(GET, "stripe.1.node.1." + tuple.t1, "Invalid input: 'stripe.1.node.1." + tuple.t1 + "'. Reason: " + tuple.t1 + " does not allow any operation at node level");
      reject(UNSET, "stripe.1.node.1." + tuple.t1, "Invalid input: 'stripe.1.node.1." + tuple.t1 + "'. Reason: " + tuple.t1 + " does not allow any operation at node level");
      reject(SET, "stripe.1.node.1." + tuple.t1 + "=" + tuple.t2, "Invalid input: 'stripe.1.node.1." + tuple.t1 + "=" + tuple.t2 + "'. Reason: " + tuple.t1 + " does not allow any operation at node level");
      reject(SET, "stripe.1.node.1." + tuple.t1 + "=", "Invalid input: 'stripe.1.node.1." + tuple.t1 + "=" + "'. Reason: " + tuple.t1 + " does not allow any operation at node level");
      reject(CONFIG, "stripe.1.node.1." + tuple.t1 + "=" + tuple.t2, "Invalid input: 'stripe.1.node.1." + tuple.t1 + "=" + tuple.t2 + "'. Reason: " + tuple.t1 + " does not allow any operation at node level");
      reject(CONFIG, "stripe.1.node.1." + tuple.t1 + "=", "Invalid input: 'stripe.1.node.1." + tuple.t1 + "=" + "'. Reason: " + tuple.t1 + " does not allow any operation at node level");
    });

    // get allowed for scope cluster
    // unset not allowed for all scopes
    // set allowed for scope cluster
    // config allowed for scope cluster
    Stream.of(
        tuple2(CLUSTER_NAME, "foo")
    ).forEach(tuple -> {
      allow(GET, tuple.t1.toString());
      reject(UNSET, tuple.t1.toString(), "Invalid input: '" + tuple.t1 + "'. Reason: " + tuple.t1 + " does not allow operation unset at cluster level");
      allow(SET, tuple.t1 + "=" + tuple.t2);
      reject(SET, tuple.t1 + "=", "Invalid input: '" + tuple.t1 + "=" + "'. Reason: Operation set requires a value");
      allow(CONFIG, tuple.t1 + "=" + tuple.t2);
      allow(CONFIG, tuple.t1 + "=");

      reject(GET, "stripe.1." + tuple.t1, "Invalid input: 'stripe.1." + tuple.t1 + "'. Reason: " + tuple.t1 + " does not allow any operation at stripe level");
      reject(UNSET, "stripe.1." + tuple.t1, "Invalid input: 'stripe.1." + tuple.t1 + "'. Reason: " + tuple.t1 + " does not allow any operation at stripe level");
      reject(SET, "stripe.1." + tuple.t1 + "=" + tuple.t2, "Invalid input: 'stripe.1." + tuple.t1 + "=" + tuple.t2 + "'. Reason: " + tuple.t1 + " does not allow any operation at stripe level");
      reject(SET, "stripe.1." + tuple.t1 + "=", "Invalid input: 'stripe.1." + tuple.t1 + "=" + "'. Reason: " + tuple.t1 + " does not allow any operation at stripe level");
      reject(CONFIG, "stripe.1." + tuple.t1 + "=" + tuple.t2, "Invalid input: 'stripe.1." + tuple.t1 + "=" + tuple.t2 + "'. Reason: " + tuple.t1 + " does not allow any operation at stripe level");
      reject(CONFIG, "stripe.1." + tuple.t1 + "=", "Invalid input: 'stripe.1." + tuple.t1 + "=" + "'. Reason: " + tuple.t1 + " does not allow any operation at stripe level");

      reject(GET, "stripe.1.node.1." + tuple.t1, "Invalid input: 'stripe.1.node.1." + tuple.t1 + "'. Reason: " + tuple.t1 + " does not allow any operation at node level");
      reject(UNSET, "stripe.1.node.1." + tuple.t1, "Invalid input: 'stripe.1.node.1." + tuple.t1 + "'. Reason: " + tuple.t1 + " does not allow any operation at node level");
      reject(SET, "stripe.1.node.1." + tuple.t1 + "=" + tuple.t2, "Invalid input: 'stripe.1.node.1." + tuple.t1 + "=" + tuple.t2 + "'. Reason: " + tuple.t1 + " does not allow any operation at node level");
      reject(SET, "stripe.1.node.1." + tuple.t1 + "=", "Invalid input: 'stripe.1.node.1." + tuple.t1 + "=" + "'. Reason: " + tuple.t1 + " does not allow any operation at node level");
      reject(CONFIG, "stripe.1.node.1." + tuple.t1 + "=" + tuple.t2, "Invalid input: 'stripe.1.node.1." + tuple.t1 + "=" + tuple.t2 + "'. Reason: " + tuple.t1 + " does not allow any operation at node level");
      reject(CONFIG, "stripe.1.node.1." + tuple.t1 + "=", "Invalid input: 'stripe.1.node.1." + tuple.t1 + "=" + "'. Reason: " + tuple.t1 + " does not allow any operation at node level");
    });

    // get allowed for scope cluster
    // unset allowed for all scopes
    // set allowed for scope cluster
    // config allowed for scope cluster
    Stream.of(
        tuple2(SECURITY_AUTHC, "certificate")
    ).forEach(tuple -> {
      allow(GET, tuple.t1.toString());
      allow(UNSET, tuple.t1.toString());
      allow(SET, tuple.t1 + "=" + tuple.t2);
      reject(SET, tuple.t1 + "=", "Invalid input: '" + tuple.t1 + "=" + "'. Reason: Operation set requires a value");
      allow(CONFIG, tuple.t1 + "=" + tuple.t2);
      allow(CONFIG, tuple.t1 + "=");

      reject(GET, "stripe.1." + tuple.t1, "Invalid input: 'stripe.1." + tuple.t1 + "'. Reason: " + tuple.t1 + " does not allow any operation at stripe level");
      reject(UNSET, "stripe.1." + tuple.t1, "Invalid input: 'stripe.1." + tuple.t1 + "'. Reason: " + tuple.t1 + " does not allow any operation at stripe level");
      reject(SET, "stripe.1." + tuple.t1 + "=" + tuple.t2, "Invalid input: 'stripe.1." + tuple.t1 + "=" + tuple.t2 + "'. Reason: " + tuple.t1 + " does not allow any operation at stripe level");
      reject(SET, "stripe.1." + tuple.t1 + "=", "Invalid input: 'stripe.1." + tuple.t1 + "=" + "'. Reason: " + tuple.t1 + " does not allow any operation at stripe level");
      reject(CONFIG, "stripe.1." + tuple.t1 + "=" + tuple.t2, "Invalid input: 'stripe.1." + tuple.t1 + "=" + tuple.t2 + "'. Reason: " + tuple.t1 + " does not allow any operation at stripe level");
      reject(CONFIG, "stripe.1." + tuple.t1 + "=", "Invalid input: 'stripe.1." + tuple.t1 + "=" + "'. Reason: " + tuple.t1 + " does not allow any operation at stripe level");

      reject(GET, "stripe.1.node.1." + tuple.t1, "Invalid input: 'stripe.1.node.1." + tuple.t1 + "'. Reason: " + tuple.t1 + " does not allow any operation at node level");
      reject(UNSET, "stripe.1.node.1." + tuple.t1, "Invalid input: 'stripe.1.node.1." + tuple.t1 + "'. Reason: " + tuple.t1 + " does not allow any operation at node level");
      reject(SET, "stripe.1.node.1." + tuple.t1 + "=" + tuple.t2, "Invalid input: 'stripe.1.node.1." + tuple.t1 + "=" + tuple.t2 + "'. Reason: " + tuple.t1 + " does not allow any operation at node level");
      reject(SET, "stripe.1.node.1." + tuple.t1 + "=", "Invalid input: 'stripe.1.node.1." + tuple.t1 + "=" + "'. Reason: " + tuple.t1 + " does not allow any operation at node level");
      reject(CONFIG, "stripe.1.node.1." + tuple.t1 + "=" + tuple.t2, "Invalid input: 'stripe.1.node.1." + tuple.t1 + "=" + tuple.t2 + "'. Reason: " + tuple.t1 + " does not allow any operation at node level");
      reject(CONFIG, "stripe.1.node.1." + tuple.t1 + "=", "Invalid input: 'stripe.1.node.1." + tuple.t1 + "=" + "'. Reason: " + tuple.t1 + " does not allow any operation at node level");
    });

    // get not allowed for all scopes
    // unset not allowed for all scopes
    // set not allowed for all scopes
    // config not allowed for all scopes
    Stream.of(
        tuple2(NODE_REPOSITORY_DIR, "foo/bar")
    ).forEach(tuple -> {
      reject(GET, tuple.t1.toString(), "Invalid input: '" + tuple.t1 + "'. Reason: " + tuple.t1 + " does not allow any operation at cluster level");
      reject(UNSET, tuple.t1.toString(), "Invalid input: '" + tuple.t1 + "'. Reason: " + tuple.t1 + " does not allow any operation at cluster level");
      reject(SET, tuple.t1 + "=" + tuple.t2, "Invalid input: '" + tuple.t1 + "=" + tuple.t2 + "'. Reason: " + tuple.t1 + " does not allow any operation at cluster level");
      reject(SET, tuple.t1 + "=", "Invalid input: '" + tuple.t1 + "=" + "'. Reason: " + tuple.t1 + " does not allow any operation at cluster level");
      reject(CONFIG, tuple.t1 + "=" + tuple.t2, "Invalid input: '" + tuple.t1 + "=" + tuple.t2 + "'. Reason: " + tuple.t1 + " does not allow any operation at cluster level");
      reject(CONFIG, tuple.t1 + "=", "Invalid input: '" + tuple.t1 + "=" + "'. Reason: " + tuple.t1 + " does not allow any operation at cluster level");

      reject(GET, "stripe.1." + tuple.t1, "Invalid input: 'stripe.1." + tuple.t1 + "'. Reason: " + tuple.t1 + " does not allow any operation at stripe level");
      reject(UNSET, "stripe.1." + tuple.t1, "Invalid input: 'stripe.1." + tuple.t1 + "'. Reason: " + tuple.t1 + " does not allow any operation at stripe level");
      reject(SET, "stripe.1." + tuple.t1 + "=" + tuple.t2, "Invalid input: 'stripe.1." + tuple.t1 + "=" + tuple.t2 + "'. Reason: " + tuple.t1 + " does not allow any operation at stripe level");
      reject(SET, "stripe.1." + tuple.t1 + "=", "Invalid input: 'stripe.1." + tuple.t1 + "=" + "'. Reason: " + tuple.t1 + " does not allow any operation at stripe level");
      reject(CONFIG, "stripe.1." + tuple.t1 + "=" + tuple.t2, "Invalid input: 'stripe.1." + tuple.t1 + "=" + tuple.t2 + "'. Reason: " + tuple.t1 + " does not allow any operation at stripe level");
      reject(CONFIG, "stripe.1." + tuple.t1 + "=", "Invalid input: 'stripe.1." + tuple.t1 + "=" + "'. Reason: " + tuple.t1 + " does not allow any operation at stripe level");

      reject(GET, "stripe.1.node.1." + tuple.t1, "Invalid input: 'stripe.1.node.1." + tuple.t1 + "'. Reason: " + tuple.t1 + " does not allow any operation at node level");
      reject(UNSET, "stripe.1.node.1." + tuple.t1, "Invalid input: 'stripe.1.node.1." + tuple.t1 + "'. Reason: " + tuple.t1 + " does not allow any operation at node level");
      reject(SET, "stripe.1.node.1." + tuple.t1 + "=" + tuple.t2, "Invalid input: 'stripe.1.node.1." + tuple.t1 + "=" + tuple.t2 + "'. Reason: " + tuple.t1 + " does not allow any operation at node level");
      reject(SET, "stripe.1.node.1." + tuple.t1 + "=", "Invalid input: 'stripe.1.node.1." + tuple.t1 + "=" + "'. Reason: " + tuple.t1 + " does not allow any operation at node level");
      reject(CONFIG, "stripe.1.node.1." + tuple.t1 + "=" + tuple.t2, "Invalid input: 'stripe.1.node.1." + tuple.t1 + "=" + tuple.t2 + "'. Reason: " + tuple.t1 + " does not allow any operation at node level");
      reject(CONFIG, "stripe.1.node.1." + tuple.t1 + "=", "Invalid input: 'stripe.1.node.1." + tuple.t1 + "=" + "'. Reason: " + tuple.t1 + " does not allow any operation at node level");
    });

    // get not allowed for all scopes
    // unset not allowed for all scopes
    // set allowed for scope cluster
    // config not allowed for all scopes
    Stream.of(
        tuple2(LICENSE_FILE, "/path/to/license.xml")
    ).forEach(tuple -> {
      reject(GET, tuple.t1.toString(), "Invalid input: '" + tuple.t1 + "'. Reason: " + tuple.t1 + " cannot be read or cleared");
      reject(UNSET, tuple.t1.toString(), "Invalid input: '" + tuple.t1 + "'. Reason: " + tuple.t1 + " cannot be read or cleared");
      allow(SET, tuple.t1 + "=" + tuple.t2);
      reject(SET, tuple.t1 + "=", "Invalid input: '" + tuple.t1 + "=" + "'. Reason: " + tuple.t1 + " requires a value");
      reject(CONFIG, tuple.t1 + "=" + tuple.t2, "Invalid input: '" + tuple.t1 + "=" + tuple.t2 + "'. Reason: " + tuple.t1 + " does not allow operation config at cluster level");
      reject(CONFIG, tuple.t1 + "=", "Invalid input: '" + tuple.t1 + "=" + "'. Reason: " + tuple.t1 + " requires a value");

      reject(GET, "stripe.1." + tuple.t1, "Invalid input: 'stripe.1." + tuple.t1 + "'. Reason: " + tuple.t1 + " does not allow any operation at stripe level");
      reject(UNSET, "stripe.1." + tuple.t1, "Invalid input: 'stripe.1." + tuple.t1 + "'. Reason: " + tuple.t1 + " does not allow any operation at stripe level");
      reject(SET, "stripe.1." + tuple.t1 + "=" + tuple.t2, "Invalid input: 'stripe.1." + tuple.t1 + "=" + tuple.t2 + "'. Reason: " + tuple.t1 + " does not allow any operation at stripe level");
      reject(SET, "stripe.1." + tuple.t1 + "=", "Invalid input: 'stripe.1." + tuple.t1 + "=" + "'. Reason: " + tuple.t1 + " does not allow any operation at stripe level");
      reject(CONFIG, "stripe.1." + tuple.t1 + "=" + tuple.t2, "Invalid input: 'stripe.1." + tuple.t1 + "=" + tuple.t2 + "'. Reason: " + tuple.t1 + " does not allow any operation at stripe level");
      reject(CONFIG, "stripe.1." + tuple.t1 + "=", "Invalid input: 'stripe.1." + tuple.t1 + "=" + "'. Reason: " + tuple.t1 + " does not allow any operation at stripe level");

      reject(GET, "stripe.1.node.1." + tuple.t1, "Invalid input: 'stripe.1.node.1." + tuple.t1 + "'. Reason: " + tuple.t1 + " does not allow any operation at node level");
      reject(UNSET, "stripe.1.node.1." + tuple.t1, "Invalid input: 'stripe.1.node.1." + tuple.t1 + "'. Reason: " + tuple.t1 + " does not allow any operation at node level");
      reject(SET, "stripe.1.node.1." + tuple.t1 + "=" + tuple.t2, "Invalid input: 'stripe.1.node.1." + tuple.t1 + "=" + tuple.t2 + "'. Reason: " + tuple.t1 + " does not allow any operation at node level");
      reject(SET, "stripe.1.node.1." + tuple.t1 + "=", "Invalid input: 'stripe.1.node.1." + tuple.t1 + "=" + "'. Reason: " + tuple.t1 + " does not allow any operation at node level");
      reject(CONFIG, "stripe.1.node.1." + tuple.t1 + "=" + tuple.t2, "Invalid input: 'stripe.1.node.1." + tuple.t1 + "=" + tuple.t2 + "'. Reason: " + tuple.t1 + " does not allow any operation at node level");
      reject(CONFIG, "stripe.1.node.1." + tuple.t1 + "=", "Invalid input: 'stripe.1.node.1." + tuple.t1 + "=" + "'. Reason: " + tuple.t1 + " does not allow any operation at node level");
    });

    // get allowed for all scopes
    // unset allowed for all scopes
    // set allowed for all scopes
    // config allowed for scope node
    Stream.of(
        tuple3(TC_PROPERTIES, "a.b.c", "d.e.f"),
        tuple3(DATA_DIRS, "a.b.c", "foo/bar")
    ).forEach(tuple -> {
      allow(GET, tuple.t1.toString());
      allow(UNSET, tuple.t1.toString());
      reject(SET, tuple.t1 + "=", "Invalid input: '" + tuple.t1 + "=" + "'. Reason: Operation set requires a value");
      allow(SET, tuple.t1 + "=" + tuple.t2 + ":" + tuple.t3);
      reject(CONFIG, tuple.t1 + "=", "Invalid input: '" + tuple.t1 + "=" + "'. Reason: " + tuple.t1 + " does not allow operation config at cluster level");
      reject(CONFIG, tuple.t1 + "=" + tuple.t2 + ":" + tuple.t3, "Invalid input: '" + tuple.t1 + "=" + tuple.t2 + ":" + tuple.t3 + "'. Reason: " + tuple.t1 + " does not allow operation config at cluster level");

      allow(GET, tuple.t1 + "." + tuple.t2);
      allow(UNSET, tuple.t1 + "." + tuple.t2);
      reject(SET, tuple.t1 + "." + tuple.t2 + "=", "Invalid input: '" + tuple.t1 + "." + tuple.t2 + "=" + "'. Reason: Operation set requires a value");
      allow(SET, tuple.t1 + "." + tuple.t2 + "=" + tuple.t3);
      reject(CONFIG, tuple.t1 + "." + tuple.t2 + "=", "Invalid input: '" + tuple.t1 + "." + tuple.t2 + "=" + "'. Reason: " + tuple.t1 + " does not allow operation config at cluster level");
      reject(CONFIG, tuple.t1 + "." + tuple.t2 + "=" + tuple.t3, "Invalid input: '" + tuple.t1 + "." + tuple.t2 + "=" + tuple.t3 + "'. Reason: " + tuple.t1 + " does not allow operation config at cluster level");

      allow(GET, "stripe.1." + tuple.t1);
      allow(UNSET, "stripe.1." + tuple.t1);
      reject(SET, "stripe.1." + tuple.t1 + "=", "Invalid input: 'stripe.1." + tuple.t1 + "=" + "'. Reason: Operation set requires a value");
      allow(SET, "stripe.1." + tuple.t1 + "=" + tuple.t2 + ":" + tuple.t3);
      reject(CONFIG, "stripe.1." + tuple.t1 + "=", "Invalid input: 'stripe.1." + tuple.t1 + "=" + "'. Reason: " + tuple.t1 + " does not allow operation config at stripe level");
      reject(CONFIG, "stripe.1." + tuple.t1 + "=" + tuple.t2 + ":" + tuple.t3, "Invalid input: 'stripe.1." + tuple.t1 + "=" + tuple.t2 + ":" + tuple.t3 + "'. Reason: " + tuple.t1 + " does not allow operation config at stripe level");

      allow(GET, "stripe.1." + tuple.t1 + "." + tuple.t2);
      allow(UNSET, "stripe.1." + tuple.t1 + "." + tuple.t2);
      reject(SET, "stripe.1." + tuple.t1 + "." + tuple.t2 + "=", "Invalid input: 'stripe.1." + tuple.t1 + "." + tuple.t2 + "=" + "'. Reason: Operation set requires a value");
      allow(SET, "stripe.1." + tuple.t1 + "." + tuple.t2 + "=" + tuple.t3);
      reject(CONFIG, "stripe.1." + tuple.t1 + "." + tuple.t2 + "=", "Invalid input: 'stripe.1." + tuple.t1 + "." + tuple.t2 + "=" + "'. Reason: " + tuple.t1 + " does not allow operation config at stripe level");
      reject(CONFIG, "stripe.1." + tuple.t1 + "." + tuple.t2 + "=" + tuple.t3, "Invalid input: 'stripe.1." + tuple.t1 + "." + tuple.t2 + "=" + tuple.t3 + "'. Reason: " + tuple.t1 + " does not allow operation config at stripe level");

      allow(GET, "stripe.1.node.1." + tuple.t1);
      allow(UNSET, "stripe.1.node.1." + tuple.t1);
      reject(SET, "stripe.1.node.1." + tuple.t1 + "=", "Invalid input: 'stripe.1.node.1." + tuple.t1 + "=" + "'. Reason: Operation set requires a value");
      allow(SET, "stripe.1.node.1." + tuple.t1 + "=" + tuple.t2 + ":" + tuple.t3);
      allow(CONFIG, "stripe.1.node.1." + tuple.t1 + "=");
      allow(CONFIG, "stripe.1.node.1." + tuple.t1 + "=" + tuple.t2 + ":" + tuple.t3);

      allow(GET, "stripe.1.node.1." + tuple.t1 + "." + tuple.t2);
      allow(UNSET, "stripe.1.node.1." + tuple.t1 + "." + tuple.t2);
      reject(SET, "stripe.1.node.1." + tuple.t1 + "." + tuple.t2 + "=", "Invalid input: 'stripe.1.node.1." + tuple.t1 + "." + tuple.t2 + "=" + "'. Reason: Operation set requires a value");
      allow(SET, "stripe.1.node.1." + tuple.t1 + "." + tuple.t2 + "=" + tuple.t3);
      allow(CONFIG, "stripe.1.node.1." + tuple.t1 + "." + tuple.t2 + "=");
      allow(CONFIG, "stripe.1.node.1." + tuple.t1 + "." + tuple.t2 + "=" + tuple.t3);
    });

    // get allowed for scope cluster
    // unset allowed for scope cluster
    // set allowed for scope cluster
    // config allowed for scope cluster
    Stream.of(
        tuple3(OFFHEAP_RESOURCES, "a.b.c", "1GB")
    ).forEach(tuple -> {
      allow(GET, tuple.t1.toString());
      allow(UNSET, tuple.t1.toString());
      reject(SET, tuple.t1 + "=", "Invalid input: '" + tuple.t1 + "=" + "'. Reason: Operation set requires a value");
      allow(SET, tuple.t1 + "=" + tuple.t2 + ":" + tuple.t3);
      allow(CONFIG, tuple.t1 + "=");
      allow(CONFIG, tuple.t1 + "=" + tuple.t2 + ":" + tuple.t3);

      allow(GET, tuple.t1 + "." + tuple.t2);
      allow(UNSET, tuple.t1 + "." + tuple.t2);
      reject(SET, tuple.t1 + "." + tuple.t2 + "=", "Invalid input: '" + tuple.t1 + "." + tuple.t2 + "=" + "'. Reason: Operation set requires a value");
      allow(SET, tuple.t1 + "." + tuple.t2 + "=" + tuple.t3);
      allow(CONFIG, tuple.t1 + "." + tuple.t2 + "=");
      allow(CONFIG, tuple.t1 + "." + tuple.t2 + "=" + tuple.t3);

      reject(GET, "stripe.1." + tuple.t1, "Invalid input: 'stripe.1." + tuple.t1 + "'. Reason: " + tuple.t1 + " does not allow any operation at stripe level");
      reject(UNSET, "stripe.1." + tuple.t1, "Invalid input: 'stripe.1." + tuple.t1 + "'. Reason: " + tuple.t1 + " does not allow any operation at stripe level");
      reject(SET, "stripe.1." + tuple.t1 + "=", "Invalid input: 'stripe.1." + tuple.t1 + "='. Reason: " + tuple.t1 + " does not allow any operation at stripe level");
      reject(SET, "stripe.1." + tuple.t1 + "=" + tuple.t2 + ":" + tuple.t3, "Invalid input: 'stripe.1." + tuple.t1 + "=" + tuple.t2 + ":" + tuple.t3 + "'. Reason: " + tuple.t1 + " does not allow any operation at stripe level");
      reject(CONFIG, "stripe.1." + tuple.t1 + "=", "Invalid input: 'stripe.1." + tuple.t1 + "=" + "'. Reason: " + tuple.t1 + " does not allow any operation at stripe level");
      reject(CONFIG, "stripe.1." + tuple.t1 + "=" + tuple.t2 + ":" + tuple.t3, "Invalid input: 'stripe.1." + tuple.t1 + "=" + tuple.t2 + ":" + tuple.t3 + "'. Reason: " + tuple.t1 + " does not allow any operation at stripe level");

      reject(GET, "stripe.1." + tuple.t1 + "." + tuple.t2, "Invalid input: 'stripe.1." + tuple.t1 + "." + tuple.t2 + "'. Reason: " + tuple.t1 + " does not allow any operation at stripe level");
      reject(UNSET, "stripe.1." + tuple.t1 + "." + tuple.t2, "Invalid input: 'stripe.1." + tuple.t1 + "." + tuple.t2 + "'. Reason: " + tuple.t1 + " does not allow any operation at stripe level");
      reject(SET, "stripe.1." + tuple.t1 + "." + tuple.t2 + "=", "Invalid input: 'stripe.1." + tuple.t1 + "." + tuple.t2 + "='. Reason: " + tuple.t1 + " does not allow any operation at stripe level");
      reject(SET, "stripe.1." + tuple.t1 + "." + tuple.t2 + "=" + tuple.t3, "Invalid input: 'stripe.1." + tuple.t1 + "." + tuple.t2 + "=" + tuple.t3 + "'. Reason: " + tuple.t1 + " does not allow any operation at stripe level");
      reject(CONFIG, "stripe.1." + tuple.t1 + "." + tuple.t2 + "=", "Invalid input: 'stripe.1." + tuple.t1 + "." + tuple.t2 + "=" + "'. Reason: " + tuple.t1 + " does not allow any operation at stripe level");
      reject(CONFIG, "stripe.1." + tuple.t1 + "." + tuple.t2 + "=" + tuple.t3, "Invalid input: 'stripe.1." + tuple.t1 + "." + tuple.t2 + "=" + tuple.t3 + "'. Reason: " + tuple.t1 + " does not allow any operation at stripe level");

      reject(GET, "stripe.1.node.1." + tuple.t1, "Invalid input: 'stripe.1.node.1." + tuple.t1 + "'. Reason: " + tuple.t1 + " does not allow any operation at node level");
      reject(UNSET, "stripe.1.node.1." + tuple.t1, "Invalid input: 'stripe.1.node.1." + tuple.t1 + "'. Reason: " + tuple.t1 + " does not allow any operation at node level");
      reject(SET, "stripe.1.node.1." + tuple.t1 + "=", "Invalid input: 'stripe.1.node.1." + tuple.t1 + "='. Reason: " + tuple.t1 + " does not allow any operation at node level");
      reject(SET, "stripe.1.node.1." + tuple.t1 + "=" + tuple.t2 + ":" + tuple.t3, "Invalid input: 'stripe.1.node.1." + tuple.t1 + "=" + tuple.t2 + ":" + tuple.t3 + "'. Reason: " + tuple.t1 + " does not allow any operation at node level");
      reject(CONFIG, "stripe.1.node.1." + tuple.t1 + "=", "Invalid input: 'stripe.1.node.1." + tuple.t1 + "=" + "'. Reason: " + tuple.t1 + " does not allow any operation at node level");
      reject(CONFIG, "stripe.1.node.1." + tuple.t1 + "=" + tuple.t2 + ":" + tuple.t3, "Invalid input: 'stripe.1.node.1." + tuple.t1 + "=" + tuple.t2 + ":" + tuple.t3 + "'. Reason: " + tuple.t1 + " does not allow any operation at node level");

      reject(GET, "stripe.1.node.1." + tuple.t1 + "." + tuple.t2, "Invalid input: 'stripe.1.node.1." + tuple.t1 + "." + tuple.t2 + "'. Reason: " + tuple.t1 + " does not allow any operation at node level");
      reject(UNSET, "stripe.1.node.1." + tuple.t1 + "." + tuple.t2, "Invalid input: 'stripe.1.node.1." + tuple.t1 + "." + tuple.t2 + "'. Reason: " + tuple.t1 + " does not allow any operation at node level");
      reject(SET, "stripe.1.node.1." + tuple.t1 + "." + tuple.t2 + "=", "Invalid input: 'stripe.1.node.1." + tuple.t1 + "." + tuple.t2 + "='. Reason: " + tuple.t1 + " does not allow any operation at node level");
      reject(SET, "stripe.1.node.1." + tuple.t1 + "." + tuple.t2 + "=" + tuple.t3, "Invalid input: 'stripe.1.node.1." + tuple.t1 + "." + tuple.t2 + "=" + tuple.t3 + "'. Reason: " + tuple.t1 + " does not allow any operation at node level");
      reject(CONFIG, "stripe.1.node.1." + tuple.t1 + "." + tuple.t2 + "=", "Invalid input: 'stripe.1.node.1." + tuple.t1 + "." + tuple.t2 + "=" + "'. Reason: " + tuple.t1 + " does not allow any operation at node level");
      reject(CONFIG, "stripe.1.node.1." + tuple.t1 + "." + tuple.t2 + "=" + tuple.t3, "Invalid input: 'stripe.1.node.1." + tuple.t1 + "." + tuple.t2 + "=" + tuple.t3 + "'. Reason: " + tuple.t1 + " does not allow any operation at node level");
    });
  }

  @Test
  public void test_match() {
    match("offheap-resources=main:1GB,second:2GB", "offheap-resources");
    matchFail("offheap-resources=main:1GB,second:2GB", "offheap-resources.main");
    matchFail("offheap-resources=main:1GB", "offheap-resources.main");

    match("offheap-resources.main=1GB", "offheap-resources.main");
    matchFail("offheap-resources.main=1GB", "offheap-resources");

    match("stripe.1.node.1.data-dirs=main:foo/bar,second:foo/baz",
        "stripe.1.node.1.data-dirs",
        "stripe.1.node.1:data-dirs",
        "stripe.1.data-dirs",
        "stripe.1:data-dirs",
        "data-dirs");

    matchFail("stripe.1.node.1.data-dirs=main:foo/bar,second:foo/baz",
        "stripe.1.node.1.data-dirs.main",
        "stripe.1.node.1:data-dirs.main",
        "stripe.1.data-dirs.main",
        "stripe.1:data-dirs.main",
        "data-dirs.main");

    match("stripe.1.node.1.data-dirs.main=foo/bar",
        "stripe.1.node.1.data-dirs.main",
        "stripe.1.node.1:data-dirs.main",
        "stripe.1.data-dirs.main",
        "stripe.1:data-dirs.main",
        "data-dirs.main");

    matchFail("stripe.1.node.1.data-dirs.main=foo/bar",
        "stripe.1.node.1.data-dirs",
        "stripe.1.node.1:data-dirs",
        "stripe.1.data-dirs",
        "stripe.1:data-dirs",
        "data-dirs");

    matchFail("offheap-resources=main:1GB", "data-dirs");
    matchFail("stripe.1.node.1.data-dirs.main=foo/bar", "stripe.2.node.1.data-dirs");
    matchFail("stripe.1.node.1.data-dirs.main=foo/bar", "stripe.1.node.2.data-dirs");
    matchFail("stripe.1.node.1.data-dirs.main=foo/bar", "stripe.2.data-dirs");
    matchFail("stripe.1.node.1.data-dirs.main=foo/bar", "stripe.1.node.1.data-dirs.second");
    matchFail("stripe.1.node.1.data-dirs=main:foo/bar", "stripe.1.node.1.data-dirs.main");
    matchFail("stripe.1.node.1.data-dirs=main:foo/bar", "stripe.1.node.1.data-dirs.second");
  }

  @Test
  public void test_apply() {
    Cluster cluster = new Cluster(new Stripe(Node.newDefaultNode("node1", "localhost")));

    // cluster wide
    assertThat(cluster.getSingleNode().get().getOffheapResources().size(), is(equalTo(1)));
    assertThat(cluster.getSingleNode().get().getOffheapResources(), hasKey("main"));
    Configuration.valueOf("offheap-resources=second:1GB").apply(cluster);
    assertThat(cluster.getSingleNode().get().getOffheapResources().size(), is(equalTo(2)));
    assertThat(cluster.getSingleNode().get().getOffheapResources(), hasKey("main"));
    assertThat(cluster.getSingleNode().get().getOffheapResources(), hasKey("second"));

    // stripe wide
    assertThat(cluster.getSingleNode().get().getNodeBackupDir(), is(nullValue()));
    Configuration.valueOf("stripe.1:node-backup-dir=foo/bar").apply(cluster);
    assertThat(cluster.getSingleNode().get().getNodeBackupDir(), is(equalTo(Paths.get("foo/bar"))));

    // node level
    assertThat(cluster.getSingleNode().get().getSecurityDir(), is(nullValue()));
    Configuration.valueOf("stripe.1.node.1:security-dir=foo/bar").apply(cluster);
    assertThat(cluster.getSingleNode().get().getSecurityDir(), is(equalTo(Paths.get("foo/bar"))));

    // unset
    Configuration.valueOf("stripe.1.node.1:security-dir=foo/bar").apply(cluster);
    Configuration.valueOf("stripe.1.node.1:security-dir=").apply(cluster);
    assertThat(cluster.getSingleNode().get().getSecurityDir(), is(nullValue()));
    Configuration.valueOf("stripe.1.node.1:security-dir=foo/bar").apply(cluster);
    Configuration.valueOf("stripe.1.node.1:security-dir").apply(cluster);
    assertThat(cluster.getSingleNode().get().getSecurityDir(), is(nullValue()));

    // special cases
    Configuration.valueOf("license-file=foo/bar").apply(cluster);
    assertThat(cluster.getName(), is(nullValue()));
    Configuration.valueOf("cluster-name=foo").apply(cluster);
    assertThat(cluster.getName(), is(equalTo("foo")));

    // bad stripe
    assertThat(
        () -> Configuration.valueOf("stripe.0:node-backup-dir=foo/bar").apply(cluster),
        is(throwing(instanceOf(IllegalArgumentException.class))
            .andMessage(is(equalTo("Invalid input: 'stripe.0:node-backup-dir=foo/bar'. Reason: Expected stripe ID to be greater than 0")))));
    assertThat(
        () -> Configuration.valueOf("stripe.2:node-backup-dir=foo/bar").apply(cluster),
        is(throwing(instanceOf(IllegalArgumentException.class))
            .andMessage(is(equalTo("Invalid input: 'stripe.2:node-backup-dir=foo/bar'. Reason: Invalid stripe ID: 2. Cluster contains: 1 stripe(s)")))));

    // bad node
    assertThat(
        () -> Configuration.valueOf("stripe.1.node.0:node-backup-dir=foo/bar").apply(cluster),
        is(throwing(instanceOf(IllegalArgumentException.class))
            .andMessage(is(equalTo("Invalid input: 'stripe.1.node.0:node-backup-dir=foo/bar'. Reason: Expected node ID to be greater than 0")))));
    assertThat(
        () -> Configuration.valueOf("stripe.1.node.2:node-backup-dir=foo/bar").apply(cluster),
        is(throwing(instanceOf(IllegalArgumentException.class))
            .andMessage(is(equalTo("Invalid input: 'stripe.1.node.2:node-backup-dir=foo/bar'. Reason: Invalid node ID: 2. Stripe ID: 1 contains: 1 node(s)")))));
  }

  @Test
  public void test_isDuplicateOf() {
    assertThat("stripe.1.node.1.node-name=foo", is(duplicating("stripe.1.node.1.node-name=bar")));
    assertThat("stripe.1.node.1.node-name", is(duplicating("stripe.1.node.1.node-name")));
    assertThat("stripe.1.node-backup-dir=foo", is(duplicating("stripe.1:node-backup-dir=bar")));
    assertThat("stripe.1.node-backup-dir", is(duplicating("stripe.1:node-backup-dir")));
    assertThat("node-backup-dir=foo", is(duplicating("node-backup-dir=bar")));
    assertThat("node-backup-dir", is(duplicating("node-backup-dir")));
    assertThat("offheap-resources", is(duplicating("offheap-resources")));
    assertThat("offheap-resources=main:1GB", is(duplicating("offheap-resources=main:1GB")));
    assertThat("offheap-resources.main=1GB", is(duplicating("offheap-resources.main=1GB")));

    assertThat("offheap-resources.main=1GB", is(incompatibleWith("offheap-resources=main:1GB")));
    assertThat("offheap-resources=main:1GB", is(incompatibleWith("offheap-resources.main=1GB")));
    assertThat("offheap-resources", is(incompatibleWith("offheap-resources=main:1GB")));
    assertThat("offheap-resources=main:1GB", is(incompatibleWith("offheap-resources")));

    assertThat("stripe.1.node.1.node-name=foo", is(not(duplicating("stripe.1.node.1.node-backup-dir=bar"))));
    assertThat("stripe.1.node.1.node-name=foo", is(not(duplicating("stripe.1.node.2.node-name=bar"))));
    assertThat("stripe.1.node-backup-dir=foo", is(not(duplicating("stripe.2:node-backup-dir=foo"))));
    assertThat("stripe.1.node-backup-dir=foo", is(not(duplicating("stripe.1.node.2:node-backup-dir=bar"))));
    assertThat("offheap-resources.main", is(not(duplicating("offheap-resources.second"))));
    assertThat("offheap-resources.main=1GB", is(not(duplicating("offheap-resources.second=1GB"))));
  }

  private Matcher<String> duplicating(String value) {
    return new CustomTypeSafeMatcher<String>(" duplicates " + value) {
      @Override
      protected boolean matchesSafely(String item) {
        return Configuration.valueOf(item).duplicates(Configuration.valueOf(value));
      }
    };
  }

  private Matcher<String> incompatibleWith(String value) {
    return new CustomTypeSafeMatcher<String>(" is incompatible with " + value) {
      @Override
      protected boolean matchesSafely(String item) {
        try {
          Configuration.valueOf(item).duplicates(Configuration.valueOf(value));
          return false;
        } catch (IllegalArgumentException e) {
          return e.getMessage().equals("Incompatible or duplicate configurations: " + item + " and " + value);
        }
      }
    };
  }

  private void match(String config, String... userInputs) {
    for (String userInput : userInputs) {
      assertTrue(userInput + " matches " + config, Configuration.valueOf(userInput).matchConfigPropertyKey(config));
    }
  }

  private void matchFail(String config, String... userInputs) {
    for (String userInput : userInputs) {
      assertFalse(userInput + " matches " + config, Configuration.valueOf(userInput).matchConfigPropertyKey(config));
    }
  }

  private void reject(Operation operation, String input, String err) {
    assertThat(() -> Configuration.valueOf(input).validate(operation), is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo(err)))));
  }

  private void allow(Operation operation, String input) {
    Configuration.valueOf(input).validate(operation);
  }

  private void rejectInput(String input, String msg) {
    assertThat(() -> Configuration.valueOf(input), is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo(msg)))));
  }

  private void allowInput(String input, Setting setting, Scope scope, Integer stripeId, Integer nodeId, String key, String value) {
    Configuration configuration = Configuration.valueOf(input);
    assertThat(configuration.getSetting(), is(equalTo(setting)));
    assertThat(configuration.getScope(), is(equalTo(scope)));
    assertThat(configuration.getKey(), is(equalTo(key)));
    assertThat(configuration.getValue(), is(equalTo(value)));
    if (stripeId != null) {
      assertThat(configuration.getStripeId(), is(equalTo(stripeId)));
    }
    if (nodeId != null) {
      assertThat(configuration.getNodeId(), is(equalTo(nodeId)));
    }
  }
}