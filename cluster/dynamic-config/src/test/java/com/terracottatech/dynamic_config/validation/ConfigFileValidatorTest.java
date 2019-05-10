/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.validation;

import com.terracottatech.dynamic_config.exception.MalformedConfigFileException;
import org.junit.Test;

import java.util.Properties;

import static com.terracottatech.dynamic_config.config.CommonOptions.NODE_PORT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;


public class ConfigFileValidatorTest {
  @Test
  public void testInsufficientKeys() {
    Properties properties = new Properties();
    properties.put("one.two.three", "something");

    try {
      ConfigFileValidator.validateProperties(properties, "test-file");
      failBecauseExceptionWasNotThrown(MalformedConfigFileException.class);
    } catch (Exception e) {
      assertThat(e.getClass()).isEqualTo(MalformedConfigFileException.class);
      assertThat(e.getMessage()).startsWith("Invalid line: one.two.three");
    }
  }

  @Test
  public void testUnknownNodeProperty() {
    Properties properties = new Properties();
    properties.put("cluster.stripe.node.blah", "something");

    try {
      ConfigFileValidator.validateProperties(properties, "test-file");
      failBecauseExceptionWasNotThrown(MalformedConfigFileException.class);
    } catch (Exception e) {
      assertThat(e.getClass()).isEqualTo(MalformedConfigFileException.class);
      assertThat(e.getMessage()).startsWith("Unrecognized property: blah");
    }
  }

  @Test
  public void testMismatchingNodeNames() {
    Properties properties = new Properties();
    properties.put("cluster.stripe.node-1.node-name", "node-2");

    try {
      ConfigFileValidator.validateProperties(properties, "test-file");
      failBecauseExceptionWasNotThrown(MalformedConfigFileException.class);
    } catch (Exception e) {
      assertThat(e.getClass()).isEqualTo(MalformedConfigFileException.class);
      assertThat(e.getMessage()).contains("Node name value should match the node name in the property");
    }
  }

  @Test
  public void testMissingNodeProperty() {
    Properties properties = new Properties();
    properties.put("my-cluster.stripe-1.node-1.node-hostname", "node-1.company.internal");
    properties.put("my-cluster.stripe-1.node-1.node-port", "19410");
    properties.put("my-cluster.stripe-1.node-1.node-group-port", "19430");
    properties.put("my-cluster.stripe-1.node-1.node-bind-address", "10.10.10.10");
    properties.put("my-cluster.stripe-1.node-1.node-group-bind-address", "10.10.10.10");
    properties.put("my-cluster.stripe-1.node-1.node-config-dir", "/home/terracotta/config");
    properties.put("my-cluster.stripe-1.node-1.node-metadata-dir", "/home/terracotta/metadata");
    properties.put("my-cluster.stripe-1.node-1.node-log-dir", "/home/terracotta/logs");
    properties.put("my-cluster.stripe-1.node-1.node-backup-dir", "/home/terracotta/backup");
    properties.put("my-cluster.stripe-1.node-1.security-dir", "/home/terracotta/security");
    properties.put("my-cluster.stripe-1.node-1.security-audit-log-dir", "/home/terracotta/audit");
    properties.put("my-cluster.stripe-1.node-1.security-authc", "file");
    properties.put("my-cluster.stripe-1.node-1.security-ssl-tls", "true");
    properties.put("my-cluster.stripe-1.node-1.security-whitelist", "true");
    properties.put("my-cluster.stripe-1.node-1.client-reconnect-window", "100s");
    properties.put("my-cluster.stripe-1.node-1.client-lease-duration", "50s");
    properties.put("my-cluster.stripe-1.node-1.failover-priority", "consistency:2");
    properties.put("my-cluster.stripe-1.node-1.offheap-resources.main", "512MB");
    properties.put("my-cluster.stripe-1.node-1.offheap-resources.second", "1GB");
    properties.put("my-cluster.stripe-1.node-1.data-dirs.main", "/home/terracotta/user-data/main");
    properties.put("my-cluster.stripe-1.node-1.data-dirs.second", "/home/terracotta/user-data/second");

    try {
      ConfigFileValidator.validateProperties(properties, "test-file");
      failBecauseExceptionWasNotThrown(MalformedConfigFileException.class);
    } catch (Exception e) {
      assertThat(e.getClass()).isEqualTo(MalformedConfigFileException.class);
      assertThat(e.getMessage()).endsWith("missing the following properties: [cluster-name, node-name]");
    }
  }

  @Test
  public void testMissingMandatoryPropertyValue() {
    Properties properties = new Properties();
    properties.put("my-cluster.stripe-1.node-1.node-name", "node-1");
    properties.put("my-cluster.stripe-1.node-1.cluster-name", "my-cluster");
    properties.put("my-cluster.stripe-1.node-1.node-hostname", "node-1.company.internal");
    properties.put("my-cluster.stripe-1.node-1.node-port", "");
    properties.put("my-cluster.stripe-1.node-1.node-group-port", "19430");
    properties.put("my-cluster.stripe-1.node-1.node-bind-address", "10.10.10.10");
    properties.put("my-cluster.stripe-1.node-1.node-group-bind-address", "10.10.10.10");
    properties.put("my-cluster.stripe-1.node-1.node-config-dir", "/home/terracotta/config");
    properties.put("my-cluster.stripe-1.node-1.node-metadata-dir", "/home/terracotta/metadata");
    properties.put("my-cluster.stripe-1.node-1.node-log-dir", "/home/terracotta/logs");
    properties.put("my-cluster.stripe-1.node-1.node-backup-dir", "/home/terracotta/backup");
    properties.put("my-cluster.stripe-1.node-1.security-dir", "/home/terracotta/security");
    properties.put("my-cluster.stripe-1.node-1.security-audit-log-dir", "/home/terracotta/audit");
    properties.put("my-cluster.stripe-1.node-1.security-authc", "file");
    properties.put("my-cluster.stripe-1.node-1.security-ssl-tls", "true");
    properties.put("my-cluster.stripe-1.node-1.security-whitelist", "true");
    properties.put("my-cluster.stripe-1.node-1.client-reconnect-window", "100s");
    properties.put("my-cluster.stripe-1.node-1.client-lease-duration", "50s");
    properties.put("my-cluster.stripe-1.node-1.failover-priority", "consistency:2");
    properties.put("my-cluster.stripe-1.node-1.offheap-resources.main", "512MB");
    properties.put("my-cluster.stripe-1.node-1.offheap-resources.second", "1GB");
    properties.put("my-cluster.stripe-1.node-1.data-dirs.main", "/home/terracotta/user-data/main");
    properties.put("my-cluster.stripe-1.node-1.data-dirs.second", "/home/terracotta/user-data/second");

    try {
      ConfigFileValidator.validateProperties(properties, "test-file");
      failBecauseExceptionWasNotThrown(MalformedConfigFileException.class);
    } catch (Exception e) {
      assertThat(e.getClass()).isEqualTo(MalformedConfigFileException.class);
      assertThat(e.getMessage()).contains("Missing value for property: " + NODE_PORT);
    }
  }

  @Test
  public void testMultipleClusterNames() {
    Properties properties = new Properties();
    properties.put("my-cluster.stripe-1.node-1.node-name", "node-1");
    properties.put("my-cluster.stripe-1.node-1.cluster-name", "my-cluster");
    properties.put("my-cluster.stripe-1.node-1.node-hostname", "node-1.company.internal");
    properties.put("my-cluster.stripe-1.node-1.node-port", "19410");
    properties.put("my-cluster.stripe-1.node-1.node-group-port", "19430");
    properties.put("my-cluster.stripe-1.node-1.node-bind-address", "10.10.10.10");
    properties.put("my-cluster.stripe-1.node-1.node-group-bind-address", "10.10.10.10");
    properties.put("my-cluster.stripe-1.node-1.node-config-dir", "/home/terracotta/config");
    properties.put("my-cluster.stripe-1.node-1.node-metadata-dir", "/home/terracotta/metadata");
    properties.put("my-cluster.stripe-1.node-1.node-log-dir", "/home/terracotta/logs");
    properties.put("my-cluster.stripe-1.node-1.node-backup-dir", "/home/terracotta/backup");
    properties.put("my-cluster.stripe-1.node-1.security-dir", "/home/terracotta/security");
    properties.put("my-cluster.stripe-1.node-1.security-audit-log-dir", "/home/terracotta/audit");
    properties.put("my-cluster.stripe-1.node-1.security-authc", "file");
    properties.put("my-cluster.stripe-1.node-1.security-ssl-tls", "true");
    properties.put("my-cluster.stripe-1.node-1.security-whitelist", "true");
    properties.put("my-cluster.stripe-1.node-1.client-reconnect-window", "100s");
    properties.put("my-cluster.stripe-1.node-1.client-lease-duration", "50s");
    properties.put("my-cluster.stripe-1.node-1.failover-priority", "consistency:2");
    properties.put("my-cluster.stripe-1.node-1.offheap-resources.main", "512MB");
    properties.put("my-cluster.stripe-1.node-1.offheap-resources.second", "1GB");
    properties.put("my-cluster.stripe-1.node-1.data-dirs.main", "/home/terracotta/user-data/main");
    properties.put("nonsense.stripe-1.node-1.data-dirs.second", "/home/terracotta/user-data/second");

    try {
      ConfigFileValidator.validateProperties(properties, "test-file");
      failBecauseExceptionWasNotThrown(MalformedConfigFileException.class);
    } catch (Exception e) {
      assertThat(e.getClass()).isEqualTo(MalformedConfigFileException.class);
      assertThat(e.getMessage()).startsWith("Expected only one cluster name");
    }
  }
}