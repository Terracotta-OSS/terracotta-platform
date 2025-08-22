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
package org.terracotta.dynamic_config.server.configuration.nomad.persistence;

import org.junit.Rule;
import org.junit.Test;
import org.terracotta.dynamic_config.api.service.IParameterSubstitutor;
import org.terracotta.org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.terracotta.dynamic_config.api.service.IParameterSubstitutor.identity;
import static org.terracotta.dynamic_config.server.configuration.nomad.persistence.NomadConfigurationManager.ConfigDirDepth;
import static org.terracotta.dynamic_config.server.configuration.nomad.persistence.NomadConfigurationManager.ConfigDirDepth.FULL;
import static org.terracotta.dynamic_config.server.configuration.nomad.persistence.NomadConfigurationManager.ConfigDirDepth.NONE;
import static org.terracotta.dynamic_config.server.configuration.nomad.persistence.NomadConfigurationManager.ConfigDirDepth.ROOT_ONLY;
import static org.terracotta.dynamic_config.server.configuration.nomad.persistence.NomadConfigurationManager.DIR_CHANGES;
import static org.terracotta.dynamic_config.server.configuration.nomad.persistence.NomadConfigurationManager.DIR_CLUSTER;
import static org.terracotta.dynamic_config.server.configuration.nomad.persistence.NomadConfigurationManager.DIR_LICENSE;
import static org.terracotta.dynamic_config.server.configuration.nomad.persistence.NomadConfigurationManager.findNodeName;
import static org.terracotta.testing.ExceptionMatcher.throwing;

public class NomadConfigurationManagerTest {
  @Rule
  public TemporaryFolder folder = new TemporaryFolder(new File("target"));

  private final Path configPath = Paths.get("config").toAbsolutePath();
  private final Path clusterPath = configPath.resolve(DIR_CLUSTER);
  private final Path licensePath = configPath.resolve(DIR_LICENSE);
  private final Path changesPath = configPath.resolve(DIR_CHANGES);
  private final IParameterSubstitutor parameterSubstitutor = identity();

  @Test
  public void testGetConfigurationPath() {
    NomadConfigurationManager configManager = new NomadConfigurationManager(configPath, parameterSubstitutor);
    assertThat(configManager.getClusterPath(), is(clusterPath));
  }

  @Test
  public void testGetSanskritPath() {
    NomadConfigurationManager configManager = new NomadConfigurationManager(configPath, parameterSubstitutor);
    assertThat(configManager.getChangesPath(), is(changesPath));
  }

  @Test
  public void testCreateConfigDirIfAbsentWithAllDirectoriesAbsent() {
    NomadConfigurationManager configManager = new NomadConfigurationManager(configPath, parameterSubstitutor);
    NomadConfigurationManager spyRepoManager = spy(configManager);
    doNothing().when(spyRepoManager).createNomadRoot();
    doNothing().when(spyRepoManager).createNomadSubDirectories();

    doReturn(NONE).when(spyRepoManager).getConfigurationDirectoryDepth();
    spyRepoManager.createDirectories();
    verify(spyRepoManager, times(1)).createNomadRoot();
    verify(spyRepoManager, times(1)).createNomadSubDirectories();
  }

  @Test
  public void testCreateConfigDirIfAbsentWithAllTheDirectoriesPresent() {
    NomadConfigurationManager configManager = new NomadConfigurationManager(configPath, parameterSubstitutor);
    NomadConfigurationManager spyRepoManager = spy(configManager);
    doNothing().when(spyRepoManager).createNomadRoot();
    doNothing().when(spyRepoManager).createNomadSubDirectories();

    doReturn(FULL).when(spyRepoManager).getConfigurationDirectoryDepth();
    spyRepoManager.createDirectories();
    verify(spyRepoManager, times(0)).createNomadRoot();
    verify(spyRepoManager, times(0)).createNomadSubDirectories();
  }

  @Test
  public void testCreateConfigDirIfAbsentWithOnlyRootPresent() {
    NomadConfigurationManager configManager = new NomadConfigurationManager(configPath, parameterSubstitutor);
    NomadConfigurationManager spyRepoManager = spy(configManager);
    doNothing().when(spyRepoManager).createNomadRoot();
    doNothing().when(spyRepoManager).createNomadSubDirectories();

    doReturn(ROOT_ONLY).when(spyRepoManager).getConfigurationDirectoryDepth();
    spyRepoManager.createDirectories();
    verify(spyRepoManager, times(0)).createNomadRoot();
    verify(spyRepoManager, times(1)).createNomadSubDirectories();
  }

  @Test
  public void testValidateConfigDirStructure() {
    NomadConfigurationManager configManager = new NomadConfigurationManager(configPath, parameterSubstitutor);
    NomadConfigurationManager spyRepoManager = spy(configManager);
    doReturn(true).when(spyRepoManager).checkDirectoryExists(configPath);
    doReturn(true).when(spyRepoManager).checkDirectoryExists(clusterPath);
    doReturn(true).when(spyRepoManager).checkDirectoryExists(licensePath);
    doReturn(true).when(spyRepoManager).checkDirectoryExists(changesPath);

    ConfigDirDepth repoDepth = spyRepoManager.getConfigurationDirectoryDepth();
    assertThat(repoDepth, is(FULL));

    doReturn(false).when(spyRepoManager).checkDirectoryExists(configPath);
    doReturn(false).when(spyRepoManager).checkDirectoryExists(clusterPath);
    doReturn(false).when(spyRepoManager).checkDirectoryExists(licensePath);
    doReturn(false).when(spyRepoManager).checkDirectoryExists(changesPath);

    repoDepth = spyRepoManager.getConfigurationDirectoryDepth();
    assertThat(repoDepth, is(NONE));

    doReturn(true).when(spyRepoManager).checkDirectoryExists(configPath);
    doReturn(false).when(spyRepoManager).checkDirectoryExists(clusterPath);
    doReturn(false).when(spyRepoManager).checkDirectoryExists(licensePath);
    doReturn(false).when(spyRepoManager).checkDirectoryExists(changesPath);

    repoDepth = spyRepoManager.getConfigurationDirectoryDepth();
    assertThat(repoDepth, is(ROOT_ONLY));

    doReturn(false).when(spyRepoManager).checkDirectoryExists(configPath);
    doReturn(false).when(spyRepoManager).checkDirectoryExists(clusterPath);
    doReturn(false).when(spyRepoManager).checkDirectoryExists(licensePath);
    doReturn(true).when(spyRepoManager).checkDirectoryExists(changesPath);

    assertThrows(IllegalStateException.class, spyRepoManager::getConfigurationDirectoryDepth);

    doReturn(false).when(spyRepoManager).checkDirectoryExists(configPath);
    doReturn(true).when(spyRepoManager).checkDirectoryExists(clusterPath);
    doReturn(false).when(spyRepoManager).checkDirectoryExists(licensePath);
    doReturn(false).when(spyRepoManager).checkDirectoryExists(changesPath);

    assertThrows(IllegalStateException.class, spyRepoManager::getConfigurationDirectoryDepth);

    doReturn(false).when(spyRepoManager).checkDirectoryExists(configPath);
    doReturn(false).when(spyRepoManager).checkDirectoryExists(clusterPath);
    doReturn(true).when(spyRepoManager).checkDirectoryExists(licensePath);
    doReturn(false).when(spyRepoManager).checkDirectoryExists(changesPath);

    assertThrows(IllegalStateException.class, spyRepoManager::getConfigurationDirectoryDepth);
  }

  @Test
  public void testCheckDirectoryExists() throws Exception {
    NomadConfigurationManager configManager = new NomadConfigurationManager(configPath, parameterSubstitutor);
    File newFolder = folder.newFolder();
    assertThat(configManager.checkDirectoryExists(newFolder.toPath()), is(true));
    File newFile = folder.newFile();

    assertThrows(IllegalArgumentException.class, () -> configManager.checkDirectoryExists(newFile.toPath()));

    assertThat(newFile.delete(), is(true));
    assertThat(configManager.checkDirectoryExists(newFile.toPath()), is(false));
  }

  @Test
  public void testGetNodeNameWithAllDirectoriesAbsent() {
    NomadConfigurationManager configManager = new NomadConfigurationManager(configPath, parameterSubstitutor);
    NomadConfigurationManager spyRepoManager = spy(configManager);
    doReturn(NONE).when(spyRepoManager).getConfigurationDirectoryDepth();

    Optional<String> nodeNameOpt = spyRepoManager.getNodeName();
    assertThat(nodeNameOpt, is(notNullValue()));
    assertThat(nodeNameOpt.isPresent(), is(false));
  }

  @Test
  public void testGetNodeNameWithOnlyRootPresent() {
    NomadConfigurationManager configManager = new NomadConfigurationManager(configPath, parameterSubstitutor);
    NomadConfigurationManager spyRepoManager = spy(configManager);

    doReturn(ROOT_ONLY).when(spyRepoManager).getConfigurationDirectoryDepth();
    Optional<String> nodeNameOpt = spyRepoManager.getNodeName();
    assertThat(nodeNameOpt, is(notNullValue()));
    assertThat(nodeNameOpt.isPresent(), is(false));
  }

  @Test
  public void testGetNodeNameWithAllDirectoriesPresent() {
    NomadConfigurationManager configManager = new NomadConfigurationManager(configPath, parameterSubstitutor);
    NomadConfigurationManager spyRepoManager = spy(configManager);

    doReturn(FULL).when(spyRepoManager).getConfigurationDirectoryDepth();
    doReturn(Optional.of("node1")).when(spyRepoManager).getNodeName();

    Optional<String> nodeNameOpt = spyRepoManager.getNodeName();
    assertThat(nodeNameOpt, is(notNullValue()));
    assertThat(nodeNameOpt.isPresent(), is(true));
    assertThat(nodeNameOpt.get(), is("node1"));
  }

  @Test
  public void testExtractNodeName() throws Exception {
    File tmp = folder.newFolder();
    Path configDir = tmp.toPath();
    Path cluster = configDir.resolve("cluster");

    // Repository is partially formed at this point
    Files.createDirectory(cluster);
    assertThat(
        () -> findNodeName(configDir, identity()),
        is(throwing(instanceOf(IllegalStateException.class)).andMessage(is(containsString("Configuration directory is partially formed"))))
    );

    // Create the full repository now
    Files.createDirectory(configDir.resolve("license"));
    Files.createDirectory(configDir.resolve("changes"));
    assertThat(findNodeName(configDir, identity()), is(Optional.empty()));

    Path configFilePath = cluster.resolve("3.node1.properties");
    Files.createFile(configFilePath);
    assertThat(findNodeName(configDir, identity()), is(Optional.empty()));

    org.terracotta.utilities.io.Files.delete(configFilePath);
    configFilePath = cluster.resolve("node1.3.properties");
    Files.createFile(configFilePath);
    String nodeName = findNodeName(configDir, identity()).get();
    assertThat(nodeName, is("node1"));

    configFilePath = cluster.resolve("node1.4.properties");
    Files.createFile(configFilePath);
    findNodeName(configDir, identity());
    assertThat(nodeName, is("node1"));

    configFilePath = cluster.resolve("node2.4.properties");
    Files.createFile(configFilePath);
    assertThat(
        () -> findNodeName(configDir, identity()),
        is(throwing(instanceOf(IllegalStateException.class))
            .andMessage(is("Found versioned cluster config files for the following different nodes: node2, node1 in: " + cluster.toAbsolutePath())))
    );

    org.terracotta.utilities.io.Files.delete(configFilePath);
    configFilePath = cluster.resolve("3.node1.properties");
    Files.createFile(configFilePath);
    findNodeName(configDir, identity());
    assertThat(nodeName, is("node1"));
  }
}
