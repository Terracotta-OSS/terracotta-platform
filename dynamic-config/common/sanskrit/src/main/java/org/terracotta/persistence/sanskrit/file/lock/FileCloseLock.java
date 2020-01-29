/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.persistence.sanskrit.file.lock;

import org.terracotta.persistence.sanskrit.Owner;

import java.io.IOException;
import java.nio.channels.Channel;
import java.nio.channels.FileLock;

import static org.terracotta.persistence.sanskrit.Owner.own;

public class FileCloseLock implements CloseLock {
  private final Channel channel;
  private final FileLock fileLock;

  public FileCloseLock(Channel channel, FileLock fileLock) {
    this.channel = channel;
    this.fileLock = fileLock;
  }

  @Override
  @SuppressWarnings("try")
  public void close() throws IOException {
    try (
        Owner<Channel, IOException> channelOwner = own(channel, IOException.class);
        Owner<FileLock, IOException> fileLockOwner = own(fileLock, IOException.class);
    ) {
      // Do nothing - the Java try-with-resources will correctly close both objects.
    }
  }
}
