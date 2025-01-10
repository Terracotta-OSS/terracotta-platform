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
package org.terracotta.common.struct;

import org.junit.Test;

import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Mathieu Carbou
 */
public class MemoryUnitTest {

  @Test
  public void convert() {
    assertThat(MemoryUnit.B.convert(4096, MemoryUnit.MB), is(equalTo(4096L * 1024 * 1024)));
    assertThat(MemoryUnit.KB.convert(4096, MemoryUnit.MB), is(equalTo(4096L * 1024)));
    assertThat(MemoryUnit.MB.convert(4096, MemoryUnit.MB), is(equalTo(4096L)));
    assertThat(MemoryUnit.GB.convert(4096, MemoryUnit.MB), is(equalTo(4L)));
    assertThat(MemoryUnit.TB.convert(4096, MemoryUnit.MB), is(equalTo(0L)));
  }

  @Test
  public void test_getBaseUnit() {
    Stream.of(MemoryUnit.values()).forEach(unit -> assertThat(unit.getBaseUnit(), is(MemoryUnit.B)));
  }
}