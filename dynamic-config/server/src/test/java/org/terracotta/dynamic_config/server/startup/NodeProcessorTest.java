/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.server.startup;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.SystemOutRule;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.Setting;
import org.terracotta.dynamic_config.api.service.ClusterFactory;
import org.terracotta.dynamic_config.api.service.IParameterSubstitutor;
import org.terracotta.dynamic_config.server.parsing.Options;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class NodeProcessorTest {
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

  private Node node;
  private Cluster cluster;
  private Options options;
  private Map<Setting, String> paramValueMap;
  private ClusterFactory clusterCreator;
  private StartupManager startupManager;
  private NodeProcessor nodeProcessor;
  private IParameterSubstitutor parameterSubstitutor;

  @Before
  public void setUp() {
    node = mock(Node.class);
    cluster = mock(Cluster.class);
    options = mock(Options.class);
    paramValueMap = new HashMap<>();
    clusterCreator = mock(ClusterFactory.class);
    startupManager = mock(StartupManager.class);
    parameterSubstitutor = mock(IParameterSubstitutor.class);
    nodeProcessor = new NodeProcessor(options, paramValueMap, clusterCreator, startupManager, parameterSubstitutor);
  }

  @Test
  public void testStartupWithConfigRepo_noParamsPassed_repoExists() {
    when(startupManager.getOrDefaultRepositoryDir(null)).thenReturn(Paths.get(NODE_REPOSITORY_DIR));
    when(startupManager.findNodeName(Paths.get(NODE_REPOSITORY_DIR))).thenReturn(Optional.of(NODE_NAME));
    doAnswer(invocation -> true).when(startupManager).startUsingConfigRepo(Paths.get(NODE_REPOSITORY_DIR), NODE_NAME);

    nodeProcessor.process();

    verify(startupManager).getOrDefaultRepositoryDir(null);
    verify(startupManager).findNodeName(Paths.get(NODE_REPOSITORY_DIR));
    verify(startupManager).startUsingConfigRepo(Paths.get(NODE_REPOSITORY_DIR), NODE_NAME);
    verifyNoMoreInteractions(startupManager);
  }

  @Test
  public void testStartupWithConfigRepo_configRepoPassed_repoExists() {
    when(options.getNodeRepositoryDir()).thenReturn(NODE_REPOSITORY_DIR);
    when(startupManager.getOrDefaultRepositoryDir(NODE_REPOSITORY_DIR)).thenReturn(Paths.get(NODE_REPOSITORY_DIR));
    when(startupManager.findNodeName(Paths.get(NODE_REPOSITORY_DIR))).thenReturn(Optional.of(NODE_NAME));
    doAnswer(invocation -> true).when(startupManager).startUsingConfigRepo(Paths.get(NODE_REPOSITORY_DIR), NODE_NAME);

    nodeProcessor.process();

    verify(startupManager).getOrDefaultRepositoryDir(NODE_REPOSITORY_DIR);
    verify(startupManager).findNodeName(Paths.get(NODE_REPOSITORY_DIR));
    verify(startupManager).startUsingConfigRepo(Paths.get(NODE_REPOSITORY_DIR), NODE_NAME);
    verifyNoMoreInteractions(startupManager);
  }

  @Test
  public void testStartupWithConfigFile_nonExistentConfigRepo() {
    when(startupManager.getOrDefaultRepositoryDir(NODE_REPOSITORY_DIR)).thenReturn(Paths.get(NODE_REPOSITORY_DIR));
    when(startupManager.findNodeName(Paths.get(NODE_REPOSITORY_DIR))).thenReturn(Optional.empty());
    when(options.getLicenseFile()).thenReturn(LICENSE_FILE);
    when(options.getConfigFile()).thenReturn(CONFIG_FILE);
    when(options.getNodeHostname()).thenReturn(HOST_NAME);
    when(options.getNodePort()).thenReturn(NODE_PORT);
    when(clusterCreator.create(Paths.get(CONFIG_FILE))).thenReturn(cluster);
    when(startupManager.getMatchingNodeFromConfigFile(HOST_NAME, NODE_PORT, CONFIG_FILE, cluster)).thenReturn(node);
    when(cluster.getNodeCount()).thenReturn(1);
    when(cluster.getName()).thenReturn(CLUSTER_NAME);
    doAnswer(invocation -> true).when(startupManager).startActivated(cluster, node, LICENSE_FILE, null);

    nodeProcessor.process();

    verify(startupManager).getMatchingNodeFromConfigFile(HOST_NAME, NODE_PORT, CONFIG_FILE, cluster);
    verify(startupManager).getOrDefaultRepositoryDir(any());
    verify(startupManager).findNodeName(any());
    verify(startupManager).startActivated(cluster, node, LICENSE_FILE, null);
    verifyNoMoreInteractions(startupManager);
  }

  @Test
  public void testPreactivatedWithConfigFile_ok() {
    when(options.getLicenseFile()).thenReturn(LICENSE_FILE);
    when(options.getConfigFile()).thenReturn(CONFIG_FILE);
    when(options.getNodeHostname()).thenReturn(HOST_NAME);
    when(options.getNodePort()).thenReturn(NODE_PORT);
    when(clusterCreator.create(Paths.get(CONFIG_FILE))).thenReturn(cluster);
    when(startupManager.getMatchingNodeFromConfigFile(HOST_NAME, NODE_PORT, CONFIG_FILE, cluster)).thenReturn(node);
    when(cluster.getNodeCount()).thenReturn(1);
    when(cluster.getName()).thenReturn(CLUSTER_NAME);
    doAnswer(invocation -> true).when(startupManager).startActivated(cluster, node, LICENSE_FILE, null);

    nodeProcessor.process();

    verify(startupManager).getOrDefaultRepositoryDir(any());
    verify(startupManager).findNodeName(any());
    verify(startupManager).getMatchingNodeFromConfigFile(HOST_NAME, NODE_PORT, CONFIG_FILE, cluster);
    verify(startupManager).startActivated(cluster, node, LICENSE_FILE, null);
    verifyNoMoreInteractions(startupManager);
  }

  @Test
  public void testPreactivatedWithConfigFile_greaterThanOneNode() {
    when(options.getLicenseFile()).thenReturn(LICENSE_FILE);
    when(options.getConfigFile()).thenReturn(CONFIG_FILE);
    when(options.getNodeHostname()).thenReturn(HOST_NAME);
    when(options.getNodePort()).thenReturn(NODE_PORT);
    when(clusterCreator.create(Paths.get(CONFIG_FILE))).thenReturn(cluster);
    when(startupManager.getMatchingNodeFromConfigFile(HOST_NAME, NODE_PORT, CONFIG_FILE, cluster)).thenReturn(node);
    when(cluster.getNodeCount()).thenReturn(2);
    when(cluster.getName()).thenReturn(CLUSTER_NAME);

    expectedException.expect(UnsupportedOperationException.class);
    expectedException.expectMessage("Cannot auto-activate a cluster of more than 1 node");

    nodeProcessor.process();

    verify(startupManager).getOrDefaultRepositoryDir(any());
    verify(startupManager).findNodeName(any());
    verifyNoMoreInteractions(startupManager);
  }

  @Test
  public void testUnconfiguredWithConfigFile() {
    when(options.getConfigFile()).thenReturn(CONFIG_FILE);
    when(options.getNodeHostname()).thenReturn(HOST_NAME);
    when(options.getNodePort()).thenReturn(NODE_PORT);
    when(clusterCreator.create(Paths.get(CONFIG_FILE))).thenReturn(cluster);
    when(startupManager.getMatchingNodeFromConfigFile(HOST_NAME, NODE_PORT, CONFIG_FILE, cluster)).thenReturn(node);
    when(cluster.getNodeCount()).thenReturn(2);
    doAnswer(invocation -> true).when(startupManager).startUnconfigured(cluster, node, null);

    nodeProcessor.process();

    verify(startupManager).getMatchingNodeFromConfigFile(HOST_NAME, NODE_PORT, CONFIG_FILE, cluster);
    verify(startupManager).getOrDefaultRepositoryDir(any());
    verify(startupManager).findNodeName(any());
    verify(startupManager).startUnconfigured(cluster, node, null);
    verifyNoMoreInteractions(startupManager);
  }

  @Test
  public void testStartupWithCliParams_nonExistentConfigRepo() {
    when(startupManager.getOrDefaultRepositoryDir(NODE_REPOSITORY_DIR)).thenReturn(Paths.get(NODE_REPOSITORY_DIR));
    when(startupManager.findNodeName(Paths.get(NODE_REPOSITORY_DIR))).thenReturn(Optional.empty());
    when(options.getLicenseFile()).thenReturn(LICENSE_FILE);
    when(options.getClusterName()).thenReturn(CLUSTER_NAME);
    when(clusterCreator.create(paramValueMap, parameterSubstitutor)).thenReturn(cluster);
    when(cluster.getSingleNode()).thenReturn(Optional.of(node));
    when(cluster.getName()).thenReturn(CLUSTER_NAME);
    doAnswer(invocation -> true).when(startupManager).startActivated(cluster, node, LICENSE_FILE, null);

    nodeProcessor.process();

    verify(startupManager).getOrDefaultRepositoryDir(any());
    verify(startupManager).findNodeName(any());
    verify(startupManager).startActivated(cluster, node, LICENSE_FILE, null);
    verifyNoMoreInteractions(startupManager);
  }

  @Test
  public void testPreactivatedWithCliParams_ok() {
    when(options.getLicenseFile()).thenReturn(LICENSE_FILE);
    when(options.getClusterName()).thenReturn(CLUSTER_NAME);
    when(clusterCreator.create(paramValueMap, parameterSubstitutor)).thenReturn(cluster);
    when(cluster.getSingleNode()).thenReturn(Optional.of(node));
    when(cluster.getName()).thenReturn(CLUSTER_NAME);
    doAnswer(invocation -> true).when(startupManager).startActivated(cluster, node, LICENSE_FILE, null);

    nodeProcessor.process();

    verify(startupManager).getOrDefaultRepositoryDir(any());
    verify(startupManager).findNodeName(any());
    verify(startupManager).startActivated(cluster, node, LICENSE_FILE, null);
    verifyNoMoreInteractions(startupManager);
  }

  @Test
  public void testPreactivatedWithCliParams_absentClusterName() {
    when(options.getLicenseFile()).thenReturn(LICENSE_FILE);
    when(clusterCreator.create(paramValueMap, parameterSubstitutor)).thenReturn(cluster);
    when(cluster.getSingleNode()).thenReturn(Optional.of(node));

    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("Cluster name is required with license file");

    nodeProcessor.process();

    verifyNoMoreInteractions(startupManager);
  }

  @Test
  public void testUnconfiguredWithCliParams() {
    when(clusterCreator.create(paramValueMap, parameterSubstitutor)).thenReturn(cluster);
    when(cluster.getSingleNode()).thenReturn(Optional.of(node));
    doAnswer(invocation -> true).when(startupManager).startUnconfigured(cluster, node, null);

    nodeProcessor.process();

    verify(startupManager).getOrDefaultRepositoryDir(any());
    verify(startupManager).findNodeName(any());
    verify(startupManager).startUnconfigured(cluster, node, null);
    verifyNoMoreInteractions(startupManager);
  }
}