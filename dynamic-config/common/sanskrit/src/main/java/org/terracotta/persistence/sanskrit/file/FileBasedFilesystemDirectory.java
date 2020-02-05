/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.persistence.sanskrit.file;

import org.terracotta.persistence.sanskrit.DirectoryLock;
import org.terracotta.persistence.sanskrit.FileData;
import org.terracotta.persistence.sanskrit.FilesystemDirectory;
import org.terracotta.persistence.sanskrit.file.lock.CloseLock;
import org.terracotta.persistence.sanskrit.file.lock.FileLockManager;
import org.terracotta.persistence.sanskrit.file.lock.LocalLockManager;
import org.terracotta.persistence.sanskrit.file.lock.LockManager;
import org.terracotta.persistence.sanskrit.file.lock.MuxLockManager;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;

/**
 * An implementation of FilesystemDirectory that uses the filesystem - i.e. the 'real' implementation.
 */
public class FileBasedFilesystemDirectory implements FilesystemDirectory {
  private static final LockManager LOCK_MANAGER = new MuxLockManager(
      new LocalLockManager(),
      new FileLockManager()
  );

  private final Path directory;

  public FileBasedFilesystemDirectory(Path directory) {
    this.directory = directory;
  }

  @Override
  public DirectoryLock lock() throws IOException {
    CloseLock lock = LOCK_MANAGER.lockOrFail(directory);
    return lock::close;
  }

  @Override
  public FileData create(String filename, boolean canExist) throws IOException {
    Path path = directory.resolve(filename);
    StandardOpenOption createOption = canExist ? CREATE : CREATE_NEW;
    FileChannel channel = FileChannel.open(path, READ, WRITE, createOption);
    return new FileChannelFileData(channel);
  }

  @Override
  public FileData getFileData(String filename) throws IOException {
    Path path = directory.resolve(filename);

    if (!Files.exists(path)) {
      return null;
    }

    if (Files.isDirectory(path)) {
      throw new IOException("Attempted to read file but it is a directory: " + filename);
    }

    FileChannel channel = FileChannel.open(path, READ, WRITE);
    return new FileChannelFileData(channel);
  }

  @Override
  public void delete(String filename) throws IOException {
    Path path = directory.resolve(filename);
    Files.deleteIfExists(path);
  }

  @Override
  public void backup(String filename) throws IOException {
    String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd.HHmmss"));
    Path path = directory.resolve(filename);
    Path dest = path.resolveSibling("backup-" + filename + "-" + time);
    Files.move(path, dest);
  }
}
