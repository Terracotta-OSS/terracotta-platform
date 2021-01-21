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
import org.terracotta.dynamic_config.api.model.RawPath;
import org.terracotta.dynamic_config.api.model.Testing;

import java.util.Random;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.terracotta.common.struct.MemoryUnit.GB;
import static org.terracotta.common.struct.TimeUnit.SECONDS;
import static org.terracotta.dynamic_config.api.model.FailoverPriority.consistency;
import static org.terracotta.dynamic_config.api.model.Testing.newTestCluster;
import static org.terracotta.dynamic_config.api.model.Testing.newTestNode;
import static org.terracotta.dynamic_config.api.model.Testing.newTestStripe;
import static org.terracotta.testing.ExceptionMatcher.throwing;

public class ClusterValidatorTest {

  private static final String securityDirError = "Within a cluster, all nodes must have a security root directory defined or no security root directory defined";
  private static final String minimumSecurityError = "When security root directories are configured across the cluster at least one of authc, ssl-tls or whitelist must also be configured";
  private static final String certificateSslTlsError = "When authc=certificate ssl-tls must be configured";
  private static final String securityDisallowedError = "When no security root directories are configured all other security settings should also be unconfigured (unset)";
  private static final String auditLogDirError = "Within a cluster, all nodes must have an audit log directory defined or no audit log directory defined";
  private static final String auditLogDirDisallowedError = "When no security root directories are configured audit-log-dir should also be unconfigured (unset) for all nodes in the cluster";

  @Rule
  public ExpectedException exception = ExpectedException.none();

  private final Random random = new Random();

  @Test
  public void testDuplicateNodeUIDs() {
    Node node1 = newTestNode("foo1", "localhost1", Testing.N_UIDS[1]);
    Node node2 = newTestNode("foo2", "localhost2", Testing.N_UIDS[1]);

    assertClusterValidationFails(
        "Duplicate UID for node: foo2 in stripe: stripe1. UID: jUhhu1kRQd-x6iNgpo9Xyw was used on node: foo1 in stripe: stripe1",
        newTestCluster(newTestStripe("stripe1").addNodes(node1, node2)));
  }

  @Test
  public void testDuplicateNodeUIDsDifferentStripes() {
    Node node1 = newTestNode("foo1", "localhost1", Testing.N_UIDS[1]);
    Node node2 = newTestNode("foo2", "localhost2", Testing.N_UIDS[1]);

    assertClusterValidationFails(
        "Duplicate UID for node: foo2 in stripe: stripe2. UID: jUhhu1kRQd-x6iNgpo9Xyw was used on node: foo1 in stripe: stripe1",
        newTestCluster(newTestStripe("stripe1").addNode(node1), newTestStripe("stripe2").setUID(Testing.S_UIDS[2]).addNode(node2)));
  }

  @Test
  public void testDuplicateStripeUIDs() {
    Node node1 = newTestNode("foo1", "localhost1", Testing.N_UIDS[1]);
    Node node2 = newTestNode("foo2", "localhost2", Testing.N_UIDS[2]);

    assertClusterValidationFails(
        "Duplicate UID for stripe: stripe2. UID: 5Zv3uphiRLavoGZthy7JNg was used on stripe: stripe1",
        newTestCluster(newTestStripe("stripe1").addNode(node1), newTestStripe("stripe2").addNode(node2)));
  }

  @Test
  public void testDuplicateUIDs() {
    Node node1 = newTestNode("foo1", "localhost1", Testing.N_UIDS[1]);
    Node node2 = newTestNode("foo2", "localhost2", Testing.N_UIDS[2]);

    assertClusterValidationFails(
        "Duplicate UID for stripe: stripe2. UID: jUhhu1kRQd-x6iNgpo9Xyw was used on node: foo1 in stripe: stripe1",
        newTestCluster(newTestStripe("stripe1").addNode(node1), newTestStripe("stripe2").setUID(Testing.N_UIDS[1]).addNode(node2)));

    assertClusterValidationFails(
        "Duplicate UID for node: foo1 in stripe: stripe1. UID: jUhhu1kRQd-x6iNgpo9Xyw was used on cluster",
        newTestCluster(newTestStripe("stripe1").addNode(node1)).setUID(Testing.N_UIDS[1]));

    assertClusterValidationFails(
        "Duplicate UID for stripe: stripe1. UID: 5Zv3uphiRLavoGZthy7JNg was used on cluster",
        newTestCluster(newTestStripe("stripe1").addNode(node1)).setUID(Testing.S_UIDS[1]));

    assertClusterValidationFails(
        "Duplicate UID for stripe: stripe1. UID: YLQguzhRSdS6y5M9vnA5mw was used on cluster",
        newTestCluster(newTestStripe("stripe1").addNode(node1).setUID(Testing.C_UIDS[0])));

    assertClusterValidationFails(
        "Duplicate UID for node: foo1 in stripe: stripe1. UID: jUhhu1kRQd-x6iNgpo9Xyw was used on stripe: stripe1",
        newTestCluster(newTestStripe("stripe1").addNode(node1).setUID(Testing.N_UIDS[1])));
  }

  @Test
  public void testDuplicateNodeNameSameStripe() {
    Node node1 = newTestNode("foo", "localhost1");
    Node node2 = newTestNode("foo", "localhost2", Testing.N_UIDS[2]);

    assertClusterValidationFails("Found duplicate node name: foo", newTestCluster(newTestStripe("stripe1").addNodes(node1, node2)));
  }

  @Test
  public void testDuplicateNodeNameDifferentStripe() {
    Node node1 = newTestNode("foo", "localhost1");
    Node node2 = newTestNode("foo", "localhost2", Testing.N_UIDS[2]);

    assertClusterValidationFails("Found duplicate node name: foo", newTestCluster(newTestStripe("stripe1").addNodes(node1), newTestStripe("stripe2", Testing.S_UIDS[2]).addNodes(node2)));
  }

  @Test
  public void testDuplicateAddress() {
    Node node1 = newTestNode("foo1", "localhost");
    Node node2 = newTestNode("foo2", "localhost", Testing.N_UIDS[2]);

    assertClusterValidationFails(
        "Nodes with names: foo1, foo2 have the same address: 'localhost:9410'",
        newTestCluster(newTestStripe("stripe1").addNodes(node1, node2)));
  }

  @Test
  public void testDuplicatePublicAddress() {
    Node node1 = newTestNode("foo1", "host1").setPublicHostname("public-host").setPublicPort(9510);
    Node node2 = newTestNode("foo2", "host2", Testing.N_UIDS[2]).setPublicHostname("public-host").setPublicPort(9510);

    assertClusterValidationFails(
        "Nodes with names: foo1, foo2 have the same public address: 'public-host:9510'",
        newTestCluster(newTestStripe("stripe1").addNodes(node1, node2)));
  }

  @Test
  public void testNotAllNodesHavePublicAddress() {
    Node node1 = newTestNode("foo1", "host1").setPublicHostname("public-host").setPublicPort(9510);
    Node node2 = newTestNode("foo2", "host2", Testing.N_UIDS[2]);

    assertClusterValidationFails(
        "Nodes with names: [foo2] don't have public addresses defined, but other nodes in the cluster do. Mutative operations on public addresses must be done simultaneously on every node in the cluster",
        newTestCluster(newTestStripe("stripe1").addNodes(node1, node2)));
  }

  @Test
  public void testSamePublicAndPrivateAddressOnSameNode() {
    Node node = newTestNode("foo1", "host").setPort(9410).setPublicHostname("host").setPublicPort(9410);
    new ClusterValidator(newTestCluster(newTestStripe("stripe1").addNodes(node))).validate();
  }

  @Test
  public void testSamePublicAndPrivateAddressAcrossNodes() {
    Node node1 = newTestNode("foo1", "host1").setPort(9410).setPublicHostname("host2").setPublicPort(9410);
    Node node2 = newTestNode("foo2", "host2", Testing.N_UIDS[2]).setPort(9410).setPublicHostname("host1").setPublicPort(9410);
    new ClusterValidator(newTestCluster(newTestStripe("stripe1").addNodes(node1, node2))).validate();
  }

  @Test
  public void testDuplicatePrivateAddressWithDifferentPublicAddresses() {
    Node node1 = newTestNode("foo1", "localhost").setPublicHostname("public-host1").setPublicPort(9510);
    Node node2 = newTestNode("foo2", "localhost", Testing.N_UIDS[2]).setPublicHostname("public-host2").setPublicPort(9510);

    assertClusterValidationFails(
        "Nodes with names: foo1, foo2 have the same address: 'localhost:9410'",
        newTestCluster(newTestStripe("stripe1").addNodes(node1, node2)));
  }

  @Test
  public void testMalformedPublicAddress_missingPublicPort() {
    Node node = newTestNode("foo", "localhost").setPublicHostname("public-host");
    assertClusterValidationFails("Public address: 'public-host:null' of node with name: foo isn't well-formed. Public hostname and port need to be set together",
        newTestCluster(newTestStripe("stripe1").addNodes(node)));
  }

  @Test
  public void testMalformedPublicAddress_missingPublicHostname() {
    Node node = newTestNode("foo", "localhost").setPublicPort(9410);
    assertClusterValidationFails("Public address: 'null:9410' of node with name: foo isn't well-formed. Public hostname and port need to be set together",
        newTestCluster(newTestStripe("stripe1").addNodes(node)));
  }

  @Test
  public void testDifferingDataDirectoryNames() {
    Node node1 = newTestNode("node1", "localhost1");
    Node node2 = newTestNode("node2", "localhost2", Testing.N_UIDS[2]);
    node1.putDataDir("dir-1", RawPath.valueOf("data"));
    node2.putDataDir("dir-2", RawPath.valueOf("data"));

    assertClusterValidationFails("Data directory names need to match across the cluster, but found the following mismatches: [[dir-2, main], [dir-1, main]]. Mutative operations on data dirs must be done simultaneously on every node in the cluster", newTestCluster(newTestStripe("stripe1").addNodes(node1, node2)));
  }

  @Test
  public void testSetSameBackupPath_ok() {
    Node node1 = newTestNode("node1", "localhost1");
    Node node2 = newTestNode("node2", "localhost2", Testing.N_UIDS[2]);
    node1.setBackupDir(RawPath.valueOf("backup"));
    node2.setBackupDir(RawPath.valueOf("backup"));
    new ClusterValidator(newTestCluster(newTestStripe("stripe1").addNodes(node1), newTestStripe("stripe2", Testing.S_UIDS[2]).addNodes(node2))).validate();
  }

  @Test
  public void testSetDifferentBackupPaths_ok() {
    Node node1 = newTestNode("node1", "localhost1");
    Node node2 = newTestNode("node2", "localhost2", Testing.N_UIDS[2]);
    node1.setBackupDir(RawPath.valueOf("backup-1"));
    node2.setBackupDir(RawPath.valueOf("backup-2"));
    new ClusterValidator(newTestCluster(newTestStripe("stripe1").addNodes(node1), newTestStripe("stripe2", Testing.S_UIDS[2]).addNodes(node2))).validate();
  }

  @Test
  public void testSetBackupOnOneStripeOnly_fail() {
    Node node1 = newTestNode("foo", "localhost1");
    Node node2 = newTestNode("node2", "localhost2", Testing.N_UIDS[2]);
    node1.setBackupDir(RawPath.valueOf("backup"));

    assertClusterValidationFails(
        "Nodes: [foo] currently have (or will have) backup directories defined, while some nodes in the cluster do not (or will not). Within a cluster, all nodes must have a backup directory defined or no backup directory defined.",
        newTestCluster(newTestStripe("stripe1").addNodes(node1, node2)));
  }

  @Test
  public void testValidCluster() {
    Node[] nodes = Stream.of(
        newTestNode("node1", "localhost1"),
        newTestNode("node2", "localhost2", Testing.N_UIDS[2])
    ).map(node -> node
        .setSecurityAuditLogDir(RawPath.valueOf("audit-" + random.nextInt()))
        .setSecurityDir(RawPath.valueOf("security-root" + random.nextInt()))
        .putDataDir("dir-1", RawPath.valueOf("some-path" + random.nextInt()))
        .setBackupDir(RawPath.valueOf("backup-" + random.nextInt()))
        .setMetadataDir(RawPath.valueOf("metadata-" + random.nextInt()))
        .setLogDir(RawPath.valueOf("logs-" + random.nextInt()))
        .setName("-" + random.nextInt())
        .setHostname("host-" + random.nextInt())
        .setPort(1 + random.nextInt(65500))
        .setGroupPort(1 + random.nextInt(65500))
        .setBindAddress(generateAddress())
        .setGroupBindAddress(generateAddress())
    ).toArray(Node[]::new);
    Cluster cluster = newTestCluster(newTestStripe("stripe1").addNodes(nodes))
        .setSecurityAuthc("file")
        .setSecuritySslTls(true)
        .setSecurityWhitelist(false)
        .putOffheapResource("main", 1L, GB)
        .setFailoverPriority(consistency())
        .setClientReconnectWindow(100L, SECONDS)
        .setClientLeaseDuration(100L, SECONDS);
    new ClusterValidator(cluster).validate();
  }

  @Test
  public void testGoodSecurity_1() {
    Node node1 = newTestNode("node1", "localhost1").setSecurityDir(RawPath.valueOf("security-dir")).setSecurityAuditLogDir(RawPath.valueOf("security-audit-dir"));
    Node node2 = newTestNode("node2", "localhost2", Testing.N_UIDS[2]).setSecurityDir(RawPath.valueOf("security-dir")).setSecurityAuditLogDir(RawPath.valueOf("security-audit-dir"));

    Cluster cluster = newTestCluster(newTestStripe("stripe1").addNodes(node1, node2)).setSecuritySslTls(false).setSecurityWhitelist(true);
    new ClusterValidator(cluster).validate();
  }

  @Test
  public void testGoodSecurity_2() {
    Node node1 = newTestNode("node1", "localhost1").setSecurityDir(RawPath.valueOf("security-dir")).setSecurityAuditLogDir(RawPath.valueOf("security-audit-dir"));
    Node node2 = newTestNode("node2", "localhost2", Testing.N_UIDS[2]).setSecurityDir(RawPath.valueOf("security-dir")).setSecurityAuditLogDir(RawPath.valueOf("security-audit-dir"));

    Cluster cluster = newTestCluster(newTestStripe("stripe1").addNodes(node1, node2)).setSecurityWhitelist(true);
    new ClusterValidator(cluster).validate();
  }

  @Test
  public void testGoodSecurity_3() {
    Node node1 = newTestNode("node1", "localhost1").setSecurityDir(RawPath.valueOf("security-root-dir"));
    Node node2 = newTestNode("node2", "localhost2", Testing.N_UIDS[2]).setSecurityDir(RawPath.valueOf("security-root-dir"));
    Cluster cluster = newTestCluster(newTestStripe("stripe1").addNodes(node1, node2)).setSecurityAuthc("file");
    new ClusterValidator(cluster).validate();
  }

  @Test
  public void testGoodSecurity_4() {
    Node node1 = newTestNode("node1", "localhost1").setSecurityDir(RawPath.valueOf("security-root-dir"));
    Node node2 = newTestNode("node2", "localhost2", Testing.N_UIDS[2]).setSecurityDir(RawPath.valueOf("security-root-dir"));

    Cluster cluster = newTestCluster(newTestStripe("stripe1").addNodes(node1, node2)).setSecuritySslTls(true).setSecurityAuthc("certificate");
    new ClusterValidator(cluster).validate();
  }

  @Test
  public void testGoodSecurity_5() {
    Node node1 = newTestNode("node1", "localhost1").setSecurityDir(RawPath.valueOf("security-root-dir")).setSecurityAuditLogDir(RawPath.valueOf("security-audit-dir"));
    Node node2 = newTestNode("node2", "localhost2", Testing.N_UIDS[2]).setSecurityDir(RawPath.valueOf("security-root-dir")).setSecurityAuditLogDir(RawPath.valueOf("security-audit-dir"));

    Cluster cluster = newTestCluster(newTestStripe("stripe1").addNodes(node1, node2))
        .setSecuritySslTls(true)
        .setSecurityAuthc("certificate")
        .setSecurityWhitelist(true);
    new ClusterValidator(cluster).validate();
  }

  @Test
  public void testBadSecurity_notAllNodesHaveSecurityDir() {
    Node node1 = newTestNode("node1", "localhost1").setSecurityDir(RawPath.valueOf("security-dir"));
    Node node2 = newTestNode("node2", "localhost2");
    Cluster cluster = newTestCluster(newTestStripe("stripe1").addNodes(node1, node2));

    assertClusterValidationFailsContainsMessage(securityDirError, cluster);
  }

  @Test
  public void testBadSecurity_authcWithoutSslTlsWithoutSecurityDir() {
    Node node = newTestNode("node1", "localhost1");
    Cluster cluster = newTestCluster(newTestStripe("stripe1").addNodes(node))
        .setSecuritySslTls(false)
        .setSecurityAuthc("certificate");

    assertClusterValidationFailsContainsMessage(securityDisallowedError, cluster);
  }

  @Test
  public void testBadSecurity_authcWithoutSslTlsWithSecurityDir() {
    Node node = newTestNode("node1", "localhost1").setSecurityDir(RawPath.valueOf("security-dir"));
    Cluster cluster = newTestCluster(newTestStripe("stripe1").addNodes(node))
            .setSecuritySslTls(false)
            .setSecurityAuthc("certificate");

    assertClusterValidationFailsContainsMessage(certificateSslTlsError, cluster);
  }

  @Test
  public void testBadSecurity_sslTlsAuthcWithoutSecurityDir() {
    Node node = newTestNode("node1", "localhost1");
    Cluster cluster = newTestCluster(newTestStripe("stripe1").addNodes(node))
        .setSecuritySslTls(true)
        .setSecurityAuthc("certificate");

    assertClusterValidationFailsContainsMessage(securityDisallowedError, cluster);
  }

  @Test
  public void testBadSecurity_sslTlsWithoutSecurityDir() {
    Node node = newTestNode("node1", "localhost1");
    Cluster cluster = newTestCluster(newTestStripe("stripe1").addNodes(node)).setSecuritySslTls(true);

    assertClusterValidationFailsContainsMessage(securityDisallowedError, cluster);
  }

  @Test
  public void testBadSecurity_authcWithoutSecurityDir() {
    Node node = newTestNode("node1", "localhost1");
    Cluster cluster = newTestCluster(newTestStripe("stripe1").addNodes(node)).setSecurityAuthc("file");

    assertClusterValidationFailsContainsMessage(securityDisallowedError, cluster);
  }

  @Test
  public void testBadSecurity_auditLogDirWithoutSecurityDir() {
    Node node = newTestNode("node1", "localhost1").setSecurityAuditLogDir(RawPath.valueOf("."));
    Cluster cluster = newTestCluster(newTestStripe("stripe1").addNodes(node));

    assertClusterValidationFailsContainsMessage(auditLogDirDisallowedError, cluster);
  }

  @Test
  public void testBadSecurity_whitelistWithoutSecurityDir() {
    Node node = newTestNode("node1", "localhost1");
    Cluster cluster = newTestCluster(newTestStripe("stripe1").addNodes(node)).setSecurityWhitelist(true);

    assertClusterValidationFailsContainsMessage(securityDisallowedError, cluster);
  }

  @Test
  public void testBadSecurity_securityDirWithoutSecurity() {
    Node node = newTestNode("node1", "localhost1").setSecurityDir(RawPath.valueOf("security-dir"));
    Cluster cluster = newTestCluster(newTestStripe("stripe1").addNodes(node));

    assertClusterValidationFailsContainsMessage(minimumSecurityError, cluster);
  }

  @Test
  public void testBadSecurity_notAllNodesHaveAuditLogDirWithSecurityDir() {
    Node node1 = newTestNode("node1", "localhost1").setSecurityDir(RawPath.valueOf("security-dir")).setSecurityAuditLogDir(RawPath.valueOf("audit"));
    Node node2 = newTestNode("node2", "localhost2", Testing.N_UIDS[2]).setSecurityDir(RawPath.valueOf("security-dir"));
    Cluster cluster = newTestCluster(newTestStripe("stripe1").addNodes(node1, node2)).setSecurityWhitelist(true);

    assertClusterValidationFailsContainsMessage(auditLogDirError, cluster);
  }

  @Test
  public void testBadSecurity_notAllNodesHaveAuditLogDirWithoutSecurityDir() {
    Node node1 = newTestNode("node1", "localhost1").setSecurityAuditLogDir(RawPath.valueOf("audit"));
    Node node2 = newTestNode("node2", "localhost2", Testing.N_UIDS[2]);
    Cluster cluster = newTestCluster(newTestStripe("stripe1").addNodes(node1, node2));

    assertClusterValidationFailsContainsMessage(auditLogDirDisallowedError, cluster);
  }

  private String generateAddress() {
    return random.nextInt(256) + "." + random.nextInt(256) + "." + random.nextInt(256) + "." + random.nextInt(256);
  }

  private void assertClusterValidationFails(String message, Cluster cluster) {
    assertThat(() -> new ClusterValidator(cluster).validate(), is(throwing(instanceOf(MalformedClusterException.class)).andMessage(is(equalTo(message)))));
  }

  private void assertClusterValidationFailsContainsMessage(String message, Cluster cluster) {
    assertThat(() -> new ClusterValidator(cluster).validate(), is(throwing(instanceOf(MalformedClusterException.class)).andMessage(is(containsString(message)))));
  }
}