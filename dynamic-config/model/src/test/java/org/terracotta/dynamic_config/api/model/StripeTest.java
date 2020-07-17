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

import java.nio.file.Paths;

import static java.net.InetSocketAddress.createUnresolved;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.terracotta.testing.ExceptionMatcher.throwing;

/**
 * @author Mathieu Carbou
 */
public class StripeTest {

  Node node1 = Node.newDefaultNode("node1", "localhost", 9410)
      .setDataDir("data", Paths.get("data"))
      .setNodeBackupDir(Paths.get("backup"))
      .setNodeBindAddress("0.0.0.0")
      .setNodeGroupBindAddress("0.0.0.0")
      .setNodeGroupPort(9430)
      .setNodeLogDir(Paths.get("log"))
      .setNodeMetadataDir(Paths.get("metadata"))
      .setSecurityAuditLogDir(Paths.get("audit"));

  Node node2 = Node.newDefaultNode("node2", "localhost", 9411)
      .setDataDir("data", Paths.get("/data/cache2"));

  Stripe stripe = new Stripe(node1);

  @Test
  public void test_containsNode() {
    assertTrue(stripe.containsNode(createUnresolved("localhost", 9410)));
    assertFalse(stripe.containsNode(createUnresolved("127.0.0.1", 9410)));
  }

  @Test
  public void test_clone() {
    assertThat(new Stripe(), is(equalTo(new Stripe().clone())));
    assertThat(stripe, is(equalTo(stripe.clone())));
  }

  @Test
  public void test_removeNode() {
    assertFalse(stripe.removeNode(createUnresolved("127.0.0.1", 9410)));
    assertTrue(stripe.removeNode(createUnresolved("localhost", 9410)));
    assertFalse(stripe.containsNode(createUnresolved("localhost", 9410)));
  }

  @Test
  public void test_isEmpty() {
    assertFalse(stripe.isEmpty());
    stripe.removeNode(createUnresolved("localhost", 9410));
    assertTrue(stripe.isEmpty());
  }

  @Test
  public void test_getNodeCount() {
    assertThat(stripe.getNodeCount(), is(equalTo(1)));
  }

  @Test
  public void test_getNode() {
    assertThat(stripe.getNode("node1").get(), is(equalTo(node1)));
    assertThat(stripe.getNode("foo").isPresent(), is(false));

    assertThat(stripe.getNode(node1.getNodeAddress()).get(), is(equalTo(node1)));
    assertThat(stripe.getNode(node2.getNodeAddress()).isPresent(), is(false));
  }

  @Test
  public void test_getNodeId() {
    assertThat(stripe.getNodeId("node1").getAsInt(), is(equalTo(1)));
    assertThat(stripe.getNodeId("foo").isPresent(), is(false));

    assertThat(stripe.getNodeId(node1.getNodeAddress()).getAsInt(), is(equalTo(1)));
    assertThat(stripe.getNodeId(node2.getNodeAddress()).isPresent(), is(false));
  }

  @Test
  public void test_getSingleNode() {
    assertThat(stripe.getSingleNode().get(), is(sameInstance(node1)));

    stripe.addNode(node2);
    assertThat(() -> stripe.getSingleNode(), is(throwing(instanceOf(IllegalStateException.class))));

    // back to normal
    stripe.removeNode(node2.getNodeAddress());
    assertThat(stripe.getSingleNode().get(), is(sameInstance(node1)));

    // empty
    stripe.removeNode(node1.getNodeAddress());
    assertThat(stripe.getSingleNode().isPresent(), is(false));
  }
}