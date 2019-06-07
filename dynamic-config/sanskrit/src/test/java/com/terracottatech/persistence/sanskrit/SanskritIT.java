/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.persistence.sanskrit;

import com.terracottatech.persistence.sanskrit.file.FileBasedFilesystemDirectory;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Path;

import static org.junit.Assert.assertEquals;

public class SanskritIT {
  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  @Test
  public void simpleWriteRead() throws Exception {
    Path root = folder.newFolder().toPath();
    FilesystemDirectory filesystemDirectory = new FileBasedFilesystemDirectory(root);

    try (Sanskrit sanskrit = Sanskrit.init(filesystemDirectory)) {
      sanskrit.setString("configHash", "49e1ceea34a674b42cd70b1764a5477227d1ffcd");
    }

    try (Sanskrit sanskrit = Sanskrit.init(filesystemDirectory)) {
      assertEquals("49e1ceea34a674b42cd70b1764a5477227d1ffcd", sanskrit.getString("configHash"));
      sanskrit.setString("configHash", "019eddbd529da0184ba78422db59539454ddc55f");
    }

    try (Sanskrit sanskrit = Sanskrit.init(filesystemDirectory)) {
      assertEquals("019eddbd529da0184ba78422db59539454ddc55f", sanskrit.getString("configHash"));
      sanskrit.setString("configHash", "b5da3b7595c55a5d241e567d576c5b59711dc95a");
    }

    try (Sanskrit sanskrit = Sanskrit.init(filesystemDirectory)) {
      assertEquals("b5da3b7595c55a5d241e567d576c5b59711dc95a", sanskrit.getString("configHash"));
    }
  }

  @Test(expected = SanskritException.class)
  @SuppressWarnings("try")
  public void locking() throws Exception {
    Path root = folder.newFolder().toPath();
    FilesystemDirectory filesystemDirectory = new FileBasedFilesystemDirectory(root);

    try (Sanskrit sanskrit = Sanskrit.init(filesystemDirectory)) {
      Sanskrit.init(filesystemDirectory);
    }
  }
}
