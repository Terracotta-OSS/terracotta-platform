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
package org.terracotta.platform;

import com.tc.classloader.BuiltinService;
import org.junit.Test;
import org.terracotta.entity.PlatformConfiguration;

import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ServerInfoProviderTest {
  @Test
  public void isBuiltinService() {
    assertNotNull(ServerInfoProvider.class.getAnnotation(BuiltinService.class));
  }

  @Test
  public void providesServerInfo() {
    ServerInfoProvider provider = new ServerInfoProvider();
    Collection<Class<?>> providedServiceTypes = provider.getProvidedServiceTypes();
    assertEquals(1, providedServiceTypes.size());
    assertEquals(ServerInfo.class, providedServiceTypes.iterator().next());
  }

  @Test
  public void capturesServerName() {
    PlatformConfiguration platformConfiguration = mock(PlatformConfiguration.class);
    when(platformConfiguration.getServerName()).thenReturn("abc");

    ServerInfoProvider provider = new ServerInfoProvider();
    provider.initialize(null, platformConfiguration);

    ServerInfo serverInfo = provider.getService(1, () -> ServerInfo.class);
    assertEquals("abc", serverInfo.getName());
  }
}
