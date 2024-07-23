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
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.terracotta.common.struct.MemoryUnit;
import org.terracotta.common.struct.TimeUnit;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.RawPath;
import org.terracotta.dynamic_config.api.model.Testing;
import org.terracotta.dynamic_config.api.service.ClusterFactory;
import org.terracotta.dynamic_config.api.service.Props;
import org.terracotta.json.DefaultJsonFactory;
import org.terracotta.json.Json;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import static java.util.Collections.emptyMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.terracotta.dynamic_config.api.model.FailoverPriority.consistency;
import static org.terracotta.dynamic_config.api.model.Testing.newTestStripe;

/**
 * @author Mathieu Carbou
 */
@RunWith(MockitoJUnitRunner.class)
public class ClusterFactoryTest {

  Cluster clusterWithDefaults = Testing.newTestCluster("my-cluster", newTestStripe("stripe1").setUID(Testing.S_UIDS[1]).addNodes(
          Testing.newTestNode("node-1", "localhost1")
              .setUID(Testing.N_UIDS[1])
              .setPort(9410)
              .setGroupPort(9430)
              .setBindAddress("0.0.0.0")
              .setGroupBindAddress("0.0.0.0")
              .setLogDir(RawPath.valueOf("%H/terracotta/logs"))
              .setMetadataDir(RawPath.valueOf("%H/terracotta/metadata"))
              .unsetDataDirs()
              .putDataDir("foo", RawPath.valueOf("%H/tc1/foo"))
              .putDataDir("bar", RawPath.valueOf("%H/tc1/bar")),
          Testing.newTestNode("node-2", "localhost2")
              .setUID(Testing.N_UIDS[2])
              .setPort(9410)
              .setGroupPort(9430)
              .setBindAddress("0.0.0.0")
              .setGroupBindAddress("0.0.0.0")
              .setLogDir(RawPath.valueOf("%H/terracotta/logs"))
              .setMetadataDir(RawPath.valueOf("%H/terracotta/metadata"))
              .unsetDataDirs()
              .putDataDir("foo", RawPath.valueOf("%H/tc2/foo"))
              .putDataDir("bar", RawPath.valueOf("%H/tc2/bar"))
              .setTcProperties(emptyMap()))) // specifically set the map to empty one by the user
      .setUID(Testing.C_UIDS[0])
      .setSecuritySslTls(false)
      .setSecurityWhitelist(false)
      .setClientReconnectWindow(120, TimeUnit.SECONDS)
      .setClientLeaseDuration(150, TimeUnit.SECONDS)
      .setFailoverPriority(consistency(2))
      .putOffheapResource("foo", 1, MemoryUnit.GB)
      .putOffheapResource("bar", 2, MemoryUnit.GB);

  Json json = new DefaultJsonFactory().withModule(new DynamicConfigJsonModule()).create();

  @Test
  public void test_parsing_expanded_values_does_not_add_defaults() throws URISyntaxException, IOException {
    Properties expectedProps = Props.load(Paths.get(getClass().getResource("/config_expanded_default.properties").toURI()));
    Cluster expectedCluster = new ClusterFactory().create(expectedProps);
    assertThat(
        "\nclusterWithDefaults: " + json.toString(clusterWithDefaults) + "\nexpectedCluster:     " + json.toString(expectedCluster),
        expectedCluster, is(equalTo(clusterWithDefaults)));
  }

  @Test
  public void test_mapping_props_json_without_defaults() throws URISyntaxException, IOException {
    Properties props = Props.load(read("/config1_without_defaults.properties"));
    Cluster fromJson = json.parse(read("/config2.json"), Cluster.class);
    Cluster fromProps = new ClusterFactory().create(props);

    assertThat(json.toString(fromProps), fromProps, is(equalTo(fromJson)));
    assertThat(
        Props.toString(fromJson.toProperties(false, false, true)),
        fromJson.toProperties(false, false, true),
        is(equalTo(props)));
    assertThat(
        Props.toString(fromJson.toProperties(false, false, true)),
        fromJson.toProperties(false, false, true),
        is(equalTo(fromProps.toProperties(false, false, true))));
    assertThat(
        json.toString(fromProps),
        fromProps,
        is(equalTo(fromJson)));
  }

  @Test
  public void test_mapping_props_json_with_defaults() throws URISyntaxException, IOException {
    Properties props = Props.load(read("/config1_with_defaults.properties"));
    Cluster fromProps = new ClusterFactory().create(props);
    Cluster fromJson = json.parse(read("/config1.json"), Cluster.class);

    assertThat(json.toString(fromProps), fromProps, is(equalTo(fromJson)));
    assertThat(
        Props.toString(fromJson.toProperties(false, true, true)),
        fromJson.toProperties(false, true, true),
        is(equalTo(props)));
    assertThat(
        Props.toString(fromJson.toProperties(false, true, true)),
        fromJson.toProperties(false, true, true),
        is(equalTo(fromProps.toProperties(false, true, true))));
    assertThat(
        json.toString(fromProps),
        fromProps,
        is(equalTo(fromJson)));
  }

  private String read(String resource) throws URISyntaxException, IOException {
    Path path = Paths.get(getClass().getResource(resource).toURI());
    return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
  }
}