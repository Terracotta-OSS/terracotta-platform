/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
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
