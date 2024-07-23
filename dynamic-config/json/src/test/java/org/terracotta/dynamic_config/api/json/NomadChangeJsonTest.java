/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
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
package org.terracotta.dynamic_config.api.json;

import org.junit.Test;
import org.terracotta.common.struct.Measure;
import org.terracotta.common.struct.MemoryUnit;
import org.terracotta.common.struct.TimeUnit;
import org.terracotta.common.struct.Tuple3;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.FailoverPriority;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.RawPath;
import org.terracotta.dynamic_config.api.model.Scope;
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.dynamic_config.api.model.Testing;
import org.terracotta.dynamic_config.api.model.UID;
import org.terracotta.dynamic_config.api.model.Version;
import org.terracotta.dynamic_config.api.model.nomad.Applicability;
import org.terracotta.dynamic_config.api.model.nomad.ClusterActivationNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.FormatUpgradeNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.MultiSettingNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.NodeAdditionNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.NodeRemovalNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.SettingNomadChange;
import org.terracotta.dynamic_config.api.service.FormatUpgrade;
import org.terracotta.json.DefaultJsonFactory;
import org.terracotta.json.Json;
import org.terracotta.nomad.client.change.NomadChange;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.terracotta.common.struct.Tuple3.tuple3;
import static org.terracotta.dynamic_config.api.model.Setting.NODE_BACKUP_DIR;
import static org.terracotta.dynamic_config.api.model.Setting.OFFHEAP_RESOURCES;
import static org.terracotta.dynamic_config.api.model.Testing.N_UIDS;
import static org.terracotta.dynamic_config.api.model.Testing.S_UIDS;
import static org.terracotta.dynamic_config.api.model.Testing.newTestCluster;
import static org.terracotta.dynamic_config.api.model.Testing.newTestNode;
import static org.terracotta.dynamic_config.api.model.Testing.newTestStripe;

/**
 * @author Mathieu Carbou
 */
public class NomadChangeJsonTest {

  private final Cluster cluster = newTestCluster("myClusterName",
      newTestStripe("stripe-1", S_UIDS[1]).addNode(newTestNode("foo", "localhost", 9410, N_UIDS[1])))
      .setClientReconnectWindow(60, TimeUnit.SECONDS)
      .putOffheapResource("foo", 1, MemoryUnit.GB);

  private final Cluster cluster2 = cluster.clone();

  @Test
  public void test_ser_deser() throws IOException, URISyntaxException {
    Testing.replaceUIDs(cluster);
    Json mapper = new DefaultJsonFactory()
        .withModule(new DynamicConfigJsonModule())
        .pretty()
        .create();

    cluster2.getSingleStripe().get().addNode(newTestNode("bar", "localhost2", 9411, N_UIDS[2]));

    NomadChange[] changes = {
        new ClusterActivationNomadChange(cluster),
        SettingNomadChange.set(Applicability.node(N_UIDS[1]), NODE_BACKUP_DIR, "backup"),
        new MultiSettingNomadChange(
            SettingNomadChange.set(Applicability.node(N_UIDS[1]), NODE_BACKUP_DIR, "backup"),
            SettingNomadChange.set(Applicability.cluster(), OFFHEAP_RESOURCES, "bar", "512MB")
        ),
        new FormatUpgradeNomadChange(Version.V1, Version.V2, new FormatUpgrade().upgrade(cluster, Version.V1)),
        new NodeAdditionNomadChange(cluster2, cluster2.getSingleStripe().get().getUID(), newTestNode("bar", "localhost2", 9411, N_UIDS[2]))
    };

    for (int i = 0; i < changes.length; i++) {
      NomadChange change = changes[i];
      String json = read("nomad/v2/change" + i + ".json");
      assertThat("nomad/v2/change" + i + ".json", mapper.toString(change), is(equalTo(json)));
      assertThat("nomad/v2/change" + i + ".json", mapper.parse(json, NomadChange.class), is(equalTo(change)));
    }
  }

  @SuppressWarnings("deprecation")
  @Test
  public void test_ser_deser_v1() {
    Testing.replaceUIDs(cluster);
    Json mapper = new DefaultJsonFactory()
        .withModule(new DynamicConfigJsonModule())
        .withModule(new DynamicConfigJsonModuleV1())
        .pretty()
        .create();

    NomadChange[] changes = {
        new ClusterActivationNomadChange(cluster),
        SettingNomadChange.set(new Applicability.V1(Scope.NODE, 1, "node1"), NODE_BACKUP_DIR, "backup"),
        new MultiSettingNomadChange(
            SettingNomadChange.set(new Applicability.V1(Scope.NODE, 1, "node1"), NODE_BACKUP_DIR, "backup"),
            SettingNomadChange.set(new Applicability.V1(Scope.CLUSTER, null, null), OFFHEAP_RESOURCES, "bar", "512MB")
        ),
        new FormatUpgradeNomadChange(Version.V1, Version.V2, new FormatUpgrade().upgrade(cluster, Version.V1))
    };

    for (int i = 0; i < changes.length; i++) {
      NomadChange change = changes[i];
      String jsonSer = read("nomad/v1/change" + i + "-ser.json");
      String jsonDeser = read("nomad/v1/change" + i + "-deser.json");
      assertThat("nomad/v1/change" + i + "-ser.json", mapper.toString(change), is(equalTo(jsonSer)));
      assertThat("nomad/v1/change" + i + "-ser.json", mapper.parse(jsonDeser, NomadChange.class), is(equalTo(change)));
    }
  }

  @SuppressWarnings("deprecation")
  @Test
  public void test_deser_v1() throws IOException, URISyntaxException {
    Json mapper = new DefaultJsonFactory()
        .withModule(new DynamicConfigJsonModule())
        .withModule(new DynamicConfigJsonModuleV1())
        .pretty()
        .create();

    final Node node = new Node()
        .putDataDir("main", RawPath.valueOf("%H/terracotta/user-data/main"))
        .setBindAddress("0.0.0.0")
        .setGroupBindAddress("0.0.0.0")
        .setGroupPort(9431)
        .setHostname("SAG-2H78NQ2-U")
        .setLogDir(RawPath.valueOf("%H/terracotta/logs"))
        .setLoggerOverrides(emptyMap())
        .setMetadataDir(RawPath.valueOf("%H/terracotta/metadata"))
        .setName("node2")
        .setPort(9411)
        .setTcProperties(emptyMap());

    final Cluster cluster = new Cluster()
        .setName("mycluster")
        .setClientLeaseDuration(Measure.of(150, TimeUnit.SECONDS))
        .setClientReconnectWindow(120, TimeUnit.SECONDS)
        .setFailoverPriority(FailoverPriority.availability())
        .putOffheapResource("main", Measure.of(512, MemoryUnit.MB))
        .setSecuritySslTls(false)
        .setSecurityWhitelist(false)
        .addStripe(new Stripe()
            .addNode(new Node()
                .putDataDir("main", RawPath.valueOf("%H/terracotta/user-data/main"))
                .setBindAddress("0.0.0.0")
                .setGroupBindAddress("0.0.0.0")
                .setGroupPort(9430)
                .setHostname("SAG-2H78NQ2-U")
                .setLogDir(RawPath.valueOf("%H/terracotta/logs"))
                .setLoggerOverrides(emptyMap())
                .setMetadataDir(RawPath.valueOf("%H/terracotta/metadata"))
                .setName("node1")
                .setPort(9410)
                .setTcProperties(emptyMap()))
            .addNode(node));

    for (Tuple3<Class<?>, String, Object> tuple : Arrays.<Tuple3<Class<?>, String, Object>>asList(
        tuple3(Applicability.class, "/nomad/v1/applicability.json", new Applicability.V1(Scope.STRIPE, 1, null)),
        tuple3(Node.class, "/nomad/v1/node.json", node
        ),
        tuple3(Cluster.class, "/nomad/v1/cluster.json", cluster),
        tuple3(NomadChange.class, "/nomad/v1/node-addition.json", null),
        tuple3(NomadChange.class, "/nomad/v1/node-removal.json", null)
    )) {
      String json = read(tuple.t2);
      Object o = mapper.parse(json, tuple.t1);

      if (tuple.t3 != null) {
        assertThat(tuple.t2, o, is(equalTo(tuple.t3)));
        assertThat(tuple.t2, mapper.toString(o), is(equalTo(json)));
      } else {
        // no reverse mapping because deserialization creates an object with random uids and a deprecated applicability
      }
    }
  }

  @Test
  public void test_deser_v2() throws IOException, URISyntaxException {
    final Node node = new Node()
        .putDataDir("main", RawPath.valueOf("node-1-2/data-dir"))
        .setGroupPort(6388)
        .setHostname("localhost")
        .setLogDir(RawPath.valueOf("node-1-2/logs"))
        .setMetadataDir(RawPath.valueOf("node-1-2/metadata"))
        .setName("node-1-2")
        .setPort(30354)
        .setUID(UID.valueOf("vK13jHNCTaG17-YEvg2R5A"));

    Cluster cluster1 = new Cluster()
        .setName("tc-cluster")
        .setFailoverPriority(FailoverPriority.availability())
        .putOffheapResource("main", Measure.of(512, MemoryUnit.MB))
        .putOffheapResource("foo", Measure.of(1, MemoryUnit.GB))
        .setUID(UID.valueOf("xBmd2K8KR3etQWlUy3HZcw"))
        .addStripe(new Stripe()
            .setUID(UID.valueOf("Z5Le2HReSjmytM5t4DTPCw"))
            .setName("Germany")
            .addNode(new Node()
                .putDataDir("main", RawPath.valueOf("node-1-1/data-dir"))
                .setGroupPort(15293)
                .setHostname("localhost")
                .setLogDir(RawPath.valueOf("node-1-1/logs"))
                .setMetadataDir(RawPath.valueOf("node-1-1/metadata"))
                .setName("node-1-1")
                .setPort(26637)
                .setUID(UID.valueOf("fqoFdUWuQYqPtZNX6py7QQ")))
            .addNode(node));

    Cluster cluster2 = cluster1.clone();
    cluster2.getSingleStripe().get().removeNode(UID.valueOf("vK13jHNCTaG17-YEvg2R5A"));

    Json mapper = new DefaultJsonFactory()
        .withModule(new DynamicConfigJsonModule())
        .pretty()
        .create();

    for (Tuple3<Class<?>, String, Object> tuple : Arrays.<Tuple3<Class<?>, String, Object>>asList(
        tuple3(NomadChange.class, "/nomad/v2/node-addition.json", new NodeAdditionNomadChange(cluster1, UID.valueOf("Z5Le2HReSjmytM5t4DTPCw"), node)),
        tuple3(NomadChange.class, "/nomad/v2/node-removal.json", new NodeRemovalNomadChange(cluster2, UID.valueOf("Z5Le2HReSjmytM5t4DTPCw"), node))
    )) {
      String json = read(tuple.t2);
      Object o = mapper.parse(json, tuple.t1);
      assertThat(tuple.t2, o, is(equalTo(tuple.t3)));
      assertThat(tuple.t2, mapper.toString(o), is(equalTo(json)));
    }
  }

  @Test
  public void test_deser_with_unexpected_fields() throws IOException, URISyntaxException {
    Json mapper = new DefaultJsonFactory().withModule(new DynamicConfigJsonModule()).create();
    String json = read("/nomad/v2/unexpected-fields.json");
    // this call below should not fail with
    // UnrecognizedPropertyException: Unrecognized field "foo" [...], not marked as ignorable
    ClusterActivationNomadChange o = mapper.parse(json, ClusterActivationNomadChange.class);
    assertNotNull(o);
  }

  protected String read(String file) {
    try {
      if (!file.startsWith("/")) {
        file = "/" + file;
      }
      return new String(Files.readAllBytes(Paths.get(getClass().getResource(file).toURI())), UTF_8).replace("\r", "");
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }
}
