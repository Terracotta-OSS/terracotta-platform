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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Configuration;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.Setting;
import org.terracotta.dynamic_config.api.model.Stripe;

import java.io.File;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.terracotta.testing.ExceptionMatcher.throwing;

/**
 * @author Mathieu Carbou
 */
@RunWith(MockitoJUnitRunner.class)
public class ConfigurationParserTest {

  private final List<Configuration> added = new ArrayList<>();

  @Mock public IParameterSubstitutor substitutor;

  @Before
  public void setUp() {
    lenient().when(substitutor.substitute("%h")).thenReturn("localhost");
    lenient().when(substitutor.substitute("%c")).thenReturn("localhost.home");
    lenient().when(substitutor.substitute("%H")).thenReturn("home");
    lenient().when(substitutor.substitute("foo")).thenReturn("foo");
    lenient().when(substitutor.substitute(startsWith("node-"))).thenReturn("<GENERATED>");
    lenient().when(substitutor.substitute("9410")).thenReturn("9410");
  }

  @Test
  public void test_cliToProperties_1() {
    // node name should be resolved from default value (%h) if not given
    assertCliEquals(
        cli("failover-priority=availability"),
        Cluster.newDefaultCluster(new Stripe(Node.newDefaultNode("<GENERATED>", "localhost"))),
        "stripe.1.node.1.hostname=localhost",
        "cluster-name=",
        "client-reconnect-window=120s",
        "client-lease-duration=150s",
        "authc=",
        "ssl-tls=false",
        "permit-list=false",
        "offheap-resources=main:512MB",
        "stripe.1.node.1.name=<GENERATED>",
        "stripe.1.node.1.port=9410",
        "stripe.1.node.1.public-port=",
        "stripe.1.node.1.public-hostname=",
        "stripe.1.node.1.group-port=9430",
        "stripe.1.node.1.bind-address=0.0.0.0",
        "stripe.1.node.1.group-bind-address=0.0.0.0",
        "stripe.1.node.1.metadata-dir=%H/terracotta/metadata",
        "stripe.1.node.1.log-dir=%H/terracotta/logs",
        "stripe.1.node.1.logger-overrides=",
        "stripe.1.node.1.backup-dir=",
        "stripe.1.node.1.tc-properties=",
        "stripe.1.node.1.security-dir=",
        "stripe.1.node.1.audit-log-dir=",
        "stripe.1.node.1.data-dirs=main:%H/terracotta/user-data/main"
    );
    verify(substitutor, times(1)).substitute("%h");
    verify(substitutor, times(1)).substitute("9410");
    verify(substitutor, times(1)).substitute(startsWith("node-"));
    verifyNoMoreInteractions(substitutor);
  }

  @Test
  public void test_cliToProperties_2() {
    // placeholder in node name should be resolved eagerly
    assertCliEquals(
        cli("failover-priority=availability", "hostname=%c"),
        Cluster.newDefaultCluster(new Stripe(Node.newDefaultNode("<GENERATED>", "localhost.home"))),
        "cluster-name=",
        "client-reconnect-window=120s",
        "client-lease-duration=150s",
        "authc=",
        "ssl-tls=false",
        "permit-list=false",
        "offheap-resources=main:512MB",
        "stripe.1.node.1.name=<GENERATED>",
        "stripe.1.node.1.port=9410",
        "stripe.1.node.1.public-port=",
        "stripe.1.node.1.public-hostname=",
        "stripe.1.node.1.group-port=9430",
        "stripe.1.node.1.bind-address=0.0.0.0",
        "stripe.1.node.1.group-bind-address=0.0.0.0",
        "stripe.1.node.1.metadata-dir=%H/terracotta/metadata",
        "stripe.1.node.1.log-dir=%H/terracotta/logs",
        "stripe.1.node.1.logger-overrides=",
        "stripe.1.node.1.backup-dir=",
        "stripe.1.node.1.tc-properties=",
        "stripe.1.node.1.security-dir=",
        "stripe.1.node.1.audit-log-dir=",
        "stripe.1.node.1.data-dirs=main:%H/terracotta/user-data/main"
    );
    verify(substitutor).substitute("%c");
    verify(substitutor, times(1)).substitute("9410");
    verify(substitutor, times(1)).substitute(startsWith("node-"));
    verifyNoMoreInteractions(substitutor);
  }

  @Test
  public void test_cliToProperties_3() {
    // node name without placeholder triggers no resolve
    assertCliEquals(
        cli("failover-priority=availability", "hostname=foo"),
        Cluster.newDefaultCluster(new Stripe(Node.newDefaultNode("<GENERATED>", "foo"))),
        "cluster-name=",
        "client-reconnect-window=120s",
        "client-lease-duration=150s",
        "authc=",
        "ssl-tls=false",
        "permit-list=false",
        "offheap-resources=main:512MB",
        "stripe.1.node.1.name=<GENERATED>",
        "stripe.1.node.1.port=9410",
        "stripe.1.node.1.public-port=",
        "stripe.1.node.1.public-hostname=",
        "stripe.1.node.1.group-port=9430",
        "stripe.1.node.1.bind-address=0.0.0.0",
        "stripe.1.node.1.group-bind-address=0.0.0.0",
        "stripe.1.node.1.metadata-dir=%H/terracotta/metadata",
        "stripe.1.node.1.log-dir=%H/terracotta/logs",
        "stripe.1.node.1.logger-overrides=",
        "stripe.1.node.1.backup-dir=",
        "stripe.1.node.1.tc-properties=",
        "stripe.1.node.1.security-dir=",
        "stripe.1.node.1.audit-log-dir=",
        "stripe.1.node.1.data-dirs=main:%H/terracotta/user-data/main"
    );
    verify(substitutor).substitute("foo");
    verify(substitutor, times(1)).substitute("9410");
    verify(substitutor, times(1)).substitute(startsWith("node-"));
    verifyNoMoreInteractions(substitutor);
  }

  @Test
  public void test_parsing_invalid() {
    // hostname required
    assertConfigFail(config(), "No configuration provided");

    // placeholder forbidden for hostname
    assertConfigFail(config("failover-priority=availability", "stripe.1.node.1.hostname=%h"), "Invalid input: 'stripe.1.node.1.hostname=%h'. Placeholders are not allowed");
    assertConfigFail(config(
        "failover-priority=availability",
        "stripe.1.node.1.hostname=localhost",
        "stripe.1.node.2.name=foo"
    ), "Invalid input: 'stripe.1.node.2.hostname=%h'. Placeholders are not allowed");

    // scope
    assertConfigFail(config("failover-priority=availability", "hostname=foo"), "Invalid input: 'hostname=foo'. Reason: hostname cannot be set at cluster level");
    assertConfigFail(config(
        "failover-priority=availability",
        "stripe.1.node.1.hostname=localhost",
        "stripe.1.backup-dir=foo/bar"
    ), "Invalid input: 'stripe.1.backup-dir=foo/bar'. Reason: stripe level configuration not allowed");
    assertConfigFail(config(
        "failover-priority=availability",
        "stripe.1.node.1.hostname=localhost",
        "backup-dir=foo/bar"
    ), "Invalid settings found at cluster level: backup-dir");
    assertConfigFail(config(
        "failover-priority=availability",
        "stripe.1.node.1.hostname=localhost",
        "stripe.1.node.1.failover-priority=availability"
    ), "Invalid input: 'stripe.1.node.1.failover-priority=availability'. Reason: failover-priority does not allow any operation at node level");

    // node and stripe ids
    assertConfigFail(config("failover-priority=availability", "stripe.1.node.2.hostname=localhost"), "Node ID must start at 1 in stripe 1");
    assertConfigFail(config("failover-priority=availability", "stripe.2.node.1.hostname=localhost"), "Stripe ID must start at 1");
    assertConfigFail(config(
        "failover-priority=availability",
        "stripe.1.node.1.hostname=localhost",
        "stripe.1.node.2.hostname=localhost",
        "stripe.1.node.4.hostname=localhost"
    ), "Node ID must end at 3 in stripe 1");
    assertConfigFail(config(
        "failover-priority=availability",
        "stripe.1.node.1.hostname=localhost",
        "stripe.2.node.1.hostname=localhost",
        "stripe.4.node.1.hostname=localhost"
    ), "Stripe ID must end at 3");

    // not allowed in config
    assertConfigFail(config(
        "failover-priority=availability",
        "stripe.1.node.1.hostname=localhost",
        "stripe.1.node.1.config-dir=foo/bar"
    ), "Invalid input: 'stripe.1.node.1.config-dir=foo/bar'. Reason: config-dir does not allow any operation at node level");
    assertConfigFail(config(
        "failover-priority=availability",
        "stripe.1.node.1.hostname=localhost",
        "license-file=foo/bar"
    ), "Invalid settings found at cluster level: license-file");
  }

  @Test
  public void test_parsing_allows_duplicates() {
    // Note about duplicate entries: we do not test them, because they made a property file invalid
    // and we support the fact that the last one will override the previous one
    assertConfigEquals(
        config(
            "failover-priority=availability",
            "stripe.1.node.1.name=node1",
            "stripe.1.node.1.name=real",
            "stripe.1.node.1.hostname=localhost",
            "stripe.1.node.1.hostname=foo"
        ),
        Cluster.newDefaultCluster(new Stripe(Node.newDefaultNode("real", "foo"))),
        "cluster-name=",
        "client-reconnect-window=120s",
        "client-lease-duration=150s",
        "authc=",
        "ssl-tls=false",
        "permit-list=false",
        "offheap-resources=main:512MB",
        "stripe.1.node.1.port=9410",
        "stripe.1.node.1.public-port=",
        "stripe.1.node.1.public-hostname=",
        "stripe.1.node.1.group-port=9430",
        "stripe.1.node.1.bind-address=0.0.0.0",
        "stripe.1.node.1.group-bind-address=0.0.0.0",
        "stripe.1.node.1.metadata-dir=%H/terracotta/metadata",
        "stripe.1.node.1.log-dir=%H/terracotta/logs",
        "stripe.1.node.1.logger-overrides=",
        "stripe.1.node.1.backup-dir=",
        "stripe.1.node.1.tc-properties=",
        "stripe.1.node.1.security-dir=",
        "stripe.1.node.1.audit-log-dir=",
        "stripe.1.node.1.data-dirs=main:%H/terracotta/user-data/main"
    );
    verifyNoMoreInteractions(substitutor);
  }

  @Test
  public void test_parsing_minimal() {
    // minimal config is to only have hostname, but to facilitate testing we add name
    assertConfigEquals(
        config(
            "failover-priority=availability",
            "stripe.1.node.1.name=node1",
            "stripe.1.node.1.hostname=localhost"
        ),
        Cluster.newDefaultCluster(new Stripe(Node.newDefaultNode("node1", "localhost"))),
        "cluster-name=",
        "client-reconnect-window=120s",
        "client-lease-duration=150s",
        "authc=",
        "ssl-tls=false",
        "permit-list=false",
        "offheap-resources=main:512MB",
        "stripe.1.node.1.port=9410",
        "stripe.1.node.1.public-port=",
        "stripe.1.node.1.public-hostname=",
        "stripe.1.node.1.group-port=9430",
        "stripe.1.node.1.bind-address=0.0.0.0",
        "stripe.1.node.1.group-bind-address=0.0.0.0",
        "stripe.1.node.1.metadata-dir=%H/terracotta/metadata",
        "stripe.1.node.1.log-dir=%H/terracotta/logs",
        "stripe.1.node.1.logger-overrides=",
        "stripe.1.node.1.backup-dir=",
        "stripe.1.node.1.tc-properties=",
        "stripe.1.node.1.security-dir=",
        "stripe.1.node.1.audit-log-dir=",
        "stripe.1.node.1.data-dirs=main:%H/terracotta/user-data/main"
    );
    verifyNoMoreInteractions(substitutor);
  }

  @Test
  public void test_parsing_minimal_2x2() {
    assertConfigEquals(
        config("failover-priority=availability",
            "stripe.1.node.1.name=node1",
            "stripe.1.node.1.hostname=localhost",
            "stripe.1.node.2.name=node2",
            "stripe.1.node.2.hostname=localhost",
            "stripe.2.node.1.name=node1",
            "stripe.2.node.1.hostname=localhost",
            "stripe.2.node.2.name=node2",
            "stripe.2.node.2.hostname=localhost"
        ),
        Cluster.newDefaultCluster(
            new Stripe(
                Node.newDefaultNode("node1", "localhost"),
                Node.newDefaultNode("node2", "localhost")),
            new Stripe(
                Node.newDefaultNode("node1", "localhost"),
                Node.newDefaultNode("node2", "localhost"))
        ),
        "cluster-name=",
        "client-reconnect-window=120s",
        "client-lease-duration=150s",
        "authc=",
        "ssl-tls=false",
        "permit-list=false",
        "offheap-resources=main:512MB",
        "stripe.1.node.1.port=9410",
        "stripe.1.node.1.public-port=",
        "stripe.1.node.1.public-hostname=",
        "stripe.1.node.1.group-port=9430",
        "stripe.1.node.1.bind-address=0.0.0.0",
        "stripe.1.node.1.group-bind-address=0.0.0.0",
        "stripe.1.node.1.metadata-dir=%H/terracotta/metadata",
        "stripe.1.node.1.log-dir=%H/terracotta/logs",
        "stripe.1.node.1.logger-overrides=",
        "stripe.1.node.1.backup-dir=",
        "stripe.1.node.1.tc-properties=",
        "stripe.1.node.1.security-dir=",
        "stripe.1.node.1.audit-log-dir=",
        "stripe.1.node.1.data-dirs=main:%H/terracotta/user-data/main",
        "stripe.1.node.2.port=9410",
        "stripe.1.node.2.public-port=",
        "stripe.1.node.2.public-hostname=",
        "stripe.1.node.2.group-port=9430",
        "stripe.1.node.2.bind-address=0.0.0.0",
        "stripe.1.node.2.group-bind-address=0.0.0.0",
        "stripe.1.node.2.metadata-dir=%H/terracotta/metadata",
        "stripe.1.node.2.log-dir=%H/terracotta/logs",
        "stripe.1.node.2.logger-overrides=",
        "stripe.1.node.2.backup-dir=",
        "stripe.1.node.2.tc-properties=",
        "stripe.1.node.2.security-dir=",
        "stripe.1.node.2.audit-log-dir=",
        "stripe.1.node.2.data-dirs=main:%H/terracotta/user-data/main",
        "stripe.2.node.1.port=9410",
        "stripe.2.node.1.public-port=",
        "stripe.2.node.1.public-hostname=",
        "stripe.2.node.1.group-port=9430",
        "stripe.2.node.1.bind-address=0.0.0.0",
        "stripe.2.node.1.group-bind-address=0.0.0.0",
        "stripe.2.node.1.metadata-dir=%H/terracotta/metadata",
        "stripe.2.node.1.log-dir=%H/terracotta/logs",
        "stripe.2.node.1.logger-overrides=",
        "stripe.2.node.1.backup-dir=",
        "stripe.2.node.1.tc-properties=",
        "stripe.2.node.1.security-dir=",
        "stripe.2.node.1.audit-log-dir=",
        "stripe.2.node.1.data-dirs=main:%H/terracotta/user-data/main",
        "stripe.2.node.2.port=9410",
        "stripe.2.node.2.public-port=",
        "stripe.2.node.2.public-hostname=",
        "stripe.2.node.2.group-port=9430",
        "stripe.2.node.2.bind-address=0.0.0.0",
        "stripe.2.node.2.group-bind-address=0.0.0.0",
        "stripe.2.node.2.metadata-dir=%H/terracotta/metadata",
        "stripe.2.node.2.log-dir=%H/terracotta/logs",
        "stripe.2.node.2.logger-overrides=",
        "stripe.2.node.2.backup-dir=",
        "stripe.2.node.2.tc-properties=",
        "stripe.2.node.2.security-dir=",
        "stripe.2.node.2.audit-log-dir=",
        "stripe.2.node.2.data-dirs=main:%H/terracotta/user-data/main"
    );
    verifyNoMoreInteractions(substitutor);
  }

  @Test
  public void test_parsing_complete_1x1() {
    // minimal config is to only have hostname, but to facilitate testing we add name
    assertConfigEquals(
        config(
            "stripe.1.node.1.name=node1",
            "stripe.1.node.1.hostname=localhost",
            "cluster-name=foo",
            "client-reconnect-window=120s",
            "failover-priority=availability",
            "client-lease-duration=150s",
            "authc=",
            "ssl-tls=false",
            "permit-list=false",
            "offheap-resources=main:512MB",
            "stripe.1.node.1.port=9410",
            "stripe.1.node.1.public-port=",
            "stripe.1.node.1.public-hostname=",
            "stripe.1.node.1.group-port=9430",
            "stripe.1.node.1.bind-address=0.0.0.0",
            "stripe.1.node.1.group-bind-address=0.0.0.0",
            "stripe.1.node.1.metadata-dir=%H/terracotta/metadata",
            "stripe.1.node.1.log-dir=%H/terracotta/logs",
            "stripe.1.node.1.logger-overrides=",
            "stripe.1.node.1.backup-dir=",
            "stripe.1.node.1.tc-properties=",
            "stripe.1.node.1.security-dir=",
            "stripe.1.node.1.audit-log-dir=",
            "stripe.1.node.1.data-dirs=main:%H/terracotta/user-data/main"
        ),
        Cluster.newDefaultCluster("foo", new Stripe(Node.newDefaultNode("node1", "localhost"))));
    verifyNoMoreInteractions(substitutor);
  }

  @Test
  public void test_setting_with_default_can_be_ommitted() {
    final Properties properties = Cluster.newDefaultCluster("foo", new Stripe(Node.newDefaultNode("node1", "localhost")))
        .setClientLeaseDuration(null)
        .toProperties(false, false);
    assertThat(properties, not(hasKey("client-lease-duration")));

    assertConfigEquals(
        config(
            "stripe.1.node.1.name=node1",
            "stripe.1.node.1.hostname=localhost",
            "cluster-name=foo",
            "client-reconnect-window=120s",
            "failover-priority=availability",
            "authc=",
            "ssl-tls=false",
            "permit-list=false",
            "offheap-resources=main:512MB",
            "stripe.1.node.1.port=9410",
            "stripe.1.node.1.public-port=",
            "stripe.1.node.1.public-hostname=",
            "stripe.1.node.1.group-port=9430",
            "stripe.1.node.1.bind-address=0.0.0.0",
            "stripe.1.node.1.group-bind-address=0.0.0.0",
            "stripe.1.node.1.metadata-dir=%H/terracotta/metadata",
            "stripe.1.node.1.log-dir=%H/terracotta/logs",
            "stripe.1.node.1.logger-overrides=",
            "stripe.1.node.1.backup-dir=",
            "stripe.1.node.1.tc-properties=",
            "stripe.1.node.1.security-dir=",
            "stripe.1.node.1.audit-log-dir=",
            "stripe.1.node.1.data-dirs=main:%H/terracotta/user-data/main"
        ),
        Cluster.newDefaultCluster("foo", new Stripe(Node.newDefaultNode("node1", "localhost"))),
        "client-lease-duration=150s");

    assertConfigEquals(
        config(
            "stripe.1.node.1.name=node1",
            "stripe.1.node.1.hostname=localhost",
            "cluster-name=foo",
            "client-reconnect-window=120s",
            "client-lease-duration=150s",
            "failover-priority=availability",
            "authc=",
            "ssl-tls=false",
            "permit-list=false",
            "offheap-resources=main:512MB",
            "stripe.1.node.1.port=9410",
            "stripe.1.node.1.public-port=",
            "stripe.1.node.1.public-hostname=",
            "stripe.1.node.1.group-port=9430",
            "stripe.1.node.1.bind-address=0.0.0.0",
            "stripe.1.node.1.group-bind-address=0.0.0.0",
            "stripe.1.node.1.metadata-dir=%H/terracotta/metadata",
            "stripe.1.node.1.log-dir=%H/terracotta/logs",
            "stripe.1.node.1.logger-overrides=",
            "stripe.1.node.1.backup-dir=",
            "stripe.1.node.1.tc-properties=",
            "stripe.1.node.1.security-dir=",
            "stripe.1.node.1.audit-log-dir=",
            "stripe.1.node.1.data-dirs=main:%H/terracotta/user-data/main"
        ),
        Cluster.newDefaultCluster("foo", new Stripe(Node.newDefaultNode("node1", "localhost"))));
  }

  @SuppressWarnings("OptionalGetWithoutIsPresent")
  private void assertCliEquals(Map<Setting, String> params, Cluster cluster, String... addedConfigurations) {
    Cluster built = ConfigurationParser.parseCommandLineParameters(params, substitutor, added::add);

    // since node name is generated when not given,
    // this is a hack that will reset to null only the node names that have been generated
    String nodeName = built.getSingleNode().get().getNodeName();
    cluster.getSingleNode()
        .filter(node -> node.getNodeName().equals("<GENERATED>"))
        .ifPresent(node -> node.setNodeName(nodeName));

    Configuration[] configurations = Stream.of(addedConfigurations)
        .map(string -> string.replace("<GENERATED>", nodeName))
        .map(string -> string.replace("/", File.separator)) // unix/win compat'
        .map(Configuration::valueOf)
        .toArray(Configuration[]::new);

    assertThat(built, is(equalTo(cluster)));
    assertThat(added.stream().map(c -> "\"" + c + "\"").collect(Collectors.joining(",\n")), added.size(), is(equalTo(configurations.length)));
    assertThat(added, hasItems(configurations));
    added.clear();
  }

  private void assertConfigEquals(Properties config, Cluster cluster, String... addedConfigurations) {
    Cluster built = ConfigurationParser.parsePropertyConfiguration(config, added::add);
    Configuration[] configurations = Stream.of(addedConfigurations)
        .map(string -> string.replace("/", File.separator)) // unix/win compat'
        .map(Configuration::valueOf)
        .toArray(Configuration[]::new);
    assertThat(built, is(equalTo(cluster)));
    assertThat(added.stream().map(c -> "\"" + c + "\"").collect(Collectors.joining(",\n")), added.size(), is(equalTo(configurations.length)));
    assertThat(added, hasItems(configurations));
    added.clear();
  }

  private void assertConfigFail(Properties config, String err) {
    err = err.replace("/", File.separator); // unix/win compat'
    assertThat(
        () -> ConfigurationParser.parsePropertyConfiguration(config, added::add),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo(err)))));
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