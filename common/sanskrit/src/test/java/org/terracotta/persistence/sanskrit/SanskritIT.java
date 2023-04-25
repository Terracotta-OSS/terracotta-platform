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
package org.terracotta.persistence.sanskrit;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.terracotta.persistence.sanskrit.file.FileBasedFilesystemDirectory;

import java.nio.file.Path;

import static org.junit.Assert.assertEquals;

public class SanskritIT {
  private final SanskritMapper mapper = new JsonSanskritMapper();

  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  @Test
  public void simpleWriteRead() throws Exception {
    Path root = folder.newFolder().toPath();
    FilesystemDirectory filesystemDirectory = new FileBasedFilesystemDirectory(root);

    try (Sanskrit sanskrit = Sanskrit.init(filesystemDirectory, mapper)) {
      sanskrit.setString("configHash", "49e1ceea34a674b42cd70b1764a5477227d1ffcd");
    }

    try (Sanskrit sanskrit = Sanskrit.init(filesystemDirectory, mapper)) {
      assertEquals("49e1ceea34a674b42cd70b1764a5477227d1ffcd", sanskrit.getString("configHash"));
      sanskrit.setString("configHash", "019eddbd529da0184ba78422db59539454ddc55f");
    }

    try (Sanskrit sanskrit = Sanskrit.init(filesystemDirectory, mapper)) {
      assertEquals("019eddbd529da0184ba78422db59539454ddc55f", sanskrit.getString("configHash"));
      sanskrit.setString("configHash", "b5da3b7595c55a5d241e567d576c5b59711dc95a");
    }

    try (Sanskrit sanskrit = Sanskrit.init(filesystemDirectory, mapper)) {
      assertEquals("b5da3b7595c55a5d241e567d576c5b59711dc95a", sanskrit.getString("configHash"));
    }
  }

  @Test(expected = SanskritException.class)
  @SuppressWarnings("try")
  public void locking() throws Exception {
    Path root = folder.newFolder().toPath();
    FilesystemDirectory filesystemDirectory = new FileBasedFilesystemDirectory(root);

    try (Sanskrit sanskrit = Sanskrit.init(filesystemDirectory, mapper)) {
      Sanskrit.init(filesystemDirectory, mapper);
    }
  }
}
