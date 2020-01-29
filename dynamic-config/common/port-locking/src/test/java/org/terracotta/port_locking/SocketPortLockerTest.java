/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.port_locking;

import org.junit.Test;

public class SocketPortLockerTest {
  @Test
  public void tryLock() {
    PortLocker locker = new SocketPortLocker();
    PortLock portLock = locker.tryLockPort(2000);
    if (portLock != null) {
      portLock.close();
    }
  }
}
