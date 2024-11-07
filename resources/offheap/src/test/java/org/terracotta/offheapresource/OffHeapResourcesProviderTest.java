/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
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

import java.math.BigInteger;
import static java.util.Collections.singletonList;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.terracotta.management.service.monitoring.EntityManagementRegistry;
import org.terracotta.management.service.monitoring.EntityMonitoringService;
import static org.terracotta.offheapresource.OffHeapResourceIdentifier.identifier;
import org.terracotta.offheapresource.config.MemoryUnit;
import org.terracotta.offheapresource.config.OffheapResourcesType;
import org.terracotta.offheapresource.config.ResourceType;
import org.terracotta.offheapresource.management.OffHeapResourceBinding;
import org.terracotta.statistics.StatisticsManager;
import org.terracotta.statistics.ValueStatistic;

public class OffHeapResourcesProviderTest {
  private ResourceType resourceConfig;
  private OffheapResourcesType configuration;

  @Before
  public void setUp() {
    resourceConfig = mock(ResourceType.class);
    configuration = mock(OffheapResourcesType.class);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testObserverExposed() {
    when(resourceConfig.getName()).thenReturn("foo");
    when(resourceConfig.getUnit()).thenReturn(MemoryUnit.MB);
    when(resourceConfig.getValue()).thenReturn(BigInteger.valueOf(2));
    when(configuration.getResource()).thenReturn(singletonList(resourceConfig));

    OffHeapResourcesProvider provider = OffHeapResourceConfigurationParser.toOffHeapResourcesProvider(configuration);
    OffHeapResource offHeapResource = provider.getOffHeapResource(identifier("foo"));
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

    OffHeapResourcesProvider provider = OffHeapResourceConfigurationParser.toOffHeapResourcesProvider(configuration);
    assertThat(provider.getOffHeapResource(identifier("foo")), notNullValue());
    assertThat(provider.getOffHeapResource(identifier("foo")).available(), is(2L * 1024 * 1024));
  }

  @Test
  public void testNullReturnOnInvalidResource() {
    when(resourceConfig.getName()).thenReturn("foo");
    when(resourceConfig.getUnit()).thenReturn(MemoryUnit.MB);
    when(resourceConfig.getValue()).thenReturn(BigInteger.valueOf(2));
    when(configuration.getResource()).thenReturn(singletonList(resourceConfig));

    OffHeapResourcesProvider provider = OffHeapResourceConfigurationParser.toOffHeapResourcesProvider(configuration);
    assertThat(provider.getOffHeapResource(identifier("bar")), nullValue());
  }

  @Test(expected = ArithmeticException.class)
  public void testResourceTooBig() {
    when(resourceConfig.getName()).thenReturn("foo");
    when(resourceConfig.getUnit()).thenReturn(MemoryUnit.B);
    when(resourceConfig.getValue()).thenReturn(BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE));
    when(configuration.getResource()).thenReturn(singletonList(resourceConfig));

    OffHeapResourceConfigurationParser.toOffHeapResourcesProvider(configuration);
  }

  @Test
  public void testResourceMax() {
    when(resourceConfig.getName()).thenReturn("foo");
    when(resourceConfig.getUnit()).thenReturn(MemoryUnit.B);
    when(resourceConfig.getValue()).thenReturn(BigInteger.valueOf(Long.MAX_VALUE));
    when(configuration.getResource()).thenReturn(singletonList(resourceConfig));

    OffHeapResourceConfigurationParser.toOffHeapResourcesProvider(configuration);
  }

  @Test
  public void testResourceAddition_ok() {
    EntityManagementRegistry registry = mock(EntityManagementRegistry.class);
    EntityMonitoringService entityMonitoringService = mock(EntityMonitoringService.class);
    when(registry.getMonitoringService()).thenReturn(entityMonitoringService);
    when(entityMonitoringService.getConsumerId()).thenReturn(1L);
    OffHeapResourcesProvider offHeapResourcesProvider = OffHeapResourceConfigurationParser.toOffHeapResourcesProvider(configuration);
    OffHeapResourceIdentifier newOffheap_preRegistry_Id = identifier("newOffheap_preRegistry");
    assertTrue(offHeapResourcesProvider.addOffHeapResource(newOffheap_preRegistry_Id, 100_000L));
    OffHeapResourceImpl newOffheap_preRegistry = offHeapResourcesProvider.getOffHeapResource(newOffheap_preRegistry_Id);
    OffHeapResourceBinding newOffheap_preRegistry_Binding = newOffheap_preRegistry.getManagementBinding();
    offHeapResourcesProvider.onManagementRegistryCreated(registry);
    verify(registry).register(newOffheap_preRegistry_Binding);

    OffHeapResourceIdentifier newOffheap_postRegistry_Id = identifier("newOffheap_postRegistry");
    assertTrue(offHeapResourcesProvider.addOffHeapResource(newOffheap_postRegistry_Id, 100_000L));
    OffHeapResourceImpl newOffheap_postRegistry = offHeapResourcesProvider.getOffHeapResource(newOffheap_postRegistry_Id);
    OffHeapResourceBinding newOffheap_postRegistry_Binding = newOffheap_postRegistry.getManagementBinding();
    verify(registry).registerAndRefresh(newOffheap_postRegistry_Binding);
    newOffheap_postRegistry.setCapacity(150_000L);
    assertTrue(newOffheap_postRegistry_Binding.getValue().capacity() == 150_000L);
  }

  @Test
  public void testResourceAddition_failForDuplicateResource() {
    OffHeapResourcesProvider offHeapResourcesProvider = OffHeapResourceConfigurationParser.toOffHeapResourcesProvider(configuration);
    assertThat(offHeapResourcesProvider.getTotalConfiguredOffheap(), equalTo(0L));

    assertTrue(offHeapResourcesProvider.addOffHeapResource(identifier("newOffheap"), 100_000L));
    assertThat(offHeapResourcesProvider.getTotalConfiguredOffheap(), equalTo(100_000L));

    assertFalse(offHeapResourcesProvider.addOffHeapResource(identifier("newOffheap"), 999L));
    assertThat(offHeapResourcesProvider.getTotalConfiguredOffheap(), equalTo(100_000L));
  }

  @Test
  public void testConcurrentOffheapAddition_noOverlap() throws Exception {
    long perThreadIncrement = 100L;
    int numThreads = 20;

    ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
    OffHeapResourcesProvider offHeapResourcesProvider = OffHeapResourceConfigurationParser.toOffHeapResourcesProvider(configuration);
    CyclicBarrier cyclicBarrier = new CyclicBarrier(numThreads + 1);
    for (int i = 0; i < numThreads; i++) {
      executorService.submit(() -> {
        try {
          cyclicBarrier.await();
          assertTrue(offHeapResourcesProvider.addOffHeapResource(identifier("newOffheap-" + Thread.currentThread().getId()), perThreadIncrement));
          cyclicBarrier.await();
        } catch (InterruptedException | BrokenBarrierException e) {
          throw new RuntimeException(e);
        }
      });
    }
    cyclicBarrier.await();
    cyclicBarrier.await();

    assertThat(offHeapResourcesProvider.getTotalConfiguredOffheap(), equalTo(numThreads * perThreadIncrement));
  }

  @Test
  public void testConcurrentOffheapAddition_someOverlap() {
    OffHeapResourcesProvider offHeapResourcesProvider = OffHeapResourceConfigurationParser.toOffHeapResourcesProvider(configuration);
    final long incrementPerRun = 100L;
    final int numIterations = 50;

    IntStream.iterate(0, operand -> operand + 1).limit(numIterations).forEach(it -> {
      int numThreads = 2;
      ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
      boolean[] addResults = new boolean[2];
      CyclicBarrier cyclicBarrier = new CyclicBarrier(numThreads + 1);
      IntStream.iterate(0, operand -> operand + 1).limit(numThreads).forEach(iteration -> {
        executorService.submit(() -> {
          try {
            cyclicBarrier.await();
            addResults[iteration] = offHeapResourcesProvider.addOffHeapResource(identifier("newOffheap-" + it), incrementPerRun);
            cyclicBarrier.await();
          } catch (InterruptedException | BrokenBarrierException e) {
            throw new RuntimeException(e);
          }
        });
      });

      try {
        cyclicBarrier.await();
        cyclicBarrier.await();
      } catch (InterruptedException | BrokenBarrierException e) {
        throw new RuntimeException(e);
      }

      assertTrue(addResults[0] ^ addResults[1]); // Only one of the add should have succeeded
    });
    assertThat(offHeapResourcesProvider.getTotalConfiguredOffheap(), equalTo(incrementPerRun * numIterations));
  }
}
