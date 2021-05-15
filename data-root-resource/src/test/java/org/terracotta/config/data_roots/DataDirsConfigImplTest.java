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
import org.terracotta.config.data_roots.management.DataRootBinding;
import org.terracotta.dynamic_config.api.service.IParameterSubstitutor;
import org.terracotta.dynamic_config.server.api.PathResolver;
import org.terracotta.management.service.monitoring.EntityManagementRegistry;
import org.terracotta.management.service.monitoring.EntityMonitoringService;
import org.terracotta.testing.TmpDir;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.IntStream.range;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DataDirsConfigImplTest {

  @Rule
  public TmpDir folder = new TmpDir(false);

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  IParameterSubstitutor parameterSubstitutor = IParameterSubstitutor.identity();

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
    dataRootConfig.addDataDirectory(postRegistry_Id, folder.getRoot().resolve("new-one").toAbsolutePath().toString());
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
    String[] ids = {"a"};
    String[] dataRootPaths = new String[ids.length];

    final DataDirsConfigImpl config = configureDataRoot(ids, dataRootPaths);

    expectedException.expect(DataDirsConfigurationException.class);
    expectedException.expectMessage("already exists");
    config.addDataDirectory("a", "foo");
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
    String sameDataPath = folder.getRoot().toAbsolutePath().toString();
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
    dataRootPaths[1] = folder.getRoot().resolve("dir").toAbsolutePath().toString();

    expectedException.expect(DataDirsConfigurationException.class);
    expectedException.expectMessage("overlap");
    configureDataRoot(ids, dataRootPaths);
  }

  @Test
  public void testSubstitutablePaths() throws Exception {
    String configuredPath = folder.getRoot().toAbsolutePath().toString();

    parameterSubstitutor = mock(IParameterSubstitutor.class);
    when(parameterSubstitutor.substitute(Paths.get(configuredPath + "-%h"))).thenReturn(Paths.get(configuredPath + "-" + InetAddress.getLocalHost().getHostName()));

    DataDirsConfigImpl dataRootConfig = new DataDirsConfigImpl(
        parameterSubstitutor,
        new PathResolver(folder.getRoot()),
        null,
        singletonMap("id", Paths.get(configuredPath + "-%h")));
    String hostName = InetAddress.getLocalHost().getHostName();
    assertThat(dataRootConfig.getRoot("id"), is(Paths.get(configuredPath + "-" + hostName)));
  }

  @Test
  public void testRelativePathWithSourceSpecified() throws Exception {
    String[] ids = {"id"};
    String[] dataRootPaths = {"folder"};
    String source = folder.getRoot().resolve("foo").toAbsolutePath().toString();

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

  private DataDirsConfigImpl configureDataRoot(String[] ids, String[] dataRootPaths) throws IOException {
    return configureDataRoot(ids, dataRootPaths, (String) null);
  }

  private DataDirsConfigImpl configureDataRoot(String[] ids, String[] dataRootPaths, String source) throws IOException {
    return configureDataRoot(ids, dataRootPaths, source, -1);
  }

  private DataDirsConfigImpl configureDataRoot(String[] ids, String[] dataRootPaths, String source, int platformRootIndex) throws IOException {
    final Map<String, Path> dirs = range(0, ids.length).boxed()
        .collect(toMap(
            idx -> ids[idx],
            idx -> {
              if (dataRootPaths[idx] == null) {
                dataRootPaths[idx] = folder.getRoot().resolve("dir-" + idx).toAbsolutePath().toString();
              }
              return Paths.get(dataRootPaths[idx]);
            }));
    Path metadataDir = platformRootIndex >= 0 ? dirs.get(ids[platformRootIndex]) : null;
    return new DataDirsConfigImpl(parameterSubstitutor, new PathResolver(source == null ? folder.getRoot() : Paths.get(source)), metadataDir, dirs);
  }
}