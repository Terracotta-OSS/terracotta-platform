/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.port_locking;

import java.security.SecureRandom;

public class LockingPortChoosers {
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
    return new LockingPortChooser(
        new RandomPortAllocator(new SecureRandom()),
        new MuxPortLocker(
            new LocalPortLocker(),
            new SocketPortLocker(),
            new GlobalFilePortLocker()
        )
    );
  }
}
