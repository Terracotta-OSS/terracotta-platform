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
package org.terracotta.dynamic_config.api.model;

import org.junit.Test;
import org.terracotta.common.struct.MemoryUnit;

import java.net.InetSocketAddress;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.terracotta.dynamic_config.api.model.FailoverPriority.consistency;
import static org.terracotta.testing.ExceptionMatcher.throwing;

/**
 * @author Mathieu Carbou
 */
public class NodeContextTest {

  Node node1 = Testing.newTestNode("node1", "localhost", 9410)
      .putDataDir("foo", Paths.get("%H/tc1/foo"))
      .putDataDir("bar", Paths.get("%H/tc1/bar"));

  Node node2 = Testing.newTestNode("node2", "localhost", 9411)
      .putDataDir("foo", Paths.get("%H/tc2/foo"))
      .putDataDir("bar", Paths.get("%H/tc2/bar"))
      .putTcProperty("server.entity.processor.threads", "64")
      .putTcProperty("topology.validate", "true");

  Cluster cluster = Testing.newTestCluster("my-cluster", new Stripe(node1), new Stripe(node2))
      .setFailoverPriority(consistency(2))
      .putOffheapResource("foo", 1, MemoryUnit.GB)
      .putOffheapResource("bar", 2, MemoryUnit.GB);

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
    assertThat(new NodeContext(cluster, node1.getAddress()).getCluster(), is(equalTo(cluster)));
    assertThat(nodeContext(node1).getCluster().getSingleNode().get(), is(equalTo(node1)));
  }

  @Test
  public void test_getStripeId() {
    assertThat(new NodeContext(cluster, 1, "node1").getStripeId(), is(equalTo(1)));
    assertThat(new NodeContext(cluster, 1, 1).getStripeId(), is(equalTo(1)));
    assertThat(new NodeContext(cluster, node2.getAddress()).getStripeId(), is(equalTo(2)));
    assertThat(nodeContext(node2).getStripeId(), is(equalTo(1)));
  }

  @Test
  public void test_getNodeId() {
    assertThat(new NodeContext(cluster, 1, "node1").getStripeId(), is(equalTo(1)));
    assertThat(new NodeContext(cluster, 1, 1).getStripeId(), is(equalTo(1)));
    assertThat(new NodeContext(cluster, node2.getAddress()).getStripeId(), is(equalTo(2)));
    assertThat(nodeContext(node2).getStripeId(), is(equalTo(1)));
  }

  @Test
  public void test_getNodeName() {
    assertThat(new NodeContext(cluster, 1, "node1").getNodeName(), is(equalTo("node1")));
    assertThat(new NodeContext(cluster, 1, 1).getNodeName(), is(equalTo("node1")));
    assertThat(new NodeContext(cluster, node2.getAddress()).getNodeName(), is(equalTo("node2")));
    assertThat(nodeContext(node2).getNodeName(), is(equalTo("node2")));
  }

  @Test
  public void test_getNode() {
    assertThat(new NodeContext(cluster, 1, "node1").getNode(), is(equalTo(node1)));
    assertThat(new NodeContext(cluster, 1, 1).getNode(), is(equalTo(node1)));
    assertThat(new NodeContext(cluster, node2.getAddress()).getNode(), is(equalTo(node2)));
    assertThat(nodeContext(node2).getNode(), is(equalTo(node2)));
  }

  @Test
  public void test_clone() {
    Stream.of(
        new NodeContext(cluster, 1, "node1"),
        new NodeContext(cluster, 1, 1),
        new NodeContext(cluster, node2.getAddress()),
        nodeContext(node2)
    ).forEach(ctx -> assertThat(ctx.clone(), is(equalTo(ctx))));
  }

  @Test
  public void test_hashCode() {
    Stream.of(
        new NodeContext(cluster, 1, "node1"),
        new NodeContext(cluster, 1, 1),
        new NodeContext(cluster, node2.getAddress()),
        nodeContext(node2)
    ).forEach(ctx -> assertThat(ctx.clone().hashCode(), is(equalTo(ctx.hashCode()))));
  }

  private static NodeContext nodeContext(Node node) {
    return new NodeContext(Testing.newTestCluster(new Stripe(node)), 1, node.getName());
  }
}