/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.port_locking;

import org.junit.Test;

public class GlobalFilePortLockerTest {
  @Test
  public void canTryLock() {
    GlobalFilePortLocker locker = new GlobalFilePortLocker();

    PortLock portLock1 = locker.tryLockPort(2000);
    if (portLock1 != null) {
      portLock1.close();
    }

    PortLock portLock2 = locker.tryLockPort(2000);
    if (portLock2 != null) {
      portLock2.close();
    }
  }
}
