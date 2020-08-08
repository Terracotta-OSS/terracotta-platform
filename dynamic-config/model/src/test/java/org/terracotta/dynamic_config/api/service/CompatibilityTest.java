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

import org.junit.Before;
import org.junit.Test;
import org.terracotta.common.struct.MemoryUnit;
import org.terracotta.common.struct.TimeUnit;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.LockContext;
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.dynamic_config.api.model.Testing;
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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.terracotta.dynamic_config.api.model.FailoverPriority.consistency;
import static org.terracotta.dynamic_config.api.model.Setting.NODE_BIND_ADDRESS;
import static org.terracotta.dynamic_config.api.model.Setting.NODE_GROUP_BIND_ADDRESS;
import static org.terracotta.dynamic_config.api.model.Setting.NODE_GROUP_PORT;
import static org.terracotta.dynamic_config.api.model.Setting.NODE_LOG_DIR;
import static org.terracotta.dynamic_config.api.model.Setting.NODE_METADATA_DIR;
import static org.terracotta.dynamic_config.api.model.Setting.NODE_PORT;
import static org.terracotta.dynamic_config.api.model.Version.V1;
import static org.terracotta.dynamic_config.api.model.Version.V2;

/**
 * @author Mathieu Carbou
 */
public class CompatibilityTest {

  private final Cluster clusterV1 = Testing.newTestCluster("my-cluster", new Stripe().addNodes(
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
    if(url == null) {
      throw new AssertionError(resource);
    }
    Path path = Paths.get(url.toURI());
    String data = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    return isWindows() ? data.replace("\r\n", "\n").replace("\n", "\r\n").replace("/", "\\\\") : data;
  }

  private static boolean isWindows() {
    return System.getProperty("os.name").toLowerCase().startsWith("windows");
  }
}