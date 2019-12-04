/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.model;

import com.terracottatech.dynamic_config.util.IParameterSubstitutor;
import com.terracottatech.utilities.Measure;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static com.terracottatech.dynamic_config.model.FailoverPriority.availability;
import static com.terracottatech.dynamic_config.model.FailoverPriority.consistency;
import static com.terracottatech.dynamic_config.util.IParameterSubstitutor.identity;
import static com.terracottatech.utilities.MemoryUnit.GB;
import static com.terracottatech.utilities.MemoryUnit.MB;
import static com.terracottatech.utilities.TimeUnit.SECONDS;
import static com.terracottatech.utilities.hamcrest.ExceptionMatcher.throwing;
import static java.io.File.separator;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasEntry;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class ConfigurationParserTest {

  private static final IParameterSubstitutor SERVER_SUBSTITUTOR_SIMULATOR = source -> "%h".equals(source) ? "localhost" : source;

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
    assertThat(
        () -> ConfigurationParser.parsePropertyConfiguration(identity(), new Properties()),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Invalid property: stripe.1.node.1.node-hostname=%h. Placeholders are not allowed.")))));

    Properties properties = new Properties();
    properties.setProperty("stripe.1.node.1.node-hostname", "localhost");
    Cluster cluster = ConfigurationParser.parsePropertyConfiguration(identity(), properties);

    Properties expected = loadProperties("minimal_with_default.properties");
    expected.put("stripe.1.node.1.node-name", cluster.getSingleNode().get().getNodeName()); // because node name is generated
    if (WINDOWS) {
      expected.stringPropertyNames().forEach(key -> expected.setProperty(key, expected.getProperty(key).replace('/', '\\'))); // windows compat'
    }

    assertThat(cluster.toProperties(false, true), is(equalTo(expected)));


    //TODO
    //    Cluster rebuilt = ConfigurationParser.parsePropertyConfiguration(identity(), actual);
//    assertThat(rebuilt, is(equalTo(cluster)));

  }

  @Test
  public void testBadOffheap_1() {
    Map<Setting, String> paramValueMap = new HashMap<>();
    paramValueMap.put(Setting.OFFHEAP_RESOURCES, "blah");
    testThrowsWithMessage(paramValueMap, "should be specified in the format <resource-name>:<quantity><unit>,<resource-name>:<quantity><unit>...");
  }

  @Test
  public void testBadOffheap_2() {
    Map<Setting, String> paramValueMap = new HashMap<>();
    paramValueMap.put(Setting.OFFHEAP_RESOURCES, "blah:foo");
    testThrowsWithMessage(paramValueMap, "Invalid measure: 'foo'. <quantity> is missing. Measure should be specified in <quantity><unit> format.");
  }

  @Test
  public void testBadOffheap_3() {
    Map<Setting, String> paramValueMap = new HashMap<>();
    paramValueMap.put(Setting.OFFHEAP_RESOURCES, "blah:200blah");
    testThrowsWithMessage(paramValueMap, "Invalid measure: '200blah'. <unit> must be one of [B, KB, MB, GB, TB, PB].");
  }

  @Test
  public void testBadOffheap_4() {
    Map<Setting, String> paramValueMap = new HashMap<>();
    paramValueMap.put(Setting.OFFHEAP_RESOURCES, "blah:200MB;blah-2:200MB");
    testThrowsWithMessage(paramValueMap, "should be specified in the format <resource-name>:<quantity><unit>,<resource-name>:<quantity><unit>...");
  }

  @Test
  public void testBadNodePort_1() {
    Map<Setting, String> paramValueMap = new HashMap<>();
    paramValueMap.put(Setting.NODE_PORT, "blah");
    testThrowsWithMessage(paramValueMap, "must be an integer between 1 and 65535");
  }

  @Test
  public void testBadNodePort_2() {
    Map<Setting, String> paramValueMap = new HashMap<>();
    paramValueMap.put(Setting.NODE_PORT, "0");
    testThrowsWithMessage(paramValueMap, "must be an integer between 1 and 65535");
  }

  @Test
  public void testBadNodePort_3() {
    Map<Setting, String> paramValueMap = new HashMap<>();
    paramValueMap.put(Setting.NODE_PORT, "100000");
    testThrowsWithMessage(paramValueMap, "must be an integer between 1 and 65535");
  }

  @Test
  public void testBadNodeGroupPort_1() {
    Map<Setting, String> paramValueMap = new HashMap<>();
    paramValueMap.put(Setting.NODE_GROUP_PORT, "blah");
    testThrowsWithMessage(paramValueMap, "must be an integer between 1 and 65535");
  }

  @Test
  public void testBadNodeGroupPort_2() {
    Map<Setting, String> paramValueMap = new HashMap<>();
    paramValueMap.put(Setting.NODE_GROUP_PORT, "0");
    testThrowsWithMessage(paramValueMap, "must be an integer between 1 and 65535");
  }

  @Test
  public void testBadNodeGroupPort_3() {
    Map<Setting, String> paramValueMap = new HashMap<>();
    paramValueMap.put(Setting.NODE_GROUP_PORT, "100000");
    testThrowsWithMessage(paramValueMap, "must be an integer between 1 and 65535");
  }

  @Test
  public void testBadHostname_1() {
    Map<Setting, String> paramValueMap = new HashMap<>();
    paramValueMap.put(Setting.NODE_HOSTNAME, "$$$$$$$$$$$");
    testThrowsWithMessage(paramValueMap, "must be a valid hostname or IP address");
  }

  @Test
  public void testBadHostname_2() {
    Map<Setting, String> paramValueMap = new HashMap<>();
    paramValueMap.put(Setting.NODE_HOSTNAME, "10..10..10..10");
    testThrowsWithMessage(paramValueMap, "must be a valid hostname or IP address");
  }

  @Test
  public void testBadHostname_3() {
    Map<Setting, String> paramValueMap = new HashMap<>();
    paramValueMap.put(Setting.NODE_HOSTNAME, "10:10::10:zz");
    testThrowsWithMessage(paramValueMap, "must be a valid hostname or IP address");
  }

  @Test
  public void testBadNodeBindAddresses_1() {
    Map<Setting, String> paramValueMap = new HashMap<>();
    paramValueMap.put(Setting.NODE_BIND_ADDRESS, "10:10::10:zz");
    testThrowsWithMessage(paramValueMap, "must be a valid IP address");
  }

  @Test
  public void testBadNodeBindAddresses_2() {
    Map<Setting, String> paramValueMap = new HashMap<>();
    paramValueMap.put(Setting.NODE_BIND_ADDRESS, "localhost");
    testThrowsWithMessage(paramValueMap, "must be a valid IP address");
  }

  @Test
  public void testBadNodeGroupBindAddresses_1() {
    Map<Setting, String> paramValueMap = new HashMap<>();
    paramValueMap.put(Setting.NODE_GROUP_BIND_ADDRESS, "10:10::10:zz");
    testThrowsWithMessage(paramValueMap, "must be a valid IP address");
  }

  @Test
  public void testBadNodeGroupBindAddresses_2() {
    Map<Setting, String> paramValueMap = new HashMap<>();
    paramValueMap.put(Setting.NODE_GROUP_BIND_ADDRESS, "localhost");
    testThrowsWithMessage(paramValueMap, "must be a valid IP address");
  }

  @Test
  public void testBadFailoverSettings_1() {
    Map<Setting, String> paramValueMap = new HashMap<>();
    paramValueMap.put(Setting.FAILOVER_PRIORITY, "blah");
    testThrowsWithMessage(paramValueMap, "failover-priority should be either 'availability', 'consistency', or 'consistency:N' (where 'N' is the voter count expressed as a positive integer)");
  }

  @Test
  public void testBadFailoverSettings_2() {
    Map<Setting, String> paramValueMap = new HashMap<>();
    paramValueMap.put(Setting.FAILOVER_PRIORITY, "availability:3");
    testThrowsWithMessage(paramValueMap, "should be either 'availability', 'consistency', or 'consistency:N'");
  }

  @Test
  public void testBadFailoverSettings_3() {
    Map<Setting, String> paramValueMap = new HashMap<>();
    paramValueMap.put(Setting.FAILOVER_PRIORITY, "availability:blah");
    testThrowsWithMessage(paramValueMap, "should be either 'availability', 'consistency', or 'consistency:N'");
  }

  @Test
  public void testBadFailoverSettings_4() {
    Map<Setting, String> paramValueMap = new HashMap<>();
    paramValueMap.put(Setting.FAILOVER_PRIORITY, "consistency:blah");
    testThrowsWithMessage(paramValueMap, "failover-priority should be either 'availability', 'consistency', or 'consistency:N' (where 'N' is the voter count expressed as a positive integer)");
  }

  @Test
  public void testBadFailoverSettings_5() {
    Map<Setting, String> paramValueMap = new HashMap<>();
    paramValueMap.put(Setting.FAILOVER_PRIORITY, "consistency;4");
    testThrowsWithMessage(paramValueMap, "failover-priority should be either 'availability', 'consistency', or 'consistency:N' (where 'N' is the voter count expressed as a positive integer)");
  }

  @Test
  public void testBadFailoverSettings_6() {
    Map<Setting, String> paramValueMap = new HashMap<>();
    paramValueMap.put(Setting.FAILOVER_PRIORITY, "consistency:0");
    testThrowsWithMessage(paramValueMap, "failover-priority should be either 'availability', 'consistency', or 'consistency:N' (where 'N' is the voter count expressed as a positive integer)");
  }

  @Test
  public void testBadClientReconnectWindow_1() {
    Map<Setting, String> paramValueMap = new HashMap<>();
    paramValueMap.put(Setting.CLIENT_RECONNECT_WINDOW, "blah");
    testThrowsWithMessage(paramValueMap, "Invalid measure: 'blah'. <quantity> is missing. Measure should be specified in <quantity><unit> format.");
  }

  @Test
  public void testBadClientReconnectWindow_2() {
    Map<Setting, String> paramValueMap = new HashMap<>();
    paramValueMap.put(Setting.CLIENT_RECONNECT_WINDOW, "20");
    testThrowsWithMessage(paramValueMap, "should be specified in <quantity><unit> format");
  }

  @Test
  public void testBadClientReconnectWindow_3() {
    Map<Setting, String> paramValueMap = new HashMap<>();
    paramValueMap.put(Setting.CLIENT_RECONNECT_WINDOW, "MB");
    testThrowsWithMessage(paramValueMap, "Invalid measure: 'MB'. <quantity> is missing. Measure should be specified in <quantity><unit> format.");
  }

  @Test
  public void testBadClientReconnectWindow_4() {
    Map<Setting, String> paramValueMap = new HashMap<>();
    paramValueMap.put(Setting.CLIENT_RECONNECT_WINDOW, "100blah");
    testThrowsWithMessage(paramValueMap, "Invalid measure: '100blah'. <unit> must be one of [s, m, h].");
  }

  @Test
  public void testBadClientLeaseDuration_1() {
    Map<Setting, String> paramValueMap = new HashMap<>();
    paramValueMap.put(Setting.CLIENT_LEASE_DURATION, "blah");
    testThrowsWithMessage(paramValueMap, "Invalid measure: 'blah'. <quantity> is missing. Measure should be specified in <quantity><unit> format.");
  }

  @Test
  public void testBadClientLeaseDuration_2() {
    Map<Setting, String> paramValueMap = new HashMap<>();
    paramValueMap.put(Setting.CLIENT_LEASE_DURATION, "20");
    testThrowsWithMessage(paramValueMap, "should be specified in <quantity><unit> format");
  }

  @Test
  public void testBadClientLeaseDuration_3() {
    Map<Setting, String> paramValueMap = new HashMap<>();
    paramValueMap.put(Setting.CLIENT_LEASE_DURATION, "MB");
    testThrowsWithMessage(paramValueMap, "Invalid measure: 'MB'. <quantity> is missing. Measure should be specified in <quantity><unit> format.");
  }

  @Test
  public void testBadClientLeaseDuration_4() {
    Map<Setting, String> paramValueMap = new HashMap<>();
    paramValueMap.put(Setting.CLIENT_LEASE_DURATION, "100blah");
    testThrowsWithMessage(paramValueMap, "Invalid measure: '100blah'. <unit> must be one of [ms, s, m, h].");
  }

  private void testThrowsWithMessage(Map<Setting, String> paramValueMap, String message) {
    exception.expect(IllegalArgumentException.class);
    exception.expectMessage(message);
    Cluster cluster = ConfigurationParser.parseCommandLineParameters(SERVER_SUBSTITUTOR_SIMULATOR, paramValueMap);
    new ClusterValidator(cluster).validate();
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