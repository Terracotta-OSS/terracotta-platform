/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */

package org.terracotta.config.data_roots;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.terracotta.data.config.DataDirectories;
import org.terracotta.data.config.DataRootMapping;
import org.terracotta.entity.PlatformConfiguration;
import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.entity.ServiceProvider;
import org.terracotta.entity.ServiceProviderCleanupException;
import org.terracotta.entity.ServiceProviderConfiguration;
import org.terracotta.passthrough.PassthroughServer;

import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author vmad
 */
public class PassthroughIntegrationTest {

  private static final String DATA_ROOT_ID = "a";
  private String DATA_ROOT_PATH;

  private PassthroughServer passthroughServer;

  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  @Before
  public void setUp() throws Exception {
    DATA_ROOT_PATH = folder.newFolder().getAbsolutePath();

    this.passthroughServer = new PassthroughServer();
    this.passthroughServer.registerExtendedConfiguration(new DataDirectoriesConfigImpl(null, getConfiguration()));
    this.passthroughServer.registerServiceProvider(new TestServiceProvider(), null);
    this.passthroughServer.registerAsynchronousServerCrasher(p -> {});
    this.passthroughServer.start(true, false);
  }

  @Test
  public void testDataRootConfig() throws Exception {
    TestService testService = TestServiceProvider.testService;
    assertNotNull(testService);
    Collection<DataDirectoriesConfig> dataDirectoriesConfigs = testService.getDataRootConfigs();
    assertNotNull(dataDirectoriesConfigs);
    assertEquals(1, dataDirectoriesConfigs.size());

    DataDirectoriesConfig dataDirectoriesConfig = dataDirectoriesConfigs.iterator().next();
    PlatformConfiguration platformConfiguration = mock(PlatformConfiguration.class);
    when(platformConfiguration.getServerName()).thenReturn("server");

    assertEquals(Paths.get(DATA_ROOT_PATH).resolve("server"), dataDirectoriesConfig.getDataDirectoriesForServer(platformConfiguration).getDataDirectory(DATA_ROOT_ID));
  }

  @After
  public void tearDown() throws Exception {
    if(this.passthroughServer != null) {
      this.passthroughServer.stop();
    }
  }

  private DataDirectories getConfiguration() throws Exception {
    DataDirectories dataDirectories = new DataDirectories();
    DataRootMapping dataRootMapping = new DataRootMapping();
    dataRootMapping.setName(DATA_ROOT_ID);
    dataRootMapping.setValue(DATA_ROOT_PATH);
    dataDirectories.getDirectory().add(dataRootMapping);

    return dataDirectories;
  }

  public static class TestServiceProvider implements ServiceProvider {

    //TODO: create a test entity to retrieve DataRootConfigs and then test
    public static TestService testService;

    @Override
    public boolean initialize(ServiceProviderConfiguration serviceProviderConfiguration,
                              PlatformConfiguration platformConfiguration) {
      testService = new TestService(platformConfiguration.getExtendedConfiguration(DataDirectoriesConfig.class));
      return true;
    }

    @Override
    public <T> T getService(long l,
                            ServiceConfiguration<T> serviceConfiguration) {
      return serviceConfiguration.getServiceType().cast(testService);
    }

    @Override
    public Collection<Class<?>> getProvidedServiceTypes() {
      return Collections.singleton(TestService.class);
    }

    @Override
    public void prepareForSynchronization() throws ServiceProviderCleanupException {
      //nothing to do
    }
  }

  public static class TestService {
    private final Collection<DataDirectoriesConfig> dataDirectoriesConfigs;
    public TestService(Collection<DataDirectoriesConfig> dataDirectoriesConfigs) {

      this.dataDirectoriesConfigs = dataDirectoriesConfigs;
    }

    public Collection<DataDirectoriesConfig> getDataRootConfigs() {
      return dataDirectoriesConfigs;
    }
  }
}