/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
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
