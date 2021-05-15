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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.terracotta.dynamic_config.api.service.IParameterSubstitutor;
import org.terracotta.dynamic_config.server.api.PathResolver;
import org.terracotta.entity.PlatformConfiguration;
import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.entity.ServiceProvider;
import org.terracotta.entity.ServiceProviderCleanupException;
import org.terracotta.entity.ServiceProviderConfiguration;
import org.terracotta.passthrough.PassthroughServer;
import org.terracotta.testing.TmpDir;

import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;

import static java.util.Collections.singletonMap;
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
  public TmpDir folder = new TmpDir();

  @Before
  public void setUp() throws Exception {
    DATA_ROOT_PATH = folder.getRoot().toAbsolutePath().toString();

    this.passthroughServer = new PassthroughServer();
    this.passthroughServer.registerExtendedConfiguration(new DataDirsConfigImpl(IParameterSubstitutor.identity(), new PathResolver(folder.getRoot()), null, singletonMap(DATA_ROOT_ID, Paths.get(DATA_ROOT_PATH))));
    this.passthroughServer.registerServiceProvider(new TestServiceProvider(), null);
    this.passthroughServer.registerAsynchronousServerCrasher(p -> {});
    this.passthroughServer.start(true, false);
  }

  @Test
  public void testDataRootConfig() throws Exception {
    TestService testService = TestServiceProvider.testService;
    assertNotNull(testService);
    Collection<DataDirsConfig> dataDirsConfigs = testService.getDataRootConfigs();
    assertNotNull(dataDirsConfigs);
    assertEquals(1, dataDirsConfigs.size());

    DataDirsConfig dataDirsConfig = dataDirsConfigs.iterator().next();
    PlatformConfiguration platformConfiguration = mock(PlatformConfiguration.class);
    when(platformConfiguration.getServerName()).thenReturn("server");

    assertEquals(Paths.get(DATA_ROOT_PATH).resolve("server"), dataDirsConfig.getDataDirectoriesForServer(platformConfiguration).getDataDirectory(DATA_ROOT_ID));
  }

  @After
  public void tearDown() throws Exception {
    if(this.passthroughServer != null) {
      this.passthroughServer.stop();
    }
  }

  public static class TestServiceProvider implements ServiceProvider {

    //TODO: create a test entity to retrieve DataRootConfigs and then test
    public static TestService testService;

    @Override
    public boolean initialize(ServiceProviderConfiguration serviceProviderConfiguration,
                              PlatformConfiguration platformConfiguration) {
      testService = new TestService(platformConfiguration.getExtendedConfiguration(DataDirsConfig.class));
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
    private final Collection<DataDirsConfig> dataDirsConfigs;
    public TestService(Collection<DataDirsConfig> dataDirsConfigs) {

      this.dataDirsConfigs = dataDirsConfigs;
    }

    public Collection<DataDirsConfig> getDataRootConfigs() {
      return dataDirsConfigs;
    }
  }
}