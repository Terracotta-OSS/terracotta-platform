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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.terracotta.lease.TestTimeSource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class LeaseMonitorThreadTest {
  @Spy
  private TestTimeSource timeSource = new TestTimeSource();

  @Test
  public void callsCheckLeasesPeriodically() throws Exception {
    LeaseState leaseState = mock(LeaseState.class);
    LeaseMonitorThread leaseMonitorThread = new LeaseMonitorThread(timeSource, leaseState);
    assertEquals("LeaseMonitorThread", leaseMonitorThread.getName());
    assertTrue(leaseMonitorThread.isDaemon());
    leaseMonitorThread.start();

    verify(timeSource, timeout(10_000L).times(1)).sleep(200L);
    verify(leaseState, times(1)).checkLeases();

    timeSource.tickMillis(200L);

    verify(timeSource, timeout(10_000L).times(2)).sleep(200L);
    verify(leaseState, times(2)).checkLeases();

    leaseMonitorThread.interrupt();
    leaseMonitorThread.join(10_000L);
    assertFalse(leaseMonitorThread.isAlive());
  }
}
