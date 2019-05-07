/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.parsing;


import com.terracottatech.dynamic_config.config.Cluster;
import com.terracottatech.dynamic_config.config.Node;
import org.junit.Test;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.terracottatech.dynamic_config.config.CommonOptions.CLIENT_LEASE_DURATION;
import static com.terracottatech.dynamic_config.config.CommonOptions.CLIENT_RECONNECT_WINDOW;
import static com.terracottatech.dynamic_config.config.CommonOptions.CLUSTER_NAME;
import static com.terracottatech.dynamic_config.config.CommonOptions.DATA_DIRS;
import static com.terracottatech.dynamic_config.config.CommonOptions.FAILOVER_PRIORITY;
import static com.terracottatech.dynamic_config.config.CommonOptions.NODE_BACKUP_DIR;
import static com.terracottatech.dynamic_config.config.CommonOptions.NODE_BIND_ADDRESS;
import static com.terracottatech.dynamic_config.config.CommonOptions.NODE_CONFIG_DIR;
import static com.terracottatech.dynamic_config.config.CommonOptions.NODE_GROUP_BIND_ADDRESS;
import static com.terracottatech.dynamic_config.config.CommonOptions.NODE_GROUP_PORT;
import static com.terracottatech.dynamic_config.config.CommonOptions.NODE_HOSTNAME;
import static com.terracottatech.dynamic_config.config.CommonOptions.NODE_LOG_DIR;
import static com.terracottatech.dynamic_config.config.CommonOptions.NODE_METADATA_DIR;
import static com.terracottatech.dynamic_config.config.CommonOptions.NODE_NAME;
import static com.terracottatech.dynamic_config.config.CommonOptions.NODE_PORT;
import static com.terracottatech.dynamic_config.config.CommonOptions.OFFHEAP_RESOURCES;
import static com.terracottatech.dynamic_config.config.CommonOptions.SECURITY_AUDIT_LOG_DIR;
import static com.terracottatech.dynamic_config.config.CommonOptions.SECURITY_AUTHC;
import static com.terracottatech.dynamic_config.config.CommonOptions.SECURITY_DIR;
import static com.terracottatech.dynamic_config.config.CommonOptions.SECURITY_SSL_TLS;
import static com.terracottatech.dynamic_config.config.CommonOptions.SECURITY_WHITELIST;
import static java.io.File.separator;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

public class ConsoleParamsParserTest {
  @Test
  public void testAllOptions() {
    Map<String, String> paramValueMap = setProperties();
    Cluster cluster = ConsoleParamsParser.parse(paramValueMap);

    assertThat(cluster.getStripes().size()).isEqualTo(1);
    assertThat(cluster.getStripes().get(0).getNodes().size()).isEqualTo(1);

    Node node = cluster.getStripes().get(0).getNodes().iterator().next();
    assertThat(node.getNodeName()).isEqualTo("node-1");
    assertThat(node.getClusterName()).isEqualTo("tc-cluster");
    assertThat(node.getNodeHostname()).isEqualTo("localhost");
    assertThat(node.getNodePort()).isEqualTo(19410);
    assertThat(node.getNodeGroupPort()).isEqualTo(19430);
    assertThat(node.getNodeBindAddress()).isEqualTo("10.10.10.10");
    assertThat(node.getNodeGroupBindAddress()).isEqualTo("20.20.20.20");
    assertThat(node.getOffheapResources()).containsExactly(entry("main", "512MB"), entry("second", "1GB"));

    assertThat(node.getNodeBackupDir().toString()).isEqualTo("backup");
    assertThat(node.getNodeConfigDir().toString()).isEqualTo("config");
    assertThat(node.getNodeLogDir().toString()).isEqualTo("logs");
    assertThat(node.getNodeMetadataDir().toString()).isEqualTo("metadata");
    assertThat(node.getSecurityDir().toString()).isEqualTo("security");
    assertThat(node.getSecurityAuditLogDir().toString()).isEqualTo("audit-logs");
    assertThat(node.getDataDirs()).containsOnly(entry("main", Paths.get("one")), entry("second", Paths.get("two")));

    assertThat(node.isSecurityWhitelist()).isTrue();
    assertThat(node.isSecuritySslTls()).isTrue();
    assertThat(node.getSecurityAuthc()).isEqualTo("ldap");

    assertThat(node.getFailoverPriority()).isEqualTo("consistency:1");
    assertThat(node.getClientReconnectWindow()).isEqualTo("100s");
    assertThat(node.getClientLeaseDuration()).isEqualTo("50s");
  }

  private Map<String, String> setProperties() {
    Map<String, String> paramValueMap = new HashMap<>();
    paramValueMap.put("--" + NODE_BACKUP_DIR, "backup");
    paramValueMap.put("--" + NODE_CONFIG_DIR, "config");
    paramValueMap.put("--" + NODE_LOG_DIR, "logs");
    paramValueMap.put("--" + NODE_METADATA_DIR, "metadata");
    paramValueMap.put("--" + SECURITY_DIR, "security");
    paramValueMap.put("--" + SECURITY_AUDIT_LOG_DIR, "audit-logs");
    paramValueMap.put("--" + DATA_DIRS, "main:one,second:two");

    paramValueMap.put("--" + NODE_NAME, "node-1");
    paramValueMap.put("--" + NODE_PORT, "19410");
    paramValueMap.put("--" + NODE_GROUP_PORT, "19430");
    paramValueMap.put("--" + NODE_BIND_ADDRESS, "10.10.10.10");
    paramValueMap.put("--" + NODE_GROUP_BIND_ADDRESS, "20.20.20.20");
    paramValueMap.put("--" + NODE_HOSTNAME, "localhost");
    paramValueMap.put("--" + CLUSTER_NAME, "tc-cluster");
    paramValueMap.put("--" + OFFHEAP_RESOURCES, "main:512MB,second:1GB");

    paramValueMap.put("--" + SECURITY_AUTHC, "ldap");
    paramValueMap.put("--" + SECURITY_SSL_TLS, "true");
    paramValueMap.put("--" + SECURITY_WHITELIST, "true");

    paramValueMap.put("--" + FAILOVER_PRIORITY, "consistency:1");
    paramValueMap.put("--" + CLIENT_RECONNECT_WINDOW, "100s");
    paramValueMap.put("--" + CLIENT_LEASE_DURATION, "50s");
    return paramValueMap;
  }

  @Test
  public void testDefaults() {
    Cluster cluster = ConsoleParamsParser.parse(Collections.emptyMap());

    assertThat(cluster.getStripes().size()).isEqualTo(1);
    assertThat(cluster.getStripes().get(0).getNodes().size()).isEqualTo(1);

    Node node = cluster.getStripes().get(0).getNodes().iterator().next();
    assertThat(node.getNodeName()).startsWith("node-");
    assertThat(node.getClusterName()).isNull();
    assertThat(node.getNodeHostname()).isEqualTo("%h");
    assertThat(node.getNodePort()).isEqualTo(9410);
    assertThat(node.getNodeGroupPort()).isEqualTo(9430);
    assertThat(node.getNodeBindAddress()).isEqualTo("0.0.0.0");
    assertThat(node.getNodeGroupBindAddress()).isEqualTo("0.0.0.0");
    assertThat(node.getOffheapResources()).containsOnly(entry("main", "512MB"));

    assertThat(node.getNodeBackupDir()).isNull();
    assertThat(node.getNodeConfigDir().toString()).isEqualTo("%H" + separator + "terracotta" + separator + "config");
    assertThat(node.getNodeLogDir().toString()).isEqualTo("%H" + separator + "terracotta" + separator + "logs");
    assertThat(node.getNodeMetadataDir().toString()).isEqualTo("%H" + separator + "terracotta" + separator + "metadata");
    assertThat(node.getSecurityDir()).isNull();
    assertThat(node.getSecurityAuditLogDir()).isNull();
    assertThat(node.getDataDirs()).containsOnly(entry("main", Paths.get("%H" + separator + "terracotta" + separator + "user-data" + separator + "main")));

    assertThat(node.isSecurityWhitelist()).isFalse();
    assertThat(node.isSecuritySslTls()).isFalse();
    assertThat(node.getSecurityAuthc()).isNull();

    assertThat(node.getFailoverPriority()).isEqualTo("availability");
    assertThat(node.getClientReconnectWindow()).isEqualTo("120s");
    assertThat(node.getClientLeaseDuration()).isEqualTo("20s");
  }
}