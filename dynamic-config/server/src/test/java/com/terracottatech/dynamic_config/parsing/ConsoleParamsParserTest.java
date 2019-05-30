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
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;


public class ConsoleParamsParserTest {
  @Test
  public void testAllOptions() {
    Map<String, String> paramValueMap = setProperties();
    Cluster cluster = ConsoleParamsParser.parse(paramValueMap);

    assertThat(cluster.getStripes().size(), is(1));
    assertThat(cluster.getStripes().get(0).getNodes().size(), is(1));

    Node node = cluster.getStripes().get(0).getNodes().iterator().next();
    assertThat(node.getNodeName(), is("node-1"));
    assertThat(node.getClusterName(), is("tc-cluster"));
    assertThat(node.getNodeHostname(), is("localhost"));
    assertThat(node.getNodePort(), is(19410));
    assertThat(node.getNodeGroupPort(), is(19430));
    assertThat(node.getNodeBindAddress(), is("10.10.10.10"));
    assertThat(node.getNodeGroupBindAddress(), is("20.20.20.20"));
    assertThat(node.getOffheapResources(), allOf(
        hasEntry("main", Measure.of(512L, MB)),
        hasEntry("second", Measure.of(1L, GB)))
    );

    assertThat(node.getNodeBackupDir().toString(), is("backup"));
    assertThat(node.getNodeConfigDir().toString(), is("config"));
    assertThat(node.getNodeLogDir().toString(), is("logs"));
    assertThat(node.getNodeMetadataDir().toString(), is("metadata"));
    assertThat(node.getSecurityDir().toString(), is("security"));
    assertThat(node.getSecurityAuditLogDir().toString(), is("audit-logs"));
    assertThat(node.getDataDirs(), allOf(
        hasEntry("main", Paths.get("one")),
        hasEntry("second", Paths.get("two")))
    );

    assertTrue(node.isSecurityWhitelist());
    assertTrue(node.isSecuritySslTls());
    assertThat(node.getSecurityAuthc(), is("ldap"));

    assertThat(node.getFailoverPriority(), is("consistency:1"));
    assertThat(node.getClientReconnectWindow(), is(Measure.of(100L, SECONDS)));
    assertThat(node.getClientLeaseDuration(), is(Measure.of(50L, SECONDS)));
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

    assertThat(cluster.getStripes().size(), is(1));
    assertThat(cluster.getStripes().get(0).getNodes().size(), is(1));

    Node node = cluster.getStripes().get(0).getNodes().iterator().next();
    assertThat(node.getNodeName(), startsWith("node-"));
    assertThat(node.getClusterName(), is(nullValue()));
    assertThat(node.getNodeHostname(), is("%h"));
    assertThat(node.getNodePort(), is(9410));
    assertThat(node.getNodeGroupPort(), is(9430));
    assertThat(node.getNodeBindAddress(), is("0.0.0.0"));
    assertThat(node.getNodeGroupBindAddress(), is("0.0.0.0"));
    assertThat(node.getOffheapResources(), hasEntry("main", Measure.of(512L, MB)));

    assertThat(node.getNodeBackupDir(), is(nullValue()));
    assertThat(node.getNodeConfigDir().toString(), is("%H" + separator + "terracotta" + separator + "config"));
    assertThat(node.getNodeLogDir().toString(), is("%H" + separator + "terracotta" + separator + "logs"));
    assertThat(node.getNodeMetadataDir().toString(), is("%H" + separator + "terracotta" + separator + "metadata"));
    assertThat(node.getSecurityDir(), is(nullValue()));
    assertThat(node.getSecurityAuditLogDir(), is(nullValue()));
    assertThat(node.getDataDirs(), hasEntry("main", Paths.get("%H" + separator + "terracotta" + separator + "user-data" + separator + "main")));

    assertFalse(node.isSecurityWhitelist());
    assertFalse(node.isSecuritySslTls());
    assertThat(node.getSecurityAuthc(), is(nullValue()));

    assertThat(node.getFailoverPriority(), is("availability"));
    assertThat(node.getClientReconnectWindow(), is(Measure.of(120L, SECONDS)));
    assertThat(node.getClientLeaseDuration(), is(Measure.of(20L, SECONDS)));
  }
}