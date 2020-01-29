/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.persistence.sanskrit.file.lock;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MuxLockManagerTest {
  @Mock
  private LockManager lockManager1;

  @Mock
  private LockManager lockManager2;

  @Mock
  private CloseLock lock1;

  @Mock
  private CloseLock lock2;

  @Mock
  private Path path;

  @Test
  public void multiLock() throws Exception {
    when(lockManager1.lockOrFail(path)).thenReturn(lock1);
    when(lockManager2.lockOrFail(path)).thenReturn(lock2);

    MuxLockManager lockManager = new MuxLockManager(lockManager1, lockManager2);
    try (CloseLock lock = lockManager.lockOrFail(path)) {
      assertNotNull(lock);
    }

    InOrder inOrder = inOrder(lockManager1, lockManager2, lock1, lock2);

    inOrder.verify(lockManager1).lockOrFail(path);
    inOrder.verify(lockManager2).lockOrFail(path);
    inOrder.verify(lock2).close();
    inOrder.verify(lock1).close();
  }

  @Test
  @SuppressWarnings("try")
  public void lockFailure() throws Exception {
    when(lockManager1.lockOrFail(path)).thenReturn(lock1);
    when(lockManager2.lockOrFail(path)).thenThrow(IOException.class);

    MuxLockManager lockManager = new MuxLockManager(lockManager1, lockManager2);
    try (CloseLock lock = lockManager.lockOrFail(path)) {
      fail("Lock acquisition should have failed");
    } catch (IOException e) {
      // Expected
    }

    InOrder inOrder = inOrder(lockManager1, lockManager2, lock1);

    inOrder.verify(lockManager1).lockOrFail(path);
    inOrder.verify(lockManager2).lockOrFail(path);
    inOrder.verify(lock1).close();
  }
}
