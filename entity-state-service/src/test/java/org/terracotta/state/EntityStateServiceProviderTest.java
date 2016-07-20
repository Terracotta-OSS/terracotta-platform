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

package org.terracotta.state;

import org.junit.Test;
import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.entity.ServiceProviderCleanupException;
import org.terracotta.entity.ServiceProviderConfiguration;
import org.terracotta.state.config.EntityStateRepositoryConfig;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

/**
 */
public class EntityStateServiceProviderTest {

  @Test
  public void testInitialize() {
    EntityStateServiceProvider serviceProvider = new EntityStateServiceProvider();

    ServiceProviderConfiguration serviceProviderConfiguration = mock(ServiceProviderConfiguration.class);

    assertTrue(serviceProvider.initialize(serviceProviderConfiguration));
  }

  @Test
  public void testGetServiceInvalidServiceConfig() {
    EntityStateServiceProvider serviceProvider = new EntityStateServiceProvider();

    ServiceConfiguration<EntityStateRepository> serviceConfiguration = mock(ServiceConfiguration.class);

    try {
      EntityStateRepository stateRepository = serviceProvider.getService(1L, serviceConfiguration);
      fail("IllegalArgumentException expected");
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), startsWith("Unexpected configuration type"));
    }

  }

  @Test
  public void testGetService() {
    EntityStateServiceProvider serviceProvider = new EntityStateServiceProvider();

    EntityStateRepository stateRepository = serviceProvider.getService(1L, new EntityStateRepositoryConfig());

    assertNotNull(stateRepository);

    EntityStateRepository sameStateRepository = serviceProvider.getService(1L, new EntityStateRepositoryConfig());

    assertSame(stateRepository, sameStateRepository);

    EntityStateRepository anotherRepository = serviceProvider.getService(2L, new EntityStateRepositoryConfig());

    assertNotNull(anotherRepository);
    assertNotSame(stateRepository, anotherRepository);

  }

  @Test
  public void testClear() throws ServiceProviderCleanupException {
    EntityStateServiceProvider serviceProvider = new EntityStateServiceProvider();

    EntityStateRepository stateRepository = serviceProvider.getService(1L, new EntityStateRepositoryConfig());
    EntityStateRepository anotherRepository = serviceProvider.getService(2L, new EntityStateRepositoryConfig());

    serviceProvider.clear();

    EntityStateRepository stateRepositoryAfterClear = serviceProvider.getService(1L, new EntityStateRepositoryConfig());
    EntityStateRepository anotherRepositoryAfterClear = serviceProvider.getService(2L, new EntityStateRepositoryConfig());

    assertNotSame(stateRepository, stateRepositoryAfterClear);
    assertNotSame(anotherRepository, anotherRepositoryAfterClear);
  }
}
