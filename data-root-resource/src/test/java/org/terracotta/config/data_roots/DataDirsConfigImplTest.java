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
package org.terracotta.config.data_roots;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.terracotta.config.data_roots.management.DataRootBinding;
import org.terracotta.config.util.ParameterSubstitutor;
import org.terracotta.data.config.DataDirectories;
import org.terracotta.data.config.DataRootMapping;
import org.terracotta.management.service.monitoring.EntityManagementRegistry;
import org.terracotta.management.service.monitoring.EntityMonitoringService;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DataDirsConfigImplTest {

  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

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

    expectedException.expect(NullPointerException.class);
    dataRootConfig.getRoot(null);
  }

  @Test
  public void testGetRootInvalidId() throws Exception {
    String[] ids = {"a", "b"};
    String[] dataRootPaths = new String[ids.length];
    DataDirsConfigImpl dataRootConfig = configureDataRoot(ids, dataRootPaths);

    expectedException.expect(IllegalArgumentException.class);
    dataRootConfig.getRoot("this_id_does_not_exists");
  }

  @Test
  public void testDuplicateRootIdentifiers() throws Exception {
    String[] ids = {"a", "a"};
    String[] dataRootPaths = new String[ids.length];

    expectedException.expect(DataDirsConfigurationException.class);
    expectedException.expectMessage("already exists");
    configureDataRoot(ids, dataRootPaths);
  }

  @Test
  public void testOverlappingPaths_pathNormalization() throws Exception {
    String[] ids = {"a", "b"};
    String[] dataRootPaths = new String[ids.length];
    dataRootPaths[0] = "./dataroot/../dataroot";
    dataRootPaths[1] = "dataroot";

    expectedException.expect(DataDirsConfigurationException.class);
    expectedException.expectMessage("overlap");

    configureDataRoot(ids, dataRootPaths);
  }

  @Test
  public void testOverlappingPaths_secondSubDirOfFirst() throws Exception {
    String[] ids = {"a", "b"};
    String[] dataRootPaths = new String[ids.length];
    dataRootPaths[0] = "/tmp/dataroot/dir/dir";
    dataRootPaths[1] = "/tmp/dataroot/dir";

    expectedException.expect(DataDirsConfigurationException.class);
    expectedException.expectMessage("overlap");
    configureDataRoot(ids, dataRootPaths);
  }

  @Test
  public void testOverlappingPaths_firstSubDirOfSecond() throws Exception {
    String[] ids = {"a", "b"};
    String[] dataRootPaths = new String[ids.length];
    dataRootPaths[0] = "/tmp/dataroot/dir/dir";
    dataRootPaths[1] = "/tmp/dataroot/dir";

    expectedException.expect(DataDirsConfigurationException.class);
    expectedException.expectMessage("overlap");
    configureDataRoot(ids, dataRootPaths);
  }

  @Test
  public void testOverlappingPaths_samePaths() throws Exception {
    String[] ids = {"a", "b"};
    String[] dataRootPaths = new String[ids.length];
    String sameDataPath = folder.newFolder().getAbsolutePath();
    dataRootPaths[0] = sameDataPath;
    dataRootPaths[1] = sameDataPath;

    expectedException.expect(DataDirsConfigurationException.class);
    expectedException.expectMessage("overlap");
    configureDataRoot(ids, dataRootPaths);
  }

  @Test
  public void testOverlappingPaths_relativeAndAbsolute() throws Exception {
    String[] ids = {"a", "b"};
    String[] dataRootPaths = new String[ids.length];
    dataRootPaths[0] = "dir";
    dataRootPaths[1] = Paths.get("dir").toAbsolutePath().toString();

    expectedException.expect(DataDirsConfigurationException.class);
    expectedException.expectMessage("overlap");
    configureDataRoot(ids, dataRootPaths);
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
  @Ignore("Fails currently, need some fixes to be done in terracotta-configuration")
  public void testSourceAsURLIsIgnored() throws Exception {
    String[] ids = {"id"};
    String[] dataRootPaths = new String[ids.length];
    String source = new URL("http://example.com/test/tc-config.xml").getPath();

    DataDirsConfigImpl dataRootConfig = configureDataRoot(ids, dataRootPaths, source);
    assertThat(dataRootConfig.getRoot(ids[0]).getParent(), is(Paths.get(".")));
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

    expectedException.expect(DataDirsConfigurationException.class);
    expectedException.expectMessage("More than one");
    configureDataRoot(dataRootMappings);
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

    return new DataDirsConfigImpl(ParameterSubstitutor::substitute, DataRootConfigParser.getPathResolver(source), dataDirectories);
  }

  private DataDirsConfigImpl configureDataRoot(DataRootMapping[] dataRootMappings) throws IOException {
    DataDirectories dataDirectories = new DataDirectories();
    for (int i = 0; i < dataRootMappings.length; i++) {
      if (dataRootMappings[i].getValue() == null) {
        dataRootMappings[i].setValue(folder.newFolder().getAbsolutePath());
      }
      dataDirectories.getDirectory().add(dataRootMappings[i]);
    }

    return new DataDirsConfigImpl(ParameterSubstitutor::substitute, DataRootConfigParser.getPathResolver(null), dataDirectories);
  }
}