/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.validation;

import com.terracottatech.dynamic_config.config.Cluster;
import com.terracottatech.dynamic_config.config.Node;
import com.terracottatech.dynamic_config.config.Stripe;
import com.terracottatech.dynamic_config.exception.MalformedClusterConfigException;
import org.junit.Test;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static com.terracottatech.utilities.MemoryUnit.GB;
import static com.terracottatech.utilities.MemoryUnit.MB;
import static com.terracottatech.utilities.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;

public class ClusterConfigValidatorTest {
  @Test
  public void testMultipleClusterNames() {
    Node node1 = new Node();
    Node node2 = new Node();
    node1.setClusterName("tc-one");
    node2.setClusterName("tc-two");

    try {
      ClusterConfigValidator.validate(createCluster(node1, node2));
      failBecauseExceptionWasNotThrown(MalformedClusterConfigException.class);
    } catch (Exception e) {
      assertThat(e.getClass()).isEqualTo(MalformedClusterConfigException.class);
      assertThat(e.getMessage()).startsWith("All the nodes should belong to the same cluster");
    }
  }

  @Test
  public void testDifferingClientLeaseDurations() {
    Node node1 = new Node();
    Node node2 = new Node();
    node1.setClientLeaseDuration(10L, SECONDS);
    node2.setClientLeaseDuration(100L, SECONDS);

    try {
      ClusterConfigValidator.validate(createCluster(node1, node2));
      failBecauseExceptionWasNotThrown(MalformedClusterConfigException.class);
    } catch (Exception e) {
      assertThat(e.getClass()).isEqualTo(MalformedClusterConfigException.class);
      assertThat(e.getMessage()).startsWith("Client lease duration of all nodes should match");
    }
  }

  @Test
  public void testDifferingClientReconnectWindows() {
    Node node1 = new Node();
    Node node2 = new Node();
    node1.setClientReconnectWindow(10L, SECONDS);
    node2.setClientReconnectWindow(100L, SECONDS);

    try {
      ClusterConfigValidator.validate(createCluster(node1, node2));
      failBecauseExceptionWasNotThrown(MalformedClusterConfigException.class);
    } catch (Exception e) {
      assertThat(e.getClass()).isEqualTo(MalformedClusterConfigException.class);
      assertThat(e.getMessage()).startsWith("Client reconnect window of all nodes should match");
    }
  }

  @Test
  public void testDifferingFailoverPriority() {
    Node node1 = new Node();
    Node node2 = new Node();
    node1.setFailoverPriority("availability");
    node2.setFailoverPriority("consistency");

    try {
      ClusterConfigValidator.validate(createCluster(node1, node2));
      failBecauseExceptionWasNotThrown(MalformedClusterConfigException.class);
    } catch (Exception e) {
      assertThat(e.getClass()).isEqualTo(MalformedClusterConfigException.class);
      assertThat(e.getMessage()).startsWith("Failover setting of all nodes should match");
    }
  }

  @Test
  public void testDifferingFailoverVoterCount() {
    Node node1 = new Node();
    Node node2 = new Node();
    node1.setFailoverPriority("consistency");
    node2.setFailoverPriority("consistency:2");

    try {
      ClusterConfigValidator.validate(createCluster(node1, node2));
      failBecauseExceptionWasNotThrown(MalformedClusterConfigException.class);
    } catch (Exception e) {
      assertThat(e.getClass()).isEqualTo(MalformedClusterConfigException.class);
      assertThat(e.getMessage()).startsWith("Failover setting of all nodes should match");
    }
  }

  @Test
  public void testDifferingDataDirectoryNames() {
    Node node1 = new Node();
    Node node2 = new Node();
    node1.setDataDir("dir-1", Paths.get("data"));
    node2.setDataDir("dir-2", Paths.get("data"));

    try {
      ClusterConfigValidator.validate(createCluster(node1, node2));
      failBecauseExceptionWasNotThrown(MalformedClusterConfigException.class);
    } catch (Exception e) {
      assertThat(e.getClass()).isEqualTo(MalformedClusterConfigException.class);
      assertThat(e.getMessage()).startsWith("Data directory names of all nodes should match");
    }
  }

  @Test
  public void testDifferingOffheapNames() {
    Node node1 = new Node();
    Node node2 = new Node();
    node1.setOffheapResource("main", 512L, MB);
    node1.setOffheapResource("other", 1L, GB);

    try {
      ClusterConfigValidator.validate(createCluster(node1, node2));
      failBecauseExceptionWasNotThrown(MalformedClusterConfigException.class);
    } catch (Exception e) {
      assertThat(e.getClass()).isEqualTo(MalformedClusterConfigException.class);
      assertThat(e.getMessage()).startsWith("Offheap resources of all nodes should match");
    }
  }

  @Test
  public void testDifferingOffheapNames_multipleOffheapResources() {
    Node node1 = new Node();
    Node node2 = new Node();
    node1.setOffheapResource("main", 1L, GB);
    node1.setOffheapResource("second", 2L, GB);
    node2.setOffheapResource("main", 1L, GB);
    node2.setOffheapResource("other", 2L, GB);

    try {
      ClusterConfigValidator.validate(createCluster(node1, node2));
      failBecauseExceptionWasNotThrown(MalformedClusterConfigException.class);
    } catch (Exception e) {
      assertThat(e.getClass()).isEqualTo(MalformedClusterConfigException.class);
      assertThat(e.getMessage()).startsWith("Offheap resources of all nodes should match");
    }
  }

  @Test
  public void testDifferingOffheapResources() {
    Node node1 = new Node();
    Node node2 = new Node();
    node1.setOffheapResource("main", 1L, GB);
    node1.setOffheapResource("second", 2L, GB);
    node2.setOffheapResource("main", 1L, GB);
    node2.setOffheapResource("other", 2L, GB);

    try {
      ClusterConfigValidator.validate(createCluster(node1, node2));
      failBecauseExceptionWasNotThrown(MalformedClusterConfigException.class);
    } catch (Exception e) {
      assertThat(e.getClass()).isEqualTo(MalformedClusterConfigException.class);
      assertThat(e.getMessage()).startsWith("Offheap resources of all nodes should match");
    }
  }

  @Test
  public void testDifferingOffheapQuantities() {
    Node node1 = new Node();
    Node node2 = new Node();
    node1.setOffheapResource("main", 1L, GB);
    node2.setOffheapResource("main", 2L, GB);

    try {
      ClusterConfigValidator.validate(createCluster(node1, node2));
      failBecauseExceptionWasNotThrown(MalformedClusterConfigException.class);
    } catch (Exception e) {
      assertThat(e.getClass()).isEqualTo(MalformedClusterConfigException.class);
      assertThat(e.getMessage()).startsWith("Offheap resources of all nodes should match");
    }
  }

  @Test
  public void testDifferingWhitelistSetting() {
    Node node1 = new Node();
    Node node2 = new Node();
    node1.setSecurityWhitelist(false);
    node1.setSecurityWhitelist(true);

    try {
      ClusterConfigValidator.validate(createCluster(node1, node2));
      failBecauseExceptionWasNotThrown(MalformedClusterConfigException.class);
    } catch (Exception e) {
      assertThat(e.getClass()).isEqualTo(MalformedClusterConfigException.class);
      assertThat(e.getMessage()).startsWith("Whitelist setting of all nodes should match");
    }
  }

  @Test
  public void testDifferingSslTlsSetting() {
    Node node1 = new Node();
    Node node2 = new Node();
    node1.setSecuritySslTls(false);
    node1.setSecuritySslTls(true);

    try {
      ClusterConfigValidator.validate(createCluster(node1, node2));
      failBecauseExceptionWasNotThrown(MalformedClusterConfigException.class);
    } catch (Exception e) {
      assertThat(e.getClass()).isEqualTo(MalformedClusterConfigException.class);
      assertThat(e.getMessage()).startsWith("SSL/TLS setting of all nodes should match");
    }
  }

  @Test
  public void testDifferingAuthcSetting() {
    Node node1 = new Node();
    Node node2 = new Node();
    node1.setSecurityAuthc("file");
    node1.setSecurityAuthc("ldap");

    try {
      ClusterConfigValidator.validate(createCluster(node1, node2));
      failBecauseExceptionWasNotThrown(MalformedClusterConfigException.class);
    } catch (Exception e) {
      assertThat(e.getClass()).isEqualTo(MalformedClusterConfigException.class);
      assertThat(e.getMessage()).startsWith("Authentication setting of all nodes should match");
    }
  }

  @Test
  public void testValidCluster() {
    Node node1 = new Node();
    Node node2 = new Node();
    setNodeProperties(node1);
    setNodeProperties(node2);

    ClusterConfigValidator.validate(createCluster(node1, node2));
  }

  private void setNodeProperties(Node node) {
    Random random = new Random();
    node.setSecurityAuthc("file");
    node.setSecuritySslTls(true);
    node.setSecurityWhitelist(false);
    node.setSecurityAuditLogDir(Paths.get("audit-" + random.nextInt()));
    node.setSecurityDir(Paths.get("security-root" + random.nextInt()));
    node.setOffheapResource("main", 1L, GB);
    node.setDataDir("dir-1", Paths.get("some-path" + random.nextInt()));
    node.setFailoverPriority("consistency");
    node.setClientReconnectWindow(100L, SECONDS);
    node.setClientLeaseDuration(100L, SECONDS);
    node.setNodeBackupDir(Paths.get("backup-" + random.nextInt()));
    node.setNodeMetadataDir(Paths.get("metadata-" + random.nextInt()));
    node.setNodeLogDir(Paths.get("logs-" + random.nextInt()));
    node.setNodeConfigDir(Paths.get("config-" + random.nextInt()));
    node.setNodeName("node-" + random.nextInt());
    node.setNodeHostname("host-" + random.nextInt());
    node.setNodePort(random.nextInt());
    node.setNodeGroupPort(random.nextInt());
    node.setNodeBindAddress(random.nextInt(256) + "." + random.nextInt(256) + "." + random.nextInt(256) + "." + random.nextInt(256));
    node.setNodeGroupBindAddress(random.nextInt(256) + "." + random.nextInt(256) + "." + random.nextInt(256) + "." + random.nextInt(256));
  }

  private Cluster createCluster(Node... nodes) {
    List<Stripe> stripes = new ArrayList<>();
    stripes.add(new Stripe(Arrays.asList(nodes)));
    return new Cluster(stripes);
  }
}