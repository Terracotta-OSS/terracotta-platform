/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.port_locking;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class EmptyPortLockTest {
  @Test
  public void getPort() {
    EmptyPortLock lock = new EmptyPortLock(1);
    assertEquals(1, lock.getPort());
  }
}
