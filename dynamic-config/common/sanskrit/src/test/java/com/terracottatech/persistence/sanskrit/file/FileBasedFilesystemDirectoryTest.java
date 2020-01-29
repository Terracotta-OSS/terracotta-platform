/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.persistence.sanskrit.file;

import com.terracottatech.persistence.sanskrit.FileData;
import com.terracottatech.persistence.sanskrit.FilesystemDirectory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

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
      fileData.write(ByteBuffer.wrap(new byte[] { 1 }));
    }
    try (FileData fileData = filesystemDirectory.getFileData("abc")) {
      assertEquals(1, fileData.size());
      ByteBuffer read = ByteBuffer.allocate(1);
      fileData.read(read);
      assertThat(read.array(), is(new byte[] { 1 }));
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
