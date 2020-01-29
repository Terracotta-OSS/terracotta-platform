/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.port_locking;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.Channel;
import java.nio.channels.FileLock;

public class GlobalFilePortLock implements PortLock {
  private final int port;
  private final RandomAccessFile file;
  private final Channel channel;
  private final FileLock lock;

  GlobalFilePortLock(int port, RandomAccessFile file, Channel channel, FileLock lock) {
    this.port = port;
    this.file = file;
    this.channel = channel;
    this.lock = lock;
  }

  @Override
  public int getPort() {
    return port;
  }

  @Override
  public void close() {
    PortLockingException closeError = new PortLockingException("Failed to unlock during close");

    try {
      lock.close();
    } catch (IOException e) {
      closeError.addSuppressed(e);
    }

    try {
      channel.close();
    } catch (IOException e) {
      closeError.addSuppressed(e);
    }

    try {
      file.close();
    } catch (IOException e) {
      closeError.addSuppressed(e);
    }

    if (closeError.getSuppressed().length > 0) {
      throw closeError;
    }
  }
}