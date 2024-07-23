/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
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
