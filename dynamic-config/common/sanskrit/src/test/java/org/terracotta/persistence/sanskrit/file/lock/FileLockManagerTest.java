/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.persistence.sanskrit.file.lock;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertNotNull;

public class FileLockManagerTest {
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void lockDoesNotExist() throws Exception {
    Path directory = temporaryFolder.newFolder().toPath();
    FileLockManager lockManager = new FileLockManager();
    try (CloseLock closeLock = lockManager.lockOrFail(directory)) {
      assertNotNull(closeLock);
    }
  }

  @Test
  public void lockExists() throws Exception {
    Path directory = temporaryFolder.newFolder().toPath();

    Path lockFile = directory.resolve("lock");
    Files.createFile(lockFile);

    FileLockManager lockManager = new FileLockManager();
    try (CloseLock closeLock = lockManager.lockOrFail(directory)) {
      assertNotNull(closeLock);
    }
  }

  @Test(expected = OverlappingFileLockException.class)
  public void locked() throws Exception {
    Path directory = temporaryFolder.newFolder().toPath();
    FileLockManager lockManager = new FileLockManager();
    try (CloseLock closeLock = lockManager.lockOrFail(directory)) {
      assertNotNull(closeLock);
      lockManager.lockOrFail(directory);
    }
  }
}
