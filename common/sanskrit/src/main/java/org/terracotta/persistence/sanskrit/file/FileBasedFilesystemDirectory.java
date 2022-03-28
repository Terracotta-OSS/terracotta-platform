/*
 * Copyright Terracotta, Inc.
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

    if (!path.toFile().exists()) {
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
    org.terracotta.utilities.io.Files.deleteIfExists(path);
  }

  @Override
  public void backup(String filename) throws IOException {
    String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd.HHmmss.SSS"));
    Path path = directory.resolve(filename);
    if (path.toFile().exists()) {
      Path dest = path.resolveSibling("backup-" + filename + "-" + time);
      org.terracotta.utilities.io.Files.relocate(path, dest);
    }
  }
}
