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
import static org.terracotta.dynamic_config.api.model.Testing.newTestNode;
import static org.terracotta.testing.ExceptionMatcher.throwing;

/**
 * @author Mathieu Carbou
 */
public class NodeTest {

  Node node = Testing.newTestNode("node1", "localhost", 9410)
      .putDataDir("data", Paths.get("data"))
      .setBackupDir(Paths.get("backup"))
      .setBindAddress("0.0.0.0")
      .setGroupBindAddress("0.0.0.0")
      .setGroupPort(9430)
      .setLogDir(Paths.get("log"))
      .setMetadataDir(Paths.get("metadata"))
      .putTcProperty("key", "val")
      .setSecurityAuditLogDir(Paths.get("audit"))
      .setSecurityDir(Paths.get("sec"));

  Node node3 = Testing.newTestNode("node3", "localhost", 9410)
      .setGroupPort(9430)
      .setBindAddress("0.0.0.0")
      .setGroupBindAddress("0.0.0.0")
      .setMetadataDir(Paths.get("%H" + separator + "terracotta" + separator + "metadata"))
      .setLogDir(Paths.get("%H" + separator + "terracotta" + separator + "logs"))
      .putDataDir("main", Paths.get("%H" + separator + "terracotta" + separator + "user-data" + separator + "main"));

  @Test
  public void test_clone() {
    assertThat(new Node(), is(equalTo(new Node().clone())));
    assertThat(node, is(equalTo(node.clone())));
    assertThat(node.hashCode(), is(equalTo(node.clone().hashCode())));
  }

  @Test
  public void test_fillDefaults() {
    assertThat(new Node().getName(), is(nullValue()));
    assertThat(newTestNode(null).getName(), is(not(nullValue())));
    assertThat(newTestNode("localhost").setName(null), is(equalTo(node3.setName(null))));
  }

  @Test
  public void test_getNodeInternalAddress() {
    assertThat(
        () -> new Node().getInternalAddress(),
        is(throwing(instanceOf(AssertionError.class)).andMessage(is(equalTo("Node null is not correctly defined with internal address: null:null")))));

    assertThat(
        () -> newTestNode(null).setPort(9410).getInternalAddress(),
        is(throwing(instanceOf(AssertionError.class)).andMessage(is(containsString(" is not correctly defined with internal address: null:9410")))));

    assertThat(
        () -> newTestNode("%h").getInternalAddress(),
        is(throwing(instanceOf(AssertionError.class)).andMessage(is(containsString(" is not correctly defined with internal address: %h:9410")))));
  }

  @Test
  public void test_getNodePublicAddress() {
    assertThat(
        newTestNode("localhost").getPublicAddress().isPresent(),
        is(false));
    assertThat(
        newTestNode("localhost").setPublicHostname("foo").getPublicAddress().isPresent(),
        is(false));
    assertThat(
        newTestNode("localhost").setPublicPort(1234).getPublicAddress().isPresent(),
        is(false));

    assertThat(
        () -> newTestNode("localhost").setPublicHostname("%h").setPublicPort(1234).getPublicAddress(),
        is(throwing(instanceOf(AssertionError.class)).andMessage(is(containsString(" is not correctly defined with public address: %h:1234")))));

    assertThat(
        newTestNode("localhost").setPublicHostname("foo").setPublicPort(1234).getPublicAddress().get(),
        is(equalTo(InetSocketAddress.createUnresolved("foo", 1234))));
  }

  @Test
  public void test_getNodeAddress() {
    assertThat(
        newTestNode("localhost").getAddress(),
        is(equalTo(InetSocketAddress.createUnresolved("localhost", 9410))));
    assertThat(
        newTestNode("localhost").setPublicHostname("foo").getAddress(),
        is(equalTo(InetSocketAddress.createUnresolved("localhost", 9410))));
    assertThat(
        newTestNode("localhost").setPublicPort(1234).getAddress(),
        is(equalTo(InetSocketAddress.createUnresolved("localhost", 9410))));
    assertThat(
        newTestNode("localhost").setPublicHostname("foo").setPublicPort(1234).getAddress(),
        is(equalTo(InetSocketAddress.createUnresolved("foo", 1234))));
  }

  @Test
  public void test_hasAddress() {
    assertThat(
        newTestNode("localhost").hasAddress(InetSocketAddress.createUnresolved("localhost", 9410)),
        is(true));
    assertThat(
        newTestNode("localhost")
            .setPublicHostname("foo").setPublicPort(1234)
            .hasAddress(InetSocketAddress.createUnresolved("localhost", 9410)),
        is(true));
    assertThat(
        newTestNode("localhost")
            .setPublicHostname("foo").setPublicPort(1234)
            .hasAddress(InetSocketAddress.createUnresolved("foo", 1234)),
        is(true));
  }
}