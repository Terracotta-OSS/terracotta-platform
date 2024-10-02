/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2025
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
package org.terracotta.config.data_roots;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

final class DataDirsWithServerName implements DataDirs {

  private final DataDirsConfigImpl wrapped;
  private final String serverName;
  private final Map<String, FileLocking> fileLockingMap = new HashMap<>();

  DataDirsWithServerName(DataDirsConfigImpl wrapped, String serverName) {
    this.wrapped = wrapped;
    this.serverName = DataDirsConfig.cleanStringForPath(serverName);

    for (String dataRoot : wrapped.getRootIdentifiers()) {
      Path resolved = wrapped.getRoot(dataRoot).resolve(serverName);
      try {
        wrapped.ensureDirectory(resolved);
        lockDirectory(dataRoot, resolved);
      } catch (IOException e) {
        throw new DataDirsConfigurationException(e.toString(), e);
      }
    }
  }

  private void lockDirectory(String identifier, Path resolved) throws FileNotFoundException {
    RandomAccessFile lockFile = new RandomAccessFile(resolved.resolve(".lock").toFile(), "rw");
    FileLock fileLock;
    try {
      fileLock = lockFile.getChannel().tryLock();
    } catch (Exception e) {
      try {
        lockFile.close();
      } catch (IOException e1) {
        // Ignore
      }
      throw new DataDirsConfigurationException("Unable to lock directory " + resolved, e);
    }
    if (fileLock == null) {
      throw new DataDirsConfigurationException("Directory " + resolved + " is already locked. " +
                                                      "Make sure multiple servers on this host do not end up having equal data directory and server name defined.");
    }

    fileLockingMap.put(identifier, new FileLocking(lockFile, fileLock));
  }

  @Override
  public Path getDataDirectory(String name) {
    return wrapped.getRoot(name).resolve(serverName);
  }

  @Override
  public Optional<String> getPlatformDataDirectoryIdentifier() {
    return wrapped.getPlatformRootIdentifier();
  }

  @Override
  public Set<String> getDataDirectoryNames() {
    return wrapped.getRootIdentifiers();
  }

  @Override
  public void close() throws IOException {
    IOException ioException = null;
    for (FileLocking fileLocking : fileLockingMap.values()) {
      try {
        if (fileLocking.fileLock.isValid()) {
          fileLocking.fileLock.release();
        }
      } catch (IOException e) {
        if (ioException == null) {
          ioException = e;
        } else {
          ioException.addSuppressed(e);
        }
      } finally {
        try {
          fileLocking.lockFile.close();
        } catch (IOException e) {
          if (ioException == null) {
            ioException = e;
          } else {
            ioException.addSuppressed(e);
          }
        }
      }
    }
    if (ioException != null) {
      throw ioException;
    }
  }

  void updateDataDir(String name) {
    Path resolved = wrapped.getRoot(name).resolve(serverName);
    try {
      wrapped.ensureDirectory(resolved);
      lockDirectory(name, resolved);
    } catch (IOException e) {
      throw new DataDirsConfigurationException(e.toString(), e);
    }
  }

  private static class FileLocking {
    private final RandomAccessFile lockFile;
    private final FileLock fileLock;

    private FileLocking(RandomAccessFile lockFile, FileLock fileLock) {
      this.lockFile = lockFile;
      this.fileLock = fileLock;
    }
  }
}
