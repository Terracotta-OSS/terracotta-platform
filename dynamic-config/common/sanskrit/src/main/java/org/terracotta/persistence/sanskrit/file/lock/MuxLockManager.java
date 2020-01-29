/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
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
