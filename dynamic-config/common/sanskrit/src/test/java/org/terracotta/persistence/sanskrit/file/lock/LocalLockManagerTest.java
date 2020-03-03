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
