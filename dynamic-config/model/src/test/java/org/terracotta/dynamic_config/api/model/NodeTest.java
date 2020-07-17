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

import java.net.InetSocketAddress;
import java.nio.file.Paths;

import static java.io.File.separator;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.terracotta.dynamic_config.api.model.Node.newDefaultNode;
import static org.terracotta.testing.ExceptionMatcher.throwing;

/**
 * @author Mathieu Carbou
 */
public class NodeTest {

  Node node = Node.newDefaultNode("node1", "localhost", 9410)
      .setDataDir("data", Paths.get("data"))
      .setNodeBackupDir(Paths.get("backup"))
      .setNodeBindAddress("0.0.0.0")
      .setNodeGroupBindAddress("0.0.0.0")
      .setNodeGroupPort(9430)
      .setNodeLogDir(Paths.get("log"))
      .setNodeMetadataDir(Paths.get("metadata"))
      .setTcProperty("key", "val")
      .setSecurityAuditLogDir(Paths.get("audit"))
      .setSecurityDir(Paths.get("sec"));

  Node node3 = Node.newDefaultNode("node3", "localhost", 9410)
      .setNodeGroupPort(9430)
      .setNodeBindAddress("0.0.0.0")
      .setNodeGroupBindAddress("0.0.0.0")
      .setNodeMetadataDir(Paths.get("%H" + separator + "terracotta" + separator + "metadata"))
      .setNodeLogDir(Paths.get("%H" + separator + "terracotta" + separator + "logs"))
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
}