/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.persistence.sanskrit.file.lock;

import com.terracottatech.persistence.sanskrit.Owner;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Path;

import static com.terracottatech.persistence.sanskrit.Owner.own;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;

public class FileLockManager implements LockManager {
  @Override
  public CloseLock lockOrFail(Path path) throws IOException {
    Path lockPath = path.resolve("lock");
    try (
        Owner<FileChannel, IOException> channelOwner = own(FileChannel.open(lockPath, READ, WRITE, CREATE), IOException.class);
        Owner<FileLock, IOException> fileLockOwner = own(channelOwner.borrow().tryLock(), IOException.class)
    ) {
      if (fileLockOwner.borrow() == null) {
        throw new IOException("File lock already held: " + path);
      }

      return new FileCloseLock(channelOwner.release(), fileLockOwner.release());
    }
  }
}
