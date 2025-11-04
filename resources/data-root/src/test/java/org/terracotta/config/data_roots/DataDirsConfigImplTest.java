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
package org.terracotta.config.data_roots;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import static org.junit.internal.matchers.ThrowableMessageMatcher.hasMessage;
import org.junit.rules.TemporaryFolder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.terracotta.config.data_roots.management.DataRootBinding;
import org.terracotta.config.util.ParameterSubstitutor;
import org.terracotta.data.config.DataDirectories;
import org.terracotta.data.config.DataRootMapping;
import org.terracotta.management.service.monitoring.EntityManagementRegistry;
import org.terracotta.management.service.monitoring.EntityMonitoringService;

public class DataDirsConfigImplTest {

  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  @Test
  public void getRoot() throws Exception {
    EntityManagementRegistry registry = mock(EntityManagementRegistry.class);
    EntityMonitoringService entityMonitoringService = mock(EntityMonitoringService.class);
    when(registry.getMonitoringService()).thenReturn(entityMonitoringService);
    when(entityMonitoringService.getConsumerId()).thenReturn(1L);
    when(entityMonitoringService.getServerName()).thenReturn("myServer");

    String[] ids = {"a", "b"};
    String[] dataRootPaths = new String[ids.length];
    DataDirsConfigImpl dataRootConfig = configureDataRoot(ids, dataRootPaths);
    DataRootBinding[] bindings = new DataRootBinding[ids.length];

    for (int i = 0; i < ids.length; i++) {
      bindings[i] = new DataRootBinding(ids[i], dataRootConfig.getRoot(ids[i]));
    }
    dataRootConfig.onManagementRegistryCreated(registry);
    for (int i = 0; i < ids.length; i++) {
      assertEquals(Paths.get(dataRootPaths[i]), dataRootConfig.getRoot(ids[i]));
      assertTrue(Files.exists(dataRootConfig.getRoot(ids[i])));
      verify(registry).register(bindings[i]);
    }

    String postRegistry_Id = "postRegistry";
    dataRootConfig.addDataDirectory(postRegistry_Id, folder.newFolder().getAbsolutePath());
    DataRootBinding newBinding = new DataRootBinding(postRegistry_Id, dataRootConfig.getRoot(postRegistry_Id));
    verify(registry).registerAndRefresh(newBinding);
  }

  @Test
  public void getRootIdentifiers() throws Exception {
    String[] ids = {"a", "b"};
    String[] dataRootPaths = new String[ids.length];
    DataDirsConfigImpl dataRootConfig = configureDataRoot(ids, dataRootPaths);

    Set<String> expectedIds = new HashSet<>();
    Collections.addAll(expectedIds, ids);
    assertEquals(expectedIds, dataRootConfig.getRootIdentifiers());
  }

  @Test
  public void testGetRootWitNull() throws Exception {
    String[] ids = {"a", "b"};
    String[] dataRootPaths = new String[ids.length];
    DataDirsConfigImpl dataRootConfig = configureDataRoot(ids, dataRootPaths);
    assertThrows(NullPointerException.class, () -> dataRootConfig.getRoot(null));
  }

  @Test
  public void testGetRootInvalidId() throws Exception {
    String[] ids = {"a", "b"};
    String[] dataRootPaths = new String[ids.length];
    DataDirsConfigImpl dataRootConfig = configureDataRoot(ids, dataRootPaths);
    assertThrows(IllegalArgumentException.class, () -> dataRootConfig.getRoot("this_id_does_not_exists"));
  }

  @Test
  public void testDuplicateRootIdentifiers() throws Exception {
    String[] ids = {"a", "a"};
    String[] dataRootPaths = new String[ids.length];
    DataDirsConfigurationException e = assertThrows(DataDirsConfigurationException.class, () -> configureDataRoot(ids, dataRootPaths));
    assertThat(e, hasMessage(equalTo("A data directory with name: a already exists")));
  }

  @Test
  public void testOverlappingPaths_pathNormalization() throws Exception {
    String[] ids = {"a", "b"};
    String[] dataRootPaths = new String[ids.length];
    dataRootPaths[0] = "./dataroot/../dataroot";
    dataRootPaths[1] = "dataroot";
    DataDirsConfigurationException e = assertThrows(DataDirsConfigurationException.class, () -> configureDataRoot(ids, dataRootPaths));
    assertThat(e, hasMessage(containsString("overlaps with the existing data directory path")));
  }

  @Test
  public void testOverlappingPaths_secondSubDirOfFirst() throws Exception {
    String[] ids = {"a", "b"};
    String[] dataRootPaths = new String[ids.length];
    dataRootPaths[0] = "/tmp/dataroot/dir/dir";
    dataRootPaths[1] = "/tmp/dataroot/dir";
    DataDirsConfigurationException e = assertThrows(DataDirsConfigurationException.class, () -> configureDataRoot(ids, dataRootPaths));
    assertThat(e, hasMessage(containsString("overlaps with the existing data directory path")));
  }

  @Test
  public void testOverlappingPaths_firstSubDirOfSecond() throws Exception {
    String[] ids = {"a", "b"};
    String[] dataRootPaths = new String[ids.length];
    dataRootPaths[0] = "/tmp/dataroot/dir/dir";
    dataRootPaths[1] = "/tmp/dataroot/dir";
    DataDirsConfigurationException e = assertThrows(DataDirsConfigurationException.class, () -> configureDataRoot(ids, dataRootPaths));
    assertThat(e, hasMessage(containsString("overlaps with the existing data directory path")));
  }

  @Test
  public void testOverlappingPaths_samePaths() throws Exception {
    String[] ids = {"a", "b"};
    String[] dataRootPaths = new String[ids.length];
    String sameDataPath = folder.newFolder().getAbsolutePath();
    dataRootPaths[0] = sameDataPath;
    dataRootPaths[1] = sameDataPath;
    DataDirsConfigurationException e = assertThrows(DataDirsConfigurationException.class, () -> configureDataRoot(ids, dataRootPaths));
    assertThat(e, hasMessage(containsString("overlaps with the existing data directory path")));
  }

  @Test
  public void testOverlappingPaths_relativeAndAbsolute() throws Exception {
    String[] ids = {"a", "b"};
    String[] dataRootPaths = new String[ids.length];
    dataRootPaths[0] = "dir";
    dataRootPaths[1] = Paths.get("dir").toAbsolutePath().toString();
    DataDirsConfigurationException e = assertThrows(DataDirsConfigurationException.class, () -> configureDataRoot(ids, dataRootPaths));
    assertThat(e, hasMessage(containsString("overlaps with the existing data directory path")));
  }

  @Test
  public void testSubstitutablePaths() throws Exception {
    String[] ids = {"id"};
    String configuredPath = folder.newFolder().getAbsolutePath();

    DataRootMapping[] dataRootMappings = new DataRootMapping[ids.length];
    dataRootMappings[0] = new DataRootMapping();
    dataRootMappings[0].setName(ids[0]);
    dataRootMappings[0].setValue(configuredPath + "-%h");

    DataDirsConfigImpl dataRootConfig = configureDataRoot(dataRootMappings);
    String hostName = ParameterSubstitutor.getHostName();
    assertThat(dataRootConfig.getRoot(ids[0]), is(Paths.get(configuredPath + "-" + hostName)));
  }

  @Test
  public void testRelativePathWithSourceSpecified() throws Exception {
    String[] ids = {"id"};
    String[] dataRootPaths = new String[ids.length];
    String source = folder.newFile("config.xml").getParent();

    DataDirsConfigImpl dataRootConfig = configureDataRoot(ids, dataRootPaths, source);
    assertThat(dataRootConfig.getRoot(ids[0]).getParent(), is(Paths.get(source)));
  }

  @Test
  public void testPlatformRootNotSpecified() throws Exception {
    DataDirsConfigImpl dataRootConfig = configureDataRoot(new String[]{"data"}, new String[1]);
    assertThat(dataRootConfig.getPlatformRootIdentifier(), is(Optional.empty()));
  }

  @Test
  public void testPlatformRootSpecified() throws Exception {
    DataDirsConfigImpl dataRootConfig = configureDataRoot(new String[]{"data"}, new String[1], null, 0);
    assertThat(dataRootConfig.getPlatformRootIdentifier().orElse("busted"), is("data"));
  }

  @Test
  public void testPlatformRootOverSpecified() throws Exception {
    DataRootMapping[] dataRootMappings = new DataRootMapping[2];
    DataRootMapping dataRootMapping = new DataRootMapping();
    dataRootMapping.setName("data");
    dataRootMapping.setValue(folder.newFolder().getAbsolutePath());
    dataRootMapping.setUseForPlatform(true);
    dataRootMappings[0] = dataRootMapping;

    dataRootMapping = new DataRootMapping();
    dataRootMapping.setName("other");
    dataRootMapping.setValue(folder.newFolder().getAbsolutePath());
    dataRootMapping.setUseForPlatform(true);
    dataRootMappings[1] = dataRootMapping;

    DataDirsConfigurationException e = assertThrows(DataDirsConfigurationException.class, () -> configureDataRoot(dataRootMappings));
    assertThat(e, hasMessage(equalTo("More than one data directory is configured to be used by platform")));
  }

  private DataDirsConfigImpl configureDataRoot(String[] ids, String[] dataRootPaths) throws IOException {
    return configureDataRoot(ids, dataRootPaths, (String) null);
  }

  private DataDirsConfigImpl configureDataRoot(String[] ids, String[] dataRootPaths, String source) throws IOException {
    return configureDataRoot(ids, dataRootPaths, source, -1);
  }

  private DataDirsConfigImpl configureDataRoot(String[] ids, String[] dataRootPaths, String source, int platformRootIndex) throws IOException {
    DataDirectories dataDirectories = new DataDirectories();
    for (int i = 0; i < ids.length; i++) {
      DataRootMapping dataRootMapping = new DataRootMapping();
      if (dataRootPaths[i] == null) {
        dataRootPaths[i] = folder.newFolder().getAbsolutePath();
      }
      dataRootMapping.setName(ids[i]);
      dataRootMapping.setValue(dataRootPaths[i]);
      if (i == platformRootIndex) {
        dataRootMapping.setUseForPlatform(true);
      }
      dataDirectories.getDirectory().add(dataRootMapping);
    }

    return DataRootConfigParser.toDataDirsConfig(dataDirectories, DataRootConfigParser.getPathResolver(source));
  }

  private DataDirsConfigImpl configureDataRoot(DataRootMapping[] dataRootMappings) throws IOException {
    DataDirectories dataDirectories = new DataDirectories();
    for (int i = 0; i < dataRootMappings.length; i++) {
      if (dataRootMappings[i].getValue() == null) {
        dataRootMappings[i].setValue(folder.newFolder().getAbsolutePath());
      }
      dataDirectories.getDirectory().add(dataRootMappings[i]);
    }

    return DataRootConfigParser.toDataDirsConfig(dataDirectories, DataRootConfigParser.getPathResolver(null));
  }
}