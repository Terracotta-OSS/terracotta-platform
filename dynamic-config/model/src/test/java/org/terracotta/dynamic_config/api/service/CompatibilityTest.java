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
package org.terracotta.dynamic_config.api.service;

import org.junit.Before;
import org.junit.Test;
import org.terracotta.common.struct.MemoryUnit;
import org.terracotta.common.struct.TimeUnit;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.LockContext;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.RawPath;
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.dynamic_config.api.model.Version;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.Properties;

import static java.util.Collections.emptyMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.terracotta.dynamic_config.api.model.FailoverPriority.availability;
import static org.terracotta.dynamic_config.api.model.FailoverPriority.consistency;
import static org.terracotta.dynamic_config.api.model.Version.V1;
import static org.terracotta.dynamic_config.api.model.Version.V2;

/**
 * @author Mathieu Carbou
 */
public class CompatibilityTest {


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
  public void test_output_versioning() throws URISyntaxException, IOException {
    for (Version version : EnumSet.of(V1, V2)) {
      Properties expected = Props.load(read("/V1.properties"));
      Properties props = clusterV1.toProperties(false, false, true, version);
      assertThat(Props.toString(props), props, is(equalTo(expected)));
    }
    for (Version version : EnumSet.of(V1, V2)) {
      Properties expected = Props.load(read("/" + version.name() + ".properties"));
      Properties props = clusterV2.toProperties(false, false, true, version);
      assertThat(Props.toString(props), props, is(equalTo(expected)));
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