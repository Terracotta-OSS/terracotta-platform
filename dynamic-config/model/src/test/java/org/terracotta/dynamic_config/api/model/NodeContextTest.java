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

import org.junit.Before;
import org.junit.Test;
import org.terracotta.common.struct.MemoryUnit;
import org.terracotta.dynamic_config.api.service.ClusterValidator;

import java.util.stream.Stream;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.terracotta.dynamic_config.api.model.FailoverPriority.consistency;
import static org.terracotta.dynamic_config.api.model.Testing.N_UIDS;
import static org.terracotta.dynamic_config.api.model.Testing.S_UIDS;
import static org.terracotta.dynamic_config.api.model.Testing.newTestStripe;
import static org.terracotta.testing.ExceptionMatcher.throwing;

/**
 * @author Mathieu Carbou
 */
public class NodeContextTest {

  Node node1 = Testing.newTestNode("node1", "localhost", 9410)
      .setUID(N_UIDS[1])
      .putDataDir("foo", RawPath.valueOf("%H/tc1/foo"))
      .putDataDir("bar", RawPath.valueOf("%H/tc1/bar"));

  Node node2 = Testing.newTestNode("node2", "localhost", 9411)
      .setUID(N_UIDS[2])
      .putDataDir("foo", RawPath.valueOf("%H/tc2/foo"))
      .putDataDir("bar", RawPath.valueOf("%H/tc2/bar"))
      .putTcProperty("server.entity.processor.threads", "64")
      .putTcProperty("topology.validate", "true");

  Cluster cluster = Testing.newTestCluster("my-cluster",
      newTestStripe("stripe-1").setUID(S_UIDS[1]).addNodes(node1),
      newTestStripe("stripe-2").setUID(S_UIDS[2]).addNodes(node2))
      .setFailoverPriority(consistency(2))
      .putOffheapResource("foo", 1, MemoryUnit.GB)
      .putOffheapResource("bar", 2, MemoryUnit.GB);

  @Before
  public void setUp() throws Exception {
    new ClusterValidator(cluster).validate(ClusterState.ACTIVATED);
  }

  @Test
  public void test_ctors() {
    assertThat(
        () -> new NodeContext(cluster, Testing.N_UIDS[4]),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Node UID: x6tPuzj0Tq2Qs1niISqVMg not found in cluster: my-cluster ( stripe-1:5Zv3uphiRLavoGZthy7JNg ( node1@localhost:9410 ), stripe-2:RUHaurjcQA-57mpGqtovOA ( node2@localhost:9411 ) )")))));
  }

  @Test
  public void test_getCluster() {
    assertThat(new NodeContext(cluster, N_UIDS[1]).getCluster(), is(equalTo(cluster)));
    assertThat(new NodeContext(cluster, node1.getUID()).getCluster(), is(equalTo(cluster)));
    assertThat(nodeContext(node1).getCluster().getSingleNode().get(), is(equalTo(node1)));
  }

  @Test
  public void test_getNodeName() {
    assertThat(new NodeContext(cluster, N_UIDS[1]).getNode().getName(), is(equalTo("node1")));
    assertThat(new NodeContext(cluster, node2.getUID()).getNode().getName(), is(equalTo("node2")));
    assertThat(nodeContext(node2).getNode().getName(), is(equalTo("node2")));
  }

  @Test
  public void test_getNode() {
    assertThat(new NodeContext(cluster, N_UIDS[1]).getNode(), is(equalTo(node1)));
    assertThat(new NodeContext(cluster, node2.getUID()).getNode(), is(equalTo(node2)));
    assertThat(nodeContext(node2).getNode(), is(equalTo(node2)));
  }

  @Test
  public void test_clone() {
    Stream.of(
        new NodeContext(cluster, node1.getUID()),
        new NodeContext(cluster, node2.getUID()),
        nodeContext(node2)
    ).forEach(ctx -> assertThat(ctx.clone(), is(equalTo(ctx))));
  }

  @Test
  public void test_hashCode() {
    Stream.of(
        new NodeContext(cluster, N_UIDS[1]),
        new NodeContext(cluster, node2.getUID()),
        nodeContext(node2)
    ).forEach(ctx -> assertThat(ctx.clone().hashCode(), is(equalTo(ctx.hashCode()))));
  }

  private static NodeContext nodeContext(Node node) {
    return new NodeContext(Testing.newTestCluster(newTestStripe("stripe-1").addNodes(node)), node.getUID());
  }
}