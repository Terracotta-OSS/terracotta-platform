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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.SystemOutRule;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.Setting;
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.dynamic_config.api.service.ClusterFactory;
import org.terracotta.dynamic_config.api.service.IParameterSubstitutor;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.terracotta.dynamic_config.api.service.IParameterSubstitutor.identity;

public class CommandLineProcessorChainTest {
  private static final String LICENSE_FILE = "/path/to/license-file";
  private static final String CONFIG_FILE = "/path/to/config-file";
  private static final String NODE_REPOSITORY_DIR = "/path/to/node-repository-dir";
  private static final String CLUSTER_NAME = "tc-cluster";
  private static final String HOST_NAME = "localhost";
  private static final String NODE_NAME = "node-1";
  private static final String NODE_PORT = "19410";

  @Rule
  public final SystemOutRule systemOutRule = new SystemOutRule().enableLog().muteForSuccessfulTests();
  @Rule
  public final TemporaryFolder tempFolder = new TemporaryFolder();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private final Node node1 = Node.newDefaultNode("node-1", "localhost", 19410);
  private final Node node2 = Node.newDefaultNode("node-2", "localhost", 9411);
  private Cluster cluster = Cluster.newDefaultCluster((String) null, new Stripe(node1));
  private NodeContext nodeContext = new NodeContext(cluster, 1, "node-1");
  private Options options;
  private Map<Setting, String> paramValueMap;
  private ClusterFactory clusterCreator;
  private ConfigurationGeneratorVisitor configurationGeneratorVisitor;
  private MainCommandLineProcessor mainCommandLineProcessor;
  private IParameterSubstitutor parameterSubstitutor;

  @Before
  public void setUp() {
    options = mock(Options.class);
    paramValueMap = new HashMap<>();
    clusterCreator = mock(ClusterFactory.class);
    configurationGeneratorVisitor = mock(ConfigurationGeneratorVisitor.class);
    parameterSubstitutor = mock(IParameterSubstitutor.class);
    mainCommandLineProcessor = new MainCommandLineProcessor(options, clusterCreator, configurationGeneratorVisitor, parameterSubstitutor);
  }

  @Test
  public void testStartupWithConfigRepo_noParamsPassed_repoExists() {
    when(configurationGeneratorVisitor.getOrDefaultRepositoryDir(null)).thenReturn(Paths.get(NODE_REPOSITORY_DIR));
    when(configurationGeneratorVisitor.findNodeName(eq(Paths.get(NODE_REPOSITORY_DIR)), any(IParameterSubstitutor.class))).thenReturn(Optional.of(NODE_NAME));

    mainCommandLineProcessor.process();

    verify(configurationGeneratorVisitor).getOrDefaultRepositoryDir(null);
    verify(configurationGeneratorVisitor).findNodeName(eq(Paths.get(NODE_REPOSITORY_DIR)), any(IParameterSubstitutor.class));
    verify(configurationGeneratorVisitor).startUsingConfigRepo(Paths.get(NODE_REPOSITORY_DIR), NODE_NAME, false);
    verifyNoMoreInteractions(configurationGeneratorVisitor);
  }

  @Test
  public void testStartupWithConfigRepo_configRepoPassed_repoExists() {
    when(options.getNodeRepositoryDir()).thenReturn(NODE_REPOSITORY_DIR);
    when(configurationGeneratorVisitor.getOrDefaultRepositoryDir(NODE_REPOSITORY_DIR)).thenReturn(Paths.get(NODE_REPOSITORY_DIR));
    when(configurationGeneratorVisitor.findNodeName(eq(Paths.get(NODE_REPOSITORY_DIR)), any(IParameterSubstitutor.class))).thenReturn(Optional.of(NODE_NAME));

    mainCommandLineProcessor.process();

    verify(configurationGeneratorVisitor).getOrDefaultRepositoryDir(NODE_REPOSITORY_DIR);
    verify(configurationGeneratorVisitor).findNodeName(eq(Paths.get(NODE_REPOSITORY_DIR)), any(IParameterSubstitutor.class));
    verify(configurationGeneratorVisitor).startUsingConfigRepo(Paths.get(NODE_REPOSITORY_DIR), NODE_NAME, false);
    verifyNoMoreInteractions(configurationGeneratorVisitor);
  }

  @Test
  public void testStartupWithConfigFile_nonExistentConfigRepo() {
    when(configurationGeneratorVisitor.getOrDefaultRepositoryDir(NODE_REPOSITORY_DIR)).thenReturn(Paths.get(NODE_REPOSITORY_DIR));
    when(configurationGeneratorVisitor.findNodeName(Paths.get(NODE_REPOSITORY_DIR), identity())).thenReturn(Optional.empty());
    when(options.getLicenseFile()).thenReturn(LICENSE_FILE);
    when(options.getConfigFile()).thenReturn(CONFIG_FILE);
    when(options.getNodeHostname()).thenReturn(HOST_NAME);
    when(options.getNodePort()).thenReturn(NODE_PORT);
    when(clusterCreator.create(Paths.get(CONFIG_FILE))).thenReturn(cluster);
    when(parameterSubstitutor.substitute(CONFIG_FILE)).thenReturn(CONFIG_FILE);
    when(configurationGeneratorVisitor.getMatchingNodeFromConfigFile(HOST_NAME, NODE_PORT, CONFIG_FILE, cluster)).thenReturn(node1);
    cluster.setName(CLUSTER_NAME);

    mainCommandLineProcessor.process();

    verify(configurationGeneratorVisitor).getMatchingNodeFromConfigFile(HOST_NAME, NODE_PORT, CONFIG_FILE, cluster);
    verify(configurationGeneratorVisitor).getOrDefaultRepositoryDir(any());
    verify(configurationGeneratorVisitor).findNodeName(any(), any(IParameterSubstitutor.class));
    verify(configurationGeneratorVisitor).startActivated(nodeContext, LICENSE_FILE, null);
    verifyNoMoreInteractions(configurationGeneratorVisitor);
  }

  @Test
  public void testPreactivatedWithConfigFile_ok() {
    when(options.getLicenseFile()).thenReturn(LICENSE_FILE);
    when(options.getConfigFile()).thenReturn(CONFIG_FILE);
    when(options.getNodeHostname()).thenReturn(HOST_NAME);
    when(options.getNodePort()).thenReturn(NODE_PORT);
    when(clusterCreator.create(Paths.get(CONFIG_FILE))).thenReturn(cluster);
    when(parameterSubstitutor.substitute(CONFIG_FILE)).thenReturn(CONFIG_FILE);
    when(configurationGeneratorVisitor.getMatchingNodeFromConfigFile(HOST_NAME, NODE_PORT, CONFIG_FILE, cluster)).thenReturn(node1);
    cluster.setName(CLUSTER_NAME);

    mainCommandLineProcessor.process();

    verify(configurationGeneratorVisitor).getOrDefaultRepositoryDir(any());
    verify(configurationGeneratorVisitor).findNodeName(any(), any(IParameterSubstitutor.class));
    verify(configurationGeneratorVisitor).getMatchingNodeFromConfigFile(HOST_NAME, NODE_PORT, CONFIG_FILE, cluster);
    verify(configurationGeneratorVisitor).startActivated(nodeContext, LICENSE_FILE, null);
    verifyNoMoreInteractions(configurationGeneratorVisitor);
  }

  @Test
  public void testPreactivatedWithConfigFile_greaterThanOneNode() {
    when(options.getLicenseFile()).thenReturn(LICENSE_FILE);
    when(options.getConfigFile()).thenReturn(CONFIG_FILE);
    when(options.getNodeHostname()).thenReturn(HOST_NAME);
    when(options.getNodePort()).thenReturn(NODE_PORT);
    when(clusterCreator.create(Paths.get(CONFIG_FILE))).thenReturn(cluster);
    when(parameterSubstitutor.substitute(CONFIG_FILE)).thenReturn(CONFIG_FILE);
    when(configurationGeneratorVisitor.getMatchingNodeFromConfigFile(HOST_NAME, NODE_PORT, CONFIG_FILE, cluster)).thenReturn(node1);
    cluster.addStripe(new Stripe(node2));
    cluster.setName(CLUSTER_NAME);

    expectedException.expect(UnsupportedOperationException.class);
    expectedException.expectMessage("Cannot start a pre-activated multi-stripe cluster");

    mainCommandLineProcessor.process();

    verify(configurationGeneratorVisitor).getOrDefaultRepositoryDir(any());
    verify(configurationGeneratorVisitor).findNodeName(any(), any(IParameterSubstitutor.class));
    verifyNoMoreInteractions(configurationGeneratorVisitor);
  }

  @Test
  public void testUnconfiguredWithConfigFile() {
    when(options.getConfigFile()).thenReturn(CONFIG_FILE);
    when(options.getNodeHostname()).thenReturn(HOST_NAME);
    when(options.getNodePort()).thenReturn(NODE_PORT);
    when(clusterCreator.create(Paths.get(CONFIG_FILE))).thenReturn(cluster);
    when(parameterSubstitutor.substitute(CONFIG_FILE)).thenReturn(CONFIG_FILE);
    when(configurationGeneratorVisitor.getMatchingNodeFromConfigFile(HOST_NAME, NODE_PORT, CONFIG_FILE, cluster)).thenReturn(node1);
    cluster.getSingleStripe().get().attachNode(node2);

    mainCommandLineProcessor.process();

    verify(configurationGeneratorVisitor).getMatchingNodeFromConfigFile(HOST_NAME, NODE_PORT, CONFIG_FILE, cluster);
    verify(configurationGeneratorVisitor).getOrDefaultRepositoryDir(any());
    verify(configurationGeneratorVisitor).findNodeName(any(), any(IParameterSubstitutor.class));
    verify(configurationGeneratorVisitor).startUnconfigured(nodeContext, null);
    verifyNoMoreInteractions(configurationGeneratorVisitor);
  }

  @Test
  public void testStartupWithCliParams_nonExistentConfigRepo() {
    when(configurationGeneratorVisitor.getOrDefaultRepositoryDir(NODE_REPOSITORY_DIR)).thenReturn(Paths.get(NODE_REPOSITORY_DIR));
    when(configurationGeneratorVisitor.findNodeName(Paths.get(NODE_REPOSITORY_DIR), identity())).thenReturn(Optional.empty());
    when(options.getLicenseFile()).thenReturn(LICENSE_FILE);
    when(options.getClusterName()).thenReturn(CLUSTER_NAME);
    when(clusterCreator.create(paramValueMap, parameterSubstitutor)).thenReturn(cluster);
    cluster.setName(CLUSTER_NAME);

    mainCommandLineProcessor.process();

    verify(configurationGeneratorVisitor).getOrDefaultRepositoryDir(any());
    verify(configurationGeneratorVisitor).findNodeName(any(), any(IParameterSubstitutor.class));
    verify(configurationGeneratorVisitor).startActivated(nodeContext, LICENSE_FILE, null);
    verifyNoMoreInteractions(configurationGeneratorVisitor);
  }

  @Test
  public void testPreactivatedWithCliParams_ok() {
    when(options.getLicenseFile()).thenReturn(LICENSE_FILE);
    when(options.getClusterName()).thenReturn(CLUSTER_NAME);
    when(clusterCreator.create(paramValueMap, parameterSubstitutor)).thenReturn(cluster);
    cluster.setName(CLUSTER_NAME);

    mainCommandLineProcessor.process();

    verify(configurationGeneratorVisitor).getOrDefaultRepositoryDir(any());
    verify(configurationGeneratorVisitor).findNodeName(any(), any(IParameterSubstitutor.class));
    verify(configurationGeneratorVisitor).startActivated(nodeContext, LICENSE_FILE, null);
    verifyNoMoreInteractions(configurationGeneratorVisitor);
  }

  @Test
  public void testPreactivatedWithCliParams_absentClusterName() {
    when(options.getLicenseFile()).thenReturn(LICENSE_FILE);
    when(clusterCreator.create(paramValueMap, parameterSubstitutor)).thenReturn(cluster);

    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("Cluster name is required with license file");

    mainCommandLineProcessor.process();

    verifyNoMoreInteractions(configurationGeneratorVisitor);
  }

  @Test
  public void testUnconfiguredWithCliParams() {
    when(clusterCreator.create(paramValueMap, parameterSubstitutor)).thenReturn(cluster);

    mainCommandLineProcessor.process();

    verify(configurationGeneratorVisitor).getOrDefaultRepositoryDir(any());
    verify(configurationGeneratorVisitor).findNodeName(any(), any(IParameterSubstitutor.class));
    verify(configurationGeneratorVisitor).startUnconfigured(nodeContext, null);
    verifyNoMoreInteractions(configurationGeneratorVisitor);
  }
}