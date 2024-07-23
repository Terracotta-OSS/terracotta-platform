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
