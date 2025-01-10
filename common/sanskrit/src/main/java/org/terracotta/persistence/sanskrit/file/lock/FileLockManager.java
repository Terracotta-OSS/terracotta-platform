/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
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
package org.terracotta.persistence.sanskrit.file.lock;

import org.terracotta.persistence.sanskrit.Owner;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Path;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;
import static org.terracotta.persistence.sanskrit.Owner.own;

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
    } catch (OverlappingFileLockException over) {
      throw new IOException("File lock already held: " + path);
    }
  }
}
