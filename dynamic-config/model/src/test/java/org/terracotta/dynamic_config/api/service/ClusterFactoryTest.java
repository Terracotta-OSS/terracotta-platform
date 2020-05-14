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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.terracotta.common.struct.MemoryUnit;
import org.terracotta.dynamic_config.api.json.DynamicConfigModelJsonModule;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.Setting;
import org.terracotta.dynamic_config.api.model.Stripe;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.lenient;
import static org.terracotta.common.struct.Tuple2.tuple2;
import static org.terracotta.dynamic_config.api.model.FailoverPriority.consistency;
import static org.terracotta.dynamic_config.api.model.Node.newDefaultNode;
import static org.terracotta.testing.ExceptionMatcher.throwing;

/**
 * @author Mathieu Carbou
 */
@RunWith(MockitoJUnitRunner.class)
public class ClusterFactoryTest {

  private final ClusterFactory clusterFactory = new ClusterFactory();

  @Mock public IParameterSubstitutor substitutor;

  ObjectMapper json = new ObjectMapper()
      .registerModule(new DynamicConfigModelJsonModule());

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
  public void test_create_cli() {
    assertCliEquals(cli("failover-priority=availability"), Cluster.newDefaultCluster(new Stripe(Node.newDefaultNode("<GENERATED>", "localhost"))));
    assertCliEquals(cli("failover-priority=availability", "hostname=%c"), Cluster.newDefaultCluster(new Stripe(Node.newDefaultNode("<GENERATED>", "localhost.home"))));
    assertCliEquals(cli("failover-priority=availability", "hostname=foo"), Cluster.newDefaultCluster(new Stripe(Node.newDefaultNode("<GENERATED>", "foo"))));
  }

  @Test
  public void test_create_cli_validated() {
    assertCliFail(cli("failover-priority=availability", "ssl-tls=false", "authc=certificate"), "ssl-tls is required for authc=certificate");
    assertCliFail(cli("failover-priority=availability", "ssl-tls=true", "authc=certificate"), "security-dir is mandatory for any of the security configuration");
    assertCliFail(cli("failover-priority=availability", "ssl-tls=true"), "security-dir is mandatory for any of the security configuration");
    assertCliFail(cli("failover-priority=availability", "authc=file"), "security-dir is mandatory for any of the security configuration");
    assertCliFail(cli("failover-priority=availability", "authc=ldap"), "security-dir is mandatory for any of the security configuration");
    assertCliFail(cli("failover-priority=availability", "audit-log-dir=foo"), "security-dir is mandatory for any of the security configuration");
    assertCliFail(cli("failover-priority=availability", "whitelist=true"), "security-dir is mandatory for any of the security configuration");
    assertCliFail(cli("failover-priority=availability", "security-dir=foo"), "One of ssl-tls, authc, or whitelist is required for security configuration");
  }

  @Test
  public void test_create_config() {
    assertConfigEquals(
        config(
            "failover-priority=availability",
            "stripe.1.node.1.name=node1",
            "stripe.1.node.1.name=real",
            "stripe.1.node.1.hostname=localhost",
            "stripe.1.node.1.hostname=foo"
        ),
        Cluster.newDefaultCluster(new Stripe(Node.newDefaultNode("real", "foo"))));

    assertConfigEquals(
        config(
            "failover-priority=availability",
            "stripe.1.node.1.name=node1",
            "stripe.1.node.1.hostname=localhost"
        ),
        Cluster.newDefaultCluster(new Stripe(Node.newDefaultNode("node1", "localhost"))));

    assertConfigEquals(
        config(
            "failover-priority=availability",
            "stripe.1.node.1.name=node1",
            "stripe.1.node.1.hostname=localhost1",
            "stripe.1.node.2.name=node2",
            "stripe.1.node.2.hostname=localhost2",
            "stripe.2.node.1.name=node1",
            "stripe.2.node.1.hostname=localhost3",
            "stripe.2.node.2.name=node2",
            "stripe.2.node.2.hostname=localhost4"
        ),
        Cluster.newDefaultCluster(
            new Stripe(
                Node.newDefaultNode("node1", "localhost1"),
                Node.newDefaultNode("node2", "localhost2")),
            new Stripe(
                Node.newDefaultNode("node1", "localhost3"),
                Node.newDefaultNode("node2", "localhost4"))
        ));

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
            "whitelist=false",
            "offheap-resources=main:512MB",
            "stripe.1.node.1.port=9410",
            "stripe.1.node.1.group-port=9430",
            "stripe.1.node.1.bind-address=0.0.0.0",
            "stripe.1.node.1.group-bind-address=0.0.0.0",
            "stripe.1.node.1.metadata-dir=%H/terracotta/metadata",
            "stripe.1.node.1.log-dir=%H/terracotta/logs",
            "stripe.1.node.1.backup-dir=",
            "stripe.1.node.1.tc-properties=",
            "stripe.1.node.1.security-dir=",
            "stripe.1.node.1.audit-log-dir=",
            "stripe.1.node.1.data-dirs=main:%H/terracotta/user-data/main"
        ),
        Cluster.newDefaultCluster("foo", new Stripe(Node.newDefaultNode("node1", "localhost"))));

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
            "whitelist=false",
            "offheap-resources=main:512MB",
            "stripe.1.node.1.port=9410",
            "stripe.1.node.1.group-port=9430",
            "stripe.1.node.1.bind-address=0.0.0.0",
            "stripe.1.node.1.group-bind-address=0.0.0.0",
            "stripe.1.node.1.metadata-dir=%H/terracotta/metadata",
            "stripe.1.node.1.log-dir=%H/terracotta/logs",
            "stripe.1.node.1.backup-dir=",
            "stripe.1.node.1.tc-properties=",
            "stripe.1.node.1.security-dir=",
            "stripe.1.node.1.audit-log-dir=",
            "stripe.1.node.1.data-dirs=main:%H/terracotta/user-data/main"
        ),
        Cluster.newDefaultCluster("foo", new Stripe(Node.newDefaultNode("node1", "localhost"))));
  }

  @Test
  public void test_create_config_validated() {
    // security
    assertConfigFail(
        config("failover-priority=availability", "stripe.1.node.1.hostname=localhost", "ssl-tls=false", "authc=certificate"),
        "ssl-tls is required for authc=certificate");
    assertConfigFail(
        config("failover-priority=availability", "stripe.1.node.1.hostname=localhost", "ssl-tls=true", "authc=certificate"),
        "security-dir is mandatory for any of the security configuration");
    assertConfigFail(
        config("failover-priority=availability", "stripe.1.node.1.hostname=localhost", "ssl-tls=true"),
        "security-dir is mandatory for any of the security configuration");
    assertConfigFail(
        config("failover-priority=availability", "stripe.1.node.1.hostname=localhost", "authc=file"),
        "security-dir is mandatory for any of the security configuration");
    assertConfigFail(
        config("failover-priority=availability", "stripe.1.node.1.hostname=localhost", "authc=ldap"),
        "security-dir is mandatory for any of the security configuration");
    assertConfigFail(
        config("failover-priority=availability", "stripe.1.node.1.hostname=localhost", "stripe.1.node.1.audit-log-dir=foo"),
        "security-dir is mandatory for any of the security configuration");
    assertConfigFail(
        config("failover-priority=availability", "stripe.1.node.1.hostname=localhost", "whitelist=true"),
        "security-dir is mandatory for any of the security configuration");
    assertConfigFail(
        config("failover-priority=availability", "stripe.1.node.1.hostname=localhost", "stripe.1.node.1.security-dir=foo"),
        "One of ssl-tls, authc, or whitelist is required for security configuration");

    // duplicate node name
    assertConfigFail(
        config(
            "failover-priority=availability",
            "stripe.1.node.1.hostname=localhost1", "stripe.1.node.1.name=foo",
            "stripe.1.node.2.hostname=localhost2", "stripe.1.node.2.name=foo"
        ),
        "Found duplicate node name: foo in stripe 1");

    // Note: all possible failures (including those above) are already tested in ClusterValidatorTest
    // ClusterFactory being much more a wiring class, we do not repeat all tests
  }

  @Test
  public void test_toProperties() {
    Cluster cluster = Cluster.newDefaultCluster("my-cluster", new Stripe(
        newDefaultNode("node-1", "localhost")
            .setDataDir("foo", Paths.get("%H/tc1/foo"))
            .setDataDir("bar", Paths.get("%H/tc1/bar")),
        newDefaultNode("node-2", "localhost")
            .setDataDir("foo", Paths.get("%H/tc2/foo"))
            .setDataDir("bar", Paths.get("%H/tc2/bar"))
            .setTcProperty("server.entity.processor.threads", "64")
            .setTcProperty("topology.validate", "true")))
        .setFailoverPriority(consistency(2))
        .setOffheapResource("foo", 1, MemoryUnit.GB)
        .setOffheapResource("bar", 2, MemoryUnit.GB);

    Stream.of(
        tuple2(cluster.toProperties(false, true), "config_with_defaults.properties"),
        tuple2(cluster.toProperties(false, false), "config_without_defaults.properties"),
        tuple2(cluster.toProperties(true, true), "config_expanded_default.properties"),
        tuple2(cluster.toProperties(true, false), "config_expanded_without_default.properties")
    ).forEach(rethrow(tuple -> {
      Properties expected = fixPaths(Props.load(Paths.get(getClass().getResource("/config-property-files/" + tuple.t2).toURI())));
      assertThat("File: " + tuple.t2 + " should perhaps be:\n" + Props.toString(tuple.t1), tuple.t1, is(equalTo(expected)));
    }));
  }

  @Test
  public void test_mapping_props_json_without_defaults() throws URISyntaxException, IOException {
    Properties props = Props.load(read("/config1_without_defaults.properties"));
    Cluster fromJson = json.readValue(read("/config1.json"), Cluster.class);
    Cluster fromProps = new ClusterFactory().create(props);

    assertThat(fromJson, is(equalTo(fromProps)));
    assertThat(
        Props.toString(fromJson.toProperties(false, false)),
        fromJson.toProperties(false, false),
        is(equalTo(props)));
    assertThat(
        Props.toString(fromJson.toProperties(false, false)),
        fromJson.toProperties(false, false),
        is(equalTo(fromProps.toProperties(false, false))));
    assertThat(
        json.writerWithDefaultPrettyPrinter().writeValueAsString(fromProps),
        fromProps,
        is(equalTo(fromJson)));
  }

  @Test
  public void test_mapping_props_json_with_defaults() throws URISyntaxException, IOException {
    Properties props = Props.load(read("/config1_with_defaults.properties"));
    Cluster fromJson = json.readValue(read("/config1.json"), Cluster.class);
    Cluster fromProps = new ClusterFactory().create(props);

    assertThat(fromJson, is(equalTo(fromProps)));
    assertThat(
        Props.toString(fromJson.toProperties(false, true)),
        fromJson.toProperties(false, true),
        is(equalTo(props)));
    assertThat(
        Props.toString(fromJson.toProperties(false, true)),
        fromJson.toProperties(false, true),
        is(equalTo(fromProps.toProperties(false, true))));
    assertThat(
        json.writerWithDefaultPrettyPrinter().writeValueAsString(fromProps),
        fromProps,
        is(equalTo(fromJson)));
  }

  public static <T> Consumer<T> rethrow(EConsumer<T> c) {
    return t -> {
      try {
        c.accept(t);
      } catch (Exception e) {
        if (e instanceof RuntimeException) {
          throw (RuntimeException) e;
        }
        throw new RuntimeException(e);
      }
    };
  }

  @FunctionalInterface
  public interface EConsumer<T> {
    void accept(T t) throws Exception;
  }

  private String read(String resource) throws URISyntaxException, IOException {
    Path path = Paths.get(getClass().getResource(resource).toURI());
    String data = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    return isWindows() ? data.replace("\r\n", "\n").replace("\n", "\r\n").replace("/", "\\\\") : data;
  }

  private Properties fixPaths(Properties props) {
    if (File.separatorChar == '\\') {
      props.entrySet().forEach(e -> e.setValue(e.getValue().toString().replace('/', '\\')));
    }
    return props;
  }

  private static boolean isWindows() {
    return System.getProperty("os.name").toLowerCase().startsWith("windows");
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