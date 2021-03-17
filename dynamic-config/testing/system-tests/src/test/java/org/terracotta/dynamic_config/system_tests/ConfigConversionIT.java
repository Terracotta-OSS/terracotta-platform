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
package org.terracotta.dynamic_config.system_tests;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.terracotta.common.struct.Measure;
import org.terracotta.common.struct.TimeUnit;
import org.terracotta.dynamic_config.api.json.DynamicConfigApiJsonModule;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.FailoverPriority;
import org.terracotta.dynamic_config.api.model.RawPath;
import org.terracotta.dynamic_config.api.service.ClusterFactory;
import org.terracotta.dynamic_config.api.service.IParameterSubstitutor;
import org.terracotta.dynamic_config.api.service.MalformedClusterException;
import org.terracotta.dynamic_config.api.service.Props;
import org.terracotta.dynamic_config.cli.upgrade_tools.config_converter.ConfigConverterTool;
import org.terracotta.dynamic_config.cli.upgrade_tools.config_converter.exception.ConfigConversionException;
import org.terracotta.dynamic_config.server.api.DynamicConfigNomadServer;
import org.terracotta.dynamic_config.server.configuration.nomad.NomadServerFactory;
import org.terracotta.dynamic_config.server.configuration.nomad.persistence.ConfigStorageException;
import org.terracotta.dynamic_config.server.configuration.nomad.persistence.NomadConfigurationManager;
import org.terracotta.json.ObjectMapperFactory;
import org.terracotta.nomad.server.NomadException;
import org.terracotta.persistence.sanskrit.SanskritException;
import org.terracotta.testing.TmpDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class ConfigConversionIT {
  @Rule
  public TmpDir tmpDir = new TmpDir(Paths.get(System.getProperty("user.dir"), "target", "test-data"), false);
  @Rule
  public ExpectedException exceptionRule = ExpectedException.none();

  @Test
  public void test_basic_conversion() {
    new ConfigConverterTool().run("convert",
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
    assertThat(cluster.getOffheapResources().get().size(), is(1));
    assertThat(cluster.getFailoverPriority(), is(FailoverPriority.availability()));
    assertFalse(cluster.getClientLeaseDuration().isConfigured());
    assertThat(cluster.getClientLeaseDuration().orDefault(), is(Measure.of(150, TimeUnit.SECONDS)));
    assertThat(cluster.getClientReconnectWindow().get(), is(Measure.of(120, TimeUnit.SECONDS)));
    assertThat(cluster.getStripeCount(), is(1));
    assertThat(cluster.getNodes().size(), is(2));
    assertThat(cluster.getNodeByName("node-1").get().getName(), is("node-1"));
    assertThat(cluster.getNodeByName("node-1").get().getHostname(), is("localhost"));
    assertThat(cluster.getNodeByName("node-1").get().getPort().get(), is(9410));
    assertThat(cluster.getNodeByName("node-1").get().getGroupPort().get(), is(9430));
    assertThat(cluster.getNodeByName("node-2").get().getName(), is("node-2"));
    assertThat(cluster.getNodeByName("node-2").get().getHostname(), is("localhost"));
    assertThat(cluster.getNodeByName("node-2").get().getPort().get(), is(9510));
    assertThat(cluster.getNodeByName("node-2").get().getGroupPort().get(), is(9530));
  }

  @Test
  public void test_server_defaults() {
    new ConfigConverterTool().run("convert",
        "-c", "src/test/resources/conversion/tc-config-4.xml",
        "-n", "my-cluster",
        "-t", "properties",
        "-d", tmpDir.getRoot().resolve("generated-configs").toAbsolutePath().toString(),
        "-f");
    Path config = tmpDir.getRoot().resolve("generated-configs").resolve("my-cluster.properties");
    assertTrue(Files.exists(config));
    Cluster cluster = new ClusterFactory().create(config);

    assertThat(cluster.getNodeByName("node-1").get().getName(), is("node-1"));
    // IMPORTANT: see NonSubstitutingTCConfigurationParser and TCConfigurationParser
    // If the server name was given, but not the host name, the host name value was picked from the server name
    // also, xml parsing was initializing all these settings
    assertThat(cluster.getNodeByName("node-1").get().getHostname(), is("node-1"));
    assertTrue(cluster.getNodeByName("node-1").get().getPort().isConfigured());
    assertTrue(cluster.getNodeByName("node-1").get().getGroupPort().isConfigured());
    assertTrue(cluster.getNodeByName("node-1").get().getLogDir().isConfigured());
    assertTrue(cluster.getNodeByName("node-1").get().getBindAddress().isConfigured());
    assertTrue(cluster.getNodeByName("node-1").get().getGroupBindAddress().isConfigured());
  }

  @Test
  public void testWithoutOffheap() {
    new ConfigConverterTool().run("convert",
        "-c", "src/test/resources/conversion/tc-config-3.xml",
        "-n", "my-cluster",
        "-t", "properties",
        "-d", tmpDir.getRoot().resolve("generated-configs").toAbsolutePath().toString(),
        "-f");
    Path config = tmpDir.getRoot().resolve("generated-configs").resolve("my-cluster.properties");
    assertTrue(Files.exists(config));
    Cluster cluster = new ClusterFactory().create(config);

    assertTrue(cluster.getOffheapResources().isConfigured());
    assertThat(cluster.getOffheapResources().get().size(), is(0));
  }

  @Test
  public void testWithoutHostPort() {
    new ConfigConverterTool().run("convert",
        "-c", "src/test/resources/conversion/tc-config-5.xml",
        "-n", "my-cluster",
        "-t", "properties",
        "-d", tmpDir.getRoot().resolve("generated-configs").toAbsolutePath().toString(),
        "-f");
    Path config = tmpDir.getRoot().resolve("generated-configs").resolve("my-cluster.properties");
    assertTrue(Files.exists(config));
    Cluster cluster = new ClusterFactory().create(config);

    assertEquals("my-cluster", cluster.getName());
    assertEquals("foo", cluster.getSingleStripe().get().getSingleNode().get().getName());
    assertEquals("foo", cluster.getSingleStripe().get().getSingleNode().get().getHostname());
  }

  @Test
  public void testWithoutServerName() {
    new ConfigConverterTool().run("convert",
        "-c", "src/test/resources/conversion/tc-config-6.xml",
        "-n", "my-cluster",
        "-t", "properties",
        "-d", tmpDir.getRoot().resolve("generated-configs").toAbsolutePath().toString(),
        "-f");
    Path config = tmpDir.getRoot().resolve("generated-configs").resolve("my-cluster.properties");
    assertTrue(Files.exists(config));
    Cluster cluster = new ClusterFactory().create(config);

    assertEquals("my-cluster", cluster.getName());
    assertEquals("foo-9410", cluster.getSingleStripe().get().getSingleNode().get().getName());
    assertEquals("foo", cluster.getSingleStripe().get().getSingleNode().get().getHostname());
  }

  @Test
  public void testBadClusterName() {
    exceptionRule.expect(MalformedClusterException.class);
    exceptionRule.expectMessage("Invalid character in cluster name: ':'");
    new ConfigConverterTool().run("convert",
        "-c", "src/test/resources/conversion/tc-config-8.xml",
        "-n", "my:cluster",
        "-t", "properties",
        "-d", tmpDir.getRoot().resolve("generated-configs").toAbsolutePath().toString(),
        "-f");
  }

  @Test
  public void testBadStripeName() {
    exceptionRule.expect(MalformedClusterException.class);
    exceptionRule.expectMessage("Invalid character in stripe name: ':'");
    new ConfigConverterTool().run("convert",
        "-c", "src/test/resources/conversion/tc-config-8.xml",
        "-n", "my-cluster",
        "-s", "my:stripe",
        "-t", "properties",
        "-d", tmpDir.getRoot().resolve("generated-configs").toAbsolutePath().toString(),
        "-f");
  }

  @Test
  public void testWithoutServerNameSameHost() {
    new ConfigConverterTool().run("convert",
        "-c", "src/test/resources/conversion/tc-config-8.xml",
        "-n", "my-cluster",
        "-t", "properties",
        "-d", tmpDir.getRoot().resolve("generated-configs").toAbsolutePath().toString(),
        "-f");
    Path config = tmpDir.getRoot().resolve("generated-configs").resolve("my-cluster.properties");
    assertTrue(Files.exists(config));
    Cluster cluster = new ClusterFactory().create(config);

    assertEquals("my-cluster", cluster.getName());
    assertEquals("foo-1234", cluster.getSingleStripe().get().getNodes().get(0).getName());
    assertEquals("foo", cluster.getSingleStripe().get().getNodes().get(0).getHostname());
    assertEquals("foo-1235", cluster.getSingleStripe().get().getNodes().get(1).getName());
    assertEquals("foo", cluster.getSingleStripe().get().getNodes().get(1).getHostname());
  }

  @Test
  public void testWithoutServerNameAndHost() {
    exceptionRule.expect(ConfigConversionException.class);
    exceptionRule.expectMessage("Unexpected error while migrating the configuration files: Conversion process requires a valid server name or hostname");
    new ConfigConverterTool().run("convert",
        "-c", "src/test/resources/conversion/tc-config-7.xml",
        "-n", "my-cluster",
        "-t", "properties",
        "-d", tmpDir.getRoot().resolve("generated-configs").toAbsolutePath().toString(),
        "-f");
  }

  @Test
  public void test_conversion_fail_due_to_relative_paths_and_not_forcing_conversion() {
    exceptionRule.expect(RuntimeException.class);
    new ConfigConverterTool().run("convert",
        "-c", "src/test/resources/conversion/tc-config.xml",
        "-n", "my-cluster",
        "-d", tmpDir.getRoot().resolve("generated-configs").toAbsolutePath().toString());
  }

  @Test
  public void test_conversion_no_server_element() {
    exceptionRule.expect(RuntimeException.class);
    exceptionRule.expectMessage("No server specified.");
    new ConfigConverterTool().run("convert",
        "-c", "src/test/resources/conversion/tc-config-1_no_server_element.xml",
        "-n", "my-cluster",
        "-t", "properties",
        "-d", tmpDir.getRoot().resolve("generated-configs").toAbsolutePath().toString(),
        "-f");
  }

  @Test
  public void testConversionWithPlaceHolders() {
    new ConfigConverterTool().run("convert",
        "-c", "src/test/resources/conversion/tc-config-1.xml",
        "-n", "my-cluster",
        "-t", "properties",
        "-d", tmpDir.getRoot().resolve("generated-configs").toAbsolutePath().toString(),
        "-f");
    Path config = tmpDir.getRoot().resolve("generated-configs").resolve("my-cluster.properties");
    assertTrue(Files.exists(config));
    Cluster cluster = new ClusterFactory().create(config);
    assertThat(cluster.getNodeByName("node-1").get().getLogDir().get(), is(RawPath.valueOf("%h-logs")));
    assertThat(cluster.getNodeByName("node-2").get().getLogDir().get(), is(RawPath.valueOf("%h-logs")));
  }

  @Test
  public void testConversionWithLeaseMissingInConfig() {
    new ConfigConverterTool().run("convert",
        "-c", "src/test/resources/conversion/tc-config-1_default_lease_failover_and_reconnect_window.xml",
        "-n", "cluster_default_lease_failover_reconnect_window",
        "-t", "properties",
        "-d", tmpDir.getRoot().resolve("generated-configs").toAbsolutePath().toString(),
        "-f");
    Path config = tmpDir.getRoot().resolve("generated-configs").resolve("cluster_default_lease_failover_reconnect_window.properties");
    assertTrue(Files.exists(config));
    Cluster cluster = new ClusterFactory().create(config);
    assertFalse(cluster.getClientLeaseDuration().isConfigured());
    assertThat(cluster.getClientLeaseDuration().orDefault(), is(Measure.of(150, TimeUnit.SECONDS)));
  }

  @Test
  public void testConversionWithoutFailoverPriority() {
    new ConfigConverterTool().run("convert",
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
  public void testConversionWithoutClientReconnectWindow() {
    new ConfigConverterTool().run("convert",
        "-c", "src/test/resources/conversion/tc-config-1_default_lease_failover_and_reconnect_window.xml",
        "-n", "cluster_default_lease_failover_reconnect_window",
        "-t", "properties",
        "-d", tmpDir.getRoot().resolve("generated-configs").toAbsolutePath().toString(),
        "-f");
    Path config = tmpDir.getRoot().resolve("generated-configs").resolve("cluster_default_lease_failover_reconnect_window.properties");
    assertTrue(Files.exists(config));
    Cluster cluster = new ClusterFactory().create(config);
    assertFalse(cluster.getClientReconnectWindow().isConfigured());
    assertThat(cluster.getClientReconnectWindow().orDefault(), is(Measure.of(120, TimeUnit.SECONDS)));
  }

  @Test
  public void testConversionWithLease() {
    new ConfigConverterTool().run("convert",
        "-c", "src/test/resources/conversion/tc-config-1_lease_failover_and_reconnect_window.xml",
        "-n", "cluster_lease_failover_reconnect_window",
        "-t", "properties",
        "-d", tmpDir.getRoot().resolve("generated-configs").toAbsolutePath().toString(),
        "-f");
    Path config = tmpDir.getRoot().resolve("generated-configs").resolve("cluster_lease_failover_reconnect_window.properties");
    assertTrue(Files.exists(config));
    Cluster cluster = new ClusterFactory().create(config);
    assertThat(cluster.getClientLeaseDuration().get(), is(Measure.of(5, TimeUnit.SECONDS)));
  }

  @Test
  public void testConversionWithFailoverPriority() {
    new ConfigConverterTool().run("convert",
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
    new ConfigConverterTool().run("convert",
        "-c", "src/test/resources/conversion/tc-config-1_lease_failover_and_reconnect_window.xml",
        "-n", "cluster_lease_failover_reconnect_window",
        "-t", "properties",
        "-d", tmpDir.getRoot().resolve("generated-configs").toAbsolutePath().toString(),
        "-f");
    Path config = tmpDir.getRoot().resolve("generated-configs").resolve("cluster_lease_failover_reconnect_window.properties");
    assertTrue(Files.exists(config));
    Cluster cluster = new ClusterFactory().create(config);
    assertThat(cluster.getClientReconnectWindow().get(), is(Measure.of(125, TimeUnit.SECONDS)));
  }

  @Test
  public void testNodePort() {
    new ConfigConverterTool().run("convert",
        "-c", "src/test/resources/conversion/tc-config-1_default_lease_failover_and_reconnect_window.xml",
        "-n", "cluster-port-bind-address",
        "-t", "properties",
        "-d", tmpDir.getRoot().resolve("generated-configs").toAbsolutePath().toString(),
        "-f");
    Path config = tmpDir.getRoot().resolve("generated-configs").resolve("cluster-port-bind-address.properties");
    assertTrue(Files.exists(config));
    Cluster cluster = new ClusterFactory().create(config);
    assertThat(cluster.getNodeByName("server11").get().getPort().get(), is(9411));
  }

  @Test
  public void testNodeBindAddress() {
    new ConfigConverterTool().run("convert",
        "-c", "src/test/resources/conversion/tc-config-1_default_lease_failover_and_reconnect_window.xml",
        "-n", "cluster-port-bind-address",
        "-t", "properties",
        "-d", tmpDir.getRoot().resolve("generated-configs").toAbsolutePath().toString(),
        "-f");
    Path config = tmpDir.getRoot().resolve("generated-configs").resolve("cluster-port-bind-address.properties");
    assertTrue(Files.exists(config));
    Cluster cluster = new ClusterFactory().create(config);
    assertThat(cluster.getNodeByName("server11").get().getBindAddress().get(), is("1.1.1.1"));
  }

  @Test
  public void testNodeGroupPort() {
    new ConfigConverterTool().run("convert",
        "-c", "src/test/resources/conversion/tc-config-1_default_lease_failover_and_reconnect_window.xml",
        "-n", "cluster-group-port-group-bind-address",
        "-t", "properties",
        "-d", tmpDir.getRoot().resolve("generated-configs").toAbsolutePath().toString(),
        "-f");
    Path config = tmpDir.getRoot().resolve("generated-configs").resolve("cluster-group-port-group-bind-address.properties");
    assertTrue(Files.exists(config));
    Cluster cluster = new ClusterFactory().create(config);
    assertThat(cluster.getNodeByName("server11").get().getGroupPort().get(), is(9431));
  }

  @Test
  public void testNodeGroupBindAddress() {
    new ConfigConverterTool().run("convert",
        "-c", "src/test/resources/conversion/tc-config-1_default_lease_failover_and_reconnect_window.xml",
        "-n", "cluster-group-port-group-bind-address",
        "-t", "properties",
        "-d", tmpDir.getRoot().resolve("generated-configs").toAbsolutePath().toString(),
        "-f");
    Path config = tmpDir.getRoot().resolve("generated-configs").resolve("cluster-group-port-group-bind-address.properties");
    assertTrue(Files.exists(config));
    Cluster cluster = new ClusterFactory().create(config);
    assertThat(cluster.getNodeByName("server11").get().getGroupBindAddress().get(), is("2.2.2.2"));
  }

  @Test
  public void testLogDir() {
    new ConfigConverterTool().run("convert",
        "-c", "src/test/resources/conversion/tc-config-1_default_lease_failover_and_reconnect_window.xml",
        "-n", "cluster-log-dir-tc-prop",
        "-t", "properties",
        "-d", tmpDir.getRoot().resolve("generated-configs").toAbsolutePath().toString(),
        "-f");
    Path config = tmpDir.getRoot().resolve("generated-configs").resolve("cluster-log-dir-tc-prop.properties");
    assertTrue(Files.exists(config));
    Cluster cluster = new ClusterFactory().create(config);
    assertThat(cluster.getNodeByName("server11").get().getLogDir().get(), is(RawPath.valueOf("abcd")));
  }

  @Test
  public void testTcProperty() {
    new ConfigConverterTool().run("convert",
        "-c", "src/test/resources/conversion/tc-config-1_default_lease_failover_and_reconnect_window.xml",
        "-n", "cluster-log-dir-tc-prop",
        "-t", "properties",
        "-d", tmpDir.getRoot().resolve("generated-configs").toAbsolutePath().toString(),
        "-f");
    Path config = tmpDir.getRoot().resolve("generated-configs").resolve("cluster-log-dir-tc-prop.properties");
    assertTrue(Files.exists(config));
    Cluster cluster = new ClusterFactory().create(config);
    assertThat(cluster.getNodeByName("server11").get().getTcProperties().orDefault().size(), is(1));
    assertThat(cluster.getNodeByName("server11").get().getTcProperties().orDefault().get("myserver"), is("server"));
  }

  @Test
  public void test_conversion_with_explicit_repository_option() throws Exception {
    new ConfigConverterTool().run("convert",
        "-c", "src/test/resources/conversion/tc-config.xml",
        "-n", "my-cluster",
        "-t", "directory",
        "-d", tmpDir.getRoot().resolve("generated-configs").toAbsolutePath().toString(),
        "-f");
    assertThat(
        tmpDir.getRoot().resolve("generated-configs").resolve("stripe-1").resolve("testServer0").resolve("cluster").resolve("testServer0.1.properties"),
        matches("/conversion/cluster-1.properties"));
  }

  @Test
  public void test_conversion_with_repository_option() throws Exception {
    new ConfigConverterTool().run("convert",
        "-c", "src/test/resources/conversion/tc-config.xml",
        "-n", "my-cluster",
        "-d", tmpDir.getRoot().resolve("generated-configs").toAbsolutePath().toString(),
        "-f");
    assertThat(
        tmpDir.getRoot().resolve("generated-configs").resolve("stripe-1").resolve("testServer0").resolve("cluster").resolve("testServer0.1.properties"),
        matches("/conversion/cluster-1.properties"));

    assertCanLoadNomadConfig(tmpDir.getRoot().resolve("generated-configs").resolve("stripe-1").resolve("testServer0"));
  }

  private void assertCanLoadNomadConfig(Path config) throws SanskritException, NomadException, ConfigStorageException {
    assertTrue(Files.exists(config));

    NomadConfigurationManager nomadConfigurationManager = new NomadConfigurationManager(config, IParameterSubstitutor.identity());
    nomadConfigurationManager.createDirectories();
    ObjectMapperFactory objectMapperFactory = new ObjectMapperFactory().withModule(new DynamicConfigApiJsonModule());
    NomadServerFactory nomadServerFactory = new NomadServerFactory(objectMapperFactory);

    try (DynamicConfigNomadServer nomadServer = nomadServerFactory.createServer(nomadConfigurationManager, "testServer0", null)) {
      nomadServer.discover().getLatestChange().getResult();
    }
  }

  private static Matcher<Path> matches(String config) throws Exception {
    Path configPath = Paths.get(ConfigConversionIT.class.getResource(config).toURI());
    Properties expectedProps = Props.load(configPath);
    return new TypeSafeMatcher<Path>() {
      Path path;
      Properties props;

      @Override
      protected boolean matchesSafely(Path path) {
        this.path = path;
        this.props = Props.load(path);
        props.entrySet().forEach(e -> {
          if (e.getKey().toString().endsWith("-uid")) {
            e.setValue("<uid>");
          }
        });
        props.entrySet().forEach(e -> {
          if (e.getKey().toString().endsWith(".stripe-name")) {
            e.setValue("<generated>");
          }
        });
        return props.equals(expectedProps);
      }

      @Override
      public void describeTo(Description description) {
        description.appendText(path.getFileName() + " to contain:\n\n")
            .appendText(Props.toString(expectedProps))
            .appendText("\nbut was:\n\n")
            .appendText(Props.toString(props));
      }
    };
  }
}
