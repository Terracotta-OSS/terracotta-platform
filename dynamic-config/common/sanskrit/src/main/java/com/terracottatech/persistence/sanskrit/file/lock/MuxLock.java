/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.persistence.sanskrit.file.lock;

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
