/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.model.config;

import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.dynamic_config.model.Node;
import com.terracottatech.utilities.Measure;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

import static com.terracottatech.dynamic_config.model.FailoverPriority.availability;
import static com.terracottatech.dynamic_config.model.FailoverPriority.consistency;
import static com.terracottatech.dynamic_config.util.IParameterSubstitutor.identity;
import static com.terracottatech.utilities.MemoryUnit.GB;
import static com.terracottatech.utilities.MemoryUnit.MB;
import static com.terracottatech.utilities.TimeUnit.SECONDS;
import static java.io.File.separator;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasEntry;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class ConfigurationParserTest {

  private static boolean WINDOWS = System.getProperty("os.name", "unknown").toLowerCase().contains("win");

  @Rule
  public ExpectedException exception = ExpectedException.none();

  private final Properties properties = new Properties();

  @Test
  public void testClusterName() throws Exception {
    String fileName = "single-stripe.properties";
    final Cluster cluster = ConfigurationParser.parsePropertyConfiguration(identity(), loadProperties(fileName));
    assertThat(cluster.getName(), is(equalTo("my-cluster")));
  }

  @Test
  public void testParse_singleStripe() throws Exception {
    String fileName = "single-stripe.properties";
    Cluster cluster = ConfigurationParser.parsePropertyConfiguration(identity(), loadProperties(fileName));
    assertThat(cluster.getStripeCount(), is(1));
    assertThat(cluster.getStripes().get(0).getNodes().size(), is(1));

    Node node = cluster.getStripes().get(0).getNodes().iterator().next();
    assertThat(node.getNodeName(), is("node-1"));
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
    assertThat(node.getNodeLogDir().toString(), is(separator + "home" + separator + "terracotta" + separator + "logs"));
    assertThat(node.getNodeMetadataDir().toString(), is(separator + "home" + separator + "terracotta" + separator + "metadata"));
    assertThat(node.getSecurityDir().toString(), is(separator + "home" + separator + "terracotta" + separator + "security"));
    assertThat(node.getSecurityAuditLogDir().toString(), is(separator + "home" + separator + "terracotta" + separator + "audit"));
    assertThat(node.getTcProperties(), allOf(
        hasEntry("topology.validate", "true"),
        hasEntry("server.entity.processor.threads", "64")
    ));
    assertThat(node.getDataDirs(), allOf(
        hasEntry("main", Paths.get(separator + "home" + separator + "terracotta" + separator + "user-data" + separator + "main")),
        hasEntry("second", Paths.get(separator + "home" + separator + "terracotta" + separator + "user-data" + separator + "second"))
    ));

    assertTrue(node.isSecurityWhitelist());
    assertTrue(node.isSecuritySslTls());
    assertThat(node.getSecurityAuthc(), is("file"));

    assertThat(node.getFailoverPriority(), is(consistency(2)));
    assertThat(node.getClientReconnectWindow(), is(Measure.of(100L, SECONDS)));
    assertThat(node.getClientLeaseDuration(), is(Measure.of(50L, SECONDS)));
  }

  @Test
  public void testParseMinimal_singleStripe() throws Exception {
    String fileName = "single-stripe_minimal.properties";
    Cluster cluster = ConfigurationParser.parsePropertyConfiguration(identity(), loadProperties(fileName));
    assertThat(cluster.getStripeCount(), is(1));
    assertThat(cluster.getStripes().get(0).getNodes().size(), is(1));

    Node node = cluster.getStripes().get(0).getNodes().iterator().next();
    assertThat(node.getNodeName(), is("node-1"));
    assertThat(node.getNodeHostname(), is("localhost"));
    assertThat(node.getNodePort(), is(9410));
    assertThat(node.getNodeGroupPort(), is(9430));
    assertThat(node.getNodeBindAddress(), is("0.0.0.0"));
    assertThat(node.getNodeGroupBindAddress(), is("0.0.0.0"));
    assertThat(node.getOffheapResources(), hasEntry("main", Measure.of(512L, MB)));

    assertNull(node.getNodeBackupDir());
    assertNull(node.getSecurityDir());
    assertNull(node.getSecurityAuditLogDir());

    assertThat(node.getNodeMetadataDir(), is(Paths.get("metadata")));
    assertThat(node.getNodeLogDir(), is(Paths.get("%H", "terracotta", "logs"))); // No substitution here
    assertThat(node.getDataDirs(), hasEntry("main", Paths.get("%H", "terracotta", "user-data", "main")));

    assertFalse(node.isSecurityWhitelist());
    assertFalse(node.isSecuritySslTls());
    assertNull(node.getSecurityAuthc());

    assertThat(node.getFailoverPriority(), is(availability()));
    assertThat(node.getClientReconnectWindow(), is(Measure.of(120L, SECONDS)));
    assertThat(node.getClientLeaseDuration(), is(Measure.of(20L, SECONDS)));
  }

  @Test
  public void testParse_multiStripe() throws Exception {
    String fileName = "multi-stripe.properties";
    Cluster cluster = ConfigurationParser.parsePropertyConfiguration(identity(), loadProperties(fileName));
    assertThat(cluster.getStripeCount(), is(2));
    assertThat(cluster.getStripes().get(0).getNodes().size(), is(2));
    assertThat(cluster.getStripes().get(1).getNodes().size(), is(2));
  }

  @Test
  public void testInsufficientKeys() {
    properties.put("one.two.three", "something");
    testThrowsWithMessage("Invalid input: 'one.two.three=something'.");
  }

  @Test
  public void testExtraKeys() {
    properties.put("stripe.0.node.0.property.foo", "bar");
    testThrowsWithMessage("Invalid input: 'stripe.0.node.0.property.foo=bar'. Reason: Illegal setting name: property");
  }

  @Test
  public void testUnknownNodeProperty() {
    properties.put("stripe.0.node.0.blah", "something");
    testThrowsWithMessage("Invalid input: 'stripe.0.node.0.blah=something'. Reason: Illegal setting name: blah");
  }

  @Test
  public void test_cluster_creation_omitting_defaults() throws IOException, URISyntaxException {
    Cluster cluster = ConfigurationParser.parsePropertyConfiguration(identity(), new Properties());

    Properties expected = loadProperties("c4.properties");
    expected.put("stripe.1.node.1.node-name", cluster.getSingleNode().get().getNodeName()); // because node name is generated
    if (WINDOWS) {
      expected.stringPropertyNames().forEach(key -> expected.setProperty(key, expected.getProperty(key).replace('/', '\\'))); // windows compat'
    }

    assertThat(cluster.toProperties(false, true), is(equalTo(expected)));
  }

  private void testThrowsWithMessage(String message) {
    exception.expect(IllegalArgumentException.class);
    exception.expectMessage(message);
    ConfigurationParser.parsePropertyConfiguration(identity(), properties);
  }

  private Properties loadProperties(String fileName) throws IOException, URISyntaxException {
    try (InputStream inputStream = Files.newInputStream(Paths.get(getClass().getResource("/config-property-files/" + fileName).toURI()))) {
      Properties properties = new Properties();
      properties.load(inputStream);
      return properties;
    }
  }
}