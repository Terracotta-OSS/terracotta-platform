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

  Node node = new Node()
      .setClientLeaseDuration(1, TimeUnit.SECONDS)
      .setClientReconnectWindow(2, TimeUnit.MINUTES)
      .setDataDir("data", Paths.get("data"))
      .setFailoverPriority("availability")
      .setNodeBackupDir(Paths.get("backup"))
      .setNodeBindAddress("0.0.0.0")
      .setNodeGroupBindAddress("0.0.0.0")
      .setNodeGroupPort(9430)
      .setNodeHostname("localhost")
      .setNodeLogDir(Paths.get("log"))
      .setNodeMetadataDir(Paths.get("metadata"))
      .setNodeProperty("key", "val")
      .setNodeName("node1")
      .setNodePort(9410)
      .setOffheapResource("off", 2, MemoryUnit.GB)
      .setSecurityAuditLogDir(Paths.get("audit"))
      .setSecurityAuthc("ldap")
      .setSecurityDir(Paths.get("sec"))
      .setSecuritySslTls(true)
      .setSecurityWhitelist(true);

  Node node1 = new Node()
      .setClientLeaseDuration(1, TimeUnit.SECONDS)
      .setClientReconnectWindow(2, TimeUnit.MINUTES)
      .setDataDir("data", Paths.get("data"))
      .setFailoverPriority("availability")
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

  @Test
  public void test_clone() {
    assertThat(new Node(), is(equalTo(new Node().clone())));
    assertThat(node, is(equalTo(node.clone())));
    assertThat(node.hashCode(), is(equalTo(node.clone().hashCode())));
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