/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.repository;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static com.terracottatech.dynamic_config.repository.NomadRepositoryManager.RepositoryDepth;
import static com.terracottatech.dynamic_config.repository.NomadRepositoryManager.RepositoryDepth.FULL;
import static com.terracottatech.dynamic_config.repository.NomadRepositoryManager.RepositoryDepth.NONE;
import static com.terracottatech.dynamic_config.repository.NomadRepositoryManager.RepositoryDepth.ROOT_ONLY;
import static com.terracottatech.dynamic_config.repository.NomadRepositoryManager.findNodeName;
import static com.terracottatech.utilities.hamcrest.ExceptionMatcher.throwing;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NomadRepositoryManagerTest {
  @Rule public ExpectedException exception = ExpectedException.none();
  @Rule public TemporaryFolder folder = new TemporaryFolder();

  @Mock Path nomadRoot;
  @Mock Path configDirPath;
  @Mock Path licenseDirPath;
  @Mock Path sanskritDirPath;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    when(nomadRoot.toAbsolutePath()).thenReturn(nomadRoot);
    when(nomadRoot.normalize()).thenReturn(nomadRoot);
    when(nomadRoot.resolve("config")).thenReturn(configDirPath);
    when(nomadRoot.resolve("license")).thenReturn(licenseDirPath);
    when(nomadRoot.resolve("sanskrit")).thenReturn(sanskritDirPath);
  }

  @Test
  public void testGetConfigurationPath() {
    NomadRepositoryManager repoManager = new NomadRepositoryManager(nomadRoot);
    assertThat(repoManager.getConfigurationPath(), is(configDirPath));
  }

  @Test
  public void testGetSanskritPath() {
    NomadRepositoryManager repoManager = new NomadRepositoryManager(nomadRoot);
    assertThat(repoManager.getSanskritPath(), is(sanskritDirPath));
  }

  @Test
  public void testCreateRepositoryIfAbsentWithAllDirectoriesAbsent() {
    NomadRepositoryManager repoManager = new NomadRepositoryManager(nomadRoot);
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
    NomadRepositoryManager repoManager = new NomadRepositoryManager(nomadRoot);
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
    NomadRepositoryManager repoManager = new NomadRepositoryManager(nomadRoot);
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
    NomadRepositoryManager repoManager = new NomadRepositoryManager(nomadRoot);
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

    exception.expect(MalformedRepositoryException.class);
    spyRepoManager.getRepositoryDepth();

    doReturn(false).when(spyRepoManager).checkDirectoryExists(nomadRoot);
    doReturn(true).when(spyRepoManager).checkDirectoryExists(configDirPath);
    doReturn(false).when(spyRepoManager).checkDirectoryExists(licenseDirPath);
    doReturn(false).when(spyRepoManager).checkDirectoryExists(sanskritDirPath);

    exception.expect(MalformedRepositoryException.class);
    spyRepoManager.getRepositoryDepth();

    doReturn(false).when(spyRepoManager).checkDirectoryExists(nomadRoot);
    doReturn(false).when(spyRepoManager).checkDirectoryExists(configDirPath);
    doReturn(true).when(spyRepoManager).checkDirectoryExists(licenseDirPath);
    doReturn(false).when(spyRepoManager).checkDirectoryExists(sanskritDirPath);

    exception.expect(MalformedRepositoryException.class);
    spyRepoManager.getRepositoryDepth();
  }

  @Test
  public void testCheckDirectoryExists() throws Exception {
    NomadRepositoryManager repoManager = new NomadRepositoryManager(nomadRoot);
    File newFolder = folder.newFolder();
    assertThat(repoManager.checkDirectoryExists(newFolder.toPath()), is(true));
    File newFile = folder.newFile();

    exception.expect(MalformedRepositoryException.class);
    repoManager.checkDirectoryExists(newFile.toPath());
    assertThat(newFile.delete(), is(true));
    assertThat(repoManager.checkDirectoryExists(newFile.toPath()), is(false));
  }

  @Test
  public void testGetNodeNameWithAllDirectoriesAbsent() {
    NomadRepositoryManager repoManager = new NomadRepositoryManager(nomadRoot);
    NomadRepositoryManager spyRepoManager = spy(repoManager);
    doReturn(NONE).when(spyRepoManager).getRepositoryDepth();

    Optional<String> nodeNameOpt = spyRepoManager.getNodeName();
    assertThat(nodeNameOpt, is(notNullValue()));
    assertThat(nodeNameOpt.isPresent(), is(false));
  }

  @Test
  public void testGetNodeNameWithOnlyRootPresent() {
    NomadRepositoryManager repoManager = new NomadRepositoryManager(nomadRoot);
    NomadRepositoryManager spyRepoManager = spy(repoManager);

    doReturn(ROOT_ONLY).when(spyRepoManager).getRepositoryDepth();
    Optional<String> nodeNameOpt = spyRepoManager.getNodeName();
    assertThat(nodeNameOpt, is(notNullValue()));
    assertThat(nodeNameOpt.isPresent(), is(false));
  }

  @Test
  public void testGetNodeNameWithAllDirectoriesPresent() {
    NomadRepositoryManager repoManager = new NomadRepositoryManager(nomadRoot);
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
    Files.createDirectory(config);

    assertThat(findNodeName(nomadRoot), is(Optional.empty()));

    Path configFilePath = config.resolve("cluster-config.3.node1.xml");
    Files.createFile(configFilePath);
    assertThat(findNodeName(nomadRoot), is(Optional.empty()));

    Files.delete(configFilePath);
    configFilePath = config.resolve("cluster-config.node1.3.xml");
    Files.createFile(configFilePath);
    String nodeName = findNodeName(nomadRoot).get();
    assertThat(nodeName, is("node1"));

    configFilePath = config.resolve("cluster-config.node1.4.xml");
    Files.createFile(configFilePath);
    findNodeName(nomadRoot);
    assertThat(nodeName, is("node1"));

    configFilePath = config.resolve("cluster-config.node2.4.xml");
    Files.createFile(configFilePath);
    assertThat(
        () -> findNodeName(nomadRoot),
        is(throwing(instanceOf(MalformedRepositoryException.class)).andMessage(is(equalTo("Found configuration files for different nodes (node2, node1) in " + config)))));

    Files.delete(configFilePath);
    configFilePath = config.resolve("cluster-config.3.node1.xml");
    Files.createFile(configFilePath);
    findNodeName(nomadRoot);
    assertThat(nodeName, is("node1"));
  }
}
