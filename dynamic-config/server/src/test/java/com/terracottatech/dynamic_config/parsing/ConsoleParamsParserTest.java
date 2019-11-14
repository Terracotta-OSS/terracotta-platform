/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.parsing;

import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.dynamic_config.model.ClusterFactory;
import com.terracottatech.dynamic_config.model.FailoverPriority;
import com.terracottatech.dynamic_config.model.Node;
import com.terracottatech.dynamic_config.model.Setting;
import com.terracottatech.dynamic_config.util.ParameterSubstitutor;
import com.terracottatech.utilities.Measure;
import com.terracottatech.utilities.MemoryUnit;
import com.terracottatech.utilities.TimeUnit;
import org.junit.Test;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.terracottatech.dynamic_config.model.Setting.CLIENT_LEASE_DURATION;
import static com.terracottatech.dynamic_config.model.Setting.CLIENT_RECONNECT_WINDOW;
import static com.terracottatech.dynamic_config.model.Setting.CLUSTER_NAME;
import static com.terracottatech.dynamic_config.model.Setting.DATA_DIRS;
import static com.terracottatech.dynamic_config.model.Setting.FAILOVER_PRIORITY;
import static com.terracottatech.dynamic_config.model.Setting.NODE_BACKUP_DIR;
import static com.terracottatech.dynamic_config.model.Setting.NODE_BIND_ADDRESS;
import static com.terracottatech.dynamic_config.model.Setting.NODE_GROUP_BIND_ADDRESS;
import static com.terracottatech.dynamic_config.model.Setting.NODE_GROUP_PORT;
import static com.terracottatech.dynamic_config.model.Setting.NODE_HOSTNAME;
import static com.terracottatech.dynamic_config.model.Setting.NODE_LOG_DIR;
import static com.terracottatech.dynamic_config.model.Setting.NODE_METADATA_DIR;
import static com.terracottatech.dynamic_config.model.Setting.NODE_NAME;
import static com.terracottatech.dynamic_config.model.Setting.NODE_PORT;
import static com.terracottatech.dynamic_config.model.Setting.OFFHEAP_RESOURCES;
import static com.terracottatech.dynamic_config.model.Setting.SECURITY_AUDIT_LOG_DIR;
import static com.terracottatech.dynamic_config.model.Setting.SECURITY_AUTHC;
import static com.terracottatech.dynamic_config.model.Setting.SECURITY_DIR;
import static com.terracottatech.dynamic_config.model.Setting.SECURITY_SSL_TLS;
import static com.terracottatech.dynamic_config.model.Setting.SECURITY_WHITELIST;
import static com.terracottatech.utilities.MemoryUnit.GB;
import static com.terracottatech.utilities.MemoryUnit.MB;
import static com.terracottatech.utilities.TimeUnit.SECONDS;
import static java.lang.Integer.parseInt;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class ConsoleParamsParserTest {
  private final ParameterSubstitutor parameterSubstitutor = new ParameterSubstitutor();
  private final ClusterFactory clusterFactory = new ClusterFactory(parameterSubstitutor);

  @Test
  public void testDefaults() {
    Cluster cluster = clusterFactory.create(Collections.emptyMap());
    assertThat(cluster.getName(), is(nullValue()));
    assertThat(cluster.getStripeCount(), is(1));
    assertThat(cluster.getStripes().get(0).getNodes().size(), is(1));

    Node node = cluster.getStripes().get(0).getNodes().iterator().next();
    assertThat(node.getNodeName(), startsWith("node-"));
    assertThat(node.getNodeHostname(), is(parameterSubstitutor.substitute(NODE_HOSTNAME.getDefaultValue())));
    assertThat(node.getNodePort(), is(parseInt(NODE_PORT.getDefaultValue())));
    assertThat(node.getNodeGroupPort(), is(parseInt(NODE_GROUP_PORT.getDefaultValue())));
    assertThat(node.getNodeBindAddress(), is(NODE_BIND_ADDRESS.getDefaultValue()));
    assertThat(node.getNodeGroupBindAddress(), is(NODE_GROUP_BIND_ADDRESS.getDefaultValue()));
    assertThat(node.getOffheapResources(), hasEntry(OFFHEAP_RESOURCES.getDefaultValue().split(":")[0], Measure.parse(OFFHEAP_RESOURCES.getDefaultValue().split(":")[1], MemoryUnit.class)));

    assertThat(node.getNodeBackupDir(), is(nullValue()));
    assertThat(node.getNodeLogDir().toString(), is(NODE_LOG_DIR.getDefaultValue()));
    assertThat(node.getNodeMetadataDir().toString(), is(NODE_METADATA_DIR.getDefaultValue()));
    assertThat(node.getSecurityDir(), is(nullValue()));
    assertThat(node.getSecurityAuditLogDir(), is(nullValue()));
    assertThat(node.getDataDirs(), hasEntry(DATA_DIRS.getDefaultValue().split(":")[0], Paths.get(DATA_DIRS.getDefaultValue().split(":")[1])));

    assertFalse(node.isSecurityWhitelist());
    assertFalse(node.isSecuritySslTls());
    assertThat(node.getSecurityAuthc(), is(nullValue()));

    assertThat(node.getFailoverPriority(), is(FailoverPriority.availability()));
    assertThat(node.getClientReconnectWindow(), is(Measure.parse(CLIENT_RECONNECT_WINDOW.getDefaultValue(), TimeUnit.class)));
    assertThat(node.getClientLeaseDuration(), is(Measure.parse(CLIENT_LEASE_DURATION.getDefaultValue(), TimeUnit.class)));
  }

  @Test
  public void testParametersInInput() {
    Cluster cluster = clusterFactory.create(Collections.emptyMap());
    assertThat(cluster.getName(), is(nullValue()));
    assertThat(cluster.getStripeCount(), is(1));
    assertThat(cluster.getStripes().get(0).getNodes().size(), is(1));

    Node node = cluster.getStripes().get(0).getNodes().iterator().next();
    assertThat(node.getNodeHostname(), is(parameterSubstitutor.substitute("%h")));
    assertThat(node.getNodeBindAddress(), is(NODE_BIND_ADDRESS.getDefaultValue()));
  }

  @Test
  public void testAllOptions() {
    Map<Setting, String> paramValueMap = setProperties();
    Cluster cluster = clusterFactory.create(paramValueMap);
    assertThat(cluster.getName(), is("tc-cluster"));
    assertThat(cluster.getStripeCount(), is(1));
    assertThat(cluster.getStripes().get(0).getNodes().size(), is(1));

    Node node = cluster.getStripes().get(0).getNodes().iterator().next();
    assertThat(node.getNodeName(), is("node-1"));
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

    assertThat(node.getFailoverPriority(), is(FailoverPriority.consistency(1)));
    assertThat(node.getClientReconnectWindow(), is(Measure.of(100L, SECONDS)));
    assertThat(node.getClientLeaseDuration(), is(Measure.of(50L, SECONDS)));
  }

  private Map<Setting, String> setProperties() {
    Map<Setting, String> paramValueMap = new HashMap<>();
    paramValueMap.put(CLUSTER_NAME, "tc-cluster");

    paramValueMap.put(NODE_BACKUP_DIR, "backup");
    paramValueMap.put(NODE_LOG_DIR, "logs");
    paramValueMap.put(NODE_METADATA_DIR, "metadata");
    paramValueMap.put(SECURITY_DIR, "security");
    paramValueMap.put(SECURITY_AUDIT_LOG_DIR, "audit-logs");
    paramValueMap.put(DATA_DIRS, "main:one,second:two");

    paramValueMap.put(NODE_NAME, "node-1");
    paramValueMap.put(NODE_PORT, "19410");
    paramValueMap.put(NODE_GROUP_PORT, "19430");
    paramValueMap.put(NODE_BIND_ADDRESS, "10.10.10.10");
    paramValueMap.put(NODE_GROUP_BIND_ADDRESS, "20.20.20.20");
    paramValueMap.put(NODE_HOSTNAME, "localhost");
    paramValueMap.put(OFFHEAP_RESOURCES, "main:512MB,second:1GB");

    paramValueMap.put(SECURITY_AUTHC, "ldap");
    paramValueMap.put(SECURITY_SSL_TLS, "true");
    paramValueMap.put(SECURITY_WHITELIST, "true");

    paramValueMap.put(FAILOVER_PRIORITY, "consistency:1");
    paramValueMap.put(CLIENT_RECONNECT_WINDOW, "100s");
    paramValueMap.put(CLIENT_LEASE_DURATION, "50s");
    return paramValueMap;
  }

  private Map<Setting, String> setPropertiesWithParameters() {
    Map<Setting, String> paramValueMap = new HashMap<>();
    paramValueMap.put(NODE_BIND_ADDRESS, "%i");
    paramValueMap.put(NODE_HOSTNAME, "%c");
    return paramValueMap;
  }
}