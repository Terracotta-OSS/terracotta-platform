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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.terracotta.connection.Connection;

import java.io.Closeable;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class CleaningLeaseMaintainerTest {
  @Mock
  private LeaseMaintainer delegate;

  @Mock
  private Connection connection;

  @Mock
  private Closeable thread1;

  @Mock
  private Closeable thread2;

  @Mock
  private Lease lease;

  @BeforeEach
  public void before() {
    initMocks(this);
  }

  @AfterEach
  public void after() {
    verifyNoMoreInteractions(delegate, connection, thread1, thread2);
  }

  @Test
  public void delegatesGetCurrentLease() {
    when(delegate.getCurrentLease()).thenReturn(lease);
    CleaningLeaseMaintainer cleaner = new CleaningLeaseMaintainer(delegate, connection, thread1, thread2);
    assertEquals(lease, cleaner.getCurrentLease());
    verify(delegate).getCurrentLease();
  }

  @Test
  public void delegatesWaitForLease() throws Exception {
    when(delegate.getCurrentLease()).thenReturn(lease);
    CleaningLeaseMaintainer cleaner = new CleaningLeaseMaintainer(delegate, connection, thread1, thread2);
    cleaner.waitForLease();
    verify(delegate).waitForLease();
  }

  @Test
  public void delegatesWaitForLeaseTimeout() throws Exception {
    when(delegate.getCurrentLease()).thenReturn(lease);
    CleaningLeaseMaintainer cleaner = new CleaningLeaseMaintainer(delegate, connection, thread1, thread2);
    cleaner.waitForLease(10, TimeUnit.SECONDS);
    verify(delegate).waitForLease(10, TimeUnit.SECONDS);
  }

  @Test
  public void closeClosesDelegateAndInterruptsThreads() throws Exception {
    when(delegate.getCurrentLease()).thenReturn(lease);
    CleaningLeaseMaintainer cleaner = new CleaningLeaseMaintainer(delegate, connection, thread1, thread2);
    cleaner.close();
    verify(delegate).close();
    verify(thread1).close();
    verify(thread2).close();
  }

  @Test
  public void destroyClosesConnectionAndInterruptsThreads() throws Exception {
    when(delegate.getCurrentLease()).thenReturn(lease);
    CleaningLeaseMaintainer cleaner = new CleaningLeaseMaintainer(delegate, connection, thread1, thread2);
    cleaner.destroy();
    verify(connection).close();
    verify(thread1).close();
    verify(thread2).close();
  }
}
