/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.xml.plugins;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Path;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class BackupRestoreTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void testCreateBackupRestore() throws IOException {
    Path backupPath = temporaryFolder.newFolder().toPath();
    com.terracottatech.config.br.BackupRestore backupRestore = new BackupRestore(backupPath).createBackupRestore();

    assertThat(backupRestore.getBackupLocation().getPath(), is(backupPath.toString()));
  }
}