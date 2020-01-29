/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.api.model;

import com.terracottatech.common.struct.MemoryUnit;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static com.terracottatech.dynamic_config.api.model.FailoverPriority.consistency;
import static com.terracottatech.dynamic_config.api.model.Node.newDefaultNode;
import static com.terracottatech.testing.ExceptionMatcher.throwing;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Mathieu Carbou
 */
public class NodeContextTest {

  Node node1 = newDefaultNode("node1", "localhost", 9410)
      .setFailoverPriority(consistency(2))
      .setOffheapResource("foo", 1, MemoryUnit.GB)
      .setOffheapResource("bar", 2, MemoryUnit.GB)
      .setDataDir("foo", Paths.get("%H/tc1/foo"))
      .setDataDir("bar", Paths.get("%H/tc1/bar"));

  Node node2 = newDefaultNode("node2", "localhost", 9411)
      .setFailoverPriority(consistency(2))
      .setOffheapResource("foo", 1, MemoryUnit.GB)
      .setOffheapResource("bar", 2, MemoryUnit.GB)
      .setDataDir("foo", Paths.get("%H/tc2/foo"))
      .setDataDir("bar", Paths.get("%H/tc2/bar"))
      .setTcProperty("server.entity.processor.threads", "64")
      .setTcProperty("topology.validate", "true");

  Cluster cluster = new Cluster("my-cluster", new Stripe(node1), new Stripe(node2));

  @Test
  public void test_ctors() {
    assertThat(
        () -> new NodeContext(cluster, 0, "node1"),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Invalid stripe ID: 0")))));
    assertThat(
        () -> new NodeContext(cluster, 1, "node2"),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Node node2 in stripe ID 1 not found")))));
    assertThat(
        () -> new NodeContext(cluster, 2, "node1"),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Node node1 in stripe ID 2 not found")))));
    assertThat(
        () -> new NodeContext(cluster, 3, "node1"),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Invalid stripe ID: 3")))));

    assertThat(
        () -> new NodeContext(cluster, 0, 1),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Invalid stripe ID: 0")))));
    assertThat(
        () -> new NodeContext(cluster, 1, 0),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Invalid node ID: 0")))));
    assertThat(
        () -> new NodeContext(cluster, 1, 2),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Node ID 2 in stripe ID 1 not found")))));
    assertThat(
        () -> new NodeContext(cluster, 1, 3),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Node ID 3 in stripe ID 1 not found")))));
    assertThat(
        () -> new NodeContext(cluster, 2, 2),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Node ID 2 in stripe ID 2 not found")))));
    assertThat(
        () -> new NodeContext(cluster, 3, 1),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Node ID 1 in stripe ID 3 not found")))));

    assertThat(
        () -> new NodeContext(cluster, InetSocketAddress.createUnresolved("foo", 9410)),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Node foo:9410 not found")))));
  }

  @Test
  public void test_getCluster() {
    assertThat(new NodeContext(cluster, 1, "node1").getCluster(), is(equalTo(cluster)));
    assertThat(new NodeContext(cluster, 1, 1).getCluster(), is(equalTo(cluster)));
    assertThat(new NodeContext(cluster, node1.getNodeAddress()).getCluster(), is(equalTo(cluster)));
    assertThat(new NodeContext(node1).getCluster().getSingleNode().get(), is(equalTo(node1)));
  }

  @Test
  public void test_getStripeId() {
    assertThat(new NodeContext(cluster, 1, "node1").getStripeId(), is(equalTo(1)));
    assertThat(new NodeContext(cluster, 1, 1).getStripeId(), is(equalTo(1)));
    assertThat(new NodeContext(cluster, node2.getNodeAddress()).getStripeId(), is(equalTo(2)));
    assertThat(new NodeContext(node2).getStripeId(), is(equalTo(1)));
  }

  @Test
  public void test_getNodeId() {
    assertThat(new NodeContext(cluster, 1, "node1").getStripeId(), is(equalTo(1)));
    assertThat(new NodeContext(cluster, 1, 1).getStripeId(), is(equalTo(1)));
    assertThat(new NodeContext(cluster, node2.getNodeAddress()).getStripeId(), is(equalTo(2)));
    assertThat(new NodeContext(node2).getStripeId(), is(equalTo(1)));
  }

  @Test
  public void test_getNodeName() {
    assertThat(new NodeContext(cluster, 1, "node1").getNodeName(), is(equalTo("node1")));
    assertThat(new NodeContext(cluster, 1, 1).getNodeName(), is(equalTo("node1")));
    assertThat(new NodeContext(cluster, node2.getNodeAddress()).getNodeName(), is(equalTo("node2")));
    assertThat(new NodeContext(node2).getNodeName(), is(equalTo("node2")));
  }

  @Test
  public void test_getNode() {
    assertThat(new NodeContext(cluster, 1, "node1").getNode(), is(equalTo(node1)));
    assertThat(new NodeContext(cluster, 1, 1).getNode(), is(equalTo(node1)));
    assertThat(new NodeContext(cluster, node2.getNodeAddress()).getNode(), is(equalTo(node2)));
    assertThat(new NodeContext(node2).getNode(), is(equalTo(node2)));
  }

  @Test
  public void test_clone() {
    Stream.of(
        new NodeContext(cluster, 1, "node1"),
        new NodeContext(cluster, 1, 1),
        new NodeContext(cluster, node2.getNodeAddress()),
        new NodeContext(node2)
    ).forEach(ctx -> assertThat(ctx.clone(), is(equalTo(ctx))));
  }

  @Test
  public void test_hashCode() {
    Stream.of(
        new NodeContext(cluster, 1, "node1"),
        new NodeContext(cluster, 1, 1),
        new NodeContext(cluster, node2.getNodeAddress()),
        new NodeContext(node2)
    ).forEach(ctx -> assertThat(ctx.clone().hashCode(), is(equalTo(ctx.hashCode()))));
  }
}