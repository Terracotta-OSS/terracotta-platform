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

import org.junit.Before;
import org.junit.Test;
import org.terracotta.offheapresource.config.MemoryUnit;
import org.terracotta.offheapresource.config.OffheapResourcesType;
import org.terracotta.offheapresource.config.ResourceType;
import org.terracotta.statistics.StatisticsManager;
import org.terracotta.statistics.ValueStatistic;

import java.math.BigInteger;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OffHeapResourcesProviderTest {
  private ResourceType resourceConfig;
  private OffheapResourcesType configuration;

  @Before
  public void setUp() {
    resourceConfig = mock(ResourceType.class);
    configuration = mock(OffheapResourcesType.class);
  }

  @Test
  public void testObserverExposed() {
    when(resourceConfig.getName()).thenReturn("foo");
    when(resourceConfig.getUnit()).thenReturn(MemoryUnit.MB);
    when(resourceConfig.getValue()).thenReturn(BigInteger.valueOf(2));
    when(configuration.getResource()).thenReturn(singletonList(resourceConfig));

    OffHeapResourcesProvider provider = new OffHeapResourcesProvider(configuration);
    OffHeapResource offHeapResource = provider.getOffHeapResource(OffHeapResourceIdentifier.identifier("foo"));
    assertThat(offHeapResource.available(), equalTo(2L * 1024 * 1024));

    assertThat(StatisticsManager.nodeFor(offHeapResource).getChildren().size(), equalTo(1));
    ValueStatistic<Long> valueStatistic = (ValueStatistic<Long>) StatisticsManager.nodeFor(offHeapResource).getChildren().iterator().next().getContext().attributes().get("this");
    assertThat(valueStatistic.value(), equalTo(0L));
  }

  @Test
  public void testInitializeWithValidConfig() {
    when(resourceConfig.getName()).thenReturn("foo");
    when(resourceConfig.getUnit()).thenReturn(MemoryUnit.MB);
    when(resourceConfig.getValue()).thenReturn(BigInteger.valueOf(2));
    when(configuration.getResource()).thenReturn(singletonList(resourceConfig));

    OffHeapResourcesProvider provider = new OffHeapResourcesProvider(configuration);
    assertThat(provider.getOffHeapResource(OffHeapResourceIdentifier.identifier("foo")), notNullValue());
    assertThat(provider.getOffHeapResource(OffHeapResourceIdentifier.identifier("foo")).available(), is(2L * 1024 * 1024));
  }

  @Test
  public void testNullReturnOnInvalidResource() {
    when(resourceConfig.getName()).thenReturn("foo");
    when(resourceConfig.getUnit()).thenReturn(MemoryUnit.MB);
    when(resourceConfig.getValue()).thenReturn(BigInteger.valueOf(2));
    when(configuration.getResource()).thenReturn(singletonList(resourceConfig));

    OffHeapResourcesProvider provider = new OffHeapResourcesProvider(configuration);
    assertThat(provider.getOffHeapResource(OffHeapResourceIdentifier.identifier("bar")), nullValue());
  }

  @Test(expected = ArithmeticException.class)
  public void testResourceTooBig() {
    when(resourceConfig.getName()).thenReturn("foo");
    when(resourceConfig.getUnit()).thenReturn(MemoryUnit.B);
    when(resourceConfig.getValue()).thenReturn(BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE));
    when(configuration.getResource()).thenReturn(singletonList(resourceConfig));

    new OffHeapResourcesProvider(configuration);
  }

  @Test
  public void testResourceMax() {
    when(resourceConfig.getName()).thenReturn("foo");
    when(resourceConfig.getUnit()).thenReturn(MemoryUnit.B);
    when(resourceConfig.getValue()).thenReturn(BigInteger.valueOf(Long.MAX_VALUE));
    when(configuration.getResource()).thenReturn(singletonList(resourceConfig));

    new OffHeapResourcesProvider(configuration);
  }

  @Test
  public void testResourceAddition_ok() {
    OffHeapResourcesProvider offHeapResourcesProvider = new OffHeapResourcesProvider(configuration);
    assertTrue(offHeapResourcesProvider.addOffHeapResource(OffHeapResourceIdentifier.identifier("newOffheap"), 100_000L));
  }

  @Test
  public void testResourceAddition_failForDuplicateResource() {
    OffHeapResourcesProvider offHeapResourcesProvider = new OffHeapResourcesProvider(configuration);
    assertTrue(offHeapResourcesProvider.addOffHeapResource(OffHeapResourceIdentifier.identifier("newOffheap"), 100_000L));
    assertFalse(offHeapResourcesProvider.addOffHeapResource(OffHeapResourceIdentifier.identifier("newOffheap"), 999L));
  }

  @Test
  public void testConcurrentOffheapAddition() throws Exception {
    long perThreadIncrement = 100L;
    int updatesPerThread = 50;
    int numThreads = 20;
    ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
    OffHeapResourcesProvider offHeapResourcesProvider = new OffHeapResourcesProvider(configuration);
    executorService.execute(() -> {
      for (int i = 0; i < numThreads * updatesPerThread; i++) {
        assertTrue(offHeapResourcesProvider.addOffHeapResource(OffHeapResourceIdentifier.identifier("newOffheap" + i), perThreadIncrement));
      }
    });
    executorService.awaitTermination(10, TimeUnit.SECONDS);
    assertThat(offHeapResourcesProvider.getTotalConfiguredOffheap(), equalTo(numThreads * updatesPerThread * perThreadIncrement));
  }
}
