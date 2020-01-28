/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.xml.plugins;

import com.terracottatech.dynamic_config.test.util.TmpDir;
import org.junit.Rule;
import org.junit.Test;

import java.nio.file.Path;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class BackupRestoreTest {

  @Rule
  public TmpDir temporaryFolder = new TmpDir();

  @Test
  public void testCreateBackupRestore() {
    Path backupPath = temporaryFolder.getRoot();
    com.terracottatech.config.br.BackupRestore backupRestore = new BackupRestore(backupPath).createBackupRestore();

    assertThat(backupRestore.getBackupLocation().getPath(), is(backupPath.toString()));
  }
}