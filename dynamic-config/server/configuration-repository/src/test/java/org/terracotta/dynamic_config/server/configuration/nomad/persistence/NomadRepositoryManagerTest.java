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
package org.terracotta.dynamic_config.server.configuration.nomad.persistence;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.terracotta.dynamic_config.api.service.IParameterSubstitutor;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.terracotta.dynamic_config.api.service.IParameterSubstitutor.identity;
import static org.terracotta.dynamic_config.server.configuration.nomad.persistence.NomadRepositoryManager.RepositoryDepth;
import static org.terracotta.dynamic_config.server.configuration.nomad.persistence.NomadRepositoryManager.RepositoryDepth.FULL;
import static org.terracotta.dynamic_config.server.configuration.nomad.persistence.NomadRepositoryManager.RepositoryDepth.NONE;
import static org.terracotta.dynamic_config.server.configuration.nomad.persistence.NomadRepositoryManager.RepositoryDepth.ROOT_ONLY;
import static org.terracotta.dynamic_config.server.configuration.nomad.persistence.NomadRepositoryManager.findNodeName;
import static org.terracotta.testing.ExceptionMatcher.throwing;

public class NomadRepositoryManagerTest {
  @Rule
  public ExpectedException exception = ExpectedException.none();
  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  private final Path nomadRoot = Paths.get("nomadRoot").toAbsolutePath();
  private final Path configDirPath = nomadRoot.resolve("config");
  private final Path licenseDirPath = nomadRoot.resolve("license");
  private final Path sanskritDirPath = nomadRoot.resolve("sanskrit");
  private final IParameterSubstitutor parameterSubstitutor = identity();

  @Test
  public void testGetConfigurationPath() {
    NomadRepositoryManager repoManager = new NomadRepositoryManager(nomadRoot, parameterSubstitutor);
    assertThat(repoManager.getConfigPath(), is(configDirPath));
  }

  @Test
  public void testGetSanskritPath() {
    NomadRepositoryManager repoManager = new NomadRepositoryManager(nomadRoot, parameterSubstitutor);
    assertThat(repoManager.getSanskritPath(), is(sanskritDirPath));
  }

  @Test
  public void testCreateRepositoryIfAbsentWithAllDirectoriesAbsent() {
    NomadRepositoryManager repoManager = new NomadRepositoryManager(nomadRoot, parameterSubstitutor);
    NomadRepositoryManager spyRepoManager = spy(repoManager);
    doNothing().when(spyRepoManager).createNomadRoot();
    doNothing().when(spyRepoManager).createNomadSubDirectories();

    doReturn(NONE).when(spyRepoManager).getRepositoryDepth();
    spyRepoManager.createDirectories();
    verify(spyRepoManager, times(1)).createNomadRoot();
    verify(spyRepoManager, times(1)).createNomadSubDirectories();
  }

  @Test
  public void testCreateRepositoryIfAbsentWithAllTheDirectoriesPresent() {
    NomadRepositoryManager repoManager = new NomadRepositoryManager(nomadRoot, parameterSubstitutor);
    NomadRepositoryManager spyRepoManager = spy(repoManager);
    doNothing().when(spyRepoManager).createNomadRoot();
    doNothing().when(spyRepoManager).createNomadSubDirectories();

    doReturn(FULL).when(spyRepoManager).getRepositoryDepth();
    spyRepoManager.createDirectories();
    verify(spyRepoManager, times(0)).createNomadRoot();
    verify(spyRepoManager, times(0)).createNomadSubDirectories();
  }

  @Test
  public void testCreateRepositoryIfAbsentWithOnlyRootPresent() {
    NomadRepositoryManager repoManager = new NomadRepositoryManager(nomadRoot, parameterSubstitutor);
    NomadRepositoryManager spyRepoManager = spy(repoManager);
    doNothing().when(spyRepoManager).createNomadRoot();
    doNothing().when(spyRepoManager).createNomadSubDirectories();

    doReturn(ROOT_ONLY).when(spyRepoManager).getRepositoryDepth();
    spyRepoManager.createDirectories();
    verify(spyRepoManager, times(0)).createNomadRoot();
    verify(spyRepoManager, times(1)).createNomadSubDirectories();
  }

  @Test
  public void testValidateRepositoryStructure() {
    NomadRepositoryManager repoManager = new NomadRepositoryManager(nomadRoot, parameterSubstitutor);
    NomadRepositoryManager spyRepoManager = spy(repoManager);
    doReturn(true).when(spyRepoManager).checkDirectoryExists(nomadRoot);
    doReturn(true).when(spyRepoManager).checkDirectoryExists(configDirPath);
    doReturn(true).when(spyRepoManager).checkDirectoryExists(licenseDirPath);
    doReturn(true).when(spyRepoManager).checkDirectoryExists(sanskritDirPath);

    RepositoryDepth repoDepth = spyRepoManager.getRepositoryDepth();
    assertThat(repoDepth, is(FULL));

    doReturn(false).when(spyRepoManager).checkDirectoryExists(nomadRoot);
    doReturn(false).when(spyRepoManager).checkDirectoryExists(configDirPath);
    doReturn(false).when(spyRepoManager).checkDirectoryExists(licenseDirPath);
    doReturn(false).when(spyRepoManager).checkDirectoryExists(sanskritDirPath);

    repoDepth = spyRepoManager.getRepositoryDepth();
    assertThat(repoDepth, is(NONE));

    doReturn(true).when(spyRepoManager).checkDirectoryExists(nomadRoot);
    doReturn(false).when(spyRepoManager).checkDirectoryExists(configDirPath);
    doReturn(false).when(spyRepoManager).checkDirectoryExists(licenseDirPath);
    doReturn(false).when(spyRepoManager).checkDirectoryExists(sanskritDirPath);

    repoDepth = spyRepoManager.getRepositoryDepth();
    assertThat(repoDepth, is(ROOT_ONLY));

    doReturn(false).when(spyRepoManager).checkDirectoryExists(nomadRoot);
    doReturn(false).when(spyRepoManager).checkDirectoryExists(configDirPath);
    doReturn(false).when(spyRepoManager).checkDirectoryExists(licenseDirPath);
    doReturn(true).when(spyRepoManager).checkDirectoryExists(sanskritDirPath);

    exception.expect(IllegalStateException.class);
    spyRepoManager.getRepositoryDepth();

    doReturn(false).when(spyRepoManager).checkDirectoryExists(nomadRoot);
    doReturn(true).when(spyRepoManager).checkDirectoryExists(configDirPath);
    doReturn(false).when(spyRepoManager).checkDirectoryExists(licenseDirPath);
    doReturn(false).when(spyRepoManager).checkDirectoryExists(sanskritDirPath);

    exception.expect(IllegalStateException.class);
    spyRepoManager.getRepositoryDepth();

    doReturn(false).when(spyRepoManager).checkDirectoryExists(nomadRoot);
    doReturn(false).when(spyRepoManager).checkDirectoryExists(configDirPath);
    doReturn(true).when(spyRepoManager).checkDirectoryExists(licenseDirPath);
    doReturn(false).when(spyRepoManager).checkDirectoryExists(sanskritDirPath);

    exception.expect(IllegalStateException.class);
    spyRepoManager.getRepositoryDepth();
  }

  @Test
  public void testCheckDirectoryExists() throws Exception {
    NomadRepositoryManager repoManager = new NomadRepositoryManager(nomadRoot, parameterSubstitutor);
    File newFolder = folder.newFolder();
    assertThat(repoManager.checkDirectoryExists(newFolder.toPath()), is(true));
    File newFile = folder.newFile();

    exception.expect(IllegalArgumentException.class);
    repoManager.checkDirectoryExists(newFile.toPath());
    assertThat(newFile.delete(), is(true));
    assertThat(repoManager.checkDirectoryExists(newFile.toPath()), is(false));
  }

  @Test
  public void testGetNodeNameWithAllDirectoriesAbsent() {
    NomadRepositoryManager repoManager = new NomadRepositoryManager(nomadRoot, parameterSubstitutor);
    NomadRepositoryManager spyRepoManager = spy(repoManager);
    doReturn(NONE).when(spyRepoManager).getRepositoryDepth();

    Optional<String> nodeNameOpt = spyRepoManager.getNodeName();
    assertThat(nodeNameOpt, is(notNullValue()));
    assertThat(nodeNameOpt.isPresent(), is(false));
  }

  @Test
  public void testGetNodeNameWithOnlyRootPresent() {
    NomadRepositoryManager repoManager = new NomadRepositoryManager(nomadRoot, parameterSubstitutor);
    NomadRepositoryManager spyRepoManager = spy(repoManager);

    doReturn(ROOT_ONLY).when(spyRepoManager).getRepositoryDepth();
    Optional<String> nodeNameOpt = spyRepoManager.getNodeName();
    assertThat(nodeNameOpt, is(notNullValue()));
    assertThat(nodeNameOpt.isPresent(), is(false));
  }

  @Test
  public void testGetNodeNameWithAllDirectoriesPresent() {
    NomadRepositoryManager repoManager = new NomadRepositoryManager(nomadRoot, parameterSubstitutor);
    NomadRepositoryManager spyRepoManager = spy(repoManager);

    doReturn(FULL).when(spyRepoManager).getRepositoryDepth();
    doReturn(Optional.of("node1")).when(spyRepoManager).getNodeName();

    Optional<String> nodeNameOpt = spyRepoManager.getNodeName();
    assertThat(nodeNameOpt, is(notNullValue()));
    assertThat(nodeNameOpt.isPresent(), is(true));
    assertThat(nodeNameOpt.get(), is("node1"));
  }

  @Test
  public void testExtractNodeName() throws Exception {
    File repository = folder.newFolder();
    Path nomadRoot = repository.toPath();
    Path config = nomadRoot.resolve("config");

    // Repository is partially formed at this point
    Files.createDirectory(config);
    assertThat(
        () -> findNodeName(nomadRoot, identity()),
        is(throwing(instanceOf(IllegalStateException.class)).andMessage(is(containsString("Repository is partially formed"))))
    );

    // Create the full repository now
    Files.createDirectory(nomadRoot.resolve("license"));
    Files.createDirectory(nomadRoot.resolve("sanskrit"));
    assertThat(findNodeName(nomadRoot, identity()), is(Optional.empty()));

    Path configFilePath = config.resolve("3.node1.properties");
    Files.createFile(configFilePath);
    assertThat(findNodeName(nomadRoot, identity()), is(Optional.empty()));

    org.terracotta.utilities.io.Files.delete(configFilePath);
    configFilePath = config.resolve("node1.3.properties");
    Files.createFile(configFilePath);
    String nodeName = findNodeName(nomadRoot, identity()).get();
    assertThat(nodeName, is("node1"));

    configFilePath = config.resolve("node1.4.properties");
    Files.createFile(configFilePath);
    findNodeName(nomadRoot, identity());
    assertThat(nodeName, is("node1"));

    configFilePath = config.resolve("node2.4.properties");
    Files.createFile(configFilePath);
    assertThat(
        () -> findNodeName(nomadRoot, identity()),
        is(throwing(instanceOf(IllegalStateException.class))
            .andMessage(is("Found versioned cluster config files for the following different nodes: node2, node1 in: " + config)))
    );

    org.terracotta.utilities.io.Files.delete(configFilePath);
    configFilePath = config.resolve("3.node1.properties");
    Files.createFile(configFilePath);
    findNodeName(nomadRoot, identity());
    assertThat(nodeName, is("node1"));
  }
}
