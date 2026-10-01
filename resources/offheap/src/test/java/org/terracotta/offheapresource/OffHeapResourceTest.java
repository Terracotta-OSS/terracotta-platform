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
package org.terracotta.offheapresource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(MockitoJUnitRunner.class)
public class OffHeapResourceTest {
  @Mock
  private Consumer<OffHeapUsageEvent> onThresholdChange;

  @Mock
  private CapacityChangeHandler onCapacityChange;

  private String identifier = "id";

  @Test
  public void testNegativeResourceSize() {
    try {
      new OffHeapResourceImpl(identifier, -1);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      //expected;
    }
  }

  @Test
  public void testZeroSizeResourceIsUseless() {
    OffHeapResource ohr = new OffHeapResourceImpl(identifier, 0);
    assertThat(ohr.reserve(1), is(false));
    assertThat(ohr.available(), is(0L));
  }

  @Test
  public void testAllocationReducesSize() {
    OffHeapResource ohr = new OffHeapResourceImpl(identifier, 20);
    assertThat(ohr.capacity(), is(20L));
    assertThat(ohr.available(), is(20L));
    assertThat(ohr.reserve(10), is(true));
    assertThat(ohr.available(), is(10L));
    assertThat(ohr.capacity(), is(20L));
  }

  @Test
  public void testNegativeAllocationFails() {
    OffHeapResource ohr = new OffHeapResourceImpl(identifier, 20);
    try {
      ohr.reserve(-1);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      //expected
    }
  }

  @Test
  public void testAllocationWhenExhaustedFails() {
    OffHeapResource ohr = new OffHeapResourceImpl(identifier, 20);
    ohr.reserve(20);
    assertThat(ohr.reserve(1), is(false));
    assertThat(ohr.available(), is(0L));
  }

  @Test
  public void testFreeIncreasesSize() {
    OffHeapResource ohr = new OffHeapResourceImpl(identifier, 20);
    ohr.reserve(20);
    assertThat(ohr.available(), is(0L));
    ohr.release(10);
    assertThat(ohr.available(), is(10L));
  }

  @Test
  public void testNegativeFreeFails() {
    OffHeapResource ohr = new OffHeapResourceImpl(identifier, 20);
    ohr.reserve(10);
    try {
      ohr.release(-10);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      //expected
    }
  }

  @Test
  public void testThresholds() {
    OffHeapResourceImpl offHeapResource = new OffHeapResourceImpl(identifier, 10);
    // TODO move to manipulating the logger to be able to do real assertions.
    offHeapResource.reserve(4); // Does not print a log statement
    offHeapResource.reserve(4); // Does print an info log statement
    offHeapResource.reserve(1); // Does print a warn log statement
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSetCapacityNegative() {
    OffHeapResourceImpl ohr = new OffHeapResourceImpl(identifier, 20L, onThresholdChange, onCapacityChange);
    ohr.setCapacity(-1L);
    verifyNoMoreInteractions(onCapacityChange);
  }

  @Test
  public void testSetCapacityBigger() {
    OffHeapResourceImpl ohr = new OffHeapResourceImpl(identifier, 20L, onThresholdChange, onCapacityChange);
    ohr.reserve(14L);
    assertThat(ohr.setCapacity(30L), is(true));
    assertThat(ohr.capacity(), is(30L));
    assertThat(ohr.available(), is(16L));
    verify(onCapacityChange).onCapacityChanged(ohr, 20L, 30L);
  }

  @Test
  public void testSetCapacitySmaller() {
    OffHeapResourceImpl ohr = new OffHeapResourceImpl(identifier, 20L, onThresholdChange, onCapacityChange);
    ohr.reserve(14L);
    assertThat(ohr.setCapacity(16L), is(true));
    assertThat(ohr.capacity(), is(16L));
    assertThat(ohr.available(), is(2L));
    verify(onCapacityChange).onCapacityChanged(ohr, 20L, 16L);
  }

  @Test
  public void testSetCapacityToReserved() {
    OffHeapResourceImpl ohr = new OffHeapResourceImpl(identifier, 20L, onThresholdChange, onCapacityChange);
    ohr.reserve(14L);
    assertThat(ohr.setCapacity(14L), is(true));
    assertThat(ohr.capacity(), is(14L));
    assertThat(ohr.available(), is(0L));
    verify(onCapacityChange).onCapacityChanged(ohr, 20L, 14L);
  }

  @Test
  public void testSetCapacityTooSmall() {
    OffHeapResourceImpl ohr = new OffHeapResourceImpl(identifier, 20L, onThresholdChange, onCapacityChange);
    ohr.reserve(14L);
    assertThat(ohr.setCapacity(13L), is(false));
    assertThat(ohr.capacity(), is(20L));
    assertThat(ohr.available(), is(6L));
    verifyNoMoreInteractions(onCapacityChange);
  }

  @Test
  public void testRisingUsageEventFiresOnThresholdCrossing() {
    OffHeapResourceImpl ohr = new OffHeapResourceImpl(identifier, 100L, onThresholdChange);
    assertThat(ohr.reserve(50L), is(true));

    verify(onThresholdChange, never()).accept(any());

    assertThat(ohr.reserve(30L), is(true));

    ArgumentCaptor<OffHeapUsageEvent> captor = ArgumentCaptor.forClass(OffHeapUsageEvent.class);
    verify(onThresholdChange, times(1)).accept(captor.capture());
    OffHeapUsageEvent event = captor.getValue();
    assertThat(event.getEventType(), is(OffHeapUsageEventType.RISING));
    assertThat(event.getUsed(), is(80L));
    assertThat(event.getAvailable(), is(20L));
    assertThat(event.getTotal(), is(100L));
    assertThat(event.getOccupancy(), is(0.8f));
  }

  @Test
  public void testNoUsageEventRefireWhileStayingAboveThreshold() {
    OffHeapResourceImpl ohr = new OffHeapResourceImpl(identifier, 100L, onThresholdChange);
    assertThat(ohr.reserve(80L), is(true));
    verify(onThresholdChange, times(1)).accept(any());

    assertThat(ohr.reserve(5L), is(true));
    ohr.release(10L);
    verify(onThresholdChange, times(1)).accept(any());
  }

  @Test
  public void testFallingUsageEventFiresWhenCrossingDownBelowThreshold() {
    OffHeapResourceImpl ohr = new OffHeapResourceImpl(identifier, 100L, onThresholdChange);
    assertThat(ohr.reserve(80L), is(true));
    ohr.release(40L);

    ArgumentCaptor<OffHeapUsageEvent> captor = ArgumentCaptor.forClass(OffHeapUsageEvent.class);
    verify(onThresholdChange, times(2)).accept(captor.capture());
    List<OffHeapUsageEvent> events = captor.getAllValues();
    assertThat(events.get(0).getEventType(), is(OffHeapUsageEventType.RISING));
    assertThat(events.get(1).getEventType(), is(OffHeapUsageEventType.FALLING));
    assertThat(events.get(1).getUsed(), is(40L));
    assertThat(events.get(1).getAvailable(), is(60L));
    assertThat(events.get(1).getTotal(), is(100L));
  }

  @Test
  public void testUsageEventsFireExactlyOncePerThresholdCrossingCycle() {
    OffHeapResourceImpl ohr = new OffHeapResourceImpl(identifier, 100L, onThresholdChange);

    assertThat(ohr.reserve(80L), is(true));
    verify(onThresholdChange, times(1)).accept(any());

    ohr.release(40L);
    verify(onThresholdChange, times(2)).accept(any());

    ohr.release(10L);
    verify(onThresholdChange, times(2)).accept(any());

    assertThat(ohr.reserve(50L), is(true));
    ArgumentCaptor<OffHeapUsageEvent> captor = ArgumentCaptor.forClass(OffHeapUsageEvent.class);
    verify(onThresholdChange, times(3)).accept(captor.capture());
    assertThat(captor.getAllValues().get(2).getEventType(), is(OffHeapUsageEventType.RISING));
    assertThat(captor.getAllValues().get(2).getUsed(), is(80L));
  }

  @Test
  public void testBothThresholdsFireAtHighOccupancy() {
    OffHeapResourceImpl ohr = new OffHeapResourceImpl(identifier, 100L, onThresholdChange);
    assertThat(ohr.reserve(95L), is(true));

    ArgumentCaptor<OffHeapUsageEvent> captor = ArgumentCaptor.forClass(OffHeapUsageEvent.class);
    verify(onThresholdChange, times(2)).accept(captor.capture());
    List<OffHeapUsageEvent> events = captor.getAllValues();
    assertThat(events.get(0).getEventType(), is(OffHeapUsageEventType.RISING));
    assertThat(events.get(1).getEventType(), is(OffHeapUsageEventType.RISING));
    assertThat(events.get(0).getUsed(), is(95L));
    assertThat(events.get(1).getUsed(), is(95L));
  }

  @Test
  public void testFallingEventsForBothFiredThresholds() {
    OffHeapResourceImpl ohr = new OffHeapResourceImpl(identifier, 100L, onThresholdChange);
    assertThat(ohr.reserve(95L), is(true));
    ohr.release(90L);

    ArgumentCaptor<OffHeapUsageEvent> captor = ArgumentCaptor.forClass(OffHeapUsageEvent.class);
    verify(onThresholdChange, times(4)).accept(captor.capture());
    List<OffHeapUsageEvent> events = captor.getAllValues();
    assertThat(events.get(2).getEventType(), is(OffHeapUsageEventType.FALLING));
    assertThat(events.get(3).getEventType(), is(OffHeapUsageEventType.FALLING));
    assertThat(events.get(2).getUsed(), is(5L));
    assertThat(events.get(3).getUsed(), is(5L));
  }

  @Test
  public void testAddedUsageListenerReceivesEventsUntilRemoved() {
    OffHeapResourceImpl ohr = new OffHeapResourceImpl(identifier, 100L, onThresholdChange);
    assertThat(ohr.reserve(40L), is(true));

    UUID listenerId = UUID.randomUUID();
    ohr.addUsageListener(listenerId, 0.5f, onThresholdChange);
    verify(onThresholdChange, never()).accept(any());

    assertThat(ohr.reserve(20L), is(true));
    ArgumentCaptor<OffHeapUsageEvent> captor = ArgumentCaptor.forClass(OffHeapUsageEvent.class);
    verify(onThresholdChange, times(1)).accept(captor.capture());
    assertThat(captor.getValue().getUsed(), is(60L));
    assertThat(captor.getValue().getEventType(), is(OffHeapUsageEventType.RISING));

    ohr.removeUsageListener(listenerId);

    ohr.release(50L);
    assertThat(ohr.reserve(20L), is(true));
    verify(onThresholdChange, times(1)).accept(any());
  }

  @Test
  public void testAddedUsageListenerFiresImmediatelyWhenAlreadyAboveThreshold() {
    OffHeapResourceImpl ohr = new OffHeapResourceImpl(identifier, 100L, onThresholdChange);
    assertThat(ohr.reserve(60L), is(true));
    verify(onThresholdChange, never()).accept(any());

    ohr.addUsageListener(UUID.randomUUID(), 0.5f, onThresholdChange);

    ArgumentCaptor<OffHeapUsageEvent> captor = ArgumentCaptor.forClass(OffHeapUsageEvent.class);
    verify(onThresholdChange, times(1)).accept(captor.capture());
    assertThat(captor.getValue().getEventType(), is(OffHeapUsageEventType.RISING));
    assertThat(captor.getValue().getUsed(), is(60L));
  }

  @Test
  public void testRemoveUsageListenerStopsItsEvents() {
    OffHeapResourceImpl ohr = new OffHeapResourceImpl(identifier, 100L, onThresholdChange);
    UUID listenerId = UUID.randomUUID();
    ohr.addUsageListener(listenerId, 0.5f, onThresholdChange);
    ohr.removeUsageListener(listenerId);

    assertThat(ohr.reserve(60L), is(true));
    verify(onThresholdChange, never()).accept(any());
  }

  @Test
  public void testRemoveUnknownUsageListenerThrows() {
    OffHeapResourceImpl ohr = new OffHeapResourceImpl(identifier, 100L, onThresholdChange);
    try {
      ohr.removeUsageListener(UUID.randomUUID());
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      //expected;
    }
  }

  @Test
  public void testSetCapacityShrinkFiresRisingEvent() {
    OffHeapResourceImpl ohr = new OffHeapResourceImpl(identifier, 100L, onThresholdChange, onCapacityChange);
    assertThat(ohr.reserve(60L), is(true));
    verify(onThresholdChange, never()).accept(any());

    assertThat(ohr.setCapacity(70L), is(true));

    ArgumentCaptor<OffHeapUsageEvent> captor = ArgumentCaptor.forClass(OffHeapUsageEvent.class);
    verify(onThresholdChange, times(1)).accept(captor.capture());
    assertThat(captor.getValue().getEventType(), is(OffHeapUsageEventType.RISING));
    verify(onCapacityChange).onCapacityChanged(ohr, 100L, 70L);
  }

  @Test
  public void testSetCapacityGrowFiresFallingEventForFiredListener() {
    OffHeapResourceImpl ohr = new OffHeapResourceImpl(identifier, 100L, onThresholdChange, onCapacityChange);
    assertThat(ohr.reserve(80L), is(true));
    verify(onThresholdChange, times(1)).accept(any());

    assertThat(ohr.setCapacity(200L), is(true));

    ArgumentCaptor<OffHeapUsageEvent> captor = ArgumentCaptor.forClass(OffHeapUsageEvent.class);
    verify(onThresholdChange, times(2)).accept(captor.capture());
    assertThat(captor.getAllValues().get(1).getEventType(), is(OffHeapUsageEventType.FALLING));
    assertThat(captor.getAllValues().get(1).getUsed(), is(80L));
    assertThat(captor.getAllValues().get(1).getTotal(), is(200L));
    verify(onCapacityChange).onCapacityChanged(ohr, 100L, 200L);
  }

  @Test
  public void testReserveZeroSucceedsAndProducesNoEvent() {
    OffHeapResourceImpl ohr = new OffHeapResourceImpl(identifier, 100L, onThresholdChange);
    assertThat(ohr.reserve(0L), is(true));
    assertThat(ohr.available(), is(100L));
    verifyNoMoreInteractions(onThresholdChange);
  }

  @Test
  public void testConcurrentReserveReleaseReturnsPoolToFull() throws Exception {
    final OffHeapResourceImpl ohr = new OffHeapResourceImpl(identifier, 40_000L, onThresholdChange);
    final int numThreads = 8;
    final int iterationsPerThread = 5_000;
    ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
    final CountDownLatch start = new CountDownLatch(1);
    final CountDownLatch done = new CountDownLatch(numThreads);
    for (int i = 0; i < numThreads; i++) {
      executorService.submit(() -> {
        try {
          start.await();
          for (int j = 0; j < iterationsPerThread; j++) {
            if (ohr.reserve(1L)) {
              ohr.release(1L);
            }
          }
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        } finally {
          done.countDown();
        }
      });
    }
    start.countDown();
    if (!done.await(60, TimeUnit.SECONDS)) {
      fail("Timed out waiting for concurrent reservers");
    }
    executorService.shutdown();

    assertThat(ohr.available(), is(40_000L));
    assertThat(ohr.capacity(), is(40_000L));
  }

  @Test
  public void testOverReleaseFloorsUsageAtZero() {
    OffHeapResourceImpl ohr = new OffHeapResourceImpl(identifier, 100L, onThresholdChange);
    assertThat(ohr.reserve(10L), is(true));

    // over-release cannot drive usage negative: it floors at zero. (The previous
    // single-state implementation would have left used == -10 here.)
    ohr.release(20L);

    assertThat(ohr.available(), is(100L));
    assertThat(ohr.reserve(100L), is(true));
  }

  @Test
  public void testThrowingUsageConsumerDoesNotCorruptSubsequentEvents() {
    OffHeapResourceImpl ohr = new OffHeapResourceImpl(identifier, 100L, onThresholdChange);
    ohr.addUsageListener(UUID.randomUUID(), 0.5f, (event) -> {
      throw new IllegalStateException("listener consumer failure");
    });

    // 60% crosses the throwing 0.5 listener: the event is dispatched, the
    // consumer throws, and the failure propagates out of reserve() exactly as it
    // did from the previous implementation's stateUpdated()
    try {
      ohr.reserve(60L);
      fail("Expected IllegalStateException from throwing listener consumer");
    } catch (IllegalStateException e) {
      //expected;
    }

    // 80% crosses the built-in 0.75 listener: it must still fire despite the
    // earlier failure - the firing flags and scan-gating watermarks were
    // updated under the lock before any consumer ran
    assertThat(ohr.reserve(20L), is(true));
    verify(onThresholdChange, times(1)).accept(any());

    // 30% falls below both thresholds: the built-in listener must receive its
    // FALLING event even though the throwing listener's consumer fails again
    // during the same dispatch batch
    try {
      ohr.release(50L);
      fail("Expected IllegalStateException from throwing listener consumer");
    } catch (IllegalStateException e) {
      //expected;
    }
    verify(onThresholdChange, times(2)).accept(any());
  }

  @Test
  public void testConcurrentCapacityChangesPreserveUsageInvariant() throws Exception {
    final OffHeapResourceImpl ohr = new OffHeapResourceImpl(identifier, 10_000L, onThresholdChange);
    final int numReservers = 8;
    final int numAdmins = 2;
    final int iterations = 2_000;
    final Queue<Throwable> failures = new ConcurrentLinkedQueue<>();
    ExecutorService executorService = Executors.newFixedThreadPool(numReservers + numAdmins);
    final CountDownLatch start = new CountDownLatch(1);
    final CountDownLatch done = new CountDownLatch(numReservers + numAdmins);
    for (int i = 0; i < numReservers; i++) {
      final int seed = i;
      executorService.submit(() -> {
        try {
          start.await();
          for (int j = 0; j < iterations; j++) {
            long amount = 1L + ((seed + j) % 50L);
            if (ohr.reserve(amount)) {
              ohr.release(amount);
            }
            if (ohr.available() < 0) {
              throw new AssertionError("negative availability observed: " + ohr.available());
            }
          }
        } catch (Throwable t) {
          failures.add(t);
        } finally {
          done.countDown();
        }
      });
    }
    for (int i = 0; i < numAdmins; i++) {
      executorService.submit(() -> {
        try {
          start.await();
          for (int j = 0; j < iterations; j++) {
            if (j % 4 == 0) {
              // usually refused: reservers hold up to 400 bytes concurrently
              ohr.setCapacity(10L);
            } else {
              ohr.setCapacity(10_000L);
            }
            if (ohr.available() < 0) {
              throw new AssertionError("negative availability observed: " + ohr.available());
            }
          }
        } catch (Throwable t) {
          failures.add(t);
        } finally {
          done.countDown();
        }
      });
    }
    start.countDown();
    if (!done.await(120, TimeUnit.SECONDS)) {
      fail("Timed out waiting for concurrent reservers and capacity changes");
    }
    executorService.shutdown();
    if (!failures.isEmpty()) {
      throw new AssertionError("concurrent task failed", failures.poll());
    }

    // every reservation was released, so the full (restored) capacity must be usable
    assertThat(ohr.setCapacity(10_000L), is(true));
    assertThat(ohr.capacity(), is(10_000L));
    assertThat(ohr.available(), is(10_000L));
  }
}
