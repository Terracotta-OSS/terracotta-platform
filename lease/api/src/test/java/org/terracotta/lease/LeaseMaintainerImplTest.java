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
package org.terracotta.lease;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Timer;
import java.util.TimerTask;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class LeaseMaintainerImplTest {
  @Mock
  private LeaseAcquirer leaseAcquirer;

  private TestTimeSource timeSource = new TestTimeSource();

  private DelayedLeaseAcquirer delayedLeaseAcquirer;

  private LeaseMaintainerImpl leaseMaintainer;

  @Before
  public void before() throws Exception {
    TimeSourceProvider.setTimeSource(timeSource);
    when(leaseAcquirer.acquireLease()).thenReturn(6000L);
    delayedLeaseAcquirer = new DelayedLeaseAcquirer(leaseAcquirer);
    leaseMaintainer = new LeaseMaintainerImpl(delayedLeaseAcquirer);
  }

  @Test
  public void closeClosesEntity() throws Exception {
    leaseMaintainer.close();
    verify(leaseAcquirer).close();
  }

  @Test
  public void currentLeaseBeforeGettingALeaseGivesInvalidLease() throws Exception {
    Lease lease = leaseMaintainer.getCurrentLease();
    assertFalse(lease.isValidAndContiguous(lease));
  }

  @Test
  public void currentLeaseAfterGettingALeaseGivesValidLease() throws Exception {
    refreshLease(leaseMaintainer, 0L, 2000L);
    Lease lease = leaseMaintainer.getCurrentLease();
    assertTrue(lease.isValidAndContiguous(lease));

    timeSource.tickMillis(5999L);
    assertTrue(lease.isValidAndContiguous(lease));

    timeSource.tickMillis(2L);
    assertFalse(lease.isValidAndContiguous(lease));
  }

  @Test
  public void periodicRenewal() throws Exception {
    refreshLease(leaseMaintainer, 0L, 2000L);
    Lease firstLease = leaseMaintainer.getCurrentLease();
    Lease previousLease = firstLease;
    for (int i = 0; i < 10; i++) {
      timeSource.tickMillis(2000L);

      refreshLease(leaseMaintainer, 0L, 2000L);
      Lease nextLease = leaseMaintainer.getCurrentLease();

      assertFalse(nextLease == previousLease);
      assertTrue(nextLease.isValidAndContiguous(previousLease));

      previousLease = nextLease;
    }

    assertTrue(previousLease.isValidAndContiguous(firstLease));
  }

  @Test
  public void gapsInLeases() throws Exception {
    refreshLease(leaseMaintainer, 0L, 2000L);
    Lease lease1 = leaseMaintainer.getCurrentLease();

    timeSource.tickMillis(7000L);

    refreshLease(leaseMaintainer, 0L, 2000L);
    Lease lease2 = leaseMaintainer.getCurrentLease();

    assertFalse(lease2 == lease1);
    assertFalse(lease2.isValidAndContiguous(lease1));
    assertTrue(lease2.isValidAndContiguous(lease2));
  }

  @Test
  public void slowResponseFromEntity() throws Exception {
    refreshLease(leaseMaintainer, 0L, 2000L);
    Lease lease1 = leaseMaintainer.getCurrentLease();

    timeSource.tickMillis(2000L);

    refreshLease(leaseMaintainer, 1000L, 1000L);
    Lease lease2 = leaseMaintainer.getCurrentLease();

    assertFalse(lease2 == lease1);
    assertTrue(lease2.isValidAndContiguous(lease1));
  }

  @Test
  public void responseFromEntityTooSlow() throws Exception {
    refreshLease(leaseMaintainer, 0L, 2000L);
    Lease lease1 = leaseMaintainer.getCurrentLease();

    timeSource.tickMillis(2000L);

    refreshLease(leaseMaintainer, 5000L, 0L);
    Lease lease2 = leaseMaintainer.getCurrentLease();

    assertFalse(lease2 == lease1);
    assertFalse(lease2.isValidAndContiguous(lease1));
    assertTrue(lease2.isValidAndContiguous(lease2));
  }

  @Test
  public void responseFromEntitySoSlowTheLeaseIsNotValid() throws Exception {
    refreshLease(leaseMaintainer, 0L, 2000L);
    Lease lease1 = leaseMaintainer.getCurrentLease();

    timeSource.tickMillis(2000L);

    refreshLease(leaseMaintainer, 7000L, 0L);
    Lease lease2 = leaseMaintainer.getCurrentLease();

    assertFalse(lease2 == lease1);
    assertFalse(lease2.isValidAndContiguous(lease1));
    assertFalse(lease2.isValidAndContiguous(lease2));
  }

  @Test(expected = InterruptedException.class)
  public void waitForLeaseWithoutALease() throws Exception {
    ThreadInterrupter.interruptIn(300L);
    leaseMaintainer.waitForLease();
  }

  @Test
  public void waitForLeaseWithLease() throws Exception {
    refreshLease(leaseMaintainer, 0L, 2000L);
    leaseMaintainer.waitForLease();
  }

  private void refreshLease(LeaseMaintainerImpl leaseMaintainer, long delay, long expectedWaitLength) throws Exception {
    delayedLeaseAcquirer.setDelay(delay);
    long waitLength = leaseMaintainer.refreshLease();
    assertEquals(expectedWaitLength, waitLength);
  }

  private class DelayedLeaseAcquirer implements LeaseAcquirer {
    private final LeaseAcquirer delegate;
    private long delay;

    public DelayedLeaseAcquirer(LeaseAcquirer delegate) {
      this.delegate = delegate;
    }

    public void setDelay(long delay) {
      this.delay = delay;
    }

    @Override
    public long acquireLease() throws LeaseException, InterruptedException {
      timeSource.tickMillis(delay);
      return delegate.acquireLease();
    }

    @Override
    public void close() {
      delegate.close();
    }
  }

  private static class ThreadInterrupter extends Thread {
    private final Thread thread;
    private final long delay;

    public static void interruptIn(long delay) {
      new ThreadInterrupter(Thread.currentThread(), delay).start();
    }

    private ThreadInterrupter(Thread thread, long delay) {
      super("ThreadInterrupter");
      this.thread = thread;
      this.delay = delay;
    }

    @Override
    public void run() {
      try {
        Thread.sleep(delay);
      } catch (InterruptedException e) {
        // Never mind
      }

      thread.interrupt();
    }
  }
}
