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

import org.terracotta.persistence.sanskrit.Owner;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.terracotta.persistence.sanskrit.Owner.own;

public class MuxLockManager implements LockManager {
  private final List<LockManager> lockManagers;

  public MuxLockManager(LockManager... lockManagers) {
    this(Arrays.asList(lockManagers));
  }

  private MuxLockManager(List<LockManager> lockManagers) {
    this.lockManagers = lockManagers;
  }

  @Override
  public CloseLock lockOrFail(Path path) throws IOException {
    try (Owner<MuxLock, IOException> lockOwner = own(new MuxLock(), IOException.class)) {
      for (LockManager lockManager : lockManagers) {
        CloseLock lock = lockManager.lockOrFail(path);
        lockOwner.borrow().addLock(lock);
      }

      return lockOwner.release();
    }
  }
}
