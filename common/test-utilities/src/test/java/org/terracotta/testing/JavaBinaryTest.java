/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
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

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assume.assumeThat;

/**
 * @author Mathieu Carbou
 */
public class JavaBinaryTest {
  @Test
  public void find_linux() {
    assumeThat(System.getProperty("os.name").toLowerCase(), not(containsString("win")));

    assertThat(JavaBinary.bin("java"), is(equalTo("java")));
    assertThat(JavaBinary.find("java").get().getFileName().toString(), is(equalTo("java"))); // in jre
    assertThat(JavaBinary.find("jps").get().getFileName().toString(), is(equalTo("jps"))); // in jdk
    assertThat(JavaBinary.find("jmap").get().getFileName().toString(), is(equalTo("jmap"))); // in jdk
    assertThat(JavaBinary.find("jstack").get().getFileName().toString(), is(equalTo("jstack"))); // in jdk
  }

  @Test
  public void find_win() {
    assumeThat(System.getProperty("os.name").toLowerCase(), containsString("win"));

    assertThat(JavaBinary.bin("java"), is(equalTo("java.exe")));
    assertThat(JavaBinary.find("java").get().getFileName().toString(), is(equalTo("java.exe"))); // in jre
    assertThat(JavaBinary.find("jps").get().getFileName().toString(), is(equalTo("jps.exe"))); // in jdk
    assertThat(JavaBinary.find("jstack").get().getFileName().toString(), is(equalTo("jstack.exe"))); // in jdk
    assertThat(JavaBinary.find("jmap").get().getFileName().toString(), is(equalTo("jmap.exe"))); // in jdk
  }
}