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
package org.terracotta.lease.service.monitor;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.lease.MockStateDumpCollector;
import org.terracotta.lease.TestTimeSource;
import org.terracotta.lease.service.closer.ClientConnectionCloser;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class LeaseStateTest {
  @Mock
  private ClientConnectionCloser clientConnectionCloser;

  @Mock
  private ClientDescriptor clientDescriptor1;

  @Mock
  private ClientDescriptor clientDescriptor2;

  @Mock
  private ClientDescriptor clientDescriptor3;

  private TestTimeSource timeSource;
  private LeaseState leaseState;

  @Before
  public void before() {
    timeSource = new TestTimeSource();
    leaseState = new LeaseState(timeSource, clientConnectionCloser);
  }

  @Test
  public void checkingTheLeasesWhenThereAreNoLeases() {
    leaseState.checkLeases();
    verifyNoMoreInteractions(clientConnectionCloser);
  }

  @Test
  public void acquireAndExpireLeases() {
    assertTrue(leaseState.acquireLease(clientDescriptor1, 10L));
    assertTrue(leaseState.acquireLease(clientDescriptor2, 20L));
    assertTrue(leaseState.acquireLease(clientDescriptor3, 30L));

    leaseState.checkLeases();
    verifyNoMoreInteractions(clientConnectionCloser);

    timeSource.tickMillis(5L);
    leaseState.checkLeases();
    verifyNoMoreInteractions(clientConnectionCloser);

    timeSource.tickMillis(10L);
    leaseState.checkLeases();
    verify(clientConnectionCloser).closeClientConnection(clientDescriptor1);
    verifyNoMoreInteractions(clientConnectionCloser);

    timeSource.tickMillis(10L);
    leaseState.checkLeases();
    verify(clientConnectionCloser).closeClientConnection(clientDescriptor2);
    verifyNoMoreInteractions(clientConnectionCloser);

    timeSource.tickMillis(10L);
    leaseState.checkLeases();
    verify(clientConnectionCloser).closeClientConnection(clientDescriptor3);
    verifyNoMoreInteractions(clientConnectionCloser);

    assertFalse(leaseState.acquireLease(clientDescriptor1, 10L));
    assertFalse(leaseState.acquireLease(clientDescriptor2, 20L));
    assertFalse(leaseState.acquireLease(clientDescriptor3, 30L));

    leaseState.disconnected(clientDescriptor1);
    leaseState.disconnected(clientDescriptor2);
    leaseState.disconnected(clientDescriptor3);
  }

  @Test
  public void reconnectingSetsTheLeaseForTheClientToALeaseThatDoesNotExpire() {
    assertTrue(leaseState.acquireLease(clientDescriptor1, 10L));
    leaseState.reconnecting(clientDescriptor1);

    timeSource.tickMillis(100L);

    leaseState.checkLeases();
    verifyNoMoreInteractions(clientConnectionCloser);
  }

  @Test
  public void reconnectedSetsTheLeaseBackToALeaseThatCanExpire() {
    assertTrue(leaseState.acquireLease(clientDescriptor1, 10L));
    leaseState.reconnecting(clientDescriptor1);

    timeSource.tickMillis(100L);

    leaseState.reconnected(clientDescriptor1, 50L);

    leaseState.checkLeases();
    verifyNoMoreInteractions(clientConnectionCloser);

    timeSource.tickMillis(30L);

    leaseState.checkLeases();
    verifyNoMoreInteractions(clientConnectionCloser);

    timeSource.tickMillis(30L);

    leaseState.checkLeases();
    verify(clientConnectionCloser).closeClientConnection(clientDescriptor1);
  }

  @Test
  public void testStateDump() {
    MockStateDumpCollector stateDumper = new MockStateDumpCollector();

    when(clientDescriptor1.toString()).thenReturn("client1");
    when(clientDescriptor2.toString()).thenReturn("client2");
    when(clientDescriptor3.toString()).thenReturn("client3");

    assertTrue(leaseState.acquireLease(clientDescriptor1, 10L));
    assertTrue(leaseState.acquireLease(clientDescriptor2, 20L));
    assertTrue(leaseState.acquireLease(clientDescriptor3, 30L));

    timeSource.tickMillis(25L);
    leaseState.checkLeases();

    leaseState.addStateTo(stateDumper);
    assertThat(stateDumper.getMapping("client1"), is("expired"));
    assertThat(stateDumper.getMapping("client2"), is("expired"));
    assertThat(stateDumper.getMapping("client3"), is("valid"));
  }


  @Test
  public void multiThreadedThrashing() throws Exception {
    final ClientDescriptor clientDescriptor = mock(ClientDescriptor.class);
    final AtomicBoolean acquired = new AtomicBoolean();
    final AtomicBoolean disconnected = new AtomicBoolean();
    final AtomicBoolean runningTest = new AtomicBoolean(true);
    final AtomicBoolean stopTest = new AtomicBoolean(false);
    final AtomicLong count = new AtomicLong();
    final AtomicLong disconnectedCount = new AtomicLong();
    final AtomicLong countSinceLastAcquire = new AtomicLong(-1);

    final CyclicBarrier latch = new CyclicBarrier(2, () -> {
      timeSource.tickMillis(5L);

      if (countSinceLastAcquire.get() > 3) {
        assertFalse(acquired.get());
      }

      if (acquired.get()) {
        countSinceLastAcquire.set(0);
      }

      if (disconnected.get()) {
        leaseState.disconnected(clientDescriptor);
        disconnectedCount.incrementAndGet();
        countSinceLastAcquire.set(-1);
      }

      acquired.set(false);
      disconnected.set(false);
      count.incrementAndGet();

      if (countSinceLastAcquire.get() >= 0) {
        countSinceLastAcquire.incrementAndGet();
      }

      if (stopTest.get()) {
        runningTest.set(false);
      }
    });

    Thread acquireThread = new Thread() {
      @Override
      public void run() {
        while (runningTest.get()) {
          if (ThreadLocalRandom.current().nextInt(3) < 1) {
            boolean success = leaseState.acquireLease(clientDescriptor, 10L);
            acquired.set(success);
            disconnected.set(!success);
          }
          await(latch);
        }
      }
    };
    Thread checkThread = new Thread() {
      @Override
      public void run() {
        while (runningTest.get()) {
          leaseState.checkLeases();
          await(latch);
        }
      }
    };

    acquireThread.start();
    checkThread.start();

    Thread.sleep(10_000L);

    stopTest.set(true);

    acquireThread.join();
    checkThread.join();

    // If these things are not true then the test may not have tested very much - perhaps increase the sleep
    // so that the test runs for longer.
    assertTrue(count.get() > 2000);
    assertTrue(disconnectedCount.get() > 1000);
    assertTrue(count.get() - disconnectedCount.get() > 1000);
  }

  private void await(CyclicBarrier latch) {
    try {
      latch.await();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    } catch (BrokenBarrierException e) {
      throw new RuntimeException(e);
    }
  }
}
