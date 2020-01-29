/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.persistence.sanskrit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.inOrder;

@RunWith(MockitoJUnitRunner.class)
public class LockReleasingSanskritTest {
  @Mock
  private Sanskrit underlying;

  @Mock
  private DirectoryLock lock;

  @Test
  public void releasesLocks() throws Exception {
    LockReleasingSanskrit sanskrit = new LockReleasingSanskrit(underlying, lock);
    sanskrit.close();

    InOrder inOrder = inOrder(underlying, lock);

    inOrder.verify(underlying).close();
    inOrder.verify(lock).close();
  }
}
