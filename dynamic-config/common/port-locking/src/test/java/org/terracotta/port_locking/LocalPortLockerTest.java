/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.port_locking;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class LocalPortLockerTest {
  @Test
  public void portLocking() {
    LocalPortLocker locker = new LocalPortLocker();
    PortLock portLock1 = locker.tryLockPort(1);
    assertNotNull(portLock1);
    assertEquals(1, portLock1.getPort());

    PortLock portLock2 = locker.tryLockPort(1);
    assertNull(portLock2);

    PortLock portLock3 = locker.tryLockPort(2);
    assertNotNull(portLock3);
    assertEquals(2, portLock3.getPort());

    portLock1.close();
    portLock3.close();

    PortLock portLock4 = locker.tryLockPort(1);
    assertNotNull(portLock4);
    assertEquals(1, portLock4.getPort());

    portLock4.close();
  }
}
