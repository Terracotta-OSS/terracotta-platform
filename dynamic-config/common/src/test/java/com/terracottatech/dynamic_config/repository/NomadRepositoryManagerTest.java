/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.repository;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static com.terracottatech.dynamic_config.repository.NomadRepositoryManager.RepositoryDepth.FULL;
import static com.terracottatech.dynamic_config.repository.NomadRepositoryManager.RepositoryDepth.NONE;
import static com.terracottatech.dynamic_config.repository.NomadRepositoryManager.RepositoryDepth.ROOT_ONLY;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NomadRepositoryManagerTest {
  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  @Test
  public void testGetConfigurationPath() {
    Path nomadRoot = mock(Path.class);
    Path config = mock(Path.class);
    Path sanskrit = mock(Path.class);
    when(nomadRoot.resolve("config")).thenReturn(config);
    when(nomadRoot.resolve("sanskrit")).thenReturn(sanskrit);
    NomadRepositoryManager repoManager = new NomadRepositoryManager(nomadRoot);
    assertThat(repoManager.getConfigurationPath(), is(config));
  }

  @Test
  public void testGetSanskritPath() {
    Path nomadRoot = mock(Path.class);
    Path config = mock(Path.class);
    Path sanskrit = mock(Path.class);
    when(nomadRoot.resolve("config")).thenReturn(config);
    when(nomadRoot.resolve("sanskrit")).thenReturn(sanskrit);
    NomadRepositoryManager repoManager = new NomadRepositoryManager(nomadRoot);
    assertThat(repoManager.getSanskritPath(), is(sanskrit));
  }

  @Test
  public void testCreateRepositoryIfAbsentWithAllDirectoriesAbsent() {
    Path nomadRoot = mock(Path.class);
    Path config = mock(Path.class);
    Path sanskrit = mock(Path.class);
    when(nomadRoot.resolve("config")).thenReturn(config);
    when(nomadRoot.resolve("sanskrit")).thenReturn(sanskrit);
    NomadRepositoryManager repoManager = new NomadRepositoryManager(nomadRoot);
    NomadRepositoryManager spyRepoManager = spy(repoManager);
    doNothing().when(spyRepoManager).createNomadRoot();
    doNothing().when(spyRepoManager).createNomadSubDirectories();

    doReturn(NONE).when(spyRepoManager).getRepositoryDepth();
    spyRepoManager.createIfAbsent();
    verify(spyRepoManager, times(1)).createNomadRoot();
    verify(spyRepoManager, times(1)).createNomadSubDirectories();
  }

  @Test
  public void testCreateRepositoryIfAbsentWithAllTheDirectoriesPresent() {
    Path nomadRoot = mock(Path.class);
    Path config = mock(Path.class);
    Path sanskrit = mock(Path.class);
    when(nomadRoot.resolve("config")).thenReturn(config);
    when(nomadRoot.resolve("sanskrit")).thenReturn(sanskrit);
    NomadRepositoryManager repoManager = new NomadRepositoryManager(nomadRoot);
    NomadRepositoryManager spyRepoManager = spy(repoManager);
    doNothing().when(spyRepoManager).createNomadRoot();
    doNothing().when(spyRepoManager).createNomadSubDirectories();

    doReturn(FULL).when(spyRepoManager).getRepositoryDepth();
    spyRepoManager.createIfAbsent();
    verify(spyRepoManager, times(0)).createNomadRoot();
    verify(spyRepoManager, times(0)).createNomadSubDirectories();
  }

  @Test
  public void testCreateRepositoryIfAbsentWithOnlyRootPresent() {
    Path nomadRoot = mock(Path.class);
    Path config = mock(Path.class);
    Path sanskrit = mock(Path.class);
    when(nomadRoot.resolve("config")).thenReturn(config);
    when(nomadRoot.resolve("sanskrit")).thenReturn(sanskrit);
    NomadRepositoryManager repoManager = new NomadRepositoryManager(nomadRoot);
    NomadRepositoryManager spyRepoManager = spy(repoManager);
    doNothing().when(spyRepoManager).createNomadRoot();
    doNothing().when(spyRepoManager).createNomadSubDirectories();

    doReturn(ROOT_ONLY).when(spyRepoManager).getRepositoryDepth();
    spyRepoManager.createIfAbsent();
    verify(spyRepoManager, times(0)).createNomadRoot();
    verify(spyRepoManager, times(1)).createNomadSubDirectories();
  }

  @Test
  public void testValidateRepositoryStructure() {
    Path nomadRoot = mock(Path.class);
    Path config = mock(Path.class);
    Path sanskrit = mock(Path.class);
    when(nomadRoot.resolve("config")).thenReturn(config);
    when(nomadRoot.resolve("sanskrit")).thenReturn(sanskrit);
    NomadRepositoryManager repoManager = new NomadRepositoryManager(nomadRoot);
    NomadRepositoryManager spyRepoManager = spy(repoManager);
    doReturn(true).when(spyRepoManager).checkDirectoryExists(nomadRoot);
    doReturn(true).when(spyRepoManager).checkDirectoryExists(sanskrit);
    doReturn(true).when(spyRepoManager).checkDirectoryExists(config);
    NomadRepositoryManager.RepositoryDepth repoDepth = spyRepoManager.getRepositoryDepth();
    assertThat(repoDepth, is(FULL));

    doReturn(false).when(spyRepoManager).checkDirectoryExists(nomadRoot);
    doReturn(false).when(spyRepoManager).checkDirectoryExists(sanskrit);
    doReturn(false).when(spyRepoManager).checkDirectoryExists(config);
    repoDepth = spyRepoManager.getRepositoryDepth();
    assertThat(repoDepth, is(NONE));

    doReturn(true).when(spyRepoManager).checkDirectoryExists(nomadRoot);
    doReturn(false).when(spyRepoManager).checkDirectoryExists(sanskrit);
    doReturn(false).when(spyRepoManager).checkDirectoryExists(config);

    repoDepth = spyRepoManager.getRepositoryDepth();
    assertThat(repoDepth, is(ROOT_ONLY));

    doReturn(false).when(spyRepoManager).checkDirectoryExists(nomadRoot);
    doReturn(true).when(spyRepoManager).checkDirectoryExists(sanskrit);
    doReturn(false).when(spyRepoManager).checkDirectoryExists(config);

    exception.expect(MalformedRepositoryException.class);
    spyRepoManager.getRepositoryDepth();

    doReturn(false).when(spyRepoManager).checkDirectoryExists(nomadRoot);
    doReturn(false).when(spyRepoManager).checkDirectoryExists(sanskrit);
    doReturn(true).when(spyRepoManager).checkDirectoryExists(config);

    exception.expect(MalformedRepositoryException.class);
    spyRepoManager.getRepositoryDepth();
  }

  @Test
  public void testCheckDirectoryExists() throws Exception {
    Path nomadRoot = mock(Path.class);
    Path config = mock(Path.class);
    Path sanskrit = mock(Path.class);
    when(nomadRoot.resolve("config")).thenReturn(config);
    when(nomadRoot.resolve("sanskrit")).thenReturn(sanskrit);
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
    Path nomadRoot = mock(Path.class);
    Path config = mock(Path.class);
    Path sanskrit = mock(Path.class);
    when(nomadRoot.resolve("config")).thenReturn(config);
    when(nomadRoot.resolve("sanskrit")).thenReturn(sanskrit);

    NomadRepositoryManager repoManager = new NomadRepositoryManager(nomadRoot);
    NomadRepositoryManager spyRepoManager = spy(repoManager);
    doReturn(NONE).when(spyRepoManager).getRepositoryDepth();

    Optional<String> nodeNameOpt = spyRepoManager.getNodeName();
    assertThat(nodeNameOpt, is(notNullValue()));
    assertThat(nodeNameOpt.isPresent(), is(false));
  }

  @Test
  public void testGetNodeNameWithOnlyRootPresent() {
    Path nomadRoot = mock(Path.class);
    Path config = mock(Path.class);
    Path sanskrit = mock(Path.class);
    when(nomadRoot.resolve("config")).thenReturn(config);
    when(nomadRoot.resolve("sanskrit")).thenReturn(sanskrit);

    NomadRepositoryManager repoManager = new NomadRepositoryManager(nomadRoot);
    NomadRepositoryManager spyRepoManager = spy(repoManager);

    doReturn(ROOT_ONLY).when(spyRepoManager).getRepositoryDepth();
    Optional<String> nodeNameOpt = spyRepoManager.getNodeName();
    assertThat(nodeNameOpt, is(notNullValue()));
    assertThat(nodeNameOpt.isPresent(), is(false));
  }

  @Test
  public void testGetNodeNameWithAllDirectoriesPresent() {
    Path nomadRoot = mock(Path.class);
    Path config = mock(Path.class);
    Path sanskrit = mock(Path.class);
    when(nomadRoot.resolve("config")).thenReturn(config);
    when(nomadRoot.resolve("sanskrit")).thenReturn(sanskrit);

    NomadRepositoryManager repoManager = new NomadRepositoryManager(nomadRoot);
    NomadRepositoryManager spyRepoManager = spy(repoManager);

    doReturn(FULL).when(spyRepoManager).getRepositoryDepth();
    doReturn("node1").when(spyRepoManager).extractNodeName();

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

    NomadRepositoryManager repoManager = new NomadRepositoryManager(nomadRoot);
    NomadRepositoryManager spyRepoManager = spy(repoManager);

    exception.expect(MalformedRepositoryException.class);
    spyRepoManager.extractNodeName();

    Path configFilePath = config.resolve("cluster-config.3.node1.xml");
    Files.createFile(configFilePath);
    exception.expect(MalformedRepositoryException.class);
    spyRepoManager.extractNodeName();

    Files.delete(configFilePath);
    configFilePath = config.resolve("cluster-config.node1.3.xml");
    Files.createFile(configFilePath);
    String nodeName = spyRepoManager.extractNodeName();
    assertThat(nodeName, is("node1"));

    configFilePath = config.resolve("cluster-config.node1.4.xml");
    Files.createFile(configFilePath);
    spyRepoManager.extractNodeName();
    assertThat(nodeName, is("node1"));

    configFilePath = config.resolve("cluster-config.node2.4.xml");
    Files.createFile(configFilePath);
    exception.expect(MalformedRepositoryException.class);
    spyRepoManager.extractNodeName();

    Files.delete(configFilePath);
    configFilePath = config.resolve("cluster-config.3.node1.xml");
    Files.createFile(configFilePath);
    spyRepoManager.extractNodeName();
    assertThat(nodeName, is("node1"));
  }
}
