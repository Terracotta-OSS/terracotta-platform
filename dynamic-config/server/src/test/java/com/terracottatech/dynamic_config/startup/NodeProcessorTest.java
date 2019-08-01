/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.startup;

import com.terracottatech.dynamic_config.diagnostic.LicensingService;
import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.dynamic_config.model.Node;
import com.terracottatech.dynamic_config.parsing.Options;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.junit.contrib.java.lang.system.SystemOutRule;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NodeProcessorTest {
  private static final String LICENSE_FILE = "/path/to/license-file";
  private static final String CONFIG_FILE = "/path/to/config-file";
  private static final String NODE_CONFIG_DIR = "/path/to/node-config-dir";
  private static final String CLUSTER_NAME = "tc-cluster";
  private static final String HOST_NAME = "localhost";
  private static final String NODE_NAME = "node-1";
  private static final String NODE_PORT = "19410";

  @Rule
  public final ExpectedSystemExit systemExit = ExpectedSystemExit.none();
  @Rule
  public final SystemOutRule systemOutRule = new SystemOutRule().enableLog().muteForSuccessfulTests();
  @Rule
  public final TemporaryFolder tempFolder = new TemporaryFolder();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private Node node;
  private Cluster cluster;
  private Options options;
  private Map<String, String> paramValueMap;
  private LicensingService licensingService;
  private ClusterCreator clusterCreator;
  private StartupManager startupManager;
  private NodeProcessor nodeProcessor;

  @Before
  public void setUp() {
    node = mock(Node.class);
    cluster = mock(Cluster.class);
    options = mock(Options.class);
    paramValueMap = new HashMap<>();
    licensingService = mock(LicensingService.class);
    clusterCreator = mock(ClusterCreator.class);
    startupManager = mock(StartupManager.class);
    nodeProcessor = new NodeProcessor(options, paramValueMap, licensingService, clusterCreator, startupManager);
  }

  @Test
  public void testStartupWithConfigRepo_noParamsPassed_repoExists() {
    when(startupManager.getOrDefaultConfigDir(null)).thenReturn(Paths.get(NODE_CONFIG_DIR));
    when(startupManager.findNodeName(Paths.get(NODE_CONFIG_DIR))).thenReturn(Optional.of(NODE_NAME));
    doAnswer(invocation -> {
      System.out.println("Node startup with config repo successful");
      System.exit(0);
      return null;
    }).when(startupManager).startUsingConfigRepo(Paths.get(NODE_CONFIG_DIR), NODE_NAME);

    systemExit.expectSystemExit();
    systemExit.checkAssertionAfterwards(() -> {
      assertThat(systemOutRule.getLog(), containsString("Node startup with config repo successful"));
    });
    nodeProcessor.process();
  }

  @Test
  public void testStartupWithConfigRepo_configRepoPassed_repoExists() {
    when(options.getNodeConfigDir()).thenReturn(NODE_CONFIG_DIR);
    when(startupManager.getOrDefaultConfigDir(NODE_CONFIG_DIR)).thenReturn(Paths.get(NODE_CONFIG_DIR));
    when(startupManager.findNodeName(Paths.get(NODE_CONFIG_DIR))).thenReturn(Optional.of(NODE_NAME));
    doAnswer(invocation -> {
      System.out.println("Node startup with config repo successful");
      System.exit(0);
      return null;
    }).when(startupManager).startUsingConfigRepo(Paths.get(NODE_CONFIG_DIR), NODE_NAME);

    systemExit.expectSystemExit();
    systemExit.checkAssertionAfterwards(() -> {
      assertThat(systemOutRule.getLog(), containsString("Node startup with config repo successful"));
    });
    nodeProcessor.process();
  }

  @Test
  public void testStartupWithConfigFile_nonExistentConfigRepo() {
    when(startupManager.getOrDefaultConfigDir(NODE_CONFIG_DIR)).thenReturn(Paths.get(NODE_CONFIG_DIR));
    when(startupManager.findNodeName(Paths.get(NODE_CONFIG_DIR))).thenReturn(Optional.empty());
    when(options.getLicenseFile()).thenReturn(LICENSE_FILE);
    when(options.getConfigFile()).thenReturn(CONFIG_FILE);
    when(options.getNodeHostname()).thenReturn(HOST_NAME);
    when(options.getNodePort()).thenReturn(NODE_PORT);
    when(clusterCreator.create(Paths.get(CONFIG_FILE), null)).thenReturn(cluster);
    when(startupManager.getMatchingNodeFromConfigFile(HOST_NAME, NODE_PORT, CONFIG_FILE, cluster)).thenReturn(node);
    when(cluster.getNodeCount()).thenReturn(1);
    doAnswer(invocation -> {
      System.out.println("Node startup in preactivated state successful");
      System.exit(0);
      return null;
    }).when(startupManager).startPreactivated(cluster, node, licensingService, LICENSE_FILE);

    systemExit.expectSystemExit();
    systemExit.checkAssertionAfterwards(() -> {
      assertThat(systemOutRule.getLog(), containsString("Node startup in preactivated state successful"));
      verify(startupManager, times(1)).startPreactivated(cluster, node, licensingService, LICENSE_FILE);
      verify(startupManager, times(0)).startUnconfigured(cluster, node);
    });
    nodeProcessor.process();
  }

  @Test
  public void testPreactivatedWithConfigFile_ok() {
    when(options.getLicenseFile()).thenReturn(LICENSE_FILE);
    when(options.getConfigFile()).thenReturn(CONFIG_FILE);
    when(options.getNodeHostname()).thenReturn(HOST_NAME);
    when(options.getNodePort()).thenReturn(NODE_PORT);
    when(clusterCreator.create(Paths.get(CONFIG_FILE), null)).thenReturn(cluster);
    when(startupManager.getMatchingNodeFromConfigFile(HOST_NAME, NODE_PORT, CONFIG_FILE, cluster)).thenReturn(node);
    when(cluster.getNodeCount()).thenReturn(1);
    doAnswer(invocation -> {
      System.out.println("Node startup in preactivated state successful");
      System.exit(0);
      return null;
    }).when(startupManager).startPreactivated(cluster, node, licensingService, LICENSE_FILE);

    systemExit.expectSystemExit();
    systemExit.checkAssertionAfterwards(() -> {
      assertThat(systemOutRule.getLog(), containsString("Node startup in preactivated state successful"));
      verify(startupManager, times(1)).startPreactivated(cluster, node, licensingService, LICENSE_FILE);
      verify(startupManager, times(0)).startUnconfigured(cluster, node);
    });
    nodeProcessor.process();
  }

  @Test
  public void testPreactivatedWithConfigFile_greaterThanOneNode() {
    when(options.getLicenseFile()).thenReturn(LICENSE_FILE);
    when(options.getConfigFile()).thenReturn(CONFIG_FILE);
    when(options.getNodeHostname()).thenReturn(HOST_NAME);
    when(options.getNodePort()).thenReturn(NODE_PORT);
    when(clusterCreator.create(Paths.get(CONFIG_FILE), null)).thenReturn(cluster);
    when(startupManager.getMatchingNodeFromConfigFile(HOST_NAME, NODE_PORT, CONFIG_FILE, cluster)).thenReturn(node);
    when(cluster.getNodeCount()).thenReturn(2);

    expectedException.expect(UnsupportedOperationException.class);
    expectedException.expectMessage("License file option can be used only with a one-node cluster config file");
    systemExit.checkAssertionAfterwards(() -> {
      verify(startupManager, times(0)).startPreactivated(cluster, node, licensingService, LICENSE_FILE);
      verify(startupManager, times(0)).startUnconfigured(cluster, node);
    });
    nodeProcessor.process();
  }

  @Test
  public void testUnconfiguredWithConfigFile() {
    when(options.getConfigFile()).thenReturn(CONFIG_FILE);
    when(options.getNodeHostname()).thenReturn(HOST_NAME);
    when(options.getNodePort()).thenReturn(NODE_PORT);
    when(clusterCreator.create(Paths.get(CONFIG_FILE), null)).thenReturn(cluster);
    when(startupManager.getMatchingNodeFromConfigFile(HOST_NAME, NODE_PORT, CONFIG_FILE, cluster)).thenReturn(node);
    when(cluster.getNodeCount()).thenReturn(2);
    doAnswer(invocation -> {
      System.out.println("Node startup in UNCONFIGURED state successful");
      System.exit(0);
      return null;
    }).when(startupManager).startUnconfigured(cluster, node);

    systemExit.expectSystemExit();
    systemExit.checkAssertionAfterwards(() -> {
      assertThat(systemOutRule.getLog(), containsString("Node startup in UNCONFIGURED state successful"));
      verify(startupManager).startUnconfigured(cluster, node);
      verify(startupManager, times(0)).startPreactivated(cluster, node, licensingService, LICENSE_FILE);
    });
    nodeProcessor.process();
  }

  @Test
  public void testStartupWithCliParams_nonExistentConfigRepo() {
    when(startupManager.getOrDefaultConfigDir(NODE_CONFIG_DIR)).thenReturn(Paths.get(NODE_CONFIG_DIR));
    when(startupManager.findNodeName(Paths.get(NODE_CONFIG_DIR))).thenReturn(Optional.empty());
    when(options.getLicenseFile()).thenReturn(LICENSE_FILE);
    when(options.getClusterName()).thenReturn(CLUSTER_NAME);
    when(clusterCreator.create(paramValueMap)).thenReturn(cluster);
    when(cluster.getSingleNode()).thenReturn(Optional.of(node));
    doAnswer(invocation -> {
      System.out.println("Node startup in preactivated state successful");
      System.exit(0);
      return null;
    }).when(startupManager).startPreactivated(cluster, node, licensingService, LICENSE_FILE);

    systemExit.expectSystemExit();
    systemExit.checkAssertionAfterwards(() -> {
      assertThat(systemOutRule.getLog(), containsString("Node startup in preactivated state successful"));
      verify(startupManager).startPreactivated(cluster, node, licensingService, LICENSE_FILE);
      verify(startupManager, times(0)).startUnconfigured(cluster, node);
    });
    nodeProcessor.process();
  }

  @Test
  public void testPreactivatedWithCliParams_ok() {
    when(options.getLicenseFile()).thenReturn(LICENSE_FILE);
    when(options.getClusterName()).thenReturn(CLUSTER_NAME);
    when(clusterCreator.create(paramValueMap)).thenReturn(cluster);
    when(cluster.getSingleNode()).thenReturn(Optional.of(node));
    doAnswer(invocation -> {
      System.out.println("Node startup in preactivated state successful");
      System.exit(0);
      return null;
    }).when(startupManager).startPreactivated(cluster, node, licensingService, LICENSE_FILE);

    systemExit.expectSystemExit();
    systemExit.checkAssertionAfterwards(() -> {
      assertThat(systemOutRule.getLog(), containsString("Node startup in preactivated state successful"));
      verify(startupManager).startPreactivated(cluster, node, licensingService, LICENSE_FILE);
      verify(startupManager, times(0)).startUnconfigured(cluster, node);
    });
    nodeProcessor.process();
  }

  @Test
  public void testPreactivatedWithCliParams_absentClusterName() {
    when(options.getLicenseFile()).thenReturn(LICENSE_FILE);
    when(clusterCreator.create(paramValueMap)).thenReturn(cluster);
    when(cluster.getSingleNode()).thenReturn(Optional.of(node));

    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("Cluster name is required with license file");
    systemExit.checkAssertionAfterwards(() -> {
      verify(startupManager, times(0)).startPreactivated(cluster, node, licensingService, LICENSE_FILE);
      verify(startupManager, times(0)).startUnconfigured(cluster, node);
    });
    nodeProcessor.process();
  }

  @Test
  public void testUnconfiguredWithCliParams() {
    when(clusterCreator.create(paramValueMap)).thenReturn(cluster);
    when(cluster.getSingleNode()).thenReturn(Optional.of(node));
    doAnswer(invocation -> {
      System.out.println("Node startup in UNCONFIGURED state successful");
      System.exit(0);
      return null;
    }).when(startupManager).startUnconfigured(cluster, node);

    systemExit.expectSystemExit();
    systemExit.checkAssertionAfterwards(() -> {
      assertThat(systemOutRule.getLog(), containsString("Node startup in UNCONFIGURED state successful"));
      verify(startupManager, times(0)).startPreactivated(cluster, node, licensingService, LICENSE_FILE);
      verify(startupManager).startUnconfigured(cluster, node);
    });
    nodeProcessor.process();
  }

  @Test
  public void testErrorWhenNoStarterIsFound() {
    when(clusterCreator.create(paramValueMap)).thenReturn(cluster);
    when(cluster.getSingleNode()).thenReturn(Optional.of(node));
    doNothing().when(startupManager).startUnconfigured(cluster, node);

    expectedException.expect(AssertionError.class);
    expectedException.expectMessage("Exhausted all methods of starting the node");
    nodeProcessor.process();
  }
}