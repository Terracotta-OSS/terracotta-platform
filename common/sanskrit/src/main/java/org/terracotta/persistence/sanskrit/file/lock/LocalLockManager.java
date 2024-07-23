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

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class LocalLockManager implements LockManager {
  private static final Set<Path> LOCKS = ConcurrentHashMap.newKeySet();

  public CloseLock lockOrFail(Path path) throws IOException {
    if (LOCKS.add(path)) {
      return new LocalLock(path);
    }

    throw new IOException("Local lock already held: " + path);
  }

  private static class LocalLock implements CloseLock {
    private final Path path;

    public LocalLock(Path path) {
      this.path = path;
    }

    @Override
    public void close() {
      if (!LOCKS.remove(path)) {
        throw new AssertionError("Attempt to unlock when lock not held: " + path);
      }
    }
  }
}
