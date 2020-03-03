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

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class LocalPortLocker implements PortLocker {
  private static final Set<Integer> localPortLocks = ConcurrentHashMap.newKeySet();

  @Override
  public PortLock tryLockPort(int port) {
    boolean added = localPortLocks.add(port);

    if (!added) {
      return null;
    }

    return new LocalPortLock(port);
  }

  private static class LocalPortLock implements PortLock {
    private final int port;

    LocalPortLock(int port) {
      this.port = port;
    }

    @Override
    public int getPort() {
      return port;
    }

    @Override
    public void close() {
      boolean removed = localPortLocks.remove(port);

      if (!removed) {
        throw new AssertionError("Attempted to remove local lock on port " + port + " but it was not present");
      }
    }
  }
}
