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
package org.terracotta.dynamic_config.api.model.nomad;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.terracotta.common.struct.MemoryUnit;
import org.terracotta.common.struct.TimeUnit;
import org.terracotta.common.struct.Tuple2;
import org.terracotta.dynamic_config.api.json.DynamicConfigApiJsonModule;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.Scope;
import org.terracotta.dynamic_config.api.model.Testing;
import org.terracotta.dynamic_config.api.model.Version;
import org.terracotta.dynamic_config.api.service.FormatUpgrade;
import org.terracotta.json.ObjectMapperFactory;
import org.terracotta.nomad.client.change.NomadChange;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.terracotta.common.struct.Tuple2.tuple2;
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
    ObjectMapper objectMapper = new ObjectMapperFactory().withModule(new DynamicConfigApiJsonModule()).create();

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

      URL jsonFile = getClass().getResource("/nomad/v2/change" + i + ".json");
      byte[] bytes = Files.readAllBytes(Paths.get(jsonFile.toURI()));
      String json = new String(bytes, StandardCharsets.UTF_8);
      assertThat(jsonFile.getPath() + "\n" + objectMapper.writeValueAsString(change), objectMapper.valueToTree(change).toString(), is(equalTo(objectMapper.readTree(json).toString())));
      assertThat(jsonFile.getPath(), objectMapper.readValue(json, NomadChange.class), is(equalTo(change)));
    }
  }

  @SuppressWarnings("deprecation")
  @Test
  public void test_ser_deser_v1() throws IOException, URISyntaxException {
    Testing.replaceUIDs(cluster);
    ObjectMapper objectMapper = new ObjectMapperFactory().withModule(new DynamicConfigApiJsonModule())
        .withModules(new org.terracotta.dynamic_config.api.json.DynamicConfigModelJsonModuleV1(), new org.terracotta.dynamic_config.api.json.DynamicConfigApiJsonModuleV1())
        .create();

    NomadChange[] changes = {
        new ClusterActivationNomadChange(cluster),
        SettingNomadChange.set(new org.terracotta.dynamic_config.api.json.ApplicabilityV1(Scope.NODE, 1, "node1"), NODE_BACKUP_DIR, "backup"),
        new MultiSettingNomadChange(
            SettingNomadChange.set(new org.terracotta.dynamic_config.api.json.ApplicabilityV1(Scope.NODE, 1, "node1"), NODE_BACKUP_DIR, "backup"),
            SettingNomadChange.set(new org.terracotta.dynamic_config.api.json.ApplicabilityV1(Scope.CLUSTER, null, null), OFFHEAP_RESOURCES, "bar", "512MB")
        ),
        new FormatUpgradeNomadChange(Version.V1, Version.V2, new FormatUpgrade().upgrade(cluster, Version.V1))
    };

    for (int i = 0; i < changes.length; i++) {
      NomadChange change = changes[i];

      URL jsonFile = getClass().getResource("/nomad/v1/change" + i + ".json");
      byte[] bytes = Files.readAllBytes(Paths.get(jsonFile.toURI()));
      String json = new String(bytes, StandardCharsets.UTF_8);
      assertThat(jsonFile.getPath() + "\n" + objectMapper.writeValueAsString(change), objectMapper.valueToTree(change).toString(), is(equalTo(objectMapper.readTree(json).toString())));
      assertThat(jsonFile.getPath(), objectMapper.readValue(json, NomadChange.class), is(equalTo(change)));
    }
  }

  @SuppressWarnings("deprecation")
  @Test
  public void test_deser_v1() throws IOException, URISyntaxException {
    ObjectMapper objectMapper = new ObjectMapperFactory().withModule(new DynamicConfigApiJsonModule())
        .withModules(new org.terracotta.dynamic_config.api.json.DynamicConfigModelJsonModuleV1(), new org.terracotta.dynamic_config.api.json.DynamicConfigApiJsonModuleV1())
        .create();

    for (Tuple2<Class<?>, String> tuple : Arrays.<Tuple2<Class<?>, String>>asList(
        tuple2(Applicability.class, "/nomad/v1/applicability.json"),
        tuple2(Node.class, "/nomad/v1/node.json"),
        tuple2(Cluster.class, "/nomad/v1/cluster.json"),
        tuple2(NomadChange.class, "/nomad/v1/node-addition.json"),
        tuple2(NomadChange.class, "/nomad/v1/node-removal.json")
    )) {
      URL jsonFile = getClass().getResource(tuple.t2);
      byte[] bytes = Files.readAllBytes(Paths.get(jsonFile.toURI()));
      String json = new String(bytes, StandardCharsets.UTF_8);

      Object o = objectMapper.readValue(json, tuple.t1);

      assertThat(tuple.t2 + "\n" + objectMapper.valueToTree(o).toString(), objectMapper.valueToTree(o).toString(), is(equalTo(objectMapper.readTree(json).toString())));
    }
  }

  @Test
  public void test_deser_v2() throws IOException, URISyntaxException {
    ObjectMapper objectMapper = new ObjectMapperFactory().withModule(new DynamicConfigApiJsonModule()).create();

    for (Tuple2<Class<?>, String> tuple : Arrays.<Tuple2<Class<?>, String>>asList(
        tuple2(NomadChange.class, "/nomad/v2/node-addition.json"),
        tuple2(NomadChange.class, "/nomad/v2/node-removal.json")
    )) {
      URL jsonFile = getClass().getResource(tuple.t2);
      byte[] bytes = Files.readAllBytes(Paths.get(jsonFile.toURI()));
      String json = new String(bytes, StandardCharsets.UTF_8);

      Object o = objectMapper.readValue(json, tuple.t1);

      assertThat(tuple.t2 + "\n" + objectMapper.valueToTree(o).toString(), objectMapper.valueToTree(o).toString(), is(equalTo(objectMapper.readTree(json).toString())));
    }
  }
}
