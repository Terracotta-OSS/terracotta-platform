/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.model;

import com.terracottatech.utilities.Json;
import com.terracottatech.utilities.MemoryUnit;
import com.terracottatech.utilities.TimeUnit;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.nio.file.Paths;

import static com.terracottatech.utilities.hamcrest.ExceptionMatcher.throwing;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author Mathieu Carbou
 */
public class ClusterTest {

  private final Node node1 = new Node()
      .setClientLeaseDuration(1, TimeUnit.SECONDS)
      .setClientReconnectWindow(2, TimeUnit.MINUTES)
      .setClusterName("c")
      .setDataDir("data", Paths.get("data"))
      .setFailoverPriority("availability")
      .setNodeBackupDir(Paths.get("backup"))
      .setNodeBindAddress("0.0.0.0")
      .setNodeConfigDir(Paths.get("cfg"))
      .setNodeGroupBindAddress("0.0.0.0")
      .setNodeGroupPort(9430)
      .setNodeHostname("localhost")
      .setNodeLogDir(Paths.get("log"))
      .setNodeMetadataDir(Paths.get("metadata"))
      .setNodeName("node1")
      .setNodePort(9410)
      .setOffheapResource("off", 2, MemoryUnit.GB)
      .setSecurityAuditLogDir(Paths.get("audit"))
      .setSecurityAuthc("ldap")
      .setSecuritySslTls(true)
      .setSecurityWhitelist(true);

  Node node2 = new Node()
      .setNodeHostname("localhost")
      .setNodePort(9411)
      .setNodeName("node2")
      .setOffheapResource("foo", 1, MemoryUnit.GB)
      .setOffheapResource("bar", 1, MemoryUnit.GB)
      .setDataDir("data", Paths.get("/data/cache2"))
      .setNodeConfigDir(Paths.get("/config/node2"));

  Stripe stripe1 = new Stripe(node1);
  Cluster cluster = new Cluster(stripe1);

  @Test
  public void test_isEmpty() {
    assertFalse(cluster.isEmpty());
    assertTrue(new Cluster().isEmpty());
    assertTrue(new Cluster(new Stripe()).isEmpty());
  }

  @Test
  public void test_getStripe() {
    assertThat(cluster.getStripe(InetSocketAddress.createUnresolved("localhost", 9410)).get(), is(equalTo(stripe1)));
    assertFalse(cluster.getStripe(InetSocketAddress.createUnresolved("127.0.0.1", 9410)).isPresent());
  }

  @Test
  public void test_getNodeAddresses() {
    assertThat(cluster.getNodeAddresses(), hasSize(1));
    assertThat(cluster.getNodeAddresses(), contains(InetSocketAddress.createUnresolved("localhost", 9410)));
  }

  @Test
  public void test_containsNode() {
    assertTrue(cluster.containsNode(InetSocketAddress.createUnresolved("localhost", 9410)));
    assertFalse(cluster.containsNode(InetSocketAddress.createUnresolved("127.0.0.1", 9410)));
  }

  @Test
  public void test_clone() {
    assertThat(cluster.clone(), is(equalTo(cluster)));
    assertThat(Json.toJsonTree(cluster.clone()), is(equalTo(Json.toJsonTree(cluster))));
  }

  @Test
  public void test_getNode() {
    assertThat(cluster.getNode(InetSocketAddress.createUnresolved("localhost", 9410)).get(), is(equalTo(node1)));
    assertFalse(cluster.getNode(InetSocketAddress.createUnresolved("127.0.0.1", 9410)).isPresent());
  }

  @Test
  public void test_detach_node() {
    cluster.detachNode(InetSocketAddress.createUnresolved("localhost", 9410));
    assertTrue(cluster.isEmpty());
    assertThat(cluster.getStripes(), hasSize(0));
  }

  @Test
  public void test_detach_stripe() {
    cluster.detachStripe(stripe1);
    assertTrue(cluster.isEmpty());
    assertThat(cluster.getStripes(), hasSize(0));
  }

  @Test
  public void test_attach() {
    assertThat(
        () -> new Cluster().attachStripe(new Stripe(new Node().setNodeHostname("localhost").setNodePort(9410))),
        is(throwing(instanceOf(IllegalStateException.class)).andMessage(is(equalTo("Empty cluster.")))));

    assertThat(
        () -> new Cluster(new Stripe()).attachStripe(new Stripe(new Node().setNodeHostname("localhost").setNodePort(9410))),
        is(throwing(instanceOf(IllegalStateException.class)).andMessage(is(equalTo("Empty cluster.")))));

    assertThat(
        () -> cluster.attachStripe(new Stripe(new Node().setNodeHostname("localhost").setNodePort(9410))),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Nodes are already in the cluster: localhost:9410.")))));

    cluster.attachStripe(new Stripe(node2));

    assertThat(cluster.getStripes(), hasSize(2));
    assertTrue(cluster.containsNode(InetSocketAddress.createUnresolved("localhost", 9411)));
    Node node2 = cluster.getNode(InetSocketAddress.createUnresolved("localhost", 9411)).get();
    assertThat(node2.getOffheapResources(), hasKey("off"));
    assertThat(node2.getOffheapResources(), not(hasKey("foo")));
    assertThat(node2.getOffheapResources(), not(hasKey("bar")));
  }
}