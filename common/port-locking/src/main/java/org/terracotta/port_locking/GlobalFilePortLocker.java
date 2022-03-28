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
package org.terracotta.port_locking;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

import static java.nio.file.attribute.PosixFilePermissions.asFileAttribute;
import static java.nio.file.attribute.PosixFilePermissions.fromString;

public class GlobalFilePortLocker implements PortLocker {
  private static final Logger LOGGER = LoggerFactory.getLogger(GlobalFilePortLocker.class);
  private static final String TEMP_DIRECTORY_PROPERTY = "java.io.tmpdir";
  private static final String PORT_LOCK_FILENAME = "tc-port-lock";
  private static final Set<PosixFilePermission> PERMISSIONS = fromString("rw-rw-rw-");

  private final File portLockFile;

  public GlobalFilePortLocker() {
    String tempDirectory = System.getProperty(TEMP_DIRECTORY_PROPERTY);

    if (tempDirectory == null) {
      throw new PortLockingException("Unable to find the OS temp directory. This is platform dependent, but can be affected by system properties.");
    }

    Path path = Paths.get(tempDirectory);
    Path portLockFilePath = path.resolve(PORT_LOCK_FILENAME);

    try {
      portLockFile = createOrReuse(portLockFilePath);
    } catch (IOException e) {
      throw new PortLockingException("Failed to create port lock file: " + portLockFilePath, e);
    }
  }

  @Override
  public PortLock tryLockPort(int port) {
    try {
      RandomAccessFile file = new RandomAccessFile(portLockFile, "rw");
      FileChannel channel = file.getChannel();
      FileLock fileLock = channel.tryLock(port, 1, false);

      if (fileLock == null) {
        try {
          channel.close();
        } finally {
          file.close();
        }
        return null;
      }

      return new GlobalFilePortLock(port, file, channel, fileLock);
    } catch (IOException e) {
      throw new PortLockingException("Error while trying to lock port", e);
    }
  }

  private static File createOrReuse(Path portLockFilePath) throws IOException {
    File file;
    try {
      try {
        file = Files.createFile(portLockFilePath, asFileAttribute(PERMISSIONS)).toFile();
      } catch (UnsupportedOperationException e) {
        // in case we cannot atomically create and set these permissions / rwx not supported we retry to create normally
        file = Files.createFile(portLockFilePath).toFile();
      }
      LOGGER.info("Created port lock file: {}", file.getAbsolutePath());
    } catch (FileAlreadyExistsException e) {
      if (!Files.isReadable(portLockFilePath)) {
        throw new PortLockingException("File is not readable: " + portLockFilePath, e);
      }
      if (!Files.isWritable(portLockFilePath)) {
        throw new PortLockingException("File is not writable: " + portLockFilePath, e);
      }
      file = portLockFilePath.toFile();
      LOGGER.info("Using existing port lock file: {}", file.getAbsolutePath());
    }
    return file;
  }
}
