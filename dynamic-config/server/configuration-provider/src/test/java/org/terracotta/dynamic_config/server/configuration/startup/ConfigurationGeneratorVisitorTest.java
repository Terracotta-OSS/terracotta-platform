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
package org.terracotta.dynamic_config.server.configuration.startup;

import org.junit.BeforeClass;
import org.junit.Test;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.Setting;
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.dynamic_config.server.api.LicenseService;
import org.terracotta.dynamic_config.server.api.PathResolver;
import org.terracotta.dynamic_config.server.configuration.service.ConfigChangeHandlerManagerImpl;
import org.terracotta.dynamic_config.server.configuration.service.NomadServerManager;
import org.terracotta.dynamic_config.server.configuration.service.ParameterSubstitutor;
import org.terracotta.json.ObjectMapperFactory;
import org.terracotta.server.Server;
import org.terracotta.server.ServerEnv;

import java.nio.file.Paths;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.terracotta.testing.ExceptionMatcher.throwing;

public class ConfigurationGeneratorVisitorTest {

  private static final ParameterSubstitutor PARAMETER_SUBSTITUTOR = new ParameterSubstitutor();
  private static final ConfigurationGeneratorVisitor STARTUP_MANAGER = new ConfigurationGeneratorVisitor(
      PARAMETER_SUBSTITUTOR,
      new NomadServerManager(PARAMETER_SUBSTITUTOR, new ConfigChangeHandlerManagerImpl(), mock(LicenseService.class), new ObjectMapperFactory()),
      ConfigurationGeneratorVisitorTest.class.getClassLoader(),
      new PathResolver(Paths.get("%(user.dir)")),
      new ObjectMapperFactory());

  private static final String CONFIG_FILE = "/path/to/config-file";

  @BeforeClass
  public static void beforeClass() throws Exception {
    ServerEnv.setDefaultServer(mock(Server.class));
  }

  @Test
  public void testConfigFileContainsOneNode_noNodeHostPortSpecified() {
    Node node = Node.newDefaultNode("localhost");
    Cluster cluster = Cluster.newDefaultCluster(new Stripe(node));
    Node matchingNode = STARTUP_MANAGER.getMatchingNodeFromConfigFileUsingHostPort(null, null, CONFIG_FILE, cluster);

    assertThat(matchingNode, equalTo(node));
  }

  @Test
  public void testConfigFileContainsOneNode_matchingNodeHostPortSpecified() {
    Node node = Node.newDefaultNode(PARAMETER_SUBSTITUTOR.substitute(Setting.NODE_HOSTNAME.getDefaultValue()));
    Cluster cluster = Cluster.newDefaultCluster(new Stripe(node));
    Node matchingNode = STARTUP_MANAGER.getMatchingNodeFromConfigFileUsingHostPort(node.getNodeHostname(), String.valueOf(node.getNodePort()), CONFIG_FILE, cluster);

    assertThat(matchingNode, equalTo(node));
  }

  @Test
  public void testConfigFileContainsOneNode_noMatchingNodeHostPortSpecified() {
    Node node = Node.newDefaultNode("localhost");
    Cluster cluster = Cluster.newDefaultCluster(new Stripe(node));

    assertThat(
        () -> STARTUP_MANAGER.getMatchingNodeFromConfigFileUsingHostPort("blah", "12345", CONFIG_FILE, cluster),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(containsString("Did not find a matching node entry in config file")))
    );
  }

  @Test
  public void testConfigFileContainsMultipleNodes_noNodeHostPortSpecified_foundMatchUsingDefaults() {
    Node node1 = Node.newDefaultNode(PARAMETER_SUBSTITUTOR.substitute(Setting.NODE_HOSTNAME.getDefaultValue()));
    Node node2 = Node.newDefaultNode("localhost", 1234);
    Cluster cluster = Cluster.newDefaultCluster(new Stripe(node1, node2));

    Node matchingNode = STARTUP_MANAGER.getMatchingNodeFromConfigFileUsingHostPort(null, null, CONFIG_FILE, cluster);
    assertThat(matchingNode, equalTo(node1));
  }

  @Test
  public void testConfigFileContainsMultipleNodes_noNodeHostPortSpecified() {
    Node node1 = Node.newDefaultNode("some-host", 1234);
    Node node2 = Node.newDefaultNode("some-host", 5678);
    Cluster cluster = Cluster.newDefaultCluster(new Stripe(node1, node2));

    assertThat(
        () -> STARTUP_MANAGER.getMatchingNodeFromConfigFileUsingHostPort(null, null, CONFIG_FILE, cluster),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(containsString("Did not find a matching node entry in config file")))
    );
  }

  @Test
  public void testConfigFileContainsMultipleNodes_matchingNodeHostPortSpecified() {
    Node node1 = Node.newDefaultNode("localhost");
    Node node2 = Node.newDefaultNode("localhost", 1234);
    Cluster cluster = Cluster.newDefaultCluster(new Stripe(node1, node2));
    Node matchingNode = STARTUP_MANAGER.getMatchingNodeFromConfigFileUsingHostPort(node2.getNodeHostname(), String.valueOf(node2.getNodePort()), CONFIG_FILE, cluster);

    assertThat(matchingNode, equalTo(node2));
  }

  @Test
  public void testConfigFileContainsMultipleNodes_noMatchingNodeHostPortSpecified() {
    Node node1 = Node.newDefaultNode("localhost");
    Node node2 = Node.newDefaultNode("localhost", 1234);
    Cluster cluster = Cluster.newDefaultCluster(new Stripe(node1, node2));

    assertThat(
        () -> STARTUP_MANAGER.getMatchingNodeFromConfigFileUsingHostPort("blah", null, CONFIG_FILE, cluster),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(containsString("Did not find a matching node entry in config file")))
    );
  }

  @Test
  public void testConfigFileContainsSingleNode_noMatchesForSpecifiedNodeName() {
    Node node1 = Node.newDefaultNode("localhost");
    Cluster cluster = Cluster.newDefaultCluster(new Stripe(node1));

    assertThat(
        () -> STARTUP_MANAGER.getMatchingNodeFromConfigFileUsingNodeName("blah", CONFIG_FILE, cluster),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(containsString("Did not find a matching node entry in config file")))
    );
  }

  @Test
  public void testConfigFileContainsMultipleNodes_noMatchesForSpecifiedNodeName() {
    Node node1 = Node.newDefaultNode("localhost");
    Node node2 = Node.newDefaultNode("localhost", 1234);
    Cluster cluster = Cluster.newDefaultCluster(new Stripe(node1, node2));

    assertThat(
        () -> STARTUP_MANAGER.getMatchingNodeFromConfigFileUsingNodeName("blah", CONFIG_FILE, cluster),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(containsString("Did not find a matching node entry in config file")))
    );
  }

  @Test
  public void testConfigFileContainsMultipleNodes_multipleMatchesForSpecifiedNodeName() {
    Node node1 = Node.newDefaultNode("node-1", "localhost");
    Node node2 = Node.newDefaultNode("node-1", "localhost", 1234);
    Cluster cluster = Cluster.newDefaultCluster(new Stripe(node1, node2));

    assertThat(
        () -> STARTUP_MANAGER.getMatchingNodeFromConfigFileUsingNodeName("node-1", CONFIG_FILE, cluster),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(containsString("Found multiple matching node entries")))
    );
  }

  @Test
  public void testConfigFileContainsSingleNode_matchingNodeNameSpecified() {
    Node node1 = Node.newDefaultNode("localhost");
    Cluster cluster = Cluster.newDefaultCluster(new Stripe(node1));
    Node matchingNode = STARTUP_MANAGER.getMatchingNodeFromConfigFileUsingNodeName(node1.getNodeName(), CONFIG_FILE, cluster);

    assertThat(matchingNode, equalTo(node1));
  }

  @Test
  public void testConfigFileContainsMultipleNodes_matchingNodeNameSpecified() {
    Node node1 = Node.newDefaultNode("localhost");
    Node node2 = Node.newDefaultNode("localhost", 1234);
    Cluster cluster = Cluster.newDefaultCluster(new Stripe(node1, node2));
    Node matchingNode = STARTUP_MANAGER.getMatchingNodeFromConfigFileUsingNodeName(node2.getNodeName(), CONFIG_FILE, cluster);

    assertThat(matchingNode, equalTo(node2));
  }
}