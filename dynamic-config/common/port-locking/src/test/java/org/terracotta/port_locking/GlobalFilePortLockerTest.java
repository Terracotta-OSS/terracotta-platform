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
