/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.model;

import com.terracottatech.utilities.MemoryUnit;
import com.terracottatech.utilities.TimeUnit;
import org.junit.Test;

import java.nio.file.Paths;

import static com.terracottatech.utilities.hamcrest.ExceptionMatcher.throwing;
import static java.net.InetSocketAddress.createUnresolved;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author Mathieu Carbou
 */
public class StripeTest {

  Node node1 = new Node()
      .setClientLeaseDuration(1, TimeUnit.SECONDS)
      .setClientReconnectWindow(2, TimeUnit.MINUTES)
      .setDataDir("data", Paths.get("data"))
      .setFailoverPriority("availability")
      .setNodeBackupDir(Paths.get("backup"))
      .setNodeBindAddress("0.0.0.0")
      .setNodeRepositoryDir(Paths.get("cfg"))
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
      .setNodeRepositoryDir(Paths.get("/config/node2"));

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
        () -> stripe.attachNode(new Node().setNodeHostname("localhost").setNodePort(9410)),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Node localhost:9410 is already in the stripe.")))));

    assertThat(
        () -> new Stripe().attachNode(new Node().setNodeHostname("localhost").setNodePort(9410)),
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
}