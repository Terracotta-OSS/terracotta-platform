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

import org.junit.Before;
import org.junit.Test;
import org.terracotta.common.struct.MemoryUnit;
import org.terracotta.common.struct.TimeUnit;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.LockContext;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.RawPath;
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.dynamic_config.api.service.ClusterFactory;
import org.terracotta.dynamic_config.api.service.Props;
import org.terracotta.json.DefaultJsonFactory;
import org.terracotta.json.Json;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.util.Collections.emptyMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.terracotta.dynamic_config.api.model.FailoverPriority.availability;
import static org.terracotta.dynamic_config.api.model.FailoverPriority.consistency;
import static org.terracotta.dynamic_config.api.model.Testing.replaceUIDs;
import static org.terracotta.dynamic_config.api.model.Version.CURRENT;
import static org.terracotta.dynamic_config.api.model.Version.V1;
import static org.terracotta.dynamic_config.api.model.Version.V2;

/**
 * @author Mathieu Carbou
 */
public class CompatibilityTest {

  private final Json json = new DefaultJsonFactory().withModule(new DynamicConfigJsonModule()).create();

  private final Cluster clusterV1 = new Cluster().setName("my-cluster").setFailoverPriority(availability()).addStripe(new Stripe().addNodes(
      new Node()
          .setName("node-1")
          .setHostname("localhost1")
          .setPort(9410)
          .setGroupPort(9430)
          .setBindAddress("0.0.0.0")
          .setGroupBindAddress("0.0.0.0")
          .setLogDir(RawPath.valueOf("%H/terracotta/logs"))
          .setMetadataDir(RawPath.valueOf("%H/terracotta/metadata"))
          .unsetDataDirs()
          .putDataDir("foo", RawPath.valueOf("%H/tc1/foo"))
          .putDataDir("bar", RawPath.valueOf("%H/tc1/bar")),
      new Node()
          .setName("node-2")
          .setHostname("localhost2")
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
      .setSecuritySslTls(false)
      .setSecurityWhitelist(false)
      .setClientReconnectWindow(120, TimeUnit.SECONDS)
      .setClientLeaseDuration(150, TimeUnit.SECONDS)
      .setFailoverPriority(consistency(2))
      .putOffheapResource("foo", 1, MemoryUnit.GB)
      .putOffheapResource("bar", 2, MemoryUnit.GB);

  private final Cluster clusterV2 = clusterV1.clone();

  @Before
  public void setUp() throws Exception {
    clusterV2.getSingleStripe().get().setName("stripe1");
    clusterV2.setConfigurationLockContext(LockContext.from("30fc1dd1-0788-4592-8f49-d7dbb4daf45e;me;tag"));
  }

  @Test
  public void test_versioned_parsing() throws URISyntaxException, IOException {
    // - config file format is V2
    // - config has V2 settings
    {
      Cluster parsed = new ClusterFactory(V2).create(Props.load(read("/V2.properties")));
      replaceUIDs(parsed);
      Cluster expected = json.parse(getClass().getResource("/V2.json"), Cluster.class);
      assertThat(json.toString(parsed), parsed, is(equalTo(expected)));
    }
    // - config file format is V2
    // - config has V1 settings only
    // defaults will be applied
    {
      Cluster parsed = new ClusterFactory(V2).create(Props.load(read("/V1.properties")));

      assertThat(parsed.getSingleStripe().get().getName(), startsWith("stripe-"));
      parsed.getSingleStripe().get().setName("stripe-XYZ");
      replaceUIDs(parsed);

      Cluster expected = json.parse(getClass().getResource("/V1_and_V2_defaults.json"), Cluster.class);
      assertThat(json.toString(parsed), parsed, is(equalTo(expected)));
    }
    // - config file format is V1
    // - config has V1 settings only
    // - output is V1 only
    {
      Cluster parsed = new ClusterFactory(V1).create(Props.load(read("/V1.properties")));
      Cluster expected = json.parse(getClass().getResource("/V1.json"), Cluster.class);
      assertThat(parsed, is(equalTo(expected)));
    }
    // - config file format has some unsupported settings
    // - output is V1 only
    // example of older client that only knows about V1 config and are getting a V2 config
    {
      Cluster parsed = new ClusterFactory(V1).create(Props.load(read("/V2.properties")));
      Cluster expected = json.parse(getClass().getResource("/V1.json"), Cluster.class);
      assertThat(parsed, is(equalTo(expected)));
    }
    // json and properties checks
    {
      Cluster parsed = new ClusterFactory(V1).create(Props.load(read("/default-node1.1.properties")));
      Cluster expected = json.parse(getClass().getResource("/default-node1.1.json"), Cluster.class);
      assertThat(json.toString(parsed),
          json.map(parsed),
          is(equalTo(json.parse(getClass().getResource("/default-node1.1.json")))));
      assertThat(parsed, is(equalTo(expected)));
      assertThat(Props.toString(parsed.toProperties(false, false, true, CURRENT)),
          parsed.toProperties(false, false, true, CURRENT),
          is(equalTo(Props.load(read("/default-node1.1-v2.properties")))));
      assertThat(Props.toString(parsed.toProperties(false, false, true, V1)),
          parsed.toProperties(false, false, true, V1),
          is(equalTo(Props.load(read("/default-node1.1-v2.properties")))));
    }
  }

  private String read(String resource) throws URISyntaxException, IOException {
    URL url = getClass().getResource(resource);
    if (url == null) {
      throw new AssertionError(resource);
    }
    Path path = Paths.get(url.toURI());
    return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
  }
}