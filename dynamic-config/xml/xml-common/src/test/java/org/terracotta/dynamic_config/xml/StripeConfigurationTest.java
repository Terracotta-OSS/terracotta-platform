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
import org.terracotta.common.struct.TimeUnit;
import org.terracotta.config.Server;
import org.terracotta.config.Servers;
import org.terracotta.config.TcConfig;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.dynamic_config.api.service.PathResolver;
import org.terracotta.testing.TmpDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class StripeConfigurationTest {

  @Rule
  public TmpDir temporaryFolder = new TmpDir();

  PathResolver pathResolver;

  @Before
  public void setUp() {
    pathResolver = new PathResolver(temporaryFolder.getRoot());
  }

  @Test
  public void testCreation() {
    List<Node> nodeList = new ArrayList<>();
    Node node1 = Node.newDefaultNode("server-1", "localhost", 94101);
    Path logPath = temporaryFolder.getRoot();
    node1.setNodeLogDir(logPath);
    node1.setClientReconnectWindow(100, TimeUnit.SECONDS);
    node1.setNodeBindAddress("127.0.0.1");
    node1.setNodeGroupBindAddress("127.0.1.1");
    node1.setNodeGroupPort(94102);
    nodeList.add(node1);

    Node node2 = Node.newDefaultNode("server-2", "localhost", 94101);
    node2.setNodeLogDir(logPath);
    node2.setClientReconnectWindow(100, TimeUnit.SECONDS);
    node2.setNodeBindAddress("127.0.0.1");
    node2.setNodeGroupBindAddress("127.0.1.1");
    node2.setNodeGroupPort(94102);
    nodeList.add(node2);

    Stripe stripe = new Stripe(nodeList);

    StripeConfiguration stripeConfiguration = new StripeConfiguration(stripe, pathResolver);

    assertThat(stripeConfiguration.get("server-1"), notNullValue());
    assertThat(stripeConfiguration.get("server-2"), notNullValue());

    TcConfig tcConfig = stripeConfiguration.get("server-1").getTcConfig();
    Servers servers = tcConfig.getServers();
    assertThat(servers, notNullValue());
    assertThat(servers.getClientReconnectWindow(), is(100));
    assertThat(servers.getServer().size(), is(2));

    verifyServers(servers);
    verifyServer(servers.getServer().get(0), logPath.toString());
  }

  private void verifyServer(Server server, String logLocation) {
    assertThat(server, notNullValue());
    assertThat(server.getTsaPort(), notNullValue());
    assertThat(server.getTsaPort().getValue(), is(94101));
    assertThat(server.getTsaPort().getBind(), is("127.0.0.1"));

    assertThat(server.getTsaGroupPort(), notNullValue());
    assertThat(server.getTsaGroupPort().getValue(), is(94102));
    assertThat(server.getTsaGroupPort().getBind(), is("127.0.1.1"));

    assertThat(server.getBind(), is("127.0.0.1"));
    assertThat(server.getHost(), is("localhost"));
    assertThat(server.getLogs(), is(logLocation));
  }

  private static void verifyServers(Servers servers) {
    Set<String> expected = new HashSet<>(Arrays.asList("server-1", "server-2"));
    Set<String> actual = new HashSet<>();
    for (Server server : servers.getServer()) {
      actual.add(server.getName());
    }

    assertThat(actual, is(expected));
  }
}