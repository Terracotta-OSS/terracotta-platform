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
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.terracotta.exception.ConnectionClosedException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class LeaseMaintenanceThreadTest {
  @Spy
  private TestTimeSource timeSource = new TestTimeSource();

  @Mock
  private LeaseMaintainerImpl leaseMaintainer;

  @Before
  public void before() throws Exception {
    TimeSourceProvider.setTimeSource(timeSource);
  }

  @Test
  public void callsRefreshOccasionally() throws Exception {
    when(leaseMaintainer.refreshLease()).thenReturn(2000L);

    LeaseMaintenanceThread leaseMaintenanceThread = new LeaseMaintenanceThread(leaseMaintainer);
    assertEquals("LeaseMaintenanceThread", leaseMaintenanceThread.getName());
    assertTrue(leaseMaintenanceThread.isDaemon());
    leaseMaintenanceThread.start();

    verify(timeSource, timeout(1000L).times(1)).sleep(2000L);
    verify(leaseMaintainer, times(1)).refreshLease();

    timeSource.tickMillis(2000L);

    verify(timeSource, timeout(1000L).times(2)).sleep(2000L);
    verify(leaseMaintainer, times(2)).refreshLease();
  }

  @Test
  public void callsRefreshImmediatelyIfZeroWaitLength() throws Exception {
    when(leaseMaintainer.refreshLease()).thenReturn(1000L, 0L, 1000L);

    LeaseMaintenanceThread leaseMaintenanceThread = new LeaseMaintenanceThread(leaseMaintainer);
    leaseMaintenanceThread.start();

    verify(timeSource, timeout(1000L).times(1)).sleep(1000L);
    verify(leaseMaintainer, times(1)).refreshLease();

    timeSource.tickMillis(1000L);

    verify(timeSource, timeout(1000L).times(2)).sleep(1000L);
    verify(leaseMaintainer, times(3)).refreshLease();
  }

  @Test
  public void closedConnectionKillsThread() throws Exception {
    when(leaseMaintainer.refreshLease()).thenThrow(new ConnectionClosedException("Connection closed"));

    LeaseMaintenanceThread leaseMaintenanceThread = new LeaseMaintenanceThread(leaseMaintainer);
    leaseMaintenanceThread.start();

    leaseMaintenanceThread.join(5000L);

    assertFalse(leaseMaintenanceThread.isAlive());
  }
}
