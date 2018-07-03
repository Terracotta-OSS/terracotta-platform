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
package org.terracotta.runnel.encoding.dataholders;

import org.junit.jupiter.api.Test;
import org.terracotta.runnel.EnumMapping;
import org.terracotta.runnel.EnumMappingBuilder;
import org.terracotta.runnel.utils.ReadBuffer;
import org.terracotta.runnel.utils.WriteBuffer;

import java.nio.ByteBuffer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * @author Ludovic Orban
 */
public class EnumMappingTest {

  private enum TestEnum {
    A,B,C
  }

  private static final EnumMapping<TestEnum> ENM = EnumMappingBuilder.newEnumMappingBuilder(TestEnum.class)
      .mapping(TestEnum.A, 1)
      .mapping(TestEnum.B, 2)
      .mapping(TestEnum.C, 3)
      .build();

  @Test
  public void testWithIndex() throws Exception {
    EnumDataHolder dataHolder = new EnumDataHolder<TestEnum>(TestEnum.B, 1, ENM);

    assertThat(dataHolder.size(true), is(3));

    ByteBuffer bb = ByteBuffer.allocate(dataHolder.size(true));
    dataHolder.encode(new WriteBuffer(bb), true);
    assertThat(bb.position(), is(3));
    bb.rewind();
    ReadBuffer readBuffer = new ReadBuffer(bb);
    assertThat(readBuffer.getVlqInt(), is(1));
    assertThat(readBuffer.getVlqInt(), is(1));
    assertThat(readBuffer.getVlqInt(), is(2));
  }

  @Test
  public void testWithoutIndex() throws Exception {
    EnumDataHolder dataHolder = new EnumDataHolder<TestEnum>(TestEnum.B, 1, ENM);

    assertThat(dataHolder.size(false), is(2));

    ByteBuffer bb = ByteBuffer.allocate(dataHolder.size(false));
    dataHolder.encode(new WriteBuffer(bb), false);
    assertThat(bb.position(), is(2));
    bb.rewind();
    ReadBuffer readBuffer = new ReadBuffer(bb);
    assertThat(readBuffer.getVlqInt(), is(1));
    assertThat(readBuffer.getVlqInt(), is(2));
  }

}
