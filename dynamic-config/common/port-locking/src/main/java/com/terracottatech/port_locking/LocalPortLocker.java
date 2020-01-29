/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.port_locking;

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
