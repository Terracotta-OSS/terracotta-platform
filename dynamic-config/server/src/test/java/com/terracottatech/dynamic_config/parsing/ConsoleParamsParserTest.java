/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.parsing;


import com.terracottatech.dynamic_config.config.CommonOptions;
import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.dynamic_config.model.Measure;
import com.terracottatech.dynamic_config.model.Node;
import org.junit.Test;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.terracottatech.utilities.MemoryUnit.GB;
import static com.terracottatech.utilities.MemoryUnit.MB;
import static com.terracottatech.utilities.TimeUnit.SECONDS;
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
    assertThat(node.getOffheapResources()).containsExactly(entry("main", Measure.of(512L, MB)), entry("second", Measure.of(1L, GB)));

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
    assertThat(node.getClientReconnectWindow()).isEqualTo(Measure.of(100L, SECONDS));
    assertThat(node.getClientLeaseDuration()).isEqualTo(Measure.of(50L, SECONDS));
  }

  private Map<String, String> setProperties() {
    Map<String, String> paramValueMap = new HashMap<>();
    paramValueMap.put(CommonOptions.NODE_BACKUP_DIR, "backup");
    paramValueMap.put(CommonOptions.NODE_CONFIG_DIR, "config");
    paramValueMap.put(CommonOptions.NODE_LOG_DIR, "logs");
    paramValueMap.put(CommonOptions.NODE_METADATA_DIR, "metadata");
    paramValueMap.put(CommonOptions.SECURITY_DIR, "security");
    paramValueMap.put(CommonOptions.SECURITY_AUDIT_LOG_DIR, "audit-logs");
    paramValueMap.put(CommonOptions.DATA_DIRS, "main:one,second:two");

    paramValueMap.put(CommonOptions.NODE_NAME, "node-1");
    paramValueMap.put(CommonOptions.NODE_PORT, "19410");
    paramValueMap.put(CommonOptions.NODE_GROUP_PORT, "19430");
    paramValueMap.put(CommonOptions.NODE_BIND_ADDRESS, "10.10.10.10");
    paramValueMap.put(CommonOptions.NODE_GROUP_BIND_ADDRESS, "20.20.20.20");
    paramValueMap.put(CommonOptions.NODE_HOSTNAME, "localhost");
    paramValueMap.put(CommonOptions.CLUSTER_NAME, "tc-cluster");
    paramValueMap.put(CommonOptions.OFFHEAP_RESOURCES, "main:512MB,second:1GB");

    paramValueMap.put(CommonOptions.SECURITY_AUTHC, "ldap");
    paramValueMap.put(CommonOptions.SECURITY_SSL_TLS, "true");
    paramValueMap.put(CommonOptions.SECURITY_WHITELIST, "true");

    paramValueMap.put(CommonOptions.FAILOVER_PRIORITY, "consistency:1");
    paramValueMap.put(CommonOptions.CLIENT_RECONNECT_WINDOW, "100s");
    paramValueMap.put(CommonOptions.CLIENT_LEASE_DURATION, "50s");
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
    assertThat(node.getOffheapResources()).containsOnly(entry("main", Measure.of(512L, MB)));

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
    assertThat(node.getClientReconnectWindow()).isEqualTo(Measure.of(120L, SECONDS));
    assertThat(node.getClientLeaseDuration()).isEqualTo(Measure.of(20L, SECONDS));
  }
}