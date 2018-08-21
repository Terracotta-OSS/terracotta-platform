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
import org.terracotta.runnel.utils.ReadBuffer;
import org.terracotta.runnel.utils.WriteBuffer;

import java.nio.ByteBuffer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * @author Ludovic Orban
 */
public class Int32Test {

  @Test
  public void testWithIndex() throws Exception {
    Int32DataHolder Int32DataHolder = new Int32DataHolder(99, 1);

    assertThat(Int32DataHolder.size(true), is(6));

    ByteBuffer bb = ByteBuffer.allocate(Int32DataHolder.size(true));
    Int32DataHolder.encode(new WriteBuffer(bb), true);
    assertThat(bb.position(), is(6));
    bb.rewind();
    ReadBuffer readBuffer = new ReadBuffer(bb);
    assertThat(readBuffer.getVlqInt(), is(1));
    assertThat(readBuffer.getVlqInt(), is(4));
    assertThat(readBuffer.getInt(), is(99));
  }

  @Test
  public void testWithoutIndex() throws Exception {
    Int32DataHolder Int32DataHolder = new Int32DataHolder(99, 1);

    assertThat(Int32DataHolder.size(false), is(5));

    ByteBuffer bb = ByteBuffer.allocate(Int32DataHolder.size(false));
    Int32DataHolder.encode(new WriteBuffer(bb), false);
    assertThat(bb.position(), is(5));
    bb.rewind();
    ReadBuffer readBuffer = new ReadBuffer(bb);
    assertThat(readBuffer.getVlqInt(), is(4));
    assertThat(readBuffer.getInt(), is(99));
  }

}
