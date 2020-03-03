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
