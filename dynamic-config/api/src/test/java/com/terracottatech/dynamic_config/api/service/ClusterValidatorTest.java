/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.api.service;

import com.terracottatech.dynamic_config.api.model.Cluster;
import com.terracottatech.dynamic_config.api.model.MalformedClusterException;
import com.terracottatech.dynamic_config.api.model.Node;
import com.terracottatech.dynamic_config.api.model.Stripe;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.nio.file.Paths;
import java.util.Random;
import java.util.stream.Stream;

import static com.terracottatech.common.struct.MemoryUnit.GB;
import static com.terracottatech.common.struct.MemoryUnit.MB;
import static com.terracottatech.common.struct.TimeUnit.SECONDS;
import static com.terracottatech.dynamic_config.api.model.FailoverPriority.availability;
import static com.terracottatech.dynamic_config.api.model.FailoverPriority.consistency;

public class ClusterValidatorTest {

  @Rule
  public ExpectedException exception = ExpectedException.none();

  private final Random random = new Random();

  @Test
  public void testDuplicateNodeName() {
    Node node1 = Node.newDefaultNode("foo", "localhost");
    Node node2 = Node.newDefaultNode("foo", "localhost");

    assertClusterValidationFails("Found duplicate node name: foo in stripe 1", node1, node2);

    // but this is OK in different stripes
    new ClusterValidator(new Cluster(new Stripe(node1), new Stripe(node2))).validate();
  }

  @Test
  public void testDifferingClientLeaseDurations() {
    Node node1 = Node.newDefaultNode("localhost");
    Node node2 = Node.newDefaultNode("localhost");
    node1.setClientLeaseDuration(10L, SECONDS);
    node2.setClientLeaseDuration(100L, SECONDS);

    assertClusterValidationFails("Client lease duration of all nodes should match", node1, node2);
  }

  @Test
  public void testDifferingClientReconnectWindows() {
    Node node1 = Node.newDefaultNode("localhost");
    Node node2 = Node.newDefaultNode("localhost");
    node1.setClientReconnectWindow(10L, SECONDS);
    node2.setClientReconnectWindow(100L, SECONDS);

    assertClusterValidationFails("Client reconnect window of all nodes should match", node1, node2);
  }

  @Test
  public void testDifferingFailoverPriority() {
    Node node1 = Node.newDefaultNode("localhost");
    Node node2 = Node.newDefaultNode("localhost");
    node1.setFailoverPriority(availability());
    node2.setFailoverPriority(consistency());

    assertClusterValidationFails("Failover setting of all nodes should match", node1, node2);
  }

  @Test
  public void testDifferingFailoverVoterCount() {
    Node node1 = Node.newDefaultNode("localhost");
    Node node2 = Node.newDefaultNode("localhost");
    node1.setFailoverPriority(consistency());
    node2.setFailoverPriority(consistency(2));

    assertClusterValidationFails("Failover setting of all nodes should match", node1, node2);
  }

  @Test
  public void testDifferingDataDirectoryNames() {
    Node node1 = Node.newDefaultNode("localhost");
    Node node2 = Node.newDefaultNode("localhost");
    node1.setDataDir("dir-1", Paths.get("data"));
    node2.setDataDir("dir-2", Paths.get("data"));

    assertClusterValidationFails("Data directory names of all nodes should match", node1, node2);
  }

  @Test
  public void testDifferingOffheapNames() {
    Node node1 = Node.newDefaultNode("localhost");
    Node node2 = Node.newDefaultNode("localhost");
    node1.setOffheapResource("main", 512L, MB);
    node1.setOffheapResource("other", 1L, GB);

    assertClusterValidationFails("Offheap resources of all nodes should match", node1, node2);
  }

  @Test
  public void testDifferingOffheapNames_multipleOffheapResources() {
    Node node1 = Node.newDefaultNode("localhost");
    Node node2 = Node.newDefaultNode("localhost");
    node1.setOffheapResource("main", 1L, GB);
    node1.setOffheapResource("second", 2L, GB);
    node2.setOffheapResource("main", 1L, GB);
    node2.setOffheapResource("other", 2L, GB);

    assertClusterValidationFails("Offheap resources of all nodes should match", node1, node2);
  }

  @Test
  public void testDifferingOffheapResources() {
    Node node1 = Node.newDefaultNode("localhost");
    Node node2 = Node.newDefaultNode("localhost");
    node1.setOffheapResource("main", 1L, GB);
    node1.setOffheapResource("second", 2L, GB);
    node2.setOffheapResource("main", 1L, GB);
    node2.setOffheapResource("other", 2L, GB);

    assertClusterValidationFails("Offheap resources of all nodes should match", node1, node2);
  }

  @Test
  public void testDifferingOffheapQuantities() {
    Node node1 = Node.newDefaultNode("localhost");
    Node node2 = Node.newDefaultNode("localhost");
    node1.setOffheapResource("main", 1L, GB);
    node2.setOffheapResource("main", 2L, GB);

    assertClusterValidationFails("Offheap resources of all nodes should match", node1, node2);
  }

  @Test
  public void testDifferingOffheapUnitsButSameQuantities() {
    Node node1 = Node.newDefaultNode("localhost");
    Node node2 = Node.newDefaultNode("localhost");
    node1.setOffheapResource("main", 1L, GB);
    node2.setOffheapResource("main", 1024L, MB);

    assertClusterValidationSucceeds(node1, node2);
  }

  @Test
  public void testDifferingWhitelistSetting() {
    Node node1 = Node.newDefaultNode("localhost");
    Node node2 = Node.newDefaultNode("localhost");
    node1.setSecurityWhitelist(false);
    node1.setSecurityWhitelist(true);

    assertClusterValidationFails("Whitelist setting of all nodes should match", node1, node2);
  }

  @Test
  public void testDifferingSslTlsSetting() {
    Node node1 = Node.newDefaultNode("localhost");
    Node node2 = Node.newDefaultNode("localhost");
    node1.setSecuritySslTls(false);
    node1.setSecuritySslTls(true);

    assertClusterValidationFails("SSL/TLS setting of all nodes should match", node1, node2);
  }

  @Test
  public void testDifferingAuthcSetting() {
    Node node1 = Node.newDefaultNode("localhost");
    Node node2 = Node.newDefaultNode("localhost");
    node1.setSecurityAuthc("file");
    node1.setSecurityAuthc("ldap");

    assertClusterValidationFails("Authentication setting of all nodes should match", node1, node2);
  }

  @Test
  public void testValidCluster() {
    Node node1 = Node.newDefaultNode("localhost");
    Node node2 = Node.newDefaultNode("localhost");
    setNodeProperties(node1);
    setNodeProperties(node2);

    assertClusterValidationSucceeds(node1, node2);
  }

  @Test
  public void testGoodSecurity_1() {
    Node[] nodes = Stream.of(Node.newDefaultNode("localhost"), Node.newDefaultNode("localhost")).peek(node -> {
      node.setSecuritySslTls(false);
      node.setSecurityWhitelist(true);
      node.setSecurityDir(Paths.get("security-dir"));
      node.setSecurityAuditLogDir(Paths.get("security-audit-dir"));
    }).toArray(Node[]::new);

    assertClusterValidationSucceeds(nodes);
  }

  @Test
  public void testGoodSecurity_2() {
    Node[] nodes = Stream.of(Node.newDefaultNode("localhost"), Node.newDefaultNode("localhost")).peek(node -> {
      node.setSecurityWhitelist(true);
      node.setSecurityDir(Paths.get("security-dir"));
      node.setSecurityAuditLogDir(Paths.get("security-audit-dir"));
    }).toArray(Node[]::new);

    assertClusterValidationSucceeds(nodes);
  }

  @Test
  public void testGoodSecurity_3() {
    Node[] nodes = Stream.of(Node.newDefaultNode("localhost"), Node.newDefaultNode("localhost")).toArray(Node[]::new);

    assertClusterValidationSucceeds(nodes);
  }

  @Test
  public void testGoodSecurity_4() {
    Node[] nodes = Stream.of(Node.newDefaultNode("localhost"), Node.newDefaultNode("localhost")).peek(node -> {
      node.setSecuritySslTls(true);
      node.setSecurityAuthc("certificate");
      node.setSecurityDir(Paths.get("security-root-dir"));
    }).toArray(Node[]::new);

    assertClusterValidationSucceeds(nodes);
  }

  @Test
  public void testGoodSecurity_5() {
    Node[] nodes = Stream.of(Node.newDefaultNode("localhost"), Node.newDefaultNode("localhost")).peek(node -> {
      node.setSecuritySslTls(true);
      node.setSecurityAuthc("certificate");
      node.setSecurityWhitelist(true);
      node.setSecurityDir(Paths.get("security-root-dir"));
      node.setSecurityAuditLogDir(Paths.get("security-audit-dir"));
    }).toArray(Node[]::new);

    assertClusterValidationSucceeds(nodes);
  }

  @Test
  public void testGoodSecurity_6() {
    Node[] nodes = Stream.of(Node.newDefaultNode("localhost"), Node.newDefaultNode("localhost")).peek(node -> {
      node.setSecuritySslTls(false);
    }).toArray(Node[]::new);

    assertClusterValidationSucceeds(nodes);
  }

  @Test
  public void testGoodSecurity_7() {
    Node[] nodes = Stream.of(Node.newDefaultNode("localhost"), Node.newDefaultNode("localhost")).peek(node -> {
      node.setSecurityDir(Paths.get("security-root-dir"));
      node.setSecurityAuthc("file");
    }).toArray(Node[]::new);

    assertClusterValidationSucceeds(nodes);
  }

  @Test
  public void testBadSecurity_1() {
    Node[] nodes = Stream.of(Node.newDefaultNode("localhost"), Node.newDefaultNode("localhost")).peek(node -> {
      node.setSecuritySslTls(false);
      node.setSecurityAuthc("certificate");
    }).toArray(Node[]::new);

    assertClusterValidationFails("security-ssl-tls is required for security-authc=certificate", nodes);
  }

  @Test
  public void testBadSecurity_2() {
    Node[] nodes = Stream.of(Node.newDefaultNode("localhost"), Node.newDefaultNode("localhost")).peek(node -> {
      node.setSecuritySslTls(true);
      node.setSecurityAuthc("certificate");
    }).toArray(Node[]::new);

    assertClusterValidationFails("security-dir is mandatory for any of the security configuration", nodes);
  }

  @Test
  public void testBadSecurity_3() {
    Node[] nodes = Stream.of(Node.newDefaultNode("localhost"), Node.newDefaultNode("localhost")).peek(node -> {
      node.setSecuritySslTls(true);
    }).toArray(Node[]::new);

    assertClusterValidationFails("security-dir is mandatory for any of the security configuration", nodes);
  }

  @Test
  public void testBadSecurity_4() {
    Node[] nodes = Stream.of(Node.newDefaultNode("localhost"), Node.newDefaultNode("localhost")).peek(node -> {
      node.setSecurityAuthc("file");
    }).toArray(Node[]::new);

    assertClusterValidationFails("security-dir is mandatory for any of the security configuration", nodes);
  }

  @Test
  public void testBadSecurity_5() {
    Node[] nodes = Stream.of(Node.newDefaultNode("localhost"), Node.newDefaultNode("localhost")).peek(node -> {
      node.setSecurityAuditLogDir(Paths.get("."));
    }).toArray(Node[]::new);

    assertClusterValidationFails("security-dir is mandatory for any of the security configuration", nodes);
  }

  @Test
  public void testBadSecurity_6() {
    Node[] nodes = Stream.of(Node.newDefaultNode("localhost"), Node.newDefaultNode("localhost")).peek(node -> {
      node.setSecurityWhitelist(true);
    }).toArray(Node[]::new);

    assertClusterValidationFails("security-dir is mandatory for any of the security configuration", nodes);
  }

  @Test
  public void testBadSecurity_7() {
    Node[] nodes = Stream.of(Node.newDefaultNode("localhost"), Node.newDefaultNode("localhost")).peek(node -> {
      node.setSecurityDir(Paths.get("."));
    }).toArray(Node[]::new);

    assertClusterValidationFails("One of security-ssl-tls, security-authc, or security-whitelist is required for security configuration", nodes);
  }

  private void setNodeProperties(Node node) {
    node.setSecurityAuthc("file");
    node.setSecuritySslTls(true);
    node.setSecurityWhitelist(false);
    node.setSecurityAuditLogDir(Paths.get("audit-" + random.nextInt()));
    node.setSecurityDir(Paths.get("security-root" + random.nextInt()));
    node.setOffheapResource("main", 1L, GB);
    node.setDataDir("dir-1", Paths.get("some-path" + random.nextInt()));
    node.setFailoverPriority(consistency());
    node.setClientReconnectWindow(100L, SECONDS);
    node.setClientLeaseDuration(100L, SECONDS);
    node.setNodeBackupDir(Paths.get("backup-" + random.nextInt()));
    node.setNodeMetadataDir(Paths.get("metadata-" + random.nextInt()));
    node.setNodeLogDir(Paths.get("logs-" + random.nextInt()));
    node.setNodeName("node-" + random.nextInt());
    node.setNodeHostname("host-" + random.nextInt());
    node.setNodePort(1 + random.nextInt(65500));
    node.setNodeGroupPort(1 + random.nextInt(65500));
    node.setNodeBindAddress(generateAddress());
    node.setNodeGroupBindAddress(generateAddress());
  }

  private String generateAddress() {
    return random.nextInt(256) + "." + random.nextInt(256) + "." + random.nextInt(256) + "." + random.nextInt(256);
  }

  private void assertClusterValidationFails(String message, Node... nodes) {
    exception.expect(MalformedClusterException.class);
    exception.expectMessage(message);
    new ClusterValidator(new Cluster(new Stripe(nodes))).validate();
  }

  private void assertClusterValidationSucceeds(Node... nodes) {
    new ClusterValidator(new Cluster(new Stripe(nodes))).validate();
  }
}