/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.port_locking;

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
