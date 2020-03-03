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

import org.junit.After;
import org.junit.Test;
import org.terracotta.config.TCConfigurationParser;
import org.terracotta.config.TcConfiguration;
import org.terracotta.entity.PlatformConfiguration;

import java.io.File;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * TcConfigWithDataDirectoriesTest
 */
public class TcConfigWithDataDirectoriesTest {

  private DataDirectoriesConfig dataDirectoriesConfig;

  @After
  public void tearDown() throws Exception {
    dataDirectoriesConfig.close();
  }

  @Test
  public void testCanParseConfigWithDataRoot() throws Exception {
    TcConfiguration configuration = TCConfigurationParser.parse(
        new File(getClass().getResource("/configs/tc-config-data.xml").getPath()));
    List<DataDirectoriesConfig> dataRoots = configuration.getExtendedConfiguration(DataDirectoriesConfig.class);

    assertThat(dataRoots, hasSize(1));

    dataDirectoriesConfig = dataRoots.get(0);

    PlatformConfiguration platformConfiguration = mock(PlatformConfiguration.class);
    when(platformConfiguration.getServerName()).thenReturn("server");

    assertThat(dataDirectoriesConfig.getDataDirectoriesForServer(platformConfiguration).getDataDirectory("data"), notNullValue());
  }

  @Test
  public void testProperlySetsPlatformRootId() throws Exception {
    TcConfiguration configuration = TCConfigurationParser.parse(
        new File(getClass().getResource("/configs/tc-config-data.xml").getPath()));
    List<DataDirectoriesConfig> dataRoots = configuration.getExtendedConfiguration(DataDirectoriesConfig.class);

    dataDirectoriesConfig = dataRoots.get(0);

    PlatformConfiguration platformConfiguration = mock(PlatformConfiguration.class);
    when(platformConfiguration.getServerName()).thenReturn("server");

    assertThat(dataDirectoriesConfig.getDataDirectoriesForServer(platformConfiguration).getPlatformDataDirectoryIdentifier().get(), is("data"));
  }
}
