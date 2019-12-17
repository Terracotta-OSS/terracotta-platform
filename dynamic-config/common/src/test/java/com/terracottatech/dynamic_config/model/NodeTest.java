/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.model;

import com.terracottatech.utilities.MemoryUnit;
import com.terracottatech.utilities.TimeUnit;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.nio.file.Paths;

import static com.terracottatech.dynamic_config.model.FailoverPriority.availability;
import static com.terracottatech.dynamic_config.model.Node.newDefaultNode;
import static com.terracottatech.utilities.hamcrest.ExceptionMatcher.throwing;
import static java.io.File.separator;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * @author Mathieu Carbou
 */
public class NodeTest {

  Node node = Node.newDefaultNode("node1", "localhost", 9410)
      .setClientLeaseDuration(1, TimeUnit.SECONDS)
      .setClientReconnectWindow(2, TimeUnit.MINUTES)
      .setDataDir("data", Paths.get("data"))
      .setFailoverPriority(FailoverPriority.valueOf("availability"))
      .setNodeBackupDir(Paths.get("backup"))
      .setNodeBindAddress("0.0.0.0")
      .setNodeGroupBindAddress("0.0.0.0")
      .setNodeGroupPort(9430)
      .setNodeLogDir(Paths.get("log"))
      .setNodeMetadataDir(Paths.get("metadata"))
      .setTcProperty("key", "val")
      .setOffheapResource("off", 2, MemoryUnit.GB)
      .setSecurityAuditLogDir(Paths.get("audit"))
      .setSecurityAuthc("ldap")
      .setSecurityDir(Paths.get("sec"))
      .setSecuritySslTls(true)
      .setSecurityWhitelist(true);

  Node node1 = Node.newDefaultNode("node1", "localhost", 9410)
      .setClientLeaseDuration(1, TimeUnit.SECONDS)
      .setClientReconnectWindow(2, TimeUnit.MINUTES)
      .setDataDir("data", Paths.get("data"))
      .setFailoverPriority(FailoverPriority.valueOf("availability"))
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

  Node node3 = Node.newDefaultNode("node3", "localhost", 9410)
      .setNodeGroupPort(9430)
      .setNodeBindAddress("0.0.0.0")
      .setNodeGroupBindAddress("0.0.0.0")
      .setNodeMetadataDir(Paths.get("%H" + separator + "terracotta" + separator + "metadata"))
      .setNodeLogDir(Paths.get("%H" + separator + "terracotta" + separator + "logs"))
      .setClientReconnectWindow(120, TimeUnit.SECONDS)
      .setFailoverPriority(availability())
      .setClientLeaseDuration(150, TimeUnit.SECONDS)
      .setSecuritySslTls(false)
      .setSecurityWhitelist(false)
      .setOffheapResource("main", 512, MemoryUnit.MB)
      .setDataDir("main", Paths.get("%H" + separator + "terracotta" + separator + "user-data" + separator + "main"));

  @Test
  public void test_clone() {
    assertThat(new Node(), is(equalTo(new Node().clone())));
    assertThat(node, is(equalTo(node.clone())));
    assertThat(node.hashCode(), is(equalTo(node.clone().hashCode())));
  }

  @Test
  public void test_fillDefaults() {
    assertThat(new Node().getNodeName(), is(nullValue()));
    assertThat(newDefaultNode(null).getNodeName(), is(not(nullValue())));
    assertThat(newDefaultNode("localhost").setNodeName(null), is(equalTo(node3.setNodeName(null))));
  }

  @Test
  public void test_getNodeInternalAddress() {
    assertThat(
        () -> new Node().getNodeInternalAddress(),
        is(throwing(instanceOf(AssertionError.class)).andMessage(is(equalTo("Node null is not correctly defined with internal address: null:9410")))));

    assertThat(
        () -> newDefaultNode(null).getNodeInternalAddress(),
        is(throwing(instanceOf(AssertionError.class)).andMessage(is(containsString(" is not correctly defined with internal address: null:9410")))));

    assertThat(
        () -> newDefaultNode("%h").getNodeInternalAddress(),
        is(throwing(instanceOf(AssertionError.class)).andMessage(is(containsString(" is not correctly defined with internal address: %h:9410")))));
  }

  @Test
  public void test_getNodePublicAddress() {
    assertThat(
        newDefaultNode("localhost").getNodePublicAddress().isPresent(),
        is(false));
    assertThat(
        newDefaultNode("localhost").setNodePublicHostname("foo").getNodePublicAddress().isPresent(),
        is(false));
    assertThat(
        newDefaultNode("localhost").setNodePublicPort(1234).getNodePublicAddress().isPresent(),
        is(false));

    assertThat(
        () -> newDefaultNode("localhost").setNodePublicHostname("%h").setNodePublicPort(1234).getNodePublicAddress(),
        is(throwing(instanceOf(AssertionError.class)).andMessage(is(containsString(" is not correctly defined with public address: %h:1234")))));

    assertThat(
        newDefaultNode("localhost").setNodePublicHostname("foo").setNodePublicPort(1234).getNodePublicAddress().get(),
        is(equalTo(InetSocketAddress.createUnresolved("foo", 1234))));
  }

  @Test
  public void test_getNodeAddress() {
    assertThat(
        newDefaultNode("localhost").getNodeAddress(),
        is(equalTo(InetSocketAddress.createUnresolved("localhost", 9410))));
    assertThat(
        newDefaultNode("localhost").setNodePublicHostname("foo").getNodeAddress(),
        is(equalTo(InetSocketAddress.createUnresolved("localhost", 9410))));
    assertThat(
        newDefaultNode("localhost").setNodePublicPort(1234).getNodeAddress(),
        is(equalTo(InetSocketAddress.createUnresolved("localhost", 9410))));
    assertThat(
        newDefaultNode("localhost").setNodePublicHostname("foo").setNodePublicPort(1234).getNodeAddress(),
        is(equalTo(InetSocketAddress.createUnresolved("foo", 1234))));
  }

  @Test
  public void test_hasAddress() {
    assertThat(
        newDefaultNode("localhost").hasAddress(InetSocketAddress.createUnresolved("localhost", 9410)),
        is(true));
    assertThat(
        newDefaultNode("localhost")
            .setNodePublicHostname("foo").setNodePublicPort(1234)
            .hasAddress(InetSocketAddress.createUnresolved("localhost", 9410)),
        is(true));
    assertThat(
        newDefaultNode("localhost")
            .setNodePublicHostname("foo").setNodePublicPort(1234)
            .hasAddress(InetSocketAddress.createUnresolved("foo", 1234)),
        is(true));
  }

  @Test
  public void test_cloneForAttachment() {
    // attaching a non-secured node to secured nodes
    node1.setSecurityDir(Paths.get("sec"));
    assertThat(
        () -> node2.cloneForAttachment(node1),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Node localhost:9411 must be started with a security directory.")))));

    // attaching a secured node to a non-secured nodes
    node1.setSecurityDir(null);
    node2.setSecurityDir(Paths.get("sec"));
    Node clone = node2.cloneForAttachment(node1);
    assertThat(clone.getSecurityDir(), is(nullValue()));

    node1.setDataDir("other", Paths.get("other"));
    assertThat(
        () -> node2.cloneForAttachment(node1),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Node localhost:9411 must declare the following data directories: other.")))));
    node1.removeDataDir("other");

    node2.setDataDir("other", Paths.get("other"));
    assertThat(
        () -> node2.cloneForAttachment(node1),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Node localhost:9411 must not declare the following data directories: other.")))));
    node2.removeDataDir("other");

    // attaching
    node1.setSecurityDir(Paths.get("Sec1"));
    node2.setSecurityDir(Paths.get("Sec2"));
    clone = node2.cloneForAttachment(node1);

    assertThat(node2.getOffheapResources(), hasKey("foo"));
    assertThat(node2.getOffheapResources(), hasKey("bar"));
    assertThat(node2.getOffheapResources(), not(hasKey("off")));
    assertThat(clone.getOffheapResources(), hasKey("off"));
    assertThat(clone.getOffheapResources(), not(hasKey("foo")));
    assertThat(clone.getOffheapResources(), not(hasKey("bar")));
  }
}