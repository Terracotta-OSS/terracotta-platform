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

  Node node1 = Testing.newTestNode("node1", "localhost", 9410)
      .putDataDir("data", RawPath.valueOf("data"))
      .setBackupDir(RawPath.valueOf("backup"))
      .setBindAddress("0.0.0.0")
      .setGroupBindAddress("0.0.0.0")
      .setGroupPort(9430)
      .setLogDir(RawPath.valueOf("log"))
      .setMetadataDir(RawPath.valueOf("metadata"))
      .setSecurityAuditLogDir(RawPath.valueOf("audit"));

  Node node2 = Testing.newTestNode("node2", "localhost", 9411)
      .putDataDir("data", RawPath.valueOf("/data/cache2"));

  Stripe stripe = new Stripe().addNodes(node1);

  @Test
  public void test_containsNode() {
    assertTrue(stripe.containsNode(createUnresolved("localhost", 9410)));
    assertFalse(stripe.containsNode(createUnresolved("127.0.0.1", 9410)));
  }

  @Test
  public void test_clone() {
    assertThat(new Stripe().setUID(Testing.S_UIDS[1]), is(equalTo(new Stripe().setUID(Testing.S_UIDS[1]).clone())));
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

    assertThat(stripe.getNode(node1.getAddress()).get(), is(equalTo(node1)));
    assertThat(stripe.getNode(node2.getAddress()).isPresent(), is(false));
  }

  @Test
  public void test_getNodeId() {
    assertThat(stripe.getNodeId("node1").getAsInt(), is(equalTo(1)));
    assertThat(stripe.getNodeId("foo").isPresent(), is(false));

    assertThat(stripe.getNodeId(node1.getAddress()).getAsInt(), is(equalTo(1)));
    assertThat(stripe.getNodeId(node2.getAddress()).isPresent(), is(false));
  }

  @Test
  public void test_getSingleNode() {
    assertThat(stripe.getSingleNode().get(), is(sameInstance(node1)));

    stripe.addNode(node2);
    assertThat(() -> stripe.getSingleNode(), is(throwing(instanceOf(IllegalStateException.class))));

    // back to normal
    stripe.removeNode(node2.getAddress());
    assertThat(stripe.getSingleNode().get(), is(sameInstance(node1)));

    // empty
    stripe.removeNode(node1.getAddress());
    assertThat(stripe.getSingleNode().isPresent(), is(false));
  }
}