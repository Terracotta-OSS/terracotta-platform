/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
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
import org.terracotta.inet.HostPort;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.terracotta.dynamic_config.api.model.Testing.newTestNode;
import static org.terracotta.testing.ExceptionMatcher.throwing;

/**
 * @author Mathieu Carbou
 */
public class NodeTest {

  Node node = Testing.newTestNode("node1", "localhost", 9410)
      .putDataDir("data", RawPath.valueOf("data"))
      .setBackupDir(RawPath.valueOf("backup"))
      .setBindAddress("0.0.0.0")
      .setGroupBindAddress("0.0.0.0")
      .setGroupPort(9430)
      .setLogDir(RawPath.valueOf("log"))
      .setMetadataDir(RawPath.valueOf("metadata"))
      .putTcProperty("key", "val")
      .setSecurityAuditLogDir(RawPath.valueOf("audit"))
      .setSecurityDir(RawPath.valueOf("sec"));

  Node node3 = Testing.newTestNode("node3", "localhost", 9410)
      .setGroupPort(9430)
      .setBindAddress("0.0.0.0")
      .setGroupBindAddress("0.0.0.0")
      .setMetadataDir(RawPath.valueOf("%H/terracotta/metadata"))
      .setLogDir(RawPath.valueOf("%H/terracotta/logs"))
      .putDataDir("main", RawPath.valueOf("%H/terracotta/user-data/main"));

  @Test
  public void test_clone() {
    assertThat(new Node().setUID(Testing.N_UIDS[1]), is(equalTo(new Node().setUID(Testing.N_UIDS[1]).clone())));
    assertThat(node, is(equalTo(node.clone())));
    assertThat(node.hashCode(), is(equalTo(node.clone().hashCode())));
  }

  @Test
  public void test_getNodeInternalAddress() {
    assertThat(
        () -> new Node().getInternalHostPort(),
        is(throwing(instanceOf(AssertionError.class)).andMessage(is(equalTo("Node null is not correctly defined with internal address: null:9410")))));

    assertThat(
        () -> new Node().setName("node1").getInternalHostPort(),
        is(throwing(instanceOf(AssertionError.class)).andMessage(is(containsString(" is not correctly defined with internal address: null:9410")))));

    assertThat(
        () -> new Node().setName("node1").setPort(9410).getInternalHostPort(),
        is(throwing(instanceOf(AssertionError.class)).andMessage(is(containsString(" is not correctly defined with internal address: null:9410")))));

    assertThat(
        () -> newTestNode("node1", "%h").getInternalHostPort(),
        is(throwing(instanceOf(AssertionError.class)).andMessage(is(containsString(" is not correctly defined with internal address: %h:9410")))));
  }

  @Test
  public void test_getNodePublicAddress() {
    assertThat(
        newTestNode("node1", "localhost").getPublicHostPort().isPresent(),
        is(false));
    assertThat(
        newTestNode("node1", "localhost").setPublicHostname("foo").getPublicHostPort().isPresent(),
        is(false));
    assertThat(
        newTestNode("node1", "localhost").setPublicPort(1234).getPublicHostPort().isPresent(),
        is(false));

    assertThat(
        () -> newTestNode("node1", "localhost").setPublicHostname("%h").setPublicPort(1234).getPublicHostPort(),
        is(throwing(instanceOf(AssertionError.class)).andMessage(is(containsString(" is not correctly defined with public address: %h:1234")))));

    assertThat(
        newTestNode("node1", "localhost").setPublicHostname("foo").setPublicPort(1234).getPublicHostPort().get(),
        is(equalTo(HostPort.create("foo", 1234))));
  }

  @Test
  public void test_isReachableWith() {
    assertThat(
        newTestNode("node1", "localhost").isReachableWith(HostPort.create("localhost", 9410)),
        is(true));
    assertThat(
        newTestNode("node1", "localhost")
            .setPublicEndpoint("foo", 9410)
            .isReachableWith(HostPort.create("localhost", 9410)),
        is(true));
    assertThat(
        newTestNode("node1", "localhost")
            .setPublicEndpoint("foo", 1234)
            .isReachableWith(HostPort.create("foo", 1234)),
        is(true));

    assertTrue(node.isReachableWith("localhost", 9410));
    assertFalse(node.isReachableWith("127.0.0.1", 9410)); // this is normal, DC never resolves, only matches with configured entries

    node = node.clone().setBindAddress("127.0.0.1");
    assertTrue(node.isReachableWith("localhost", 9410));
    assertTrue(node.isReachableWith("127.0.0.1", 9410));

    node = node.clone().setBindAddress("127.0.0.1").setPublicEndpoint("foo", 9610);
    assertTrue(node.isReachableWith("localhost", 9410));
    assertTrue(node.isReachableWith("127.0.0.1", 9410));
    assertTrue(node.isReachableWith("foo", 9610));
  }

  @Test
  public void test_getEndpoint() {
    assertThat(node.findEndpoints("localhost", 9410), hasItem(node.getInternalEndpoint()));
    assertTrue(node.findEndpoints("127.0.0.1", 9410).isEmpty()); // this is normal, DC never resolves, only matches with configured entries

    node = node.clone().setBindAddress("127.0.0.1");
    assertThat(node.findEndpoints("localhost", 9410), hasItem(node.getInternalEndpoint()));
    assertThat(node.findEndpoints("127.0.0.1", 9410), hasItem(node.getBindEndpoint()));

    node = node.clone().setBindAddress("127.0.0.1").setPublicEndpoint("foo", 9610);
    assertThat(node.findEndpoints("localhost", 9410), hasItem(node.getInternalEndpoint()));
    assertThat(node.findEndpoints("127.0.0.1", 9410), hasItem(node.getBindEndpoint()));
    assertThat(node.findEndpoints("foo", 9610), hasItem(node.getBindEndpoint()));
  }

  @Test
  public void test_getSimilarEndpoint() {
    node = node.clone();
    assertThat(node.determineEndpoint(node.getInternalEndpoint()), is(equalTo(node.getInternalEndpoint())));
    assertThat(node.determineEndpoint(node.getBindEndpoint()), is(equalTo(node.getInternalEndpoint())));

    node = node.clone().setBindAddress("127.0.0.1");
    assertThat(node.determineEndpoint(node.getInternalEndpoint()), is(equalTo(node.getInternalEndpoint())));
    assertThat(node.determineEndpoint(node.getBindEndpoint()), is(equalTo(node.getBindEndpoint())));

    node = node.clone().setPublicEndpoint("foo", 9610);
    assertThat(node.determineEndpoint(node.getInternalEndpoint()), is(equalTo(node.getInternalEndpoint())));
    assertThat(node.determineEndpoint(node.getBindEndpoint()), is(equalTo(node.getBindEndpoint())));
    assertThat(node.determineEndpoint(node.getPublicEndpoint().get()), is(equalTo(node.getPublicEndpoint().get())));

    node = node.clone().setBindAddress("0.0.0.0").setPublicEndpoint("foo", 9610);
    assertThat(node.determineEndpoint(node.getInternalEndpoint()), is(equalTo(node.getInternalEndpoint())));
    assertThat(node.determineEndpoint(node.getBindEndpoint()), is(equalTo(node.getPublicEndpoint().get())));
    assertThat(node.determineEndpoint(node.getPublicEndpoint().get()), is(equalTo(node.getPublicEndpoint().get())));
  }
}