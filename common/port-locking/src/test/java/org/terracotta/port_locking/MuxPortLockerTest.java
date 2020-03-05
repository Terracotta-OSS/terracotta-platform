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
package org.terracotta.port_locking;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MuxPortLockerTest {
  @Mock
  private PortLocker locker1;

  @Mock
  private PortLocker locker2;

  @Mock
  private PortLock lock1;

  @Mock
  private PortLock lock2;

  @Test
  public void usesMultipleLockers() {
    when(locker1.tryLockPort(1)).thenReturn(lock1);
    when(locker2.tryLockPort(1)).thenReturn(lock2);

    PortLocker locker = new MuxPortLocker(locker1, locker2);

    PortLock portLock = locker.tryLockPort(1);
    assertNotNull(portLock);
    assertEquals(1, portLock.getPort());

    portLock.close();

    verify(lock1).close();
    verify(lock2).close();
  }

  @Test
  public void stopAtFirstLockFailure() {
    PortLocker locker = new MuxPortLocker(locker1, locker2);

    PortLock portLock = locker.tryLockPort(1);
    assertNull(portLock);
  }

  @Test
  public void stopAtSecondLockFailure() {
    when(locker1.tryLockPort(1)).thenReturn(lock1);

    PortLocker locker = new MuxPortLocker(locker1, locker2);

    PortLock portLock = locker.tryLockPort(1);
    assertNull(portLock);

    verify(lock1).close();
  }
}
