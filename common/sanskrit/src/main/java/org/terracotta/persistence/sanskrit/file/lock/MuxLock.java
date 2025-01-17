/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
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
import java.util.ArrayDeque;
import java.util.Deque;

public class MuxLock implements CloseLock {
  private final Deque<CloseLock> locks;

  public MuxLock() {
    this.locks = new ArrayDeque<>();
  }

  public void addLock(CloseLock lock) {
    locks.push(lock);
  }

  @Override
  public void close() throws IOException {
    IOException error = new IOException("Failed to close locks");

    while (true) {
      CloseLock lock = locks.peek();

      if (lock == null) {
        break;
      }

      try {
        lock.close();
      } catch (IOException e) {
        error.addSuppressed(e);
      }

      locks.pop();
    }

    if (error.getSuppressed().length > 0) {
      throw error;
    }
  }
}
