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
package org.terracotta.offheapresource;

import org.junit.Test;
import org.terracotta.offheapresource.config.MemoryUnit;
import org.terracotta.offheapresource.config.OffheapResourcesType;
import org.terracotta.offheapresource.config.ResourceType;
import org.terracotta.statistics.StatisticsManager;
import org.terracotta.statistics.ValueStatistic;

import java.math.BigInteger;

import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OffHeapResourcesProviderTest {

  @Test
  public void testObserverExposed() {
    ResourceType resourceConfig = mock(ResourceType.class);
    when(resourceConfig.getName()).thenReturn("foo");
    when(resourceConfig.getUnit()).thenReturn(MemoryUnit.MB);
    when(resourceConfig.getValue()).thenReturn(BigInteger.valueOf(2));

    OffheapResourcesType configuration = mock(OffheapResourcesType.class);
    when(configuration.getResource()).thenReturn(singletonList(resourceConfig));

    OffHeapResourcesProvider provider = new OffHeapResourcesProvider(configuration);

    OffHeapResource offHeapResource = provider.getOffHeapResource(OffHeapResourceIdentifier.identifier("foo"));
    assertThat(offHeapResource.available(), equalTo(2L * 1024 * 1024));

    assertThat(StatisticsManager.nodeFor(offHeapResource).getChildren().size(), equalTo(1));
    ValueStatistic valueStatistic = (ValueStatistic) StatisticsManager.nodeFor(offHeapResource).getChildren().iterator().next().getContext().attributes().get("this");
    assertThat(valueStatistic.value(), equalTo(0L));
  }

  @Test
  public void testInitializeWithValidConfig() {
    ResourceType resourceConfig = mock(ResourceType.class);
    when(resourceConfig.getName()).thenReturn("foo");
    when(resourceConfig.getUnit()).thenReturn(MemoryUnit.MB);
    when(resourceConfig.getValue()).thenReturn(BigInteger.valueOf(2));

    OffheapResourcesType configuration = mock(OffheapResourcesType.class);
    when(configuration.getResource()).thenReturn(singletonList(resourceConfig));

    OffHeapResourcesProvider provider = new OffHeapResourcesProvider(configuration);

    assertThat(provider.getOffHeapResource(OffHeapResourceIdentifier.identifier("foo")), notNullValue());
    assertThat(provider.getOffHeapResource(OffHeapResourceIdentifier.identifier("foo")).available(), is(2L * 1024 * 1024));
  }

  @Test
  public void testNullReturnOnInvalidResource() {
    ResourceType resourceConfig = mock(ResourceType.class);
    when(resourceConfig.getName()).thenReturn("foo");
    when(resourceConfig.getUnit()).thenReturn(MemoryUnit.MB);
    when(resourceConfig.getValue()).thenReturn(BigInteger.valueOf(2));

    OffheapResourcesType configuration = mock(OffheapResourcesType.class);
    when(configuration.getResource()).thenReturn(singletonList(resourceConfig));

    OffHeapResourcesProvider provider = new OffHeapResourcesProvider(configuration);

    assertThat(provider.getOffHeapResource(OffHeapResourceIdentifier.identifier("bar")), nullValue());
  }


  @Test
  public void testResourceTooBig() throws Exception {
    ResourceType resourceConfig = mock(ResourceType.class);
    when(resourceConfig.getName()).thenReturn("foo");
    when(resourceConfig.getUnit()).thenReturn(MemoryUnit.B);
    when(resourceConfig.getValue()).thenReturn(BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE));

    OffheapResourcesType configuration = mock(OffheapResourcesType.class);
    when(configuration.getResource()).thenReturn(singletonList(resourceConfig));

    try {
      new OffHeapResourcesProvider(configuration);
      fail("Should have failed with exception");
    } catch (ArithmeticException e) {
      // expected
    }
  }

  @Test
  public void testResourceMax() throws Exception {
    ResourceType resourceConfig = mock(ResourceType.class);
    when(resourceConfig.getName()).thenReturn("foo");
    when(resourceConfig.getUnit()).thenReturn(MemoryUnit.B);
    when(resourceConfig.getValue()).thenReturn(BigInteger.valueOf(Long.MAX_VALUE));

    OffheapResourcesType configuration = mock(OffheapResourcesType.class);
    when(configuration.getResource()).thenReturn(singletonList(resourceConfig));

    OffHeapResourcesProvider provider = new OffHeapResourcesProvider(configuration);
  }
}
