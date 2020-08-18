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
import org.terracotta.dynamic_config.api.model.Testing;
import org.terracotta.dynamic_config.api.service.ClusterFactory;
import org.terracotta.dynamic_config.api.service.IParameterSubstitutor;
import org.terracotta.server.Server;
import org.terracotta.server.ServerEnv;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.terracotta.dynamic_config.api.model.FailoverPriority.availability;
import static org.terracotta.dynamic_config.api.service.IParameterSubstitutor.identity;

public class CommandLineProcessorChainTest {
  private static final String LICENSE_FILE = "/path/to/license-file";
  private static final String CONFIG_FILE = "/path/to/config-file";
  private static final String NODE_REPOSITORY_DIR = "/path/to/config-dir";
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

  private final Node node1 = Testing.newTestNode("node-1", "localhost", 19410);
  private final Node node2 = Testing.newTestNode("node-2", "localhost", 9411);
  private final Cluster cluster = Testing.newTestCluster((String) null, new Stripe().addNodes(node1));
  private final NodeContext nodeContext = new NodeContext(cluster, 1, "node-1");
  private Options options;
  private Map<Setting, String> paramValueMap;
  private ClusterFactory clusterCreator;
  private ConfigurationGeneratorVisitor configurationGeneratorVisitor;
  private MainCommandLineProcessor mainCommandLineProcessor;
  private IParameterSubstitutor parameterSubstitutor;

  @Before
  public void setUp() {
    ServerEnv.setDefaultServer(mock(Server.class));
    options = mock(Options.class);
    paramValueMap = new HashMap<>();
    clusterCreator = mock(ClusterFactory.class);
    configurationGeneratorVisitor = mock(ConfigurationGeneratorVisitor.class);
    parameterSubstitutor = mock(IParameterSubstitutor.class);
    mainCommandLineProcessor = new MainCommandLineProcessor(options, clusterCreator, configurationGeneratorVisitor, parameterSubstitutor);
  }

  @Test
  public void testStartupWithConfigRepo_noParamsPassed_repoExists() {
    when(configurationGeneratorVisitor.getOrDefaultConfigurationDirectory(null)).thenReturn(Paths.get(NODE_REPOSITORY_DIR));
    when(configurationGeneratorVisitor.findNodeName(eq(Paths.get(NODE_REPOSITORY_DIR)), any(IParameterSubstitutor.class))).thenReturn(Optional.of(NODE_NAME));
    when(clusterCreator.create(any(), eq(parameterSubstitutor))).thenReturn(cluster);

    mainCommandLineProcessor.process();

    verify(configurationGeneratorVisitor).getOrDefaultConfigurationDirectory(null);
    verify(configurationGeneratorVisitor).findNodeName(eq(Paths.get(NODE_REPOSITORY_DIR)), any(IParameterSubstitutor.class));
    verify(configurationGeneratorVisitor).startUsingConfigRepo(Paths.get(NODE_REPOSITORY_DIR), NODE_NAME, false, nodeContext);
    verifyNoMoreInteractions(configurationGeneratorVisitor);
  }

  @Test
  public void testStartupWithConfigRepo_configRepoPassed_repoExists() {
    when(options.getConfigDir()).thenReturn(NODE_REPOSITORY_DIR);
    when(configurationGeneratorVisitor.getOrDefaultConfigurationDirectory(NODE_REPOSITORY_DIR)).thenReturn(Paths.get(NODE_REPOSITORY_DIR));
    when(configurationGeneratorVisitor.findNodeName(eq(Paths.get(NODE_REPOSITORY_DIR)), any(IParameterSubstitutor.class))).thenReturn(Optional.of(NODE_NAME));
    when(clusterCreator.create(any(), eq(parameterSubstitutor))).thenReturn(cluster);

    mainCommandLineProcessor.process();

    verify(configurationGeneratorVisitor).getOrDefaultConfigurationDirectory(NODE_REPOSITORY_DIR);
    verify(configurationGeneratorVisitor).findNodeName(eq(Paths.get(NODE_REPOSITORY_DIR)), any(IParameterSubstitutor.class));
    verify(configurationGeneratorVisitor).startUsingConfigRepo(Paths.get(NODE_REPOSITORY_DIR), NODE_NAME, false, nodeContext);
    verifyNoMoreInteractions(configurationGeneratorVisitor);
  }

  @Test
  public void testStartupWithConfigFile_nonExistentConfigRepo() {
    when(options.allowsAutoActivation()).thenReturn(true);
    when(configurationGeneratorVisitor.getOrDefaultConfigurationDirectory(NODE_REPOSITORY_DIR)).thenReturn(Paths.get(NODE_REPOSITORY_DIR));
    when(configurationGeneratorVisitor.findNodeName(Paths.get(NODE_REPOSITORY_DIR), identity())).thenReturn(Optional.empty());
    when(options.getLicenseFile()).thenReturn(LICENSE_FILE);
    when(options.getConfigFile()).thenReturn(CONFIG_FILE);
    when(options.getHostname()).thenReturn(HOST_NAME);
    when(options.getPort()).thenReturn(NODE_PORT);
    when(clusterCreator.create(Paths.get(CONFIG_FILE))).thenReturn(cluster);
    when(parameterSubstitutor.substitute(CONFIG_FILE)).thenReturn(CONFIG_FILE);
    when(configurationGeneratorVisitor.getMatchingNodeFromConfigFileUsingHostPort(HOST_NAME, NODE_PORT, CONFIG_FILE, cluster)).thenReturn(node1);
    cluster.setName(CLUSTER_NAME);

    mainCommandLineProcessor.process();

    verify(configurationGeneratorVisitor).getMatchingNodeFromConfigFileUsingHostPort(HOST_NAME, NODE_PORT, CONFIG_FILE, cluster);
    verify(configurationGeneratorVisitor).getOrDefaultConfigurationDirectory(any());
    verify(configurationGeneratorVisitor).findNodeName(any(), any(IParameterSubstitutor.class));
    verify(configurationGeneratorVisitor).startActivated(nodeContext, LICENSE_FILE, null);
    verifyNoMoreInteractions(configurationGeneratorVisitor);
  }

  @Test
  public void testPreactivatedWithConfigFileUsingHostPort_ok() {
    when(options.allowsAutoActivation()).thenReturn(true);
    when(options.getLicenseFile()).thenReturn(LICENSE_FILE);
    when(options.getConfigFile()).thenReturn(CONFIG_FILE);
    when(options.getHostname()).thenReturn(HOST_NAME);
    when(options.getPort()).thenReturn(NODE_PORT);
    when(clusterCreator.create(Paths.get(CONFIG_FILE))).thenReturn(cluster);
    when(parameterSubstitutor.substitute(CONFIG_FILE)).thenReturn(CONFIG_FILE);
    when(configurationGeneratorVisitor.getMatchingNodeFromConfigFileUsingHostPort(HOST_NAME, NODE_PORT, CONFIG_FILE, cluster)).thenReturn(node1);
    cluster.setName(CLUSTER_NAME);

    mainCommandLineProcessor.process();

    verify(configurationGeneratorVisitor).getOrDefaultConfigurationDirectory(any());
    verify(configurationGeneratorVisitor).findNodeName(any(), any(IParameterSubstitutor.class));
    verify(configurationGeneratorVisitor).getMatchingNodeFromConfigFileUsingHostPort(HOST_NAME, NODE_PORT, CONFIG_FILE, cluster);
    verify(configurationGeneratorVisitor).startActivated(nodeContext, LICENSE_FILE, null);
    verifyNoMoreInteractions(configurationGeneratorVisitor);
  }

  @Test
  public void testPreactivatedWithConfigFileUsingNodeName_ok() {
    when(options.allowsAutoActivation()).thenReturn(true);
    when(options.getLicenseFile()).thenReturn(LICENSE_FILE);
    when(options.getConfigFile()).thenReturn(CONFIG_FILE);
    when(options.getNodeName()).thenReturn(NODE_NAME);
    when(clusterCreator.create(Paths.get(CONFIG_FILE))).thenReturn(cluster);
    when(parameterSubstitutor.substitute(CONFIG_FILE)).thenReturn(CONFIG_FILE);
    when(configurationGeneratorVisitor.getMatchingNodeFromConfigFileUsingNodeName(NODE_NAME, CONFIG_FILE, cluster)).thenReturn(node1);
    cluster.setName(CLUSTER_NAME);

    mainCommandLineProcessor.process();

    verify(configurationGeneratorVisitor).getOrDefaultConfigurationDirectory(any());
    verify(configurationGeneratorVisitor).findNodeName(any(), any(IParameterSubstitutor.class));
    verify(configurationGeneratorVisitor).getMatchingNodeFromConfigFileUsingNodeName(NODE_NAME, CONFIG_FILE, cluster);
    verify(configurationGeneratorVisitor).startActivated(nodeContext, LICENSE_FILE, null);
    verifyNoMoreInteractions(configurationGeneratorVisitor);
  }

  @Test
  public void testPreactivatedWithConfigFile_greaterThanOneNode() {
    when(options.allowsAutoActivation()).thenReturn(true);
    when(options.getLicenseFile()).thenReturn(LICENSE_FILE);
    when(options.getConfigFile()).thenReturn(CONFIG_FILE);
    when(options.getHostname()).thenReturn(HOST_NAME);
    when(options.getPort()).thenReturn(NODE_PORT);
    when(clusterCreator.create(Paths.get(CONFIG_FILE))).thenReturn(cluster);
    when(parameterSubstitutor.substitute(CONFIG_FILE)).thenReturn(CONFIG_FILE);
    when(configurationGeneratorVisitor.getMatchingNodeFromConfigFileUsingHostPort(HOST_NAME, NODE_PORT, CONFIG_FILE, cluster)).thenReturn(node1);
    cluster.addStripe(new Stripe().addNodes(node2));
    cluster.setName(CLUSTER_NAME);

    doThrow(new UnsupportedOperationException("Cannot start a pre-activated multi-stripe cluster"))
        .when(configurationGeneratorVisitor).startActivated(nodeContext, LICENSE_FILE, null);

    try {
      mainCommandLineProcessor.process();
      fail("Expected exception");
    } catch (Exception e) {
      assertThat(e, instanceOf(UnsupportedOperationException.class));
      assertThat(e.getMessage(), containsString("Cannot start a pre-activated multi-stripe cluster"));
    }

    verify(configurationGeneratorVisitor).getMatchingNodeFromConfigFileUsingHostPort(HOST_NAME, NODE_PORT, CONFIG_FILE, cluster);
    verify(configurationGeneratorVisitor).getOrDefaultConfigurationDirectory(any());
    verify(configurationGeneratorVisitor).findNodeName(any(), any(IParameterSubstitutor.class));
    verify(configurationGeneratorVisitor).startActivated(nodeContext, LICENSE_FILE, null);
    verifyNoMoreInteractions(configurationGeneratorVisitor);
  }

  @Test
  public void testUnconfiguredWithConfigFileUsingHostPort() {
    when(options.getConfigFile()).thenReturn(CONFIG_FILE);
    when(options.getHostname()).thenReturn(HOST_NAME);
    when(options.getPort()).thenReturn(NODE_PORT);
    when(clusterCreator.create(Paths.get(CONFIG_FILE))).thenReturn(cluster);
    when(parameterSubstitutor.substitute(CONFIG_FILE)).thenReturn(CONFIG_FILE);
    when(configurationGeneratorVisitor.getMatchingNodeFromConfigFileUsingHostPort(HOST_NAME, NODE_PORT, CONFIG_FILE, cluster)).thenReturn(node1);
    cluster.getSingleStripe().get().addNode(node2);

    mainCommandLineProcessor.process();

    verify(configurationGeneratorVisitor).getMatchingNodeFromConfigFileUsingHostPort(HOST_NAME, NODE_PORT, CONFIG_FILE, cluster);
    verify(configurationGeneratorVisitor).getOrDefaultConfigurationDirectory(any());
    verify(configurationGeneratorVisitor).findNodeName(any(), any(IParameterSubstitutor.class));
    verify(configurationGeneratorVisitor).startUnconfigured(nodeContext, null);
    verifyNoMoreInteractions(configurationGeneratorVisitor);
  }

  @Test
  public void testUnconfiguredWithConfigFileUsingNodeName() {
    when(options.getConfigFile()).thenReturn(CONFIG_FILE);
    when(options.getNodeName()).thenReturn(NODE_NAME);
    when(clusterCreator.create(Paths.get(CONFIG_FILE))).thenReturn(cluster);
    when(parameterSubstitutor.substitute(CONFIG_FILE)).thenReturn(CONFIG_FILE);
    when(configurationGeneratorVisitor.getMatchingNodeFromConfigFileUsingNodeName(NODE_NAME, CONFIG_FILE, cluster)).thenReturn(node1);
    cluster.getSingleStripe().get().addNode(node2);

    mainCommandLineProcessor.process();

    verify(configurationGeneratorVisitor).getMatchingNodeFromConfigFileUsingNodeName(NODE_NAME, CONFIG_FILE, cluster);
    verify(configurationGeneratorVisitor).getOrDefaultConfigurationDirectory(any());
    verify(configurationGeneratorVisitor).findNodeName(any(), any(IParameterSubstitutor.class));
    verify(configurationGeneratorVisitor).startUnconfigured(nodeContext, null);
    verifyNoMoreInteractions(configurationGeneratorVisitor);
  }

  @Test
  public void testStartupWithCliParams_nonExistentConfigRepo() {
    when(options.allowsAutoActivation()).thenReturn(true);
    when(configurationGeneratorVisitor.getOrDefaultConfigurationDirectory(NODE_REPOSITORY_DIR)).thenReturn(Paths.get(NODE_REPOSITORY_DIR));
    when(configurationGeneratorVisitor.findNodeName(Paths.get(NODE_REPOSITORY_DIR), identity())).thenReturn(Optional.empty());
    when(options.getLicenseFile()).thenReturn(LICENSE_FILE);
    when(options.getClusterName()).thenReturn(CLUSTER_NAME);
    when(options.getFailoverPriority()).thenReturn(availability().toString());
    when(clusterCreator.create(paramValueMap, parameterSubstitutor)).thenReturn(cluster);
    cluster.setName(CLUSTER_NAME);

    mainCommandLineProcessor.process();

    verify(configurationGeneratorVisitor).getOrDefaultConfigurationDirectory(any());
    verify(configurationGeneratorVisitor).findNodeName(any(), any(IParameterSubstitutor.class));
    verify(configurationGeneratorVisitor).startActivated(nodeContext, LICENSE_FILE, null);
    verifyNoMoreInteractions(configurationGeneratorVisitor);
  }

  @Test
  public void testPreactivatedWithCliParams_ok() {
    when(options.allowsAutoActivation()).thenReturn(true);
    when(options.getLicenseFile()).thenReturn(LICENSE_FILE);
    when(options.getFailoverPriority()).thenReturn(availability().toString());
    when(options.getClusterName()).thenReturn(CLUSTER_NAME);
    when(clusterCreator.create(paramValueMap, parameterSubstitutor)).thenReturn(cluster);
    cluster.setName(CLUSTER_NAME);

    mainCommandLineProcessor.process();

    verify(configurationGeneratorVisitor).getOrDefaultConfigurationDirectory(any());
    verify(configurationGeneratorVisitor).findNodeName(any(), any(IParameterSubstitutor.class));
    verify(configurationGeneratorVisitor).startActivated(nodeContext, LICENSE_FILE, null);
    verifyNoMoreInteractions(configurationGeneratorVisitor);
  }

  @Test
  public void testPreactivatedWithCliParams_absentClusterName() {
    when(options.allowsAutoActivation()).thenReturn(true);
    when(options.getFailoverPriority()).thenReturn(availability().toString());
    when(options.getLicenseFile()).thenReturn(LICENSE_FILE);
    when(clusterCreator.create(paramValueMap, parameterSubstitutor)).thenReturn(cluster);

    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("Cluster name is required with license file");
    mainCommandLineProcessor.process();
  }

  @Test
  public void testUnconfiguredWithCliParams() {
    when(clusterCreator.create(paramValueMap, parameterSubstitutor)).thenReturn(cluster);
    when(options.getFailoverPriority()).thenReturn(availability().toString());

    mainCommandLineProcessor.process();

    verify(configurationGeneratorVisitor).getOrDefaultConfigurationDirectory(any());
    verify(configurationGeneratorVisitor).findNodeName(any(), any(IParameterSubstitutor.class));
    verify(configurationGeneratorVisitor).startUnconfigured(nodeContext, null);
    verifyNoMoreInteractions(configurationGeneratorVisitor);
  }

  @Test
  public void testWithCliParams_missingFailoverPriority() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("failover-priority is required");
    mainCommandLineProcessor.process();
  }
}