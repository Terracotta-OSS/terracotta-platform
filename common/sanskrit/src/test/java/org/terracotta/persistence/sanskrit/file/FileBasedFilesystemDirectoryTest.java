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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.terracotta.persistence.sanskrit.FileData;
import org.terracotta.persistence.sanskrit.FilesystemDirectory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class FileBasedFilesystemDirectoryTest {
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private Path testPath;
  private FilesystemDirectory filesystemDirectory;

  @Before
  public void before() throws Exception {
    testPath = temporaryFolder.newFolder().toPath();
    filesystemDirectory = new FileBasedFilesystemDirectory(testPath);
  }

  @Test
  public void lock() throws Exception {
    filesystemDirectory.lock().close();
    assertTrue(Files.exists(testPath.resolve("lock")));
  }

  @Test
  public void createNotExistsButCan() throws Exception {
    filesystemDirectory.create("abc", true).close();
    assertTrue(Files.exists(testPath.resolve("abc")));
  }

  @Test
  public void createNotExistsAndCannot() throws Exception {
    filesystemDirectory.create("abc", false).close();
    assertTrue(Files.exists(testPath.resolve("abc")));
  }

  @Test
  public void createExistsAndCan() throws Exception {
    filesystemDirectory.create("abc", true).close();
    filesystemDirectory.create("abc", true).close();
    assertTrue(Files.exists(testPath.resolve("abc")));
  }

  @Test(expected = IOException.class)
  public void createExistsAndCannot() throws Exception {
    filesystemDirectory.create("abc", true).close();
    filesystemDirectory.create("abc", false);
  }

  @Test
  public void readMissing() throws Exception {
    try (FileData fileData = filesystemDirectory.getFileData("abc")) {
      assertNull(fileData);
    }
  }

  @Test
  public void readPresent() throws Exception {
    try (FileData fileData = filesystemDirectory.create("abc", true)) {
      fileData.write(ByteBuffer.wrap(new byte[]{1}));
    }
    try (FileData fileData = filesystemDirectory.getFileData("abc")) {
      assertEquals(1, fileData.size());
      ByteBuffer read = ByteBuffer.allocate(1);
      fileData.read(read);
      assertThat(read.array(), is(new byte[]{1}));
    }
  }

  @Test
  public void deleteFileThatDoesNotExist() throws Exception {
    filesystemDirectory.delete("abc");
    assertFalse(Files.exists(testPath.resolve("abc")));
  }

  @Test
  @SuppressWarnings("try")
  public void deleteFileThatExists() throws Exception {
    filesystemDirectory.create("abc", true).close();
    filesystemDirectory.delete("abc");
    assertFalse(Files.exists(testPath.resolve("abc")));
  }
}
