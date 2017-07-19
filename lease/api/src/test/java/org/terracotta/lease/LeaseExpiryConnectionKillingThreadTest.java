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
import org.terracotta.connection.Connection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class LeaseExpiryConnectionKillingThreadTest {
  @Spy
  private TestTimeSource timeSource = new TestTimeSource();

  @Mock
  private LeaseMaintainerImpl leaseMaintainer;

  @Mock
  private Connection connection;

  private LeaseExpiryConnectionKillingThread thread;

  @Before
  public void before() throws Exception {
    TimeSourceProvider.setTimeSource(timeSource);
    thread = new LeaseExpiryConnectionKillingThread(leaseMaintainer, connection);
  }

  @Test
  public void threadBasics() throws Exception {
    assertEquals("LeaseExpiryConnectionKillingThread", thread.getName());
    assertTrue(thread.isDaemon());
  }

  @Test
  public void whenLeaseIsValidConnectionIsNotClosed() throws Exception {
    when(leaseMaintainer.getCurrentLease()).thenReturn(new LeaseImpl(timeSource, 100L, 200L));
    thread.start();
    verify(timeSource, timeout(1000L).times(1)).sleep(200L);
    verifyNoMoreInteractions(connection);
  }

  @Test
  public void whenLeaseWasNeverValidConnectionIsNotClosed() throws Exception {
    when(leaseMaintainer.getCurrentLease()).thenReturn(new NullLease());
    thread.start();
    verify(timeSource, timeout(1000L).times(1)).sleep(200L);
    verifyNoMoreInteractions(connection);
  }

  @Test
  public void whenLeaseBecomesInvalidConnectionIsClosed() throws Exception {
    when(leaseMaintainer.getCurrentLease()).thenReturn(new LeaseImpl(timeSource, -200L, -100L));
    thread.start();
    thread.join();
    verify(connection).close();
  }

  @Test
  public void whenLeaseBecomesInvalidConnectionIsClosedCopingWithConnectionAlreadyClosed() throws Exception {
    when(leaseMaintainer.getCurrentLease()).thenReturn(new LeaseImpl(timeSource, -200L, -100L));
    doThrow(IllegalStateException.class).when(connection).close();
    thread.start();
    thread.join();
    verify(connection).close();
  }
}
