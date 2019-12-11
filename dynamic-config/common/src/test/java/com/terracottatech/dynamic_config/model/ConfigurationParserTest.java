/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.model;

import com.terracottatech.dynamic_config.util.IParameterSubstitutor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.terracottatech.utilities.hamcrest.ExceptionMatcher.throwing;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

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
  }

  @Test
  public void test_cliToProperties_1() {
    // node name should be resolved from default value (%h) if not given
    assertCliEquals(
        cli(),
        new Cluster(new Stripe(Node.newDefaultNode("<GENERATED>", "localhost"))),
        "stripe.1.node.1.node-hostname=localhost",
        "cluster-name=",
        "client-reconnect-window=120s",
        "failover-priority=availability",
        "client-lease-duration=150s",
        "security-authc=",
        "security-ssl-tls=false",
        "security-whitelist=false",
        "offheap-resources=main:512MB",
        "stripe.1.node.1.node-name=<GENERATED>",
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
    );
    verify(substitutor, times(1)).substitute("%h");
    verifyNoMoreInteractions(substitutor);
  }

  @Test
  public void test_cliToProperties_2() {
    // placeholder in node name should be resolved eagerly
    assertCliEquals(
        cli("node-hostname=%c"),
        new Cluster(new Stripe(Node.newDefaultNode("<GENERATED>", "localhost.home"))),
        "cluster-name=",
        "client-reconnect-window=120s",
        "failover-priority=availability",
        "client-lease-duration=150s",
        "security-authc=",
        "security-ssl-tls=false",
        "security-whitelist=false",
        "offheap-resources=main:512MB",
        "stripe.1.node.1.node-name=<GENERATED>",
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
    );
    verify(substitutor).substitute("%c");
    verifyNoMoreInteractions(substitutor);
  }

  @Test
  public void test_cliToProperties_3() {
    // node name without placeholder triggers no resolve
    assertCliEquals(
        cli("node-hostname=foo"),
        new Cluster(new Stripe(Node.newDefaultNode("<GENERATED>", "foo"))),
        "cluster-name=",
        "client-reconnect-window=120s",
        "failover-priority=availability",
        "client-lease-duration=150s",
        "security-authc=",
        "security-ssl-tls=false",
        "security-whitelist=false",
        "offheap-resources=main:512MB",
        "stripe.1.node.1.node-name=<GENERATED>",
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
    );
    verify(substitutor).substitute("foo");
    verifyNoMoreInteractions(substitutor);
  }

  @Test
  public void test_parsing_invalid() {
    // node-hostname required
    assertConfigFail(config(), "No configuration provided");
    assertConfigFail(config("security-ssl-tls=false"), "node-hostname is missing");

    // placeholder forbidden for node-hostname
    assertConfigFail(config("stripe.1.node.1.node-hostname=%h"), "node-hostname cannot contain any placeholders");
    assertConfigFail(config(
        "stripe.1.node.1.node-hostname=localhost",
        "stripe.1.node.2.node-name=foo"
    ), "Invalid input: 'stripe.1.node.2.node-hostname=%h'. Placeholders are not allowed");

    // scope
    assertConfigFail(config("node-hostname=foo"), "Invalid input: 'node-hostname=foo'. Reason: node-hostname cannot be set at cluster level");
    assertConfigFail(config(
        "stripe.1.node.1.node-hostname=localhost",
        "stripe.1.node-backup-dir=foo/bar"
    ), "Invalid input: 'stripe.1.node-backup-dir=foo/bar'. Reason: stripe level configuration not allowed");
    assertConfigFail(config(
        "stripe.1.node.1.node-hostname=localhost",
        "node-backup-dir=foo/bar"
    ), "Invalid settings found at cluster level: node-backup-dir");
    assertConfigFail(config(
        "stripe.1.node.1.node-hostname=localhost",
        "stripe.1.node.1.failover-priority=availability"
    ), "Invalid input: 'stripe.1.node.1.failover-priority=availability'. Reason: failover-priority does not allow any operation at node level");

    // node and stripe ids
    assertConfigFail(config("stripe.1.node.2.node-hostname=localhost"), "Node ID must start at 1 in stripe 1");
    assertConfigFail(config("stripe.2.node.1.node-hostname=localhost"), "Stripe ID must start at 1");
    assertConfigFail(config(
        "stripe.1.node.1.node-hostname=localhost",
        "stripe.1.node.2.node-hostname=localhost",
        "stripe.1.node.4.node-hostname=localhost"
    ), "Node ID must end at 3 in stripe 1");
    assertConfigFail(config(
        "stripe.1.node.1.node-hostname=localhost",
        "stripe.2.node.1.node-hostname=localhost",
        "stripe.4.node.1.node-hostname=localhost"
    ), "Stripe ID must end at 3");

    // not allowed in config
    assertConfigFail(config(
        "stripe.1.node.1.node-hostname=localhost",
        "stripe.1.node.1.node-repository-dir=foo/bar"
    ), "Invalid input: 'stripe.1.node.1.node-repository-dir=foo/bar'. Reason: node-repository-dir does not allow any operation at node level");
    assertConfigFail(config(
        "stripe.1.node.1.node-hostname=localhost",
        "license-file=foo/bar"
    ), "Invalid settings found at cluster level: license-file");
  }

  @Test
  public void test_parsing_allows_duplicates() {
    // Note about duplicate entries: we do not test them, because they made a property file invalid
    // and we support the fact that the last one will override the previous one
    assertConfigEquals(
        config(
            "stripe.1.node.1.node-name=node1",
            "stripe.1.node.1.node-name=real",
            "stripe.1.node.1.node-hostname=localhost",
            "stripe.1.node.1.node-hostname=foo"
        ),
        new Cluster(new Stripe(Node.newDefaultNode("real", "foo"))),
        "cluster-name=",
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
    );
    verifyNoMoreInteractions(substitutor);
  }

  @Test
  public void test_parsing_minimal() {
    // minimal config is to only have node-hostname, but to facilitate testing we add node-name
    assertConfigEquals(
        config(
            "stripe.1.node.1.node-name=node1",
            "stripe.1.node.1.node-hostname=localhost"
        ),
        new Cluster(new Stripe(Node.newDefaultNode("node1", "localhost"))),
        "cluster-name=",
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
    );
    verifyNoMoreInteractions(substitutor);
  }

  @Test
  public void test_parsing_minimal_2x2() {
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
        new Cluster(
            new Stripe(
                Node.newDefaultNode("node1", "localhost"),
                Node.newDefaultNode("node2", "localhost")),
            new Stripe(
                Node.newDefaultNode("node1", "localhost"),
                Node.newDefaultNode("node2", "localhost"))
        ),
        "cluster-name=",
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
        "stripe.1.node.1.data-dirs=main:%H/terracotta/user-data/main",
        "stripe.1.node.2.node-port=9410",
        "stripe.1.node.2.node-group-port=9430",
        "stripe.1.node.2.node-bind-address=0.0.0.0",
        "stripe.1.node.2.node-group-bind-address=0.0.0.0",
        "stripe.1.node.2.node-metadata-dir=%H/terracotta/metadata",
        "stripe.1.node.2.node-log-dir=%H/terracotta/logs",
        "stripe.1.node.2.node-backup-dir=",
        "stripe.1.node.2.tc-properties=",
        "stripe.1.node.2.security-dir=",
        "stripe.1.node.2.security-audit-log-dir=",
        "stripe.1.node.2.data-dirs=main:%H/terracotta/user-data/main",
        "stripe.2.node.1.node-port=9410",
        "stripe.2.node.1.node-group-port=9430",
        "stripe.2.node.1.node-bind-address=0.0.0.0",
        "stripe.2.node.1.node-group-bind-address=0.0.0.0",
        "stripe.2.node.1.node-metadata-dir=%H/terracotta/metadata",
        "stripe.2.node.1.node-log-dir=%H/terracotta/logs",
        "stripe.2.node.1.node-backup-dir=",
        "stripe.2.node.1.tc-properties=",
        "stripe.2.node.1.security-dir=",
        "stripe.2.node.1.security-audit-log-dir=",
        "stripe.2.node.1.data-dirs=main:%H/terracotta/user-data/main",
        "stripe.2.node.2.node-port=9410",
        "stripe.2.node.2.node-group-port=9430",
        "stripe.2.node.2.node-bind-address=0.0.0.0",
        "stripe.2.node.2.node-group-bind-address=0.0.0.0",
        "stripe.2.node.2.node-metadata-dir=%H/terracotta/metadata",
        "stripe.2.node.2.node-log-dir=%H/terracotta/logs",
        "stripe.2.node.2.node-backup-dir=",
        "stripe.2.node.2.tc-properties=",
        "stripe.2.node.2.security-dir=",
        "stripe.2.node.2.security-audit-log-dir=",
        "stripe.2.node.2.data-dirs=main:%H/terracotta/user-data/main"
    );
    verifyNoMoreInteractions(substitutor);
  }

  @Test
  public void test_parsing_complete_1x1() {
    // minimal config is to only have node-hostname, but to facilitate testing we add node-name
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
        new Cluster("foo", new Stripe(Node.newDefaultNode("node1", "localhost"))));
    verifyNoMoreInteractions(substitutor);
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