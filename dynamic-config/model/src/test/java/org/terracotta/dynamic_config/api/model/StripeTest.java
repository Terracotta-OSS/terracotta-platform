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
import org.terracotta.common.struct.TimeUnit;

import java.nio.file.Paths;

import static java.net.InetSocketAddress.createUnresolved;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.terracotta.dynamic_config.api.model.FailoverPriority.availability;
import static org.terracotta.testing.ExceptionMatcher.throwing;

/**
 * @author Mathieu Carbou
 */
public class StripeTest {

  Node node1 = Node.newDefaultNode("node1", "localhost", 9410)
      .setClientLeaseDuration(1, TimeUnit.SECONDS)
      .setClientReconnectWindow(2, TimeUnit.MINUTES)
      .setDataDir("data", Paths.get("data"))
      .setFailoverPriority(availability())
      .setNodeBackupDir(Paths.get("backup"))
      .setNodeBindAddress("0.0.0.0")
      .setNodeGroupBindAddress("0.0.0.0")
      .setNodeGroupPort(9430)
      .setNodeLogDir(Paths.get("log"))
      .setNodeMetadataDir(Paths.get("metadata"))
      .setOffheapResource("off", 2, MemoryUnit.GB)
      .setSecurityAuditLogDir(Paths.get("audit"))
      .setSecurityAuthc("ldap")
      .setSecuritySslTls(true)
      .setSecurityWhitelist(true);

  Node node2 = Node.newDefaultNode("node2", "localhost", 9411)
      .setOffheapResource("foo", 1, MemoryUnit.GB)
      .setOffheapResource("bar", 1, MemoryUnit.GB)
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
  public void test_detach() {
    assertFalse(stripe.detachNode(createUnresolved("127.0.0.1", 9410)));
    assertTrue(stripe.detachNode(createUnresolved("localhost", 9410)));
    assertFalse(stripe.containsNode(createUnresolved("localhost", 9410)));
  }

  @Test
  public void test_isEmpty() {
    assertFalse(stripe.isEmpty());
    stripe.detachNode(createUnresolved("localhost", 9410));
    assertTrue(stripe.isEmpty());
  }

  @Test
  public void test_attach() {
    assertThat(
        () -> stripe.attachNode(Node.newDefaultNode("localhost", 9410)),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Node localhost:9410 is already in the stripe.")))));

    assertThat(
        () -> new Stripe().attachNode(Node.newDefaultNode("localhost", 9410)),
        is(throwing(instanceOf(IllegalStateException.class)).andMessage(is(equalTo("Empty stripe.")))));

    // attaching a non-secured node to secured nodes
    node1.setSecurityDir(Paths.get("sec"));
    assertThat(
        () -> stripe.attachNode(node2),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Node localhost:9411 must be started with a security directory.")))));

    // attaching a secured node to a non-secured nodes
    node1.setSecurityDir(null);
    node2.setSecurityDir(Paths.get("sec"));
    stripe.attachNode(node2);
    assertThat(stripe.getNode(createUnresolved("localhost", 9411)).get().getSecurityDir(), is(nullValue()));
    stripe.detachNode(createUnresolved("localhost", 9411));

    node1.setDataDir("other", Paths.get("other"));
    assertThat(
        () -> stripe.attachNode(node2),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Node localhost:9411 must declare the following data directories: other.")))));
    node1.removeDataDir("other");

    node2.setDataDir("other", Paths.get("other"));
    assertThat(
        () -> stripe.attachNode(node2),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Node localhost:9411 must not declare the following data directories: other.")))));
    node2.removeDataDir("other");

    // attaching
    node1.setSecurityDir(Paths.get("Sec1"));
    node2.setSecurityDir(Paths.get("Sec2"));
    stripe.attachNode(node2);

    assertThat(node2.getOffheapResources(), hasKey("foo"));
    assertThat(node2.getOffheapResources(), hasKey("bar"));
    assertThat(node2.getOffheapResources(), not(hasKey("off")));
    assertThat(stripe.getNode(node2.getNodeAddress()).get().getOffheapResources(), hasKey("off"));
    assertThat(stripe.getNode(node2.getNodeAddress()).get().getOffheapResources(), not(hasKey("foo")));
    assertThat(stripe.getNode(node2.getNodeAddress()).get().getOffheapResources(), not(hasKey("bar")));
  }

  @Test
  public void test_cloneForAttachment() {
    Stripe newStripe = new Stripe(node2).cloneForAttachment(node1);
    Node newNode = newStripe.getNodes().iterator().next();
    assertThat(newNode.getOffheapResources(), hasKey("off"));
    assertThat(newNode.getOffheapResources(), not(hasKey("foo")));
    assertThat(newNode.getOffheapResources(), not(hasKey("bar")));
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

    stripe.attachNode(node2);
    assertThat(() -> stripe.getSingleNode(), is(throwing(instanceOf(IllegalStateException.class))));

    // back to normal
    stripe.detachNode(node2.getNodeAddress());
    assertThat(stripe.getSingleNode().get(), is(sameInstance(node1)));

    // empty
    stripe.detachNode(node1.getNodeAddress());
    assertThat(stripe.getSingleNode().isPresent(), is(false));
  }
}