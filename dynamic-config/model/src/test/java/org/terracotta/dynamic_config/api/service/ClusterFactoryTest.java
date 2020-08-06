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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.terracotta.common.struct.MemoryUnit;
import org.terracotta.common.struct.TimeUnit;
import org.terracotta.dynamic_config.api.json.DynamicConfigModelJsonModule;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.LockContext;
import org.terracotta.dynamic_config.api.model.Setting;
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.dynamic_config.api.model.Testing;
import org.terracotta.json.ObjectMapperFactory;

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

import static java.util.Collections.emptyMap;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.either;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.lenient;
import static org.terracotta.common.struct.Tuple2.tuple2;
import static org.terracotta.dynamic_config.api.model.FailoverPriority.availability;
import static org.terracotta.dynamic_config.api.model.FailoverPriority.consistency;
import static org.terracotta.dynamic_config.api.model.Setting.NODE_BIND_ADDRESS;
import static org.terracotta.dynamic_config.api.model.Setting.NODE_GROUP_BIND_ADDRESS;
import static org.terracotta.dynamic_config.api.model.Setting.NODE_GROUP_PORT;
import static org.terracotta.dynamic_config.api.model.Setting.NODE_LOG_DIR;
import static org.terracotta.dynamic_config.api.model.Setting.NODE_METADATA_DIR;
import static org.terracotta.dynamic_config.api.model.Setting.NODE_PORT;
import static org.terracotta.testing.ExceptionMatcher.throwing;

/**
 * @author Mathieu Carbou
 */
@RunWith(MockitoJUnitRunner.class)
public class ClusterFactoryTest {

  private final ClusterFactory clusterFactory = new ClusterFactory();

  @Mock public IParameterSubstitutor substitutor;

  ObjectMapper json = new ObjectMapperFactory().withModule(new DynamicConfigModelJsonModule()).create();

  @Before
  public void setUp() {
    lenient().when(substitutor.substitute("%h")).thenReturn("localhost");
    lenient().when(substitutor.substitute("%c")).thenReturn("localhost.home");
    lenient().when(substitutor.substitute("%H")).thenReturn("home");
    lenient().when(substitutor.substitute("foo")).thenReturn("foo");
    lenient().when(substitutor.substitute(startsWith("node-"))).thenReturn("<GENERATED>");
    lenient().when(substitutor.substitute("9410")).thenReturn("9410");
    lenient().when(substitutor.substitute("")).thenReturn("");
    lenient().when(substitutor.substitute("availability")).thenReturn("availability");
  }

  @Test
  public void test_create_cli() {
    assertCliEquals(cli("failover-priority=availability"), Testing.newTestCluster(new Stripe(Testing.newTestNode("<GENERATED>", "localhost"))));
    assertCliEquals(cli("failover-priority=availability", "hostname=%c"), Testing.newTestCluster(new Stripe(Testing.newTestNode("<GENERATED>", "localhost.home"))));
    assertCliEquals(cli("failover-priority=availability", "hostname=foo"), Testing.newTestCluster(new Stripe(Testing.newTestNode("<GENERATED>", "foo"))));
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
        Testing.newTestCluster(new Stripe(Testing.newTestNode("real", "foo"))));

    assertConfigEquals(
        config(
            "failover-priority=availability",
            "stripe.1.node.1.name=node1",
            "stripe.1.node.1.hostname=localhost",
            "lock-context=a4684c73-a96c-46c1-834d-3843014f50af;platform;dynamic-scale"
        ),
        Testing.newTestCluster(new Stripe(Testing.newTestNode("node1", "localhost"))).setConfigurationLockContext(LockContext.from("a4684c73-a96c-46c1-834d-3843014f50af;platform;dynamic-scale")));

    assertConfigEquals(
        config(
            "failover-priority=availability",
            "stripe.1.node.1.name=node1",
            "stripe.1.node.1.hostname=localhost"
        ),
        Testing.newTestCluster(new Stripe(Testing.newTestNode("node1", "localhost"))));

    assertConfigEquals(
        config(
            "failover-priority=availability",
            "stripe.1.node.1.name=node1",
            "stripe.1.node.1.hostname=localhost1",
            "stripe.1.node.2.name=node2",
            "stripe.1.node.2.hostname=localhost2",
            "stripe.2.node.1.name=node3",
            "stripe.2.node.1.hostname=localhost3",
            "stripe.2.node.2.name=node4",
            "stripe.2.node.2.hostname=localhost4"
        ),
        Testing.newTestCluster(
            new Stripe(
                Testing.newTestNode("node1", "localhost1"),
                Testing.newTestNode("node2", "localhost2")),
            new Stripe(
                Testing.newTestNode("node3", "localhost3"),
                Testing.newTestNode("node4", "localhost4"))
        ));

    assertConfigEquals(
        config(
            "stripe.1.node.1.name=node1",
            "stripe.1.node.1.hostname=localhost",
            "cluster-name=foo",
            "failover-priority=availability",
            "stripe.1.node.1.tc-properties="
        ),
        Testing.newTestCluster("foo", new Stripe(Testing.newTestNode("node1", "localhost").setTcProperties(emptyMap()))));

    assertConfigEquals(
        config(
            "stripe.1.node.1.name=node1",
            "stripe.1.node.1.hostname=localhost",
            "cluster-name=foo",
            "client-reconnect-window=120s",
            "failover-priority=availability",
            "client-lease-duration=150s",
            "ssl-tls=false",
            "whitelist=false",
            "offheap-resources=main:512MB",
            "stripe.1.node.1.port=9410",
            "stripe.1.node.1.group-port=9430",
            "stripe.1.node.1.bind-address=0.0.0.0",
            "stripe.1.node.1.group-bind-address=0.0.0.0",
            "stripe.1.node.1.metadata-dir=%H/terracotta/metadata",
            "stripe.1.node.1.log-dir=%H/terracotta/logs",
            "stripe.1.node.1.tc-properties=",
            "stripe.1.node.1.data-dirs=main:%H/terracotta/user-data/main"
        ),
        Testing.newTestCluster("foo", new Stripe(Testing.newTestNode("node1", "localhost")
            .setPort(9410)
            .setGroupPort(9430)
            .setBindAddress("0.0.0.0")
            .setGroupBindAddress("0.0.0.0")
            .setMetadataDir(Paths.get("%H", "terracotta", "metadata"))
            .setLogDir(Paths.get("%H", "terracotta", "logs"))
            .setTcProperties(emptyMap())
            .putDataDir("main", Paths.get("%H", "terracotta", "user-data", "main"))
        ))
            .setClientReconnectWindow(120, TimeUnit.SECONDS)
            .setFailoverPriority(availability())
            .setClientLeaseDuration(150, TimeUnit.SECONDS)
            .setSecuritySslTls(false)
            .setSecurityWhitelist(false)
            .putOffheapResource("main", 512, MemoryUnit.MB)
    );
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
        "Found duplicate node name: foo");
    assertConfigFail(
        config(
            "failover-priority=availability",
            "stripe.1.node.1.hostname=localhost1", "stripe.1.node.1.name=foo",
            "stripe.2.node.1.hostname=localhost2", "stripe.2.node.1.name=foo"
        ),
        "Found duplicate node name: foo");

    // Note: all possible failures (including those above) are already tested in ClusterValidatorTest
    // ClusterFactory being much more a wiring class, we do not repeat all tests
  }

  @Test
  public void test_toProperties() {
    Cluster cluster = Testing.newTestCluster("my-cluster", new Stripe(
        Testing.newTestNode("node-1", "localhost1")
            .putDataDir("foo", Paths.get("%H/tc1/foo"))
            .putDataDir("bar", Paths.get("%H/tc1/bar")),
        Testing.newTestNode("node-2", "localhost2")
            .putDataDir("foo", Paths.get("%H/tc2/foo"))
            .putDataDir("bar", Paths.get("%H/tc2/bar"))
            .setTcProperties(emptyMap()))) // specifically set the map to empty one by the user
        .setFailoverPriority(consistency(2))
        .putOffheapResource("foo", 1, MemoryUnit.GB)
        .putOffheapResource("bar", 2, MemoryUnit.GB);

    Cluster clusterWithDefaults = Testing.newTestCluster("my-cluster", new Stripe(
        Testing.newTestNode("node-1", "localhost1")
            .setPort(NODE_PORT.getDefaultValue())
            .setGroupPort(NODE_GROUP_PORT.getDefaultValue())
            .setBindAddress(NODE_BIND_ADDRESS.getDefaultValue())
            .setGroupBindAddress(NODE_GROUP_BIND_ADDRESS.getDefaultValue())
            .setLogDir(NODE_LOG_DIR.getDefaultValue())
            .setMetadataDir(NODE_METADATA_DIR.getDefaultValue())
            .putDataDir("foo", Paths.get("%H/tc1/foo"))
            .putDataDir("bar", Paths.get("%H/tc1/bar")),
        Testing.newTestNode("node-2", "localhost2")
            .setPort(NODE_PORT.getDefaultValue())
            .setGroupPort(NODE_GROUP_PORT.getDefaultValue())
            .setBindAddress(NODE_BIND_ADDRESS.getDefaultValue())
            .setGroupBindAddress(NODE_GROUP_BIND_ADDRESS.getDefaultValue())
            .setLogDir(NODE_LOG_DIR.getDefaultValue())
            .setMetadataDir(NODE_METADATA_DIR.getDefaultValue())
            .putDataDir("foo", Paths.get("%H/tc2/foo"))
            .putDataDir("bar", Paths.get("%H/tc2/bar"))
            .setTcProperties(emptyMap()))) // specifically set the map to empty one by teh user
        .setSecuritySslTls(false)
        .setSecurityWhitelist(false)
        .setClientReconnectWindow(120, TimeUnit.SECONDS)
        .setClientLeaseDuration(150, TimeUnit.SECONDS)
        .setFailoverPriority(consistency(2))
        .putOffheapResource("foo", 1, MemoryUnit.GB)
        .putOffheapResource("bar", 2, MemoryUnit.GB);

    Stream.of(
        tuple2(cluster.toProperties(false, true), "config_with_defaults.properties"),
        tuple2(cluster.toProperties(false, false), "config_without_defaults.properties"),
        tuple2(cluster.toProperties(true, true), "config_expanded_default.properties"),
        tuple2(cluster.toProperties(true, false), "config_expanded_without_default.properties")
    ).forEach(rethrow(tuple -> {
      Properties expectedProps = fixPaths(Props.load(Paths.get(getClass().getResource("/config-property-files/" + tuple.t2).toURI())));

      assertThat(
          "File: " + tuple.t2 + " should perhaps be:\n" + Props.toString(tuple.t1),
          tuple.t1,
          is(equalTo(expectedProps)));

      Cluster expectedCluster = new ClusterFactory().create(expectedProps);

      assertThat(
          "File: " + tuple.t2,
          expectedCluster,
          either(equalTo(cluster)).or(equalTo(clusterWithDefaults)));
    }));
  }

  @Test
  public void test_mapping_props_json_without_defaults() throws URISyntaxException, IOException {
    Properties props = Props.load(read("/config1_without_defaults.properties"));
    Cluster fromJson = json.readValue(read("/config2.json"), Cluster.class);
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
        json.writeValueAsString(fromProps),
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
        json.writeValueAsString(fromProps),
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
    String nodeName = built.getSingleNode().get().getName();
    cluster.getSingleNode()
        .filter(node -> node.getName().equals("<GENERATED>"))
        .ifPresent(node -> node.setName(nodeName));

    assertThat(built, is(equalTo(cluster)));
  }

  private void assertCliFail(Map<Setting, String> params, String err) {
    err = err.replace("/", File.separator); // unix/win compat'
    assertThat(
        () -> clusterFactory.create(params, substitutor),
        is(throwing(instanceOf(MalformedClusterException.class)).andMessage(containsString(err))));
  }

  private void assertConfigEquals(Properties config, Cluster cluster) {
    Cluster built = clusterFactory.create(config);
    assertThat(built, is(equalTo(cluster)));
  }

  private void assertConfigFail(Properties config, String err) {
    err = err.replace("/", File.separator); // unix/win compat'
    assertThat(
        () -> clusterFactory.create(config),
        is(throwing(instanceOf(MalformedClusterException.class)).andMessage(containsString(err))));
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