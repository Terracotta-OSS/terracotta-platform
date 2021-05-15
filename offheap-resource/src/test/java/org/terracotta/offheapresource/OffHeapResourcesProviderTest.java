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
import org.terracotta.common.struct.Measure;
import org.terracotta.common.struct.MemoryUnit;
import org.terracotta.management.service.monitoring.EntityManagementRegistry;
import org.terracotta.management.service.monitoring.EntityMonitoringService;
import org.terracotta.offheapresource.management.OffHeapResourceBinding;
import org.terracotta.statistics.StatisticsManager;
import org.terracotta.statistics.ValueStatistic;

import java.math.BigInteger;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.terracotta.offheapresource.OffHeapResourceIdentifier.identifier;

public class OffHeapResourcesProviderTest {

  @SuppressWarnings("unchecked")
  @Test
  public void testObserverExposed() {
    OffHeapResourcesProvider provider = new OffHeapResourcesProvider(singletonMap("foo", Measure.of(2, MemoryUnit.MB)));
    OffHeapResource offHeapResource = provider.getOffHeapResource(identifier("foo"));
    assertThat(offHeapResource.available(), equalTo(2L * 1024 * 1024));

    assertThat(StatisticsManager.nodeFor(offHeapResource).getChildren().size(), equalTo(1));
    ValueStatistic<Long> valueStatistic = (ValueStatistic<Long>) StatisticsManager.nodeFor(offHeapResource).getChildren().iterator().next().getContext().attributes().get("this");
    assertThat(valueStatistic.value(), equalTo(0L));
  }

  @Test
  public void testInitializeWithValidConfig() {
    OffHeapResourcesProvider provider = new OffHeapResourcesProvider(singletonMap("foo", Measure.of(2, MemoryUnit.MB)));
    assertThat(provider.getOffHeapResource(identifier("foo")), notNullValue());
    assertThat(provider.getOffHeapResource(identifier("foo")).available(), is(2L * 1024 * 1024));
  }

  @Test
  public void testNullReturnOnInvalidResource() {
    OffHeapResourcesProvider provider = new OffHeapResourcesProvider(singletonMap("foo", Measure.of(2, MemoryUnit.MB)));
    assertThat(provider.getOffHeapResource(identifier("bar")), nullValue());
  }

  @Test(expected = ArithmeticException.class)
  public void testResourceTooBig() {
    new OffHeapResourcesProvider(singletonMap("foo", Measure.of(BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE), MemoryUnit.B)));
  }

  @Test
  public void testResourceMax() {
    new OffHeapResourcesProvider(singletonMap("foo", Measure.of(BigInteger.valueOf(Long.MAX_VALUE), MemoryUnit.B)));
  }

  @Test
  public void testResourceAddition_ok() {
    EntityManagementRegistry registry = mock(EntityManagementRegistry.class);
    EntityMonitoringService entityMonitoringService = mock(EntityMonitoringService.class);
    when(registry.getMonitoringService()).thenReturn(entityMonitoringService);
    when(entityMonitoringService.getConsumerId()).thenReturn(1L);
    OffHeapResourcesProvider offHeapResourcesProvider = new OffHeapResourcesProvider(emptyMap());
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
    OffHeapResourcesProvider offHeapResourcesProvider = new OffHeapResourcesProvider(emptyMap());
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
    OffHeapResourcesProvider offHeapResourcesProvider = new OffHeapResourcesProvider(emptyMap());
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
    OffHeapResourcesProvider offHeapResourcesProvider = new OffHeapResourcesProvider(emptyMap());
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
