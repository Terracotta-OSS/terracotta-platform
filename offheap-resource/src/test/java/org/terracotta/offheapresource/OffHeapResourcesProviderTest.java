/*
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is OffHeap Resource.
 *
 * The Initial Developer of the Covered Software is
 * Terracotta, Inc., a Software AG company
 */

package org.terracotta.offheapresource;

import java.math.BigInteger;
import java.util.Collections;
import org.junit.Test;
import org.terracotta.entity.ServiceProvider;
import org.terracotta.entity.ServiceProviderConfiguration;

import org.terracotta.offheapresource.config.MemoryUnit;
import org.terracotta.offheapresource.config.ResourceType;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OffHeapResourcesProviderTest {

  @Test
  public void testInitializeWithWrongConfig() {
    OffHeapResourcesProvider provider = new OffHeapResourcesProvider();

    assertThat(provider.initialize(new ServiceProviderConfiguration() {
      @Override
      public Class<? extends ServiceProvider> getServiceProviderType() {
        return OffHeapResourcesProvider.class;
      }
    }), is(false));
  }

  @Test
  public void testInitializeWithValidConfig() {
    ResourceType resourceConfig = mock(ResourceType.class);
    when(resourceConfig.getName()).thenReturn("foo");
    when(resourceConfig.getUnit()).thenReturn(MemoryUnit.MB);
    when(resourceConfig.getValue()).thenReturn(BigInteger.valueOf(2));

    OffHeapResourcesConfiguration config = mock(OffHeapResourcesConfiguration.class);
    when(config.getResources()).thenReturn(Collections.singleton(resourceConfig));

    OffHeapResourcesProvider provider = new OffHeapResourcesProvider();
    provider.initialize(config);

    assertThat(provider.getService(42L, OffHeapResourceIdentifier.identifier("foo")), notNullValue());
    assertThat(provider.getService(42L, OffHeapResourceIdentifier.identifier("foo")).available(), is(2L * 1024 * 1024));
  }

  @Test
  public void testDoubleInitialize() {
    ResourceType resourceConfig = mock(ResourceType.class);
    when(resourceConfig.getName()).thenReturn("foo");
    when(resourceConfig.getUnit()).thenReturn(MemoryUnit.MB);
    when(resourceConfig.getValue()).thenReturn(BigInteger.valueOf(2));

    OffHeapResourcesConfiguration config = mock(OffHeapResourcesConfiguration.class);
    when(config.getResources()).thenReturn(Collections.singleton(resourceConfig));

    OffHeapResourcesProvider provider = new OffHeapResourcesProvider();
    provider.initialize(config);
    try {
      provider.initialize(config);
      fail("Expected IllegalStateException");
    } catch (IllegalStateException e) {
      //expected
    }
  }

  @Test
  public void testNullReturnOnInvalidResource() {
    ResourceType resourceConfig = mock(ResourceType.class);
    when(resourceConfig.getName()).thenReturn("foo");
    when(resourceConfig.getUnit()).thenReturn(MemoryUnit.MB);
    when(resourceConfig.getValue()).thenReturn(BigInteger.valueOf(2));

    OffHeapResourcesConfiguration config = mock(OffHeapResourcesConfiguration.class);
    when(config.getResources()).thenReturn(Collections.singleton(resourceConfig));

    OffHeapResourcesProvider provider = new OffHeapResourcesProvider();
    provider.initialize(config);

    assertThat(provider.getService(42L, OffHeapResourceIdentifier.identifier("bar")), nullValue());
  }


  @Test
  public void testNullReturnAfterClear() {
    ResourceType resourceConfig = mock(ResourceType.class);
    when(resourceConfig.getName()).thenReturn("foo");
    when(resourceConfig.getUnit()).thenReturn(MemoryUnit.MB);
    when(resourceConfig.getValue()).thenReturn(BigInteger.valueOf(2));

    OffHeapResourcesConfiguration config = mock(OffHeapResourcesConfiguration.class);
    when(config.getResources()).thenReturn(Collections.singleton(resourceConfig));

    OffHeapResourcesProvider provider = new OffHeapResourcesProvider();
    provider.initialize(config);
    provider.clear();
    assertThat(provider.getService(42L, OffHeapResourceIdentifier.identifier("foo")), nullValue());
  }
}
