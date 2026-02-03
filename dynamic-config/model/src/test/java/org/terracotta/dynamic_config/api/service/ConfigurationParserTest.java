/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2026
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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.terracotta.common.struct.MemoryUnit;
import org.terracotta.common.struct.TimeUnit;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Configuration;
import org.terracotta.dynamic_config.api.model.RawPath;
import org.terracotta.dynamic_config.api.model.Setting;
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.dynamic_config.api.model.Testing;
import org.terracotta.dynamic_config.api.model.Version;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptyMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.terracotta.dynamic_config.api.model.FailoverPriority.availability;
import static org.terracotta.testing.ExceptionMatcher.throwing;

/**
 * @author Mathieu Carbou
 */
@RunWith(MockitoJUnitRunner.class)
public class ConfigurationParserTest {

  private final List<Configuration> added = new ArrayList<>();

  private final Consumer<Configuration> addedListener = configuration -> {
    if (configuration.getSetting().toString().endsWith("-uid") || configuration.getValue().get().startsWith("node-") || configuration.getValue().get().startsWith("stripe-")) {
      added.add(Configuration.valueOf(configuration.toString().replace(configuration.getValue().get(), "<GENERATED>")));
    } else {
      added.add(configuration);
    }
  };

  public IParameterSubstitutor substitutor = source -> {
    if (source == null) {
      return null;
    }
    switch (source) {
      case "%h":
        return "localhost";
      case "%c":
        return "localhost.home";
      case "%H":
        return "home";
      default:
        return source;
    }
  };

  @Test
  public void test_cliToProperties_1() {
    // node hostname should be resolved from default value (%h) if not given
    assertCliEquals(
        cli("failover-priority=availability"),
        Testing.newTestCluster(new Stripe().addNodes(Testing.newTestNode("<GENERATED>", "localhost")))
            .setFailoverPriority(availability()),
        "stripe.1.node.1.name=<GENERATED>",
        "stripe.1.stripe-name=<GENERATED>",
        "stripe.1.node.1.node-uid=<GENERATED>",
        "stripe.1.stripe-uid=<GENERATED>",
        "cluster-uid=<GENERATED>",
        "stripe.1.node.1.hostname=localhost"
    );
  }

  @Test
  public void test_cli_no_logDir() {
    // node hostname should be resolved from default value (%h) if not given
    assertCliEquals(
        cli("failover-priority=availability", "log-dir="),
        Testing.newTestCluster(new Stripe().addNodes(Testing.newTestNode("<GENERATED>", "localhost").setLogDir(null)))
            .setFailoverPriority(availability()),
        "stripe.1.node.1.name=<GENERATED>",
        "stripe.1.stripe-name=<GENERATED>",
        "stripe.1.node.1.node-uid=<GENERATED>",
        "stripe.1.stripe-uid=<GENERATED>",
        "cluster-uid=<GENERATED>",
        "stripe.1.node.1.hostname=localhost"
    );
  }

  @Test
  public void test_cliToProperties_2() {
    // placeholder in node hostname should be resolved eagerly
    assertCliEquals(
        cli("failover-priority=availability", "hostname=%c"),
        Testing.newTestCluster(new Stripe().addNodes(Testing.newTestNode("<GENERATED>", "localhost.home")))
            .setFailoverPriority(availability()),
        "stripe.1.node.1.name=<GENERATED>",
        "stripe.1.stripe-name=<GENERATED>",
        "stripe.1.node.1.node-uid=<GENERATED>",
        "stripe.1.stripe-uid=<GENERATED>",
        "cluster-uid=<GENERATED>"
    );
  }

  @Test
  public void test_cliToProperties_3() {
    // node hostname without placeholder triggers no resolve
    assertCliEquals(
        cli("failover-priority=availability", "hostname=foo"),
        Testing.newTestCluster(new Stripe().addNodes(Testing.newTestNode("<GENERATED>", "foo")))
            .setFailoverPriority(availability()),
        "stripe.1.node.1.name=<GENERATED>",
        "stripe.1.stripe-name=<GENERATED>",
        "stripe.1.node.1.node-uid=<GENERATED>",
        "stripe.1.stripe-uid=<GENERATED>",
        "cluster-uid=<GENERATED>"
    );
  }

  @Test
  public void test_parsing_invalid() {
    // hostname required
    assertConfigFail(config(), "No configuration provided");

    // placeholder forbidden for hostname
    assertConfigFail(config("failover-priority=availability", "stripe.1.node.1.hostname=%h"), "Invalid input: 'stripe.1.node.1.hostname=%h'. Reason: Placeholders are not allowed");
    assertConfigFail(config(
        "failover-priority=availability",
        "stripe.1.stripe-name=<GENERATED>",
        "stripe.1.node.1.hostname=localhost",
        "stripe.1.node.2.name=foo"
    ), "Required setting: 'hostname' is missing for node ID: 2 in stripe ID: 1");

    // scope
    assertConfigFail(config("failover-priority=availability", "hostname=foo"), "Invalid input: 'hostname=foo'. Reason: Setting 'hostname' cannot be set at cluster level");
    assertConfigFail(config(
        "failover-priority=availability",
        "stripe.1.stripe-name=<GENERATED>",
        "stripe.1.node.1.hostname=localhost",
        "stripe.1.backup-dir=foo/bar"
    ), "Invalid input: 'stripe.1.backup-dir=foo/bar'. Reason: Setting 'backup-dir' cannot be import at stripe level when node is configuring");
    assertConfigFail(config(
        "failover-priority=availability",
        "stripe.1.stripe-name=<GENERATED>",
        "stripe.1.node.1.hostname=localhost",
        "backup-dir=foo/bar"
    ), "Invalid input: 'backup-dir=foo/bar'. Reason: Setting 'backup-dir' cannot be import at cluster level when node is configuring");
    assertConfigFail(config(
        "failover-priority=availability",
        "stripe.1.stripe-name=<GENERATED>",
        "stripe.1.node.1.hostname=localhost",
        "stripe.1.node.1.failover-priority=availability"
    ), "Invalid input: 'stripe.1.node.1.failover-priority=availability'. Reason: Setting 'failover-priority' does not allow any operation at node level");

    // node and stripe ids
    assertConfigFail(config("failover-priority=availability", "stripe.1.node.2.hostname=localhost"), "Node ID must start at 1 in stripe 1");
    assertConfigFail(config("failover-priority=availability", "stripe.2.node.1.hostname=localhost"), "Stripe ID must start at 1");
    assertConfigFail(config(
        "failover-priority=availability",
        "stripe.1.stripe-name=<GENERATED>",
        "stripe.1.node.1.hostname=localhost",
        "stripe.1.node.2.hostname=localhost",
        "stripe.1.node.4.hostname=localhost"
    ), "Node ID must end at 3 in stripe 1");
    assertConfigFail(config(
        "failover-priority=availability",
        "stripe.1.stripe-name=<GENERATED>",
        "stripe.1.node.1.hostname=localhost",
        "stripe.2.node.1.hostname=localhost",
        "stripe.4.node.1.hostname=localhost"
    ), "Stripe ID must end at 3");

    // not allowed in config
    assertConfigFail(config(
        "failover-priority=availability",
        "stripe.1.stripe-name=<GENERATED>",
        "stripe.1.node.1.hostname=localhost",
        "stripe.1.node.1.config-dir=foo/bar"
    ), "Invalid input: 'stripe.1.node.1.config-dir=foo/bar'. Reason: Setting 'config-dir' does not allow any operation at node level");
    assertConfigFail(config(
        "failover-priority=availability",
        "stripe.1.stripe-name=<GENERATED>",
        "stripe.1.node.1.hostname=localhost",
        "license-file=foo/bar"
    ), "Invalid input: 'license-file=foo/bar'. Reason: now allowed");
  }

  @Test
  public void test_parsing_allows_duplicates() {
    // Note about duplicate entries: we do not test them, because they made a property file invalid,
    // and we support the fact that the last one will override the previous one
    assertConfigEquals(
        config(
            "failover-priority=availability",
            "stripe.1.stripe-name=<GENERATED>",
            "stripe.1.node.1.name=node1",
            "stripe.1.node.1.name=real",
            "stripe.1.node.1.hostname=localhost",
            "stripe.1.node.1.hostname=foo"
        ),
        Testing.newTestCluster(new Stripe().addNodes(Testing.newTestNode("real", "foo"))),
        "cluster-uid=<GENERATED>",
        "stripe.1.stripe-uid=<GENERATED>",
        "stripe.1.node.1.node-uid=<GENERATED>"
    );
  }

  @Test
  public void test_parsing_minimal() {
    // minimal config is to only have hostname, but to facilitate testing we add name
    assertConfigEquals(
        config(
            "failover-priority=availability",
            "stripe.1.stripe-name=<GENERATED>",
            "stripe.1.node.1.name=node1",
            "stripe.1.node.1.hostname=localhost"
        ),
        Testing.newTestCluster(new Stripe().addNodes(Testing.newTestNode("node1", "localhost"))),
        "cluster-uid=<GENERATED>",
        "stripe.1.stripe-uid=<GENERATED>",
        "stripe.1.node.1.node-uid=<GENERATED>"
    );
  }

  @Test
  public void test_parsing_minimal_2x2() {
    assertConfigEquals(
        config("failover-priority=availability",
            "stripe.1.stripe-name=<GENERATED>",
            "stripe.1.node.1.name=node1",
            "stripe.1.node.1.hostname=localhost",
            "stripe.1.node.2.name=node2",
            "stripe.1.node.2.hostname=localhost",
            "stripe.2.stripe-name=<GENERATED>",
            "stripe.2.node.1.name=node1",
            "stripe.2.node.1.hostname=localhost",
            "stripe.2.node.2.name=node2",
            "stripe.2.node.2.hostname=localhost"
        ),
        Testing.newTestCluster(
            new Stripe().addNodes(
                Testing.newTestNode("node1", "localhost"),
                Testing.newTestNode("node2", "localhost")),
            new Stripe().addNodes(
                Testing.newTestNode("node1", "localhost"),
                Testing.newTestNode("node2", "localhost"))
        ),
        "cluster-uid=<GENERATED>",
        "stripe.1.stripe-uid=<GENERATED>",
        "stripe.1.node.1.node-uid=<GENERATED>",
        "stripe.1.node.2.node-uid=<GENERATED>",
        "stripe.2.stripe-uid=<GENERATED>",
        "stripe.2.node.1.node-uid=<GENERATED>",
        "stripe.2.node.2.node-uid=<GENERATED>"
    );
  }

  @Test
  public void test_parsing_complete_1x1_minimal() {
    // minimal config is to only have hostname, but to facilitate testing we add name
    assertConfigEquals(
        config(
            "stripe.1.stripe-name=<GENERATED>",
            "stripe.1.node.1.name=node1",
            "stripe.1.node.1.hostname=localhost",
            "cluster-name=foo"
        ),
        Testing.newTestCluster("foo", new Stripe().addNodes(Testing.newTestNode("node1", "localhost"))).setFailoverPriority(null),
        "cluster-uid=<GENERATED>",
        "stripe.1.stripe-uid=<GENERATED>",
        "stripe.1.node.1.node-uid=<GENERATED>"
    );
  }

  @Test
  public void test_parsing_no_log_dir() {
    // minimal config is to only have hostname, but to facilitate testing we add name
    assertConfigEquals(
        config(
            "stripe.1.stripe-name=<GENERATED>",
            "stripe.1.node.1.name=node1",
            "stripe.1.node.1.hostname=localhost",
            "stripe.1.node.1.log-dir=",
            "cluster-name=foo"
        ),
        Testing.newTestCluster("foo", new Stripe().addNodes(Testing.newTestNode("node1", "localhost").setLogDir(null))).setFailoverPriority(null),
        "cluster-uid=<GENERATED>",
        "stripe.1.stripe-uid=<GENERATED>",
        "stripe.1.node.1.node-uid=<GENERATED>"
    );
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
            "lock-context=",
            "authc=",
            "ssl-tls=false",
            "whitelist=false",
            "offheap-resources=main:512MB",
            "stripe.1.stripe-name=<GENERATED>",
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
            "stripe.1.node.1.relay-source-hostname=",
            "stripe.1.node.1.relay-source-port=",
            "stripe.1.node.1.relay-destination-hostname=",
            "stripe.1.node.1.relay-destination-port=",
            "stripe.1.node.1.relay-destination-group-port="
        ),
        Testing.newTestCluster("foo", new Stripe().addNodes(Testing.newTestNode("node1", "localhost")
            .setPort(9410)
            .setGroupPort(9430)
            .setBindAddress("0.0.0.0")
            .setGroupBindAddress("0.0.0.0")
            .setMetadataDir(RawPath.valueOf("%H/terracotta/metadata"))
            .setLogDir(RawPath.valueOf("%H/terracotta/logs"))
            .setLoggerOverrides(emptyMap())
            .setTcProperties(emptyMap())
            .putDataDir("main", RawPath.valueOf("%H/terracotta/user-data/main"))
        ))
            .setClientReconnectWindow(120, TimeUnit.SECONDS)
            .setFailoverPriority(availability())
            .setClientLeaseDuration(150, TimeUnit.SECONDS)
            .setSecuritySslTls(false)
            .setSecurityWhitelist(false)
            .putOffheapResource("main", 512, MemoryUnit.MB),
        "cluster-uid=<GENERATED>",
        "stripe.1.stripe-uid=<GENERATED>",
        "stripe.1.node.1.node-uid=<GENERATED>"
    );
  }

  @Test
  public void test_setting_with_default_can_be_ommitted() {
    Cluster cluster = Testing.newTestCluster("foo", new Stripe().addNodes(Testing.newTestNode("node1", "localhost")));

    Properties properties = cluster.toProperties(false, false, true);
    assertThat(properties.toString(), properties, not(hasKey("client-lease-duration")));

    properties = cluster.toProperties(false, true, true);
    assertThat(properties.toString(), properties, hasKey("client-lease-duration"));

    assertConfigEquals(
        config(
            "cluster-name=foo",
            "stripe.1.stripe-name=<GENERATED>",
            "stripe.1.node.1.name=node1",
            "stripe.1.node.1.hostname=localhost",
            "failover-priority=availability"
        ),
        Testing.newTestCluster("foo", new Stripe().addNodes(Testing.newTestNode("node1", "localhost"))),
        "cluster-uid=<GENERATED>",
        "stripe.1.stripe-uid=<GENERATED>",
        "stripe.1.node.1.node-uid=<GENERATED>"
    );
  }

  @SuppressWarnings("OptionalGetWithoutIsPresent")
  private void assertCliEquals(Map<Setting, String> params, Cluster expectedCluster, String... addedConfigurations) {
    Testing.resetRequiredUIDs(expectedCluster, Testing.A_UID);
    Testing.resetRequiredNames(expectedCluster, "<GENERATED>");

    Cluster built = ConfigurationParser.parseCommandLineParameters(params, substitutor, addedListener);

    Testing.replaceRequiredUIDs(built, Testing.A_UID);
    Testing.replaceRequiredNames(built, "<GENERATED>");

    // since node name is generated when not given,
    // this is a hack that will reset to null only the node names that have been generated
    String nodeName = built.getSingleNode().get().getName();
    expectedCluster.getSingleNode()
        .filter(node -> node.getName().equals("<GENERATED>"))
        .ifPresent(node -> node.setName(nodeName));

    Configuration[] configurations = Stream.of(addedConfigurations)
        .map(string -> string.replace("<GENERATED>", nodeName))
        .map(Configuration::valueOf)
        .toArray(Configuration[]::new);

    assertThat(built, is(equalTo(expectedCluster)));
    assertThat(added.stream().map(c -> "\"" + c + "\"").collect(Collectors.joining(",\n")), added.size(), is(equalTo(configurations.length)));
    assertThat(added, hasItems(configurations));
    added.clear();
  }

  private void assertConfigEquals(Properties config, Cluster expectedCluster, String... addedConfigurations) {
    Testing.resetRequiredUIDs(expectedCluster, Testing.A_UID);
    Testing.resetRequiredNames(expectedCluster, "<GENERATED>");

    Cluster built = ConfigurationParser.parsePropertyConfiguration(config, Version.CURRENT, addedListener);

    Testing.replaceRequiredUIDs(built, Testing.A_UID);
    Testing.replaceRequiredNames(built, "<GENERATED>");

    Configuration[] configurations = Stream.of(addedConfigurations)
        .map(Configuration::valueOf)
        .toArray(Configuration[]::new);
    assertThat(built, is(equalTo(expectedCluster)));
    assertThat(added.stream().map(c -> "\"" + c + "\"").collect(Collectors.joining(",\n")), added.size(), is(equalTo(configurations.length)));
    assertThat(added, hasItems(configurations));
    added.clear();
  }

  private void assertConfigFail(Properties config, String err) {
    assertThat(
        () -> ConfigurationParser.parsePropertyConfiguration(config, Version.CURRENT, added::add),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo(err)))));
  }

  private static Map<Setting, String> cli(String... params) {
    return Stream.of(params)
        .map(p -> p.split("="))
        .map(kv -> new AbstractMap.SimpleEntry<>(Setting.fromName(kv[0]), kv.length == 2 ? kv[1] : ""))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private static Properties config(String... params) {
    return Stream.of(params)
        .map(p -> p.split("="))
        .reduce(new Properties(), (props, kv) -> {
          props.setProperty(kv[0], kv.length == 1 ? "" : kv[1]);
          return props;
        }, (p1, p2) -> {
          throw new UnsupportedOperationException();
        });
  }
}