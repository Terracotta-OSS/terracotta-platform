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

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Mathieu Carbou
 */
public class ThreadDumpTest {

  @Rule
  public TmpDir tmpDir = new TmpDir(Paths.get(System.getProperty("user.dir"), "target"), false);

  @Rule
  public Timeout timeout = Timeout.builder()
      .withLookingForStuckThread(true)
      .withThreadDump(Paths.get(System.getProperty("user.dir")).resolve("target").resolve("thread-dumps"))
      .withTimeout(20, TimeUnit.SECONDS)
      .build();

  @Test
  public void dump_to_map() {
    assertThat(ThreadDump.dumpAll().parallel().count(), is(greaterThanOrEqualTo(1L)));
  }

  @Test
  public void dump_to_folder() throws IOException {
    ThreadDump.dumpAll(tmpDir.getRoot());
    assertThat(Files.list(tmpDir.getRoot()).collect(toList()), hasSize(greaterThanOrEqualTo(1)));
  }


  @Ignore
  @Test
  public void test_timeout() throws InterruptedException {
    Thread.sleep(30_000);
  }
}
