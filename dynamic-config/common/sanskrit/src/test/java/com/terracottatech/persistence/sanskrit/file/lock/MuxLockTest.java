/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.persistence.sanskrit.file.lock;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.inOrder;

@RunWith(MockitoJUnitRunner.class)
public class MuxLockTest {
  @Mock
  private CloseLock lock1;

  @Mock
  private CloseLock lock2;

  @Test
  public void closesAll() throws Exception {
    MuxLock lock = new MuxLock();
    lock.addLock(lock1);
    lock.addLock(lock2);

    lock.close();

    InOrder inOrder = inOrder(lock1, lock2);

    inOrder.verify(lock2).close();
    inOrder.verify(lock1).close();
  }
}
