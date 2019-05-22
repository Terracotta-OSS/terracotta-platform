/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.parsing;

import com.terracottatech.dynamic_config.config.Cluster;
import com.terracottatech.dynamic_config.config.Measure;
import com.terracottatech.dynamic_config.config.Node;
import org.junit.Test;

import java.io.File;
import java.nio.file.Paths;

import static com.terracottatech.utilities.MemoryUnit.GB;
import static com.terracottatech.utilities.MemoryUnit.MB;
import static com.terracottatech.utilities.TimeUnit.SECONDS;
import static java.io.File.separator;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;


public class ConfigFileParserTest {
  @Test
  public void testParse_singleStripe() throws Exception {
    Cluster cluster = ConfigFileParser.parse(new File(getClass().getResource("/config-property-files/single-stripe_full.properties").toURI()));
    assertThat(cluster.getStripes().size()).isEqualTo(1);
    assertThat(cluster.getStripes().get(0).getNodes().size()).isEqualTo(1);

    Node node = cluster.getStripes().get(0).getNodes().iterator().next();
    assertThat(node.getNodeName()).isEqualTo("node-1");
    assertThat(node.getClusterName()).isEqualTo("my-cluster");
    assertThat(node.getNodeHostname()).isEqualTo("node-1.company.internal");
    assertThat(node.getNodePort()).isEqualTo(19410);
    assertThat(node.getNodeGroupPort()).isEqualTo(19430);
    assertThat(node.getNodeBindAddress()).isEqualTo("10.10.10.10");
    assertThat(node.getNodeGroupBindAddress()).isEqualTo("10.10.10.10");
    assertThat(node.getOffheapResources()).containsOnly(entry("main", Measure.of(512L, MB)), entry("second", Measure.of(1L, GB)));

    assertThat(node.getNodeBackupDir().toString()).isEqualTo(separator + "home" + separator + "terracotta" + separator + "backup");
    assertThat(node.getNodeConfigDir().toString()).isEqualTo(separator + "home" + separator + "terracotta" + separator + "config");
    assertThat(node.getNodeLogDir().toString()).isEqualTo(separator + "home" + separator + "terracotta" + separator + "logs");
    assertThat(node.getNodeMetadataDir().toString()).isEqualTo(separator + "home" + separator + "terracotta" + separator + "metadata");
    assertThat(node.getSecurityDir().toString()).isEqualTo(separator + "home" + separator + "terracotta" + separator + "security");
    assertThat(node.getSecurityAuditLogDir().toString()).isEqualTo(separator + "home" + separator + "terracotta" + separator + "audit");
    assertThat(node.getDataDirs()).containsOnly(
        entry("main", Paths.get(separator + "home" + separator + "terracotta" + separator + "user-data" + separator + "main")),
        entry("second", Paths.get(separator + "home" + separator + "terracotta" + separator + "user-data" + separator + "second"))
    );

    assertThat(node.isSecurityWhitelist()).isTrue();
    assertThat(node.isSecuritySslTls()).isTrue();
    assertThat(node.getSecurityAuthc()).isEqualTo("file");

    assertThat(node.getFailoverPriority()).isEqualTo("consistency:2");
    assertThat(node.getClientReconnectWindow()).isEqualTo(Measure.of(100L, SECONDS));
    assertThat(node.getClientLeaseDuration()).isEqualTo(Measure.of(50L, SECONDS));
  }

  @Test
  public void testParse_multiStripe() throws Exception {
    Cluster cluster = ConfigFileParser.parse(new File(getClass().getResource("/config-property-files/multi-stripe.properties").toURI()));
    assertThat(cluster.getStripes().size()).isEqualTo(2);
    assertThat(cluster.getStripes().get(0).getNodes().size()).isEqualTo(2);
    assertThat(cluster.getStripes().get(1).getNodes().size()).isEqualTo(2);
  }
}