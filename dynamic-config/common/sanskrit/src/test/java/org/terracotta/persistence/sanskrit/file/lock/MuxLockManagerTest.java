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
