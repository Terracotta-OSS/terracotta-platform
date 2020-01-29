/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
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

class DataDirectoriesWithServerName implements DataDirectories {

  private final DataDirectoriesConfigImpl wrapped;
  private final String serverName;
  private final Map<String, FileLocking> fileLockingMap = new HashMap<>();

  DataDirectoriesWithServerName(DataDirectoriesConfigImpl wrapped, String serverName) {
    this.wrapped = wrapped;
    this.serverName = DataDirectoriesConfig.cleanStringForPath(serverName);

    for (String dataRoot : wrapped.getRootIdentifiers()) {
      Path resolved = wrapped.getRoot(dataRoot).resolve(serverName);
      try {
        wrapped.ensureDirectory(resolved);
        lockDirectory(dataRoot, resolved);
      } catch (IOException e) {
        throw new DataDirectoriesConfigurationException("Unable to create data directory: " + resolved, e);
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
      throw new DataDirectoriesConfigurationException("Unable to lock directory " + resolved, e);
    }
    if (fileLock == null) {
      throw new DataDirectoriesConfigurationException("Directory " + resolved + " is already locked. " +
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

  private static class FileLocking {
    private final RandomAccessFile lockFile;
    private final FileLock fileLock;

    private FileLocking(RandomAccessFile lockFile, FileLock fileLock) {
      this.lockFile = lockFile;
      this.fileLock = fileLock;
    }
  }
}
