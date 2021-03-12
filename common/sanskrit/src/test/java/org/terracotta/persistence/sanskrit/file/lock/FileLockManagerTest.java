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
package org.terracotta.persistence.sanskrit.file.lock;

import java.io.IOException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

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

  @Test(expected = IOException.class)
  public void locked() throws Exception {
    Path directory = temporaryFolder.newFolder().toPath();
    FileLockManager lockManager = new FileLockManager();
    try (CloseLock closeLock = lockManager.lockOrFail(directory)) {
      assertNotNull(closeLock);
      lockManager.lockOrFail(directory);
    }
  }
}
