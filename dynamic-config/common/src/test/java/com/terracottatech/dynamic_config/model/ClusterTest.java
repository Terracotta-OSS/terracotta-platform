/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.model;

import com.terracottatech.dynamic_config.util.Props;
import com.terracottatech.utilities.Json;
import com.terracottatech.utilities.MemoryUnit;
import com.terracottatech.utilities.TimeUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.net.InetSocketAddress;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import static com.terracottatech.dynamic_config.model.FailoverPriority.availability;
import static com.terracottatech.dynamic_config.model.FailoverPriority.consistency;
import static com.terracottatech.dynamic_config.model.Node.newDefaultNode;
import static com.terracottatech.utilities.Tuple2.tuple2;
import static com.terracottatech.utilities.fn.Fn.rethrow;
import static com.terracottatech.utilities.hamcrest.ExceptionMatcher.throwing;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;

/**
 * @author Mathieu Carbou
 */
@RunWith(MockitoJUnitRunner.class)
public class ClusterTest {

  @Mock BiConsumer<Integer, Node> consumer;

  private final Node node1 = new Node()
      .setClientLeaseDuration(1, TimeUnit.SECONDS)
      .setClientReconnectWindow(2, TimeUnit.MINUTES)
      .setDataDir("data", Paths.get("data"))
      .setFailoverPriority(availability())
      .setNodeBackupDir(Paths.get("backup"))
      .setNodeBindAddress("0.0.0.0")
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
      .setDataDir("data", Paths.get("/data/cache2"));

  Stripe stripe1 = new Stripe(node1);
  Cluster cluster = new Cluster("c", stripe1);

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

    assertThat(cluster.getNode(1, "node1").get(), is(equalTo(node1)));
    assertFalse(cluster.getNode(2, "node-1").isPresent());
    assertThat(
        () -> cluster.getNode(0, "node1"),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Invalid stripe ID: 0")))));

    assertThat(cluster.getNode(1, 1).get(), is(equalTo(node1)));
    assertFalse(cluster.getNode(2, 1).isPresent());
    assertFalse(cluster.getNode(1, 2).isPresent());
    assertFalse(cluster.getNode(2, 2).isPresent());
    assertThat(
        () -> cluster.getNode(0, 1),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Invalid stripe ID: 0")))));
    assertThat(
        () -> cluster.getNode(1, 0),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Invalid node ID: 0")))));
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

  @Test
  public void test_getSingleNode() {
    assertThat(cluster.getSingleNode().get(), is(sameInstance(node1)));

    stripe1.attachNode(node2);
    assertThat(() -> cluster.getSingleNode(), is(throwing(instanceOf(IllegalStateException.class))));

    // back to normal
    stripe1.detachNode(node2.getNodeAddress());
    assertThat(cluster.getSingleNode().get(), is(sameInstance(node1)));

    cluster.attachStripe(new Stripe(node2));
    assertThat(() -> cluster.getSingleNode(), is(throwing(instanceOf(IllegalStateException.class))));

    // back to normal
    cluster.detachNode(node2.getNodeAddress());
    assertThat(cluster.getSingleNode().get(), is(sameInstance(node1)));

    // empty
    stripe1.detachNode(node1.getNodeAddress());
    assertThat(cluster.getSingleNode().isPresent(), is(false));
  }

  @Test
  public void test_getSingleStripe() {
    assertThat(cluster.getSingleStripe().get(), is(sameInstance(stripe1)));

    Stripe stripe2 = new Stripe(node2);
    cluster.attachStripe(stripe2);
    assertThat(() -> cluster.getSingleStripe(), is(throwing(instanceOf(IllegalStateException.class))));

    // back to normal
    cluster.detachStripe(cluster.getStripes().get(1));
    assertThat(cluster.getSingleStripe().get(), is(sameInstance(stripe1)));

    // empty
    cluster.detachStripe(cluster.getStripes().get(0));
    assertThat(cluster.getSingleStripe().isPresent(), is(false));
  }

  @Test
  public void test_getStripeId() {
    assertThat(cluster.getStripeId(node1.getNodeAddress()).getAsInt(), is(equalTo(1)));
    assertThat(cluster.getStripeId(node2.getNodeAddress()).isPresent(), is(false));

    cluster.attachStripe(new Stripe(node2));
    assertThat(cluster.getStripeId(node2.getNodeAddress()).getAsInt(), is(2));
  }

  @Test
  public void test_getNodeId() {
    assertThat(cluster.getNodeId(node1.getNodeAddress()).getAsInt(), is(equalTo(1)));
    assertThat(cluster.getNodeId(node2.getNodeAddress()).isPresent(), is(false));

    cluster.attachStripe(new Stripe(node2));
    assertThat(cluster.getNodeId(node2.getNodeAddress()).getAsInt(), is(1));

    assertThat(cluster.getNodeId(1, "node1").getAsInt(), is(1));
    assertThat(cluster.getNodeId(1, "node-foo").isPresent(), is(false));
    assertThat(cluster.getNodeId(10, "node-foo").isPresent(), is(false));
    assertThat(
        () -> cluster.getNodeId(0, "node-foo"),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Invalid stripe ID: 0")))));
  }

  @Test
  public void test_forEach() {
    cluster.attachStripe(new Stripe(node2));
    Node node1 = cluster.getNode(1, 1).get();
    Node node2 = cluster.getNode(2, 1).get();

    cluster.forEach(consumer);

    verify(consumer).accept(1, node1);
    verify(consumer).accept(2, node2);
  }

  @Test
  public void test_getStripeCount() {
    assertThat(cluster.getStripeCount(), is(equalTo(1)));
  }

  @Test
  public void test_getNodeCount() {
    assertThat(cluster.getNodeCount(), is(equalTo(1)));
  }

  @Test
  public void test_toProperties() {
    Cluster cluster = new Cluster("my-cluster", new Stripe(
        newDefaultNode("node-1", "localhost")
            .setFailoverPriority(consistency(2))
            .setOffheapResource("foo", 1, MemoryUnit.GB)
            .setOffheapResource("bar", 2, MemoryUnit.GB)
            .setDataDir("foo", Paths.get("%H/tc1/foo"))
            .setDataDir("bar", Paths.get("%H/tc1/bar")),
        newDefaultNode("node-2", "localhost")
            .setFailoverPriority(consistency(2))
            .setOffheapResource("foo", 1, MemoryUnit.GB)
            .setOffheapResource("bar", 2, MemoryUnit.GB)
            .setDataDir("foo", Paths.get("%H/tc2/foo"))
            .setDataDir("bar", Paths.get("%H/tc2/bar"))
            .setTcProperty("server.entity.processor.threads", "64")
            .setTcProperty("topology.validate", "true")
    ));

    Stream.of(
        tuple2(cluster.toProperties(), "config_default.properties"),
        tuple2(cluster.toProperties(false, true), "config_default.properties"),
        tuple2(cluster.toProperties(false, false), "config_without_default.properties"),
        tuple2(cluster.toProperties(true, true), "config_expanded_default.properties"),
        tuple2(cluster.toProperties(true, false), "config_expanded_without_default.properties")
    ).forEach(rethrow(tuple -> {
      Properties expected = fixPaths(Props.load(Paths.get(getClass().getResource("/config-property-files/" + tuple.t2).toURI())));
      assertThat("File: " + tuple.t2, tuple.t1, is(equalTo(expected)));
    }));
  }

  private Properties fixPaths(Properties props) {
    if (File.separatorChar == '\\') {
      props.entrySet().forEach(e -> e.setValue(e.getValue().toString().replace('/', '\\')));
    }
    return props;
  }
}