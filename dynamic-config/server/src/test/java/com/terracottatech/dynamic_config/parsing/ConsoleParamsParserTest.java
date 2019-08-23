/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.parsing;

import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.dynamic_config.model.Node;
import com.terracottatech.dynamic_config.model.config.CommonOptions;
import com.terracottatech.dynamic_config.util.IParameterSubstitutor;
import com.terracottatech.dynamic_config.util.ParameterSubstitutor;
import com.terracottatech.utilities.Measure;
import com.terracottatech.utilities.MemoryUnit;
import com.terracottatech.utilities.TimeUnit;
import org.junit.Test;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.terracottatech.dynamic_config.DynamicConfigConstants.DEFAULT_BIND_ADDRESS;
import static com.terracottatech.dynamic_config.DynamicConfigConstants.DEFAULT_CLIENT_LEASE_DURATION;
import static com.terracottatech.dynamic_config.DynamicConfigConstants.DEFAULT_CLIENT_RECONNECT_WINDOW;
import static com.terracottatech.dynamic_config.DynamicConfigConstants.DEFAULT_DATA_DIR;
import static com.terracottatech.dynamic_config.DynamicConfigConstants.DEFAULT_FAILOVER_PRIORITY;
import static com.terracottatech.dynamic_config.DynamicConfigConstants.DEFAULT_GROUP_BIND_ADDRESS;
import static com.terracottatech.dynamic_config.DynamicConfigConstants.DEFAULT_GROUP_PORT;
import static com.terracottatech.dynamic_config.DynamicConfigConstants.DEFAULT_HOSTNAME;
import static com.terracottatech.dynamic_config.DynamicConfigConstants.DEFAULT_LOG_DIR;
import static com.terracottatech.dynamic_config.DynamicConfigConstants.DEFAULT_METADATA_DIR;
import static com.terracottatech.dynamic_config.DynamicConfigConstants.DEFAULT_OFFHEAP_RESOURCE;
import static com.terracottatech.dynamic_config.DynamicConfigConstants.DEFAULT_PORT;
import static com.terracottatech.dynamic_config.DynamicConfigConstants.PARAM_INTERNAL_SEP;
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
import static org.terracotta.config.util.ParameterSubstitutor.substitute;

public class ConsoleParamsParserTest {
  private static final IParameterSubstitutor PARAMETER_SUBSTITUTOR = new ParameterSubstitutor();

  @Test
  public void testDefaults() {
    Cluster cluster = new ConsoleParamsParser(Collections.emptyMap(), PARAMETER_SUBSTITUTOR).parse();
    assertThat(cluster.getName(), is(nullValue()));
    assertThat(cluster.getStripes().size(), is(1));
    assertThat(cluster.getStripes().get(0).getNodes().size(), is(1));

    Node node = cluster.getStripes().get(0).getNodes().iterator().next();
    assertThat(node.getNodeName(), startsWith("node-"));
    assertThat(node.getNodeHostname(), is(substitute(DEFAULT_HOSTNAME)));
    assertThat(node.getNodePort(), is(parseInt(DEFAULT_PORT)));
    assertThat(node.getNodeGroupPort(), is(parseInt(DEFAULT_GROUP_PORT)));
    assertThat(node.getNodeBindAddress(), is(DEFAULT_BIND_ADDRESS));
    assertThat(node.getNodeGroupBindAddress(), is(DEFAULT_GROUP_BIND_ADDRESS));
    assertThat(node.getOffheapResources(), hasEntry(DEFAULT_OFFHEAP_RESOURCE.split(PARAM_INTERNAL_SEP)[0], Measure.parse(DEFAULT_OFFHEAP_RESOURCE.split(PARAM_INTERNAL_SEP)[1], MemoryUnit.class)));

    assertThat(node.getNodeBackupDir(), is(nullValue()));
    assertThat(node.getNodeLogDir().toString(), is(DEFAULT_LOG_DIR));
    assertThat(node.getNodeMetadataDir().toString(), is(DEFAULT_METADATA_DIR));
    assertThat(node.getSecurityDir(), is(nullValue()));
    assertThat(node.getSecurityAuditLogDir(), is(nullValue()));
    assertThat(node.getDataDirs(), hasEntry(DEFAULT_DATA_DIR.split(PARAM_INTERNAL_SEP)[0], Paths.get(DEFAULT_DATA_DIR.split(PARAM_INTERNAL_SEP)[1])));

    assertFalse(node.isSecurityWhitelist());
    assertFalse(node.isSecuritySslTls());
    assertThat(node.getSecurityAuthc(), is(nullValue()));

    assertThat(node.getFailoverPriority(), is(DEFAULT_FAILOVER_PRIORITY));
    assertThat(node.getClientReconnectWindow(), is(Measure.parse(DEFAULT_CLIENT_RECONNECT_WINDOW, TimeUnit.class)));
    assertThat(node.getClientLeaseDuration(), is(Measure.parse(DEFAULT_CLIENT_LEASE_DURATION, TimeUnit.class)));
  }

  @Test
  public void testParametersInInput() {
    Cluster cluster = new ConsoleParamsParser(setPropertiesWithParameters(), PARAMETER_SUBSTITUTOR).parse();
    assertThat(cluster.getName(), is(nullValue()));
    assertThat(cluster.getStripes().size(), is(1));
    assertThat(cluster.getStripes().get(0).getNodes().size(), is(1));

    Node node = cluster.getStripes().get(0).getNodes().iterator().next();
    assertThat(node.getNodeHostname(), is(substitute("%c")));
    assertThat(node.getNodeBindAddress(), is("%i"));
  }

  @Test
  public void testAllOptions() {
    Map<String, String> paramValueMap = setProperties();
    Cluster cluster = new ConsoleParamsParser(paramValueMap, PARAMETER_SUBSTITUTOR).parse();
    assertThat(cluster.getName(), is("tc-cluster"));
    assertThat(cluster.getStripes().size(), is(1));
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

    assertThat(node.getFailoverPriority(), is("consistency:1"));
    assertThat(node.getClientReconnectWindow(), is(Measure.of(100L, SECONDS)));
    assertThat(node.getClientLeaseDuration(), is(Measure.of(50L, SECONDS)));
  }

  private Map<String, String> setProperties() {
    Map<String, String> paramValueMap = new HashMap<>();
    paramValueMap.put("cluster-name", "tc-cluster");

    paramValueMap.put(CommonOptions.NODE_BACKUP_DIR, "backup");
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
    paramValueMap.put(CommonOptions.OFFHEAP_RESOURCES, "main:512MB,second:1GB");

    paramValueMap.put(CommonOptions.SECURITY_AUTHC, "ldap");
    paramValueMap.put(CommonOptions.SECURITY_SSL_TLS, "true");
    paramValueMap.put(CommonOptions.SECURITY_WHITELIST, "true");

    paramValueMap.put(CommonOptions.FAILOVER_PRIORITY, "consistency:1");
    paramValueMap.put(CommonOptions.CLIENT_RECONNECT_WINDOW, "100s");
    paramValueMap.put(CommonOptions.CLIENT_LEASE_DURATION, "50s");
    return paramValueMap;
  }

  private Map<String, String> setPropertiesWithParameters() {
    Map<String, String> paramValueMap = new HashMap<>();
    paramValueMap.put(CommonOptions.NODE_BIND_ADDRESS, "%i");
    paramValueMap.put(CommonOptions.NODE_HOSTNAME, "%c");
    return paramValueMap;
  }
}