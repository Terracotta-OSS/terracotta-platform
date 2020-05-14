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
package org.terracotta.dynamic_config.api.service;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.Stripe;

import java.nio.file.Paths;
import java.util.Random;
import java.util.stream.Stream;

import static org.terracotta.common.struct.MemoryUnit.GB;
import static org.terracotta.common.struct.TimeUnit.SECONDS;
import static org.terracotta.dynamic_config.api.model.FailoverPriority.consistency;

public class ClusterValidatorTest {

  @Rule
  public ExpectedException exception = ExpectedException.none();

  private final Random random = new Random();

  @Test
  public void testDuplicateNodeNameSameStripe() {
    Node node1 = Node.newDefaultNode("foo", "localhost1");
    Node node2 = Node.newDefaultNode("foo", "localhost2");

    assertClusterValidationFails("Found duplicate node name: foo in stripe 1", Cluster.newDefaultCluster(new Stripe(node1, node2)));
  }

  @Test
  public void testDuplicateNodeNameDifferentStripe() {
    Node node1 = Node.newDefaultNode("foo", "localhost1");
    Node node2 = Node.newDefaultNode("foo", "localhost2");

    // but this is OK in different stripes
    new ClusterValidator(Cluster.newDefaultCluster(new Stripe(node1), new Stripe(node2))).validate();
  }

  @Test
  public void testDuplicateAddressesSameStripe() {
    Node node1 = Node.newDefaultNode("foo1", "localhost");
    Node node2 = Node.newDefaultNode("foo2", "localhost");

    assertClusterValidationFails("Duplicate node addresses found: localhost:9410", Cluster.newDefaultCluster(new Stripe(node1, node2)));
  }

  @Test
  public void testDuplicateAddressesDifferentStripes() {
    Node node1 = Node.newDefaultNode("foo1", "localhost");
    Node node2 = Node.newDefaultNode("foo2", "localhost");

    assertClusterValidationFails("Duplicate node addresses found: localhost:9410", Cluster.newDefaultCluster(new Stripe(node1), new Stripe(node2)));
  }

  @Test
  public void testDifferingDataDirectoryNames() {
    Node node1 = Node.newDefaultNode("localhost1");
    Node node2 = Node.newDefaultNode("localhost2");
    node1.setDataDir("dir-1", Paths.get("data"));
    node2.setDataDir("dir-2", Paths.get("data"));

    assertClusterValidationFails("Data directory names of all nodes should match", Cluster.newDefaultCluster(new Stripe(node1, node2)));
  }

  @Test
  public void testValidCluster() {
    Node[] nodes = Stream.of(
        Node.newDefaultNode("localhost1"),
        Node.newDefaultNode("localhost2")
    ).map(node -> node
        .setSecurityAuditLogDir(Paths.get("audit-" + random.nextInt()))
        .setSecurityDir(Paths.get("security-root" + random.nextInt()))
        .setDataDir("dir-1", Paths.get("some-path" + random.nextInt()))
        .setNodeBackupDir(Paths.get("backup-" + random.nextInt()))
        .setNodeMetadataDir(Paths.get("metadata-" + random.nextInt()))
        .setNodeLogDir(Paths.get("logs-" + random.nextInt()))
        .setNodeName("-" + random.nextInt())
        .setNodeHostname("host-" + random.nextInt())
        .setNodePort(1 + random.nextInt(65500))
        .setNodeGroupPort(1 + random.nextInt(65500))
        .setNodeBindAddress(generateAddress())
        .setNodeGroupBindAddress(generateAddress())
    ).toArray(Node[]::new);
    Cluster cluster = Cluster.newDefaultCluster(new Stripe(nodes))
        .setSecurityAuthc("file")
        .setSecuritySslTls(true)
        .setSecurityWhitelist(false)
        .setOffheapResource("main", 1L, GB)
        .setFailoverPriority(consistency())
        .setClientReconnectWindow(100L, SECONDS)
        .setClientLeaseDuration(100L, SECONDS);
    new ClusterValidator(cluster).validate();
  }

  @Test
  public void testGoodSecurity_1() {
    Node[] nodes = Stream.of(Node.newDefaultNode("localhost1"), Node.newDefaultNode("localhost2")).peek(node -> {
      node.setSecurityDir(Paths.get("security-dir"));
      node.setSecurityAuditLogDir(Paths.get("security-audit-dir"));
    }).toArray(Node[]::new);

    Cluster cluster = Cluster.newDefaultCluster(new Stripe(nodes))
        .setSecuritySslTls(false)
        .setSecurityWhitelist(true);
    new ClusterValidator(cluster).validate();
  }

  @Test
  public void testGoodSecurity_2() {
    Node[] nodes = Stream.of(Node.newDefaultNode("localhost1"), Node.newDefaultNode("localhost2")).peek(node -> {
      node.setSecurityDir(Paths.get("security-dir"));
      node.setSecurityAuditLogDir(Paths.get("security-audit-dir"));
    }).toArray(Node[]::new);

    Cluster cluster = Cluster.newDefaultCluster(new Stripe(nodes))
        .setSecurityWhitelist(true);
    new ClusterValidator(cluster).validate();
  }

  @Test
  public void testGoodSecurity_3() {
    Node[] nodes = Stream.of(Node.newDefaultNode("localhost1"), Node.newDefaultNode("localhost2")).toArray(Node[]::new);

    Cluster cluster = Cluster.newDefaultCluster(new Stripe(nodes));
    new ClusterValidator(cluster).validate();
  }

  @Test
  public void testGoodSecurity_4() {
    Node[] nodes = Stream.of(Node.newDefaultNode("localhost1"), Node.newDefaultNode("localhost2")).peek(node -> {
      node.setSecurityDir(Paths.get("security-root-dir"));
    }).toArray(Node[]::new);

    Cluster cluster = Cluster.newDefaultCluster(new Stripe(nodes))
        .setSecuritySslTls(true)
        .setSecurityAuthc("certificate");
    new ClusterValidator(cluster).validate();
  }

  @Test
  public void testGoodSecurity_5() {
    Node[] nodes = Stream.of(Node.newDefaultNode("localhost1"), Node.newDefaultNode("localhost2")).peek(node -> {
      node.setSecurityDir(Paths.get("security-root-dir"));
      node.setSecurityAuditLogDir(Paths.get("security-audit-dir"));
    }).toArray(Node[]::new);

    Cluster cluster = Cluster.newDefaultCluster(new Stripe(nodes))
        .setSecuritySslTls(true)
        .setSecurityAuthc("certificate")
        .setSecurityWhitelist(true);
    new ClusterValidator(cluster).validate();
  }

  @Test
  public void testGoodSecurity_6() {
    Node[] nodes = new Node[]{Node.newDefaultNode("localhost1"), Node.newDefaultNode("localhost2")};

    Cluster cluster = Cluster.newDefaultCluster(new Stripe(nodes))
        .setSecuritySslTls(false);
    new ClusterValidator(cluster).validate();
  }

  @Test
  public void testGoodSecurity_7() {
    Node[] nodes = Stream.of(Node.newDefaultNode("localhost1"), Node.newDefaultNode("localhost2")).peek(node -> {
      node.setSecurityDir(Paths.get("security-root-dir"));
    }).toArray(Node[]::new);

    Cluster cluster = Cluster.newDefaultCluster(new Stripe(nodes))
        .setSecurityAuthc("file");
    new ClusterValidator(cluster).validate();
  }

  @Test
  public void testBadSecurity_1() {
    Node[] nodes = new Node[]{Node.newDefaultNode("localhost1"), Node.newDefaultNode("localhost2")};

    Cluster cluster = Cluster.newDefaultCluster(new Stripe(nodes))
        .setSecuritySslTls(false)
        .setSecurityAuthc("certificate");

    assertClusterValidationFails("ssl-tls is required for authc=certificate", cluster);
  }

  @Test
  public void testBadSecurity_2() {
    Node[] nodes = new Node[]{Node.newDefaultNode("localhost1"), Node.newDefaultNode("localhost2")};

    Cluster cluster = Cluster.newDefaultCluster(new Stripe(nodes))
        .setSecuritySslTls(true)
        .setSecurityAuthc("certificate");

    assertClusterValidationFails("security-dir is mandatory for any of the security configuration", cluster);
  }

  @Test
  public void testBadSecurity_3() {
    Node[] nodes = new Node[]{Node.newDefaultNode("localhost1"), Node.newDefaultNode("localhost2")};

    Cluster cluster = Cluster.newDefaultCluster(new Stripe(nodes))
        .setSecuritySslTls(true);

    assertClusterValidationFails("security-dir is mandatory for any of the security configuration", cluster);
  }

  @Test
  public void testBadSecurity_4() {
    Node[] nodes = new Node[]{Node.newDefaultNode("localhost1"), Node.newDefaultNode("localhost2")};

    Cluster cluster = Cluster.newDefaultCluster(new Stripe(nodes))
        .setSecurityAuthc("file");

    assertClusterValidationFails("security-dir is mandatory for any of the security configuration", cluster);
  }

  @Test
  public void testBadSecurity_5() {
    Node[] nodes = Stream.of(Node.newDefaultNode("localhost1"), Node.newDefaultNode("localhost2")).peek(node -> {
      node.setSecurityAuditLogDir(Paths.get("."));
    }).toArray(Node[]::new);

    Cluster cluster = Cluster.newDefaultCluster(new Stripe(nodes));

    assertClusterValidationFails("security-dir is mandatory for any of the security configuration", cluster);
  }

  @Test
  public void testBadSecurity_6() {
    Node[] nodes = new Node[]{Node.newDefaultNode("localhost1"), Node.newDefaultNode("localhost2")};

    Cluster cluster = Cluster.newDefaultCluster(new Stripe(nodes))
        .setSecurityWhitelist(true);

    assertClusterValidationFails("security-dir is mandatory for any of the security configuration", cluster);
  }

  @Test
  public void testBadSecurity_7() {
    Node[] nodes = Stream.of(Node.newDefaultNode("localhost1"), Node.newDefaultNode("localhost2")).peek(node -> {
      node.setSecurityDir(Paths.get("."));
    }).toArray(Node[]::new);

    Cluster cluster = Cluster.newDefaultCluster(new Stripe(nodes));

    assertClusterValidationFails("One of ssl-tls, authc, or whitelist is required for security configuration", cluster);
  }

  private String generateAddress() {
    return random.nextInt(256) + "." + random.nextInt(256) + "." + random.nextInt(256) + "." + random.nextInt(256);
  }

  private void assertClusterValidationFails(String message, Cluster cluster) {
    exception.expect(MalformedClusterException.class);
    exception.expectMessage(message);
    new ClusterValidator(cluster).validate();
  }
}