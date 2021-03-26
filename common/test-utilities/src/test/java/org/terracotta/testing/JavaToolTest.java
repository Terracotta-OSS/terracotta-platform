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
package org.terracotta.testing;

import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Mathieu Carbou
 */
public class JavaToolTest {

  @Rule
  public TmpDir tmpDir = new TmpDir(Paths.get(System.getProperty("user.dir"), "target"), false);

  @Test
  public void threadDumpToMemory() {
    assertThat(JavaTool.threadDumps(null).count(), is(greaterThanOrEqualTo(1L)));
  }

  @Test
  public void threadDumpToFolder() throws IOException {
    JavaTool.threadDumps(tmpDir.getRoot(), null);
    assertThat(Files.list(tmpDir.getRoot()).collect(toList()), hasSize(greaterThanOrEqualTo(1)));
  }

  @Test
  public void memoryDump() throws IOException {
    JavaTool.memoryDumps(tmpDir.getRoot(), null);
    assertThat(Files.list(tmpDir.getRoot()).collect(toList()), hasSize(greaterThanOrEqualTo(1)));
  }
}
