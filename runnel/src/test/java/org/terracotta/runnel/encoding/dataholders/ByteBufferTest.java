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

import org.hamcrest.MatcherAssert;
import org.junit.Test;
import org.terracotta.runnel.utils.ReadBuffer;
import org.terracotta.runnel.utils.WriteBuffer;

import java.nio.ByteBuffer;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author Ludovic Orban
 */
public class ByteBufferTest {

  @Test
  public void testWithIndex() throws Exception {
    ByteBufferDataHolder dataHolder = new ByteBufferDataHolder(buffer(500, 'X'), 5);

    assertThat(dataHolder.size(true), is(503));

    ByteBuffer bb = ByteBuffer.allocate(dataHolder.size(true));
    dataHolder.encode(new WriteBuffer(bb), true);
    assertThat(bb.position(), is(503));
    bb.rewind();
    ReadBuffer readBuffer = new ReadBuffer(bb);
    assertThat(readBuffer.getVlqInt(), is(5));
    assertThat(readBuffer.getVlqInt(), is(500));
    ByteBuffer buffer = readBuffer.getByteBuffer(500);
    assertThat(buffer.remaining(), is(500));
    for (int i = 0; i < 500; i++) {
      byte b = buffer.get();
      assertThat(b, is((byte) 'X'));
    }
  }

  @Test
  public void testWithoutIndex() throws Exception {
    ByteBufferDataHolder dataHolder = new ByteBufferDataHolder(buffer(3000, '$'), 50000);

    assertThat(dataHolder.size(false), is(3002));

    ByteBuffer bb = ByteBuffer.allocate(dataHolder.size(true));
    dataHolder.encode(new WriteBuffer(bb), false);
    assertThat(bb.position(), is(3002));
    bb.rewind();
    ReadBuffer readBuffer = new ReadBuffer(bb);
    assertThat(readBuffer.getVlqInt(), is(3000));
    ByteBuffer buffer = readBuffer.getByteBuffer(3000);
    assertThat(buffer.remaining(), is(3000));
    for (int i = 0; i < 3000; i++) {
      byte b = buffer.get();
      assertThat(b, is((byte) '$'));
    }
  }

  private static ByteBuffer buffer(int size, char c) {
    ByteBuffer byteBuffer = ByteBuffer.allocate(size);
    for (int i = 0; i < size; i++) {
      byteBuffer.put((byte) c);
    }
    byteBuffer.rewind();
    return byteBuffer;
  }
}
