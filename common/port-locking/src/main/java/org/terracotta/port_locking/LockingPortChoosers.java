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

import java.security.SecureRandom;

public class LockingPortChoosers {

  private static final LockingPortChooser SINGLETON = new LockingPortChooser(
      new RandomPortAllocator(new SecureRandom()),
      new MuxPortLocker(
          new LocalPortLocker(),
          new SocketPortLocker(),
          new GlobalFilePortLocker()
      )
  );

  /**
   * Returns a LockingPortChooser that works by acquiring a lock for a port by creating a FileLock on the corresponding
   * byte in an agreed upon file. The FileLocks are created by GlobalFilePortLocker.
   * Because FileLocks are held by the whole JVM, we also need a system of locking within this JVM, which is handled by
   * the LocalPortLocker.
   * In the SocketPortLocker, we also check that the socket is available.
   *
   * @return the LockingPortChooser that can generate ports that should be free to use as long as the lock is held
   */
  public static LockingPortChooser getFileLockingPortChooser() {
    return SINGLETON;
  }
}
