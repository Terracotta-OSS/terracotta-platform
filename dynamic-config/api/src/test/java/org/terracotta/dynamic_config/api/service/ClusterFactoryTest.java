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
package org.terracotta.dynamic_config.api.service;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.Setting;
import org.terracotta.dynamic_config.api.model.Stripe;

import java.io.File;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.lenient;
import static org.terracotta.testing.ExceptionMatcher.throwing;

/**
 * @author Mathieu Carbou
 */
@RunWith(MockitoJUnitRunner.class)
public class ClusterFactoryTest {

  private final ClusterFactory clusterFactory = new ClusterFactory();

  @Mock public IParameterSubstitutor substitutor;

  @Before
  public void setUp() {
    lenient().when(substitutor.substitute("%h")).thenReturn("localhost");
    lenient().when(substitutor.substitute("%c")).thenReturn("localhost.home");
    lenient().when(substitutor.substitute("%H")).thenReturn("home");
    lenient().when(substitutor.substitute("foo")).thenReturn("foo");
  }

  @Test
  public void test_create_cli() {
    assertCliEquals(cli(), Cluster.newDefaultCluster(new Stripe(Node.newDefaultNode("<GENERATED>", "localhost"))));
    assertCliEquals(cli("node-hostname=%c"), Cluster.newDefaultCluster(new Stripe(Node.newDefaultNode("<GENERATED>", "localhost.home"))));
    assertCliEquals(cli("node-hostname=foo"), Cluster.newDefaultCluster(new Stripe(Node.newDefaultNode("<GENERATED>", "foo"))));
  }

  @Test
  public void test_create_cli_validated() {
    assertCliFail(cli("security-ssl-tls=false", "security-authc=certificate"), "security-ssl-tls is required for security-authc=certificate");
    assertCliFail(cli("security-ssl-tls=true", "security-authc=certificate"), "security-dir is mandatory for any of the security configuration");
    assertCliFail(cli("security-ssl-tls=true"), "security-dir is mandatory for any of the security configuration");
    assertCliFail(cli("security-authc=file"), "security-dir is mandatory for any of the security configuration");
    assertCliFail(cli("security-authc=ldap"), "security-dir is mandatory for any of the security configuration");
    assertCliFail(cli("security-audit-log-dir=foo"), "security-dir is mandatory for any of the security configuration");
    assertCliFail(cli("security-whitelist=true"), "security-dir is mandatory for any of the security configuration");
    assertCliFail(cli("security-dir=foo"), "One of security-ssl-tls, security-authc, or security-whitelist is required for security configuration");
  }

  @Test
  public void test_create_config() {
    assertConfigEquals(
        config(
            "stripe.1.node.1.node-name=node1",
            "stripe.1.node.1.node-name=real",
            "stripe.1.node.1.node-hostname=localhost",
            "stripe.1.node.1.node-hostname=foo"
        ),
        Cluster.newDefaultCluster(new Stripe(Node.newDefaultNode("real", "foo"))));

    assertConfigEquals(
        config(
            "stripe.1.node.1.node-name=node1",
            "stripe.1.node.1.node-hostname=localhost"
        ),
        Cluster.newDefaultCluster(new Stripe(Node.newDefaultNode("node1", "localhost"))));

    assertConfigEquals(
        config(
            "stripe.1.node.1.node-name=node1",
            "stripe.1.node.1.node-hostname=localhost",
            "stripe.1.node.2.node-name=node2",
            "stripe.1.node.2.node-hostname=localhost",
            "stripe.2.node.1.node-name=node1",
            "stripe.2.node.1.node-hostname=localhost",
            "stripe.2.node.2.node-name=node2",
            "stripe.2.node.2.node-hostname=localhost"
        ),
        Cluster.newDefaultCluster(
            new Stripe(
                Node.newDefaultNode("node1", "localhost"),
                Node.newDefaultNode("node2", "localhost")),
            new Stripe(
                Node.newDefaultNode("node1", "localhost"),
                Node.newDefaultNode("node2", "localhost"))
        ));

    assertConfigEquals(
        config(
            "stripe.1.node.1.node-name=node1",
            "stripe.1.node.1.node-hostname=localhost",
            "cluster-name=foo",
            "client-reconnect-window=120s",
            "failover-priority=availability",
            "client-lease-duration=150s",
            "security-authc=",
            "security-ssl-tls=false",
            "security-whitelist=false",
            "offheap-resources=main:512MB",
            "stripe.1.node.1.node-port=9410",
            "stripe.1.node.1.node-group-port=9430",
            "stripe.1.node.1.node-bind-address=0.0.0.0",
            "stripe.1.node.1.node-group-bind-address=0.0.0.0",
            "stripe.1.node.1.node-metadata-dir=%H/terracotta/metadata",
            "stripe.1.node.1.node-log-dir=%H/terracotta/logs",
            "stripe.1.node.1.node-backup-dir=",
            "stripe.1.node.1.tc-properties=",
            "stripe.1.node.1.security-dir=",
            "stripe.1.node.1.security-audit-log-dir=",
            "stripe.1.node.1.data-dirs=main:%H/terracotta/user-data/main"
        ),
        Cluster.newDefaultCluster("foo", new Stripe(Node.newDefaultNode("node1", "localhost"))));

    assertConfigEquals(
        config(
            "stripe.1.node.1.node-name=node1",
            "stripe.1.node.1.node-hostname=localhost",
            "cluster-name=foo",
            "client-reconnect-window=120s",
            "failover-priority=availability",
            "client-lease-duration=150s",
            "security-authc=",
            "security-ssl-tls=false",
            "security-whitelist=false",
            "offheap-resources=main:512MB",
            "stripe.1.node.1.node-port=9410",
            "stripe.1.node.1.node-group-port=9430",
            "stripe.1.node.1.node-bind-address=0.0.0.0",
            "stripe.1.node.1.node-group-bind-address=0.0.0.0",
            "stripe.1.node.1.node-metadata-dir=%H/terracotta/metadata",
            "stripe.1.node.1.node-log-dir=%H/terracotta/logs",
            "stripe.1.node.1.node-backup-dir=",
            "stripe.1.node.1.tc-properties=",
            "stripe.1.node.1.security-dir=",
            "stripe.1.node.1.security-audit-log-dir=",
            "stripe.1.node.1.data-dirs=main:%H/terracotta/user-data/main"
        ),
        Cluster.newDefaultCluster("foo", new Stripe(Node.newDefaultNode("node1", "localhost"))));
  }

  @Test
  public void test_create_config_validated() {
    // security
    assertConfigFail(
        config("stripe.1.node.1.node-hostname=localhost", "security-ssl-tls=false", "security-authc=certificate"),
        "security-ssl-tls is required for security-authc=certificate");
    assertConfigFail(
        config("stripe.1.node.1.node-hostname=localhost", "security-ssl-tls=true", "security-authc=certificate"),
        "security-dir is mandatory for any of the security configuration");
    assertConfigFail(
        config("stripe.1.node.1.node-hostname=localhost", "security-ssl-tls=true"),
        "security-dir is mandatory for any of the security configuration");
    assertConfigFail(
        config("stripe.1.node.1.node-hostname=localhost", "security-authc=file"),
        "security-dir is mandatory for any of the security configuration");
    assertConfigFail(
        config("stripe.1.node.1.node-hostname=localhost", "security-authc=ldap"),
        "security-dir is mandatory for any of the security configuration");
    assertConfigFail(
        config("stripe.1.node.1.node-hostname=localhost", "stripe.1.node.1.security-audit-log-dir=foo"),
        "security-dir is mandatory for any of the security configuration");
    assertConfigFail(
        config("stripe.1.node.1.node-hostname=localhost", "security-whitelist=true"),
        "security-dir is mandatory for any of the security configuration");
    assertConfigFail(
        config("stripe.1.node.1.node-hostname=localhost", "stripe.1.node.1.security-dir=foo"),
        "One of security-ssl-tls, security-authc, or security-whitelist is required for security configuration");

    // duplicate node name
    assertConfigFail(
        config(
            "stripe.1.node.1.node-hostname=localhost1", "stripe.1.node.1.node-name=foo",
            "stripe.1.node.2.node-hostname=localhost2", "stripe.1.node.2.node-name=foo"
        ),
        "Found duplicate node name: foo in stripe 1");

    // Note: all possible failures (including those above) are already tested in ClusterValidatorTest
    // ClusterFactory being much more a wiring class, we do not repeat all tests
  }

  @SuppressWarnings("OptionalGetWithoutIsPresent")
  private void assertCliEquals(Map<Setting, String> params, Cluster cluster) {
    Cluster built = clusterFactory.create(params, substitutor);

    // since node name is generated when not given,
    // this is a hack that will reset to null only the node names that have been generated
    String nodeName = built.getSingleNode().get().getNodeName();
    cluster.getSingleNode()
        .filter(node -> node.getNodeName().equals("<GENERATED>"))
        .ifPresent(node -> node.setNodeName(nodeName));

    assertThat(built, Matchers.is(equalTo(cluster)));
  }

  private void assertCliFail(Map<Setting, String> params, String err) {
    err = err.replace("/", File.separator); // unix/win compat'
    assertThat(
        () -> clusterFactory.create(params, substitutor),
        Matchers.is(throwing(instanceOf(MalformedClusterException.class)).andMessage(Matchers.is(equalTo(err)))));
  }

  private void assertConfigEquals(Properties config, Cluster cluster) {
    Cluster built = clusterFactory.create(config);
    assertThat(built, Matchers.is(equalTo(cluster)));
  }

  private void assertConfigFail(Properties config, String err) {
    err = err.replace("/", File.separator); // unix/win compat'
    assertThat(
        () -> clusterFactory.create(config),
        Matchers.is(throwing(instanceOf(MalformedClusterException.class)).andMessage(Matchers.is(equalTo(err)))));
  }

  private static Map<Setting, String> cli(String... params) {
    return Stream.of(params)
        .map(string -> string.replace("/", File.separator)) // unix/win compat'
        .map(p -> p.split("="))
        .map(kv -> new AbstractMap.SimpleEntry<>(Setting.fromName(kv[0]), kv[1]))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private static Properties config(String... params) {
    return Stream.of(params)
        .map(string -> string.replace("/", File.separator)) // unix/win compat'
        .map(p -> p.split("="))
        .reduce(new Properties(), (props, kv) -> {
          props.setProperty(kv[0], kv.length == 1 ? "" : kv[1]);
          return props;
        }, (p1, p2) -> {
          throw new UnsupportedOperationException();
        });
  }
}