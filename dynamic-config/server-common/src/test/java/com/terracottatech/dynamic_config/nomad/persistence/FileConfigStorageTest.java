/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.nomad.persistence;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class FileConfigStorageTest {
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void saveAndRetrieve() throws Exception {
    Path root = temporaryFolder.getRoot().toPath();
    FileConfigStorage storage = new FileConfigStorage(root, index -> "file." + index);

    storage.saveConfig(1L, "abc\ndef");
    assertThat(storage.getConfig(1L), is("abc\ndef"));

    byte[] bytes = Files.readAllBytes(root.resolve("file.1"));
    String fileContents = new String(bytes, StandardCharsets.UTF_8);
    assertThat(fileContents, is("abc\ndef"));
  }
}