/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.model.parsing;

import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.dynamic_config.model.Node;
import com.terracottatech.utilities.Measure;
import org.junit.Test;

import java.io.File;
import java.nio.file.Paths;

import static com.terracottatech.utilities.MemoryUnit.GB;
import static com.terracottatech.utilities.MemoryUnit.MB;
import static com.terracottatech.utilities.TimeUnit.SECONDS;
import static java.io.File.separator;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasEntry;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class ConfigFileParserTest {

  @Test
  public void test_cluster_name() throws Exception {
    String name = ConfigFileParser.getClusterName(new File(getClass().getResource("/config-property-files/single-stripe.properties").toURI()), "my-cluster");
    assertThat(name, is(equalTo("my-cluster")));

    name = ConfigFileParser.getClusterName(new File(getClass().getResource("/config-property-files/single-stripe.properties").toURI()), null);
    assertThat(name, is(equalTo("single-stripe")));
  }

  @Test
  public void testParse_singleStripe() throws Exception {
    Cluster cluster = ConfigFileParser.parse(new File(getClass().getResource("/config-property-files/single-stripe.properties").toURI()), "my-cluster");
    assertThat(cluster.getStripes().size(), is(1));
    assertThat(cluster.getStripes().get(0).getNodes().size(), is(1));

    Node node = cluster.getStripes().get(0).getNodes().iterator().next();
    assertThat(node.getNodeName(), is("node-1"));
    assertThat(cluster.getName(), is("my-cluster"));
    assertThat(node.getNodeHostname(), is("node-1.company.internal"));
    assertThat(node.getNodePort(), is(19410));
    assertThat(node.getNodeGroupPort(), is(19430));
    assertThat(node.getNodeBindAddress(), is("10.10.10.10"));
    assertThat(node.getNodeGroupBindAddress(), is("10.10.10.10"));
    assertThat(node.getOffheapResources(), allOf(
        hasEntry("main", Measure.of(512L, MB)),
        hasEntry("second", Measure.of(1L, GB)))
    );

    assertThat(node.getNodeBackupDir().toString(), is(separator + "home" + separator + "terracotta" + separator + "backup"));
    assertThat(node.getNodeConfigDir().toString(), is(separator + "home" + separator + "terracotta" + separator + "config"));
    assertThat(node.getNodeLogDir().toString(), is(separator + "home" + separator + "terracotta" + separator + "logs"));
    assertThat(node.getNodeMetadataDir().toString(), is(separator + "home" + separator + "terracotta" + separator + "metadata"));
    assertThat(node.getSecurityDir().toString(), is(separator + "home" + separator + "terracotta" + separator + "security"));
    assertThat(node.getSecurityAuditLogDir().toString(), is(separator + "home" + separator + "terracotta" + separator + "audit"));
    assertThat(node.getDataDirs(), allOf(
        hasEntry("main", Paths.get(separator + "home" + separator + "terracotta" + separator + "user-data" + separator + "main")),
        hasEntry("second", Paths.get(separator + "home" + separator + "terracotta" + separator + "user-data" + separator + "second"))
    ));

    assertTrue(node.isSecurityWhitelist());
    assertTrue(node.isSecuritySslTls());
    assertThat(node.getSecurityAuthc(), is("file"));

    assertThat(node.getFailoverPriority(), is("consistency:2"));
    assertThat(node.getClientReconnectWindow(), is(Measure.of(100L, SECONDS)));
    assertThat(node.getClientLeaseDuration(), is(Measure.of(50L, SECONDS)));
  }

  @Test
  public void testParse_multiStripe() throws Exception {
    Cluster cluster = ConfigFileParser.parse(new File(getClass().getResource("/config-property-files/multi-stripe.properties").toURI()), "my-cluster");
    assertThat(cluster.getStripes().size(), is(2));
    assertThat(cluster.getStripes().get(0).getNodes().size(), is(2));
    assertThat(cluster.getStripes().get(1).getNodes().size(), is(2));
  }
}