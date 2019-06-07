/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.persistence.sanskrit.file.lock;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.Assert.assertNotNull;

public class LocalLockManagerTest {
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void differentDirectoriesDoNotContend() throws Exception {
    Path directory1 = temporaryFolder.newFolder().toPath();
    Path directory2 = temporaryFolder.newFolder().toPath();

    LocalLockManager lockManager = new LocalLockManager();
    try (CloseLock lock1 = lockManager.lockOrFail(directory1)) {
      try (CloseLock lock2 = lockManager.lockOrFail(directory2)) {
        assertNotNull(lock1);
        assertNotNull(lock2);
      }
    }
  }

  @Test(expected = IOException.class)
  public void sameDirectoryDoesContend() throws Exception {
    Path directory = temporaryFolder.newFolder().toPath();

    LocalLockManager lockManager = new LocalLockManager();
    try (CloseLock lock1 = lockManager.lockOrFail(directory)) {
      assertNotNull(lock1);
      lockManager.lockOrFail(directory);
    }
  }
}
