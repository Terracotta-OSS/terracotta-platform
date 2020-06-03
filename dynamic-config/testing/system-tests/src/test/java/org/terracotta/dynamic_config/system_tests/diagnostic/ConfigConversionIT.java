/*
 * Copyright Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.dynamic_config.system_tests.diagnostic;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.terracotta.common.struct.Measure;
import org.terracotta.common.struct.TimeUnit;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.FailoverPriority;
import org.terracotta.dynamic_config.api.service.ClusterFactory;
import org.terracotta.dynamic_config.api.service.Props;
import org.terracotta.dynamic_config.cli.upgrade_tools.config_converter.ConfigConverterTool;
import org.terracotta.testing.TmpDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class ConfigConversionIT {
  @Rule
  public TmpDir tmpDir = new TmpDir(Paths.get(System.getProperty("user.dir"), "target"), false);
  @Rule
  public ExpectedException exceptionRule = ExpectedException.none();

  @Test
  public void test_basic_conversion() {
    ConfigConverterTool.start("convert",
        "-c", "src/test/resources/conversion/tc-config-1.xml",
        "-n", "my-cluster",
        "-t", "properties",
        "-d", tmpDir.getRoot().resolve("generated-configs").toAbsolutePath().toString(),
        "-f");
    Path config = tmpDir.getRoot().resolve("generated-configs").resolve("my-cluster.properties");
    assertTrue(Files.exists(config));
    Cluster cluster = new ClusterFactory().create(config);

    assertThat(cluster.getName(), is("my-cluster"));
    assertThat(cluster.getDataDirNames().size(), is(1));
    assertThat(cluster.getOffheapResources().size(), is(1));
    assertThat(cluster.getFailoverPriority(), is(FailoverPriority.availability()));
    assertThat(cluster.getClientLeaseDuration(), is(Measure.of(150, TimeUnit.SECONDS)));
    assertThat(cluster.getClientReconnectWindow(), is(Measure.of(120, TimeUnit.SECONDS)));
    assertThat(cluster.getStripeCount(), is(1));
    assertThat(cluster.getNodes().size(), is(2));
    assertThat(cluster.getNode(1, 1).get().getNodeName(), is("node-1"));
    assertThat(cluster.getNode(1, 1).get().getNodeHostname(), is("localhost"));
    assertThat(cluster.getNode(1, 1).get().getNodePort(), is(9410));
    assertThat(cluster.getNode(1, 1).get().getNodeGroupPort(), is(9430));
    assertThat(cluster.getNode(1, 2).get().getNodeName(), is("node-2"));
    assertThat(cluster.getNode(1, 2).get().getNodeHostname(), is("localhost"));
    assertThat(cluster.getNode(1, 2).get().getNodePort(), is(9510));
    assertThat(cluster.getNode(1, 2).get().getNodeGroupPort(), is(9530));
  }

  @Test
  public void test_conversion_fail_due_to_relative_paths_and_not_forcing_conversion() {
    exceptionRule.expect(RuntimeException.class);
    ConfigConverterTool.start("convert",
        "-c", "src/test/resources/conversion/tc-config.xml",
        "-n", "my-cluster",
        "-d", tmpDir.getRoot().resolve("generated-configs").toAbsolutePath().toString());
  }

  @Test
  public void test_conversion_no_server_element() {
    exceptionRule.expect(RuntimeException.class);
    exceptionRule.expectMessage("No server specified.");
    ConfigConverterTool.start("convert",
        "-c", "src/test/resources/conversion/tc-config-1_no_server_element.xml",
        "-n", "my-cluster",
        "-t", "properties",
        "-d", tmpDir.getRoot().resolve("generated-configs").toAbsolutePath().toString(),
        "-f");
  }

  @Test
  public void testConversionWithPlaceHolders() {
    ConfigConverterTool.start("convert",
        "-c", "src/test/resources/conversion/tc-config-1.xml",
        "-n", "my-cluster",
        "-t", "properties",
        "-d", tmpDir.getRoot().resolve("generated-configs").toAbsolutePath().toString(),
        "-f");
    Path config = tmpDir.getRoot().resolve("generated-configs").resolve("my-cluster.properties");
    assertTrue(Files.exists(config));
    Cluster cluster = new ClusterFactory().create(config);
    assertThat(cluster.getNode(1, 1).get().getNodeLogDir(), is(Paths.get("%h-logs")));
    assertThat(cluster.getNode(1, 2).get().getNodeLogDir(), is(Paths.get("%h-logs")));
  }

  @Test
  public void testConversionWithLeaseMissingInConfig() {
    ConfigConverterTool.start("convert",
        "-c", "src/test/resources/conversion/tc-config-1_default_lease_failover_and_reconnect_window.xml",
        "-n", "cluster_default_lease_failover_reconnect_window",
        "-t", "properties",
        "-d", tmpDir.getRoot().resolve("generated-configs").toAbsolutePath().toString(),
        "-f");
    Path config = tmpDir.getRoot().resolve("generated-configs").resolve("cluster_default_lease_failover_reconnect_window.properties");
    assertTrue(Files.exists(config));
    Cluster cluster = new ClusterFactory().create(config);
    assertThat(cluster.getClientLeaseDuration(), is(Measure.of(150, TimeUnit.SECONDS)));
  }

  @Test
  public void testConversionWithFailoverPriorityMissingInConfig() {
    ConfigConverterTool.start("convert",
        "-c", "src/test/resources/conversion/tc-config-1_default_lease_failover_and_reconnect_window.xml",
        "-n", "cluster_default_lease_failover_reconnect_window",
        "-t", "properties",
        "-d", tmpDir.getRoot().resolve("generated-configs").toAbsolutePath().toString(),
        "-f");
    Path config = tmpDir.getRoot().resolve("generated-configs").resolve("cluster_default_lease_failover_reconnect_window.properties");
    assertTrue(Files.exists(config));
    Cluster cluster = new ClusterFactory().create(config);
    assertThat(cluster.getFailoverPriority(), is(FailoverPriority.availability()));
  }

  @Test
  public void testConversionWithClientReconnectWindowMissingInConfig() {
    ConfigConverterTool.start("convert",
        "-c", "src/test/resources/conversion/tc-config-1_default_lease_failover_and_reconnect_window.xml",
        "-n", "cluster_default_lease_failover_reconnect_window",
        "-t", "properties",
        "-d", tmpDir.getRoot().resolve("generated-configs").toAbsolutePath().toString(),
        "-f");
    Path config = tmpDir.getRoot().resolve("generated-configs").resolve("cluster_default_lease_failover_reconnect_window.properties");
    assertTrue(Files.exists(config));
    Cluster cluster = new ClusterFactory().create(config);
    assertThat(cluster.getClientReconnectWindow(), is(Measure.of(120, TimeUnit.SECONDS)));
  }

  @Test
  public void testConversionWithLease() {
    ConfigConverterTool.start("convert",
        "-c", "src/test/resources/conversion/tc-config-1_lease_failover_and_reconnect_window.xml",
        "-n", "cluster_lease_failover_reconnect_window",
        "-t", "properties",
        "-d", tmpDir.getRoot().resolve("generated-configs").toAbsolutePath().toString(),
        "-f");
    Path config = tmpDir.getRoot().resolve("generated-configs").resolve("cluster_lease_failover_reconnect_window.properties");
    assertTrue(Files.exists(config));
    Cluster cluster = new ClusterFactory().create(config);
    assertThat(cluster.getClientLeaseDuration(), is(Measure.of(5, TimeUnit.SECONDS)));
  }

  @Test
  public void testConversionWithFailoverPriority() {
    ConfigConverterTool.start("convert",
        "-c", "src/test/resources/conversion/tc-config-1_lease_failover_and_reconnect_window.xml",
        "-n", "cluster_lease_failover_reconnect_window",
        "-t", "properties",
        "-d", tmpDir.getRoot().resolve("generated-configs").toAbsolutePath().toString(),
        "-f");
    Path config = tmpDir.getRoot().resolve("generated-configs").resolve("cluster_lease_failover_reconnect_window.properties");
    assertTrue(Files.exists(config));
    Cluster cluster = new ClusterFactory().create(config);
    assertThat(cluster.getFailoverPriority(), is(FailoverPriority.availability()));
  }

  @Test
  public void testConversionWithClientReconnectWindow() {
    ConfigConverterTool.start("convert",
        "-c", "src/test/resources/conversion/tc-config-1_lease_failover_and_reconnect_window.xml",
        "-n", "cluster_lease_failover_reconnect_window",
        "-t", "properties",
        "-d", tmpDir.getRoot().resolve("generated-configs").toAbsolutePath().toString(),
        "-f");
    Path config = tmpDir.getRoot().resolve("generated-configs").resolve("cluster_lease_failover_reconnect_window.properties");
    assertTrue(Files.exists(config));
    Cluster cluster = new ClusterFactory().create(config);
    assertThat(cluster.getClientReconnectWindow(), is(Measure.of(125, TimeUnit.SECONDS)));
  }

  @Test
  public void testNodePort() {
    ConfigConverterTool.start("convert",
        "-c", "src/test/resources/conversion/tc-config-1_default_lease_failover_and_reconnect_window.xml",
        "-n", "cluster-port-bind-address",
        "-t", "properties",
        "-d", tmpDir.getRoot().resolve("generated-configs").toAbsolutePath().toString(),
        "-f");
    Path config = tmpDir.getRoot().resolve("generated-configs").resolve("cluster-port-bind-address.properties");
    assertTrue(Files.exists(config));
    Cluster cluster = new ClusterFactory().create(config);
    assertThat(cluster.getNode(1, 1).get().getNodePort(), is(9411));
  }

  @Test
  public void testNodeBindAddress() {
    ConfigConverterTool.start("convert",
        "-c", "src/test/resources/conversion/tc-config-1_default_lease_failover_and_reconnect_window.xml",
        "-n", "cluster-port-bind-address",
        "-t", "properties",
        "-d", tmpDir.getRoot().resolve("generated-configs").toAbsolutePath().toString(),
        "-f");
    Path config = tmpDir.getRoot().resolve("generated-configs").resolve("cluster-port-bind-address.properties");
    assertTrue(Files.exists(config));
    Cluster cluster = new ClusterFactory().create(config);
    assertThat(cluster.getNode(1, 1).get().getNodeBindAddress(), is("1.1.1.1"));
  }

  @Test
  public void testNodeGroupPort() {
    ConfigConverterTool.start("convert",
        "-c", "src/test/resources/conversion/tc-config-1_default_lease_failover_and_reconnect_window.xml",
        "-n", "cluster-group-port-group-bind-address",
        "-t", "properties",
        "-d", tmpDir.getRoot().resolve("generated-configs").toAbsolutePath().toString(),
        "-f");
    Path config = tmpDir.getRoot().resolve("generated-configs").resolve("cluster-group-port-group-bind-address.properties");
    assertTrue(Files.exists(config));
    Cluster cluster = new ClusterFactory().create(config);
    assertThat(cluster.getNode(1, 1).get().getNodeGroupPort(), is(9431));
  }

  @Test
  public void testNodeGroupBindAddress() {
    ConfigConverterTool.start("convert",
        "-c", "src/test/resources/conversion/tc-config-1_default_lease_failover_and_reconnect_window.xml",
        "-n", "cluster-group-port-group-bind-address",
        "-t", "properties",
        "-d", tmpDir.getRoot().resolve("generated-configs").toAbsolutePath().toString(),
        "-f");
    Path config = tmpDir.getRoot().resolve("generated-configs").resolve("cluster-group-port-group-bind-address.properties");
    assertTrue(Files.exists(config));
    Cluster cluster = new ClusterFactory().create(config);
    assertThat(cluster.getNode(1, 1).get().getNodeGroupBindAddress(), is("2.2.2.2"));
  }

  @Test
  public void testLogDir() {
    ConfigConverterTool.start("convert",
        "-c", "src/test/resources/conversion/tc-config-1_default_lease_failover_and_reconnect_window.xml",
        "-n", "cluster-log-dir-tc-prop",
        "-t", "properties",
        "-d", tmpDir.getRoot().resolve("generated-configs").toAbsolutePath().toString(),
        "-f");
    Path config = tmpDir.getRoot().resolve("generated-configs").resolve("cluster-log-dir-tc-prop.properties");
    assertTrue(Files.exists(config));
    Cluster cluster = new ClusterFactory().create(config);
    assertThat(cluster.getNode(1, 1).get().getNodeLogDir(), is(Paths.get("abcd")));
  }

  @Test
  public void testTcProperty() {
    ConfigConverterTool.start("convert",
        "-c", "src/test/resources/conversion/tc-config-1_default_lease_failover_and_reconnect_window.xml",
        "-n", "cluster-log-dir-tc-prop",
        "-t", "properties",
        "-d", tmpDir.getRoot().resolve("generated-configs").toAbsolutePath().toString(),
        "-f");
    Path config = tmpDir.getRoot().resolve("generated-configs").resolve("cluster-log-dir-tc-prop.properties");
    assertTrue(Files.exists(config));
    Cluster cluster = new ClusterFactory().create(config);
    assertThat(cluster.getNode(1, 1).get().getTcProperties().size(), is(1));
    assertThat(cluster.getNode(1, 1).get().getTcProperties().get("myserver"), is("server"));
  }

  @Test
  public void test_conversion_with_explicit_repository_option() {
    ConfigConverterTool.start("convert",
        "-c", "src/test/resources/conversion/tc-config.xml",
        "-n", "my-cluster",
        "-t", "directory",
        "-d", tmpDir.getRoot().resolve("generated-configs").toAbsolutePath().toString(),
        "-f");
    Path config = tmpDir.getRoot().resolve("generated-configs").resolve("stripe-1").resolve("testServer0").resolve("cluster").resolve("testServer0.1.properties");
    assertTrue(Files.exists(config));
    assertThat(Props.load(getClass().getResourceAsStream("/conversion/cluster-1.properties")), is(equalTo(Props.load(config))));
  }

  @Test
  public void test_conversion_with_repository_option() {
    ConfigConverterTool.start("convert",
        "-c", "src/test/resources/conversion/tc-config.xml",
        "-n", "my-cluster",
        "-d", tmpDir.getRoot().resolve("generated-configs").toAbsolutePath().toString(),
        "-f");
    Path config = tmpDir.getRoot().resolve("generated-configs").resolve("stripe-1").resolve("testServer0").resolve("cluster").resolve("testServer0.1.properties");
    assertTrue(Files.exists(config));
    assertThat(Props.load(getClass().getResourceAsStream("/conversion/cluster-1.properties")), is(equalTo(Props.load(config))));
  }
}
