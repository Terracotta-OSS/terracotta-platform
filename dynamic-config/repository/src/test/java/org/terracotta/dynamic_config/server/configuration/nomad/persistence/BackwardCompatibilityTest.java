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
package org.terracotta.dynamic_config.server.configuration.nomad.persistence;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import org.junit.Rule;
import org.junit.Test;
import org.terracotta.common.struct.Tuple2;
import org.terracotta.dynamic_config.api.json.DynamicConfigJsonModule;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.nomad.DynamicConfigNomadChange;
import org.terracotta.dynamic_config.api.server.DynamicConfigNomadServer;
import org.terracotta.dynamic_config.api.service.IParameterSubstitutor;
import org.terracotta.dynamic_config.api.service.Props;
import org.terracotta.dynamic_config.server.configuration.nomad.NomadServerFactory;
import org.terracotta.json.DefaultJsonFactory;
import org.terracotta.json.Json;
import org.terracotta.nomad.server.ChangeApplicator;
import org.terracotta.testing.TmpDir;
import static org.terracotta.utilities.io.Files.ExtendedOption.RECURSIVE;

public class BackwardCompatibilityTest {
  @Rule
  public TmpDir temporaryFolder = new TmpDir(Paths.get(System.getProperty("user.dir"), "build"), false);

  private String[] newV2Props = new String[]{"stripe.1.stripe-name", "cluster-uid", "stripe.1.stripe-uid", "stripe.1.node.1.node-uid", "this.version", "this.node-uid"};

  @Test
  public void test_automatic_upgrade_of_config_repository() throws Exception {
    Tuple2<NodeContext, Json.Factory> res = upgradedTopology("config-v1", "default-node1", 1);
    Json json = res.getT2().create();
    assertThat(
        json.map(res.getT1()),
        is(equalTo(json.parse(read("/topology.json")))));
  }

  @Test
  public void test_automatic_upgrade_of_config_repository_with_setting_change() throws Exception {
    Tuple2<NodeContext, Json.Factory> res = upgradedTopology("config-v1_with_setting", "node-1", 2);
    Json json = res.getT2().create();
    assertThat(
        json.map(res.getT1()),
        is(equalTo(json.parse(read("/topology_with_setting.json")))));
  }

  @Test
  public void test_automatic_upgrade_of_config_repository_with_node_addition_change() throws Exception {
    newV2Props = new String[]{"stripe.1.stripe-name", "cluster-uid", "stripe.1.stripe-uid", "stripe.1.node.1.node-uid", "this.version", "this.node-uid", "stripe.1.node.2.node-uid"};
    upgradedTopology("config-v1_with_addition_change", "node1", 2);
  }

  @Test
  public void test_automatic_upgrade_of_config_repository_with_node_deletion_change() throws Exception {
    upgradedTopology("config-v1_with_deletion_change", "node1", 2);
  }

  @Test
  public void test_automatic_upgrade_of_config_repository_for_10_7_0_0_315() throws Exception {
    assertThatConfigDirIsCompatible("config-v2-10.7.0.0.315", "node1", 2);
  }

  private URL read(String resource)  {
    URL url = getClass().getResource(resource);
    if (url == null) {
      throw new AssertionError(resource);
    }
    return url;
  }

  private Tuple2<NodeContext, Json.Factory> upgradedTopology(String name, String nodeName, int lastCommittedVersion) throws Exception {
    Path resourcesRoot = Paths.get(getClass().getResource("/" + name).toURI());

    // copy config folder in a temporary location
    Path config = temporaryFolder.getRoot().resolve(name);
    org.terracotta.utilities.io.Files.copy(resourcesRoot, config, RECURSIVE);
    Files.createDirectories(config.resolve("license"));

    // before...
    Properties before = Props.load(config.resolve("cluster").resolve(nodeName + "." + lastCommittedVersion + ".properties"));
    assertThat(before.stringPropertyNames(), not(hasItem("stripe.1.stripe-name")));

    // create nomad server
    NomadConfigurationManager nomadConfigurationManager = new NomadConfigurationManager(config, IParameterSubstitutor.identity());
    nomadConfigurationManager.createDirectories();
    Json.Factory jsonFactory = new DefaultJsonFactory().withModule(new DynamicConfigJsonModule());
    NomadServerFactory nomadServerFactory = new NomadServerFactory(jsonFactory);

    try (DynamicConfigNomadServer nomadServer = nomadServerFactory.createServer(nomadConfigurationManager, nodeName, null)) {
      nomadServer.setChangeApplicator(ChangeApplicator.allow((nodeContext, change) -> nodeContext.withCluster(((DynamicConfigNomadChange) change).apply(nodeContext.getCluster())).get()));

      // upgrade should have been done
      int nextInd = lastCommittedVersion + 1;
      Properties after = Props.load(config.resolve("cluster").resolve(nodeName + "." + nextInd + ".properties"));

      String[] removedV1Props = {"this.stripe-id", "this.node-id", "this.name"};
      assertThat(after.stringPropertyNames(), hasItems(newV2Props));

      // check content should match v1 content plus these 2 fields
      Stream.of(newV2Props).forEach(prop -> before.setProperty(prop, after.getProperty(prop)));
      Stream.of(removedV1Props).forEach(before::remove);
      assertThat(after, is(equalTo(before)));

      // check topology
      NodeContext topology = nomadServer.discover().getLatestChange().getResult();

      // subsequent calls are outputting the same result always after an upgrade
      assertThat(nomadServer.discover().getLatestChange().getResult(), is(equalTo(topology)));

      return Tuple2.tuple2(topology, jsonFactory);
    }
  }

  private Tuple2<NodeContext, Json.Factory> assertThatConfigDirIsCompatible(String name, String nodeName, int lastCommittedVersion) throws Exception {
    Path resourcesRoot = Paths.get(getClass().getResource("/" + name).toURI());

    // copy config folder in a temporary location
    Path config = temporaryFolder.getRoot().resolve(name);
    org.terracotta.utilities.io.Files.copy(resourcesRoot, config, RECURSIVE);
    Files.createDirectories(config.resolve("license"));

    // before...
    Properties before = Props.load(config.resolve("cluster").resolve(nodeName + "." + lastCommittedVersion + ".properties"));

    // create nomad server
    NomadConfigurationManager nomadConfigurationManager = new NomadConfigurationManager(config, IParameterSubstitutor.identity());
    nomadConfigurationManager.createDirectories();
    Json.Factory jsonFactory = new DefaultJsonFactory().withModule(new DynamicConfigJsonModule());
    NomadServerFactory nomadServerFactory = new NomadServerFactory(jsonFactory);

    try (DynamicConfigNomadServer nomadServer = nomadServerFactory.createServer(nomadConfigurationManager, nodeName, null)) {
      nomadServer.setChangeApplicator(ChangeApplicator.allow((nodeContext, change) -> nodeContext.withCluster(((DynamicConfigNomadChange) change).apply(nodeContext.getCluster())).get()));

      // upgrade should have been done
      int nextInd = lastCommittedVersion;
      Properties after = Props.load(config.resolve("cluster").resolve(nodeName + "." + nextInd + ".properties"));
      assertThat(after, is(equalTo(before)));

      // check topology
      NodeContext topology = nomadServer.discover().getLatestChange().getResult();

      // subsequent calls are outputting the same result always after an upgrade
      assertThat(nomadServer.discover().getLatestChange().getResult(), is(equalTo(topology)));

      return Tuple2.tuple2(topology, jsonFactory);
    }
  }
}