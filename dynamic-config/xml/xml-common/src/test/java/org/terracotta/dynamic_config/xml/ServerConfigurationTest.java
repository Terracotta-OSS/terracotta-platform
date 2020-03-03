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
package org.terracotta.dynamic_config.xml;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.terracotta.common.struct.MemoryUnit;
import org.terracotta.common.struct.TimeUnit;
import org.terracotta.config.Config;
import org.terracotta.config.FailoverPriority;
import org.terracotta.config.Servers;
import org.terracotta.config.Service;
import org.terracotta.config.Services;
import org.terracotta.config.TcConfig;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.service.PathResolver;
import org.terracotta.testing.TmpDir;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.terracotta.dynamic_config.api.model.FailoverPriority.consistency;

public class ServerConfigurationTest {

  @Rule
  public TmpDir temporaryFolder = new TmpDir();

  PathResolver pathResolver;

  @Before
  public void setUp() {
    pathResolver = new PathResolver(temporaryFolder.getRoot());
  }

  @Test
  public void testCreation() {
    Node node = Node.newDefaultNode("localhost");
    node.setOffheapResource("main", 100, MemoryUnit.MB);
    node.setNodeBackupDir(temporaryFolder.getRoot());
    node.setSecurityDir(temporaryFolder.getRoot());
    node.setClientLeaseDuration(10, TimeUnit.MINUTES);
    node.setDataDir("root", temporaryFolder.getRoot());
    node.setNodeMetadataDir(temporaryFolder.getRoot());
    node.setFailoverPriority(consistency(2));

    Servers servers = new Servers();

    ServerConfiguration serverConfiguration = new ServerConfiguration(node, servers, pathResolver);

    TcConfig tcConfig = serverConfiguration.getTcConfig();

    assertThat(tcConfig, notNullValue());
    assertThat(tcConfig.getServers(), is(servers));

    verifyPlugins(tcConfig);
    verifyFailOverPriority(tcConfig);
  }

  private static void verifyFailOverPriority(TcConfig tcConfig) {
    FailoverPriority failoverPriority = tcConfig.getFailoverPriority();
    assertThat(failoverPriority, notNullValue());
    assertThat(failoverPriority.getConsistency(), notNullValue());
    assertThat(failoverPriority.getConsistency().getVoter(), notNullValue());
    assertThat(failoverPriority.getConsistency().getVoter().getCount(), is(2));
  }

  private static void verifyPlugins(TcConfig tcConfig) {
    Services plugins = tcConfig.getPlugins();

    assertThat(plugins, notNullValue());

    List<Object> configOrService = plugins.getConfigOrService();
    assertThat(configOrService, notNullValue());

    Set<String> actualPlugins = new HashSet<>();

    for (Object obj : configOrService) {
      if (obj instanceof Config) {
        actualPlugins.add(((Config) obj).getConfigContent().getTagName());
      } else {
        actualPlugins.add(((Service) obj).getServiceContent().getTagName());
      }
    }

    Set<String> expectedPlugins =
        new HashSet<>(Arrays.asList("offheap-resources", "connection-leasing", "data-directories"));

    assertThat(actualPlugins, is(expectedPlugins));
  }
}