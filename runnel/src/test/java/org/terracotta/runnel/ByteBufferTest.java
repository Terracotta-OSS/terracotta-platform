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
package org.terracotta.runnel;

import org.junit.jupiter.api.Test;
import org.terracotta.runnel.decoding.StructDecoder;

import java.nio.ByteBuffer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * @author Ludovic Orban
 */
public class ByteBufferTest {

  @Test
  public void testStructWithByteBuffer() throws Exception {
    Struct struct = StructBuilder.newStructBuilder()
        .byteBuffer("blob", 10)
        .build();

    ByteBuffer encoded = struct.encoder()
        .byteBuffer("blob", buffer(128, 'x'))
        .encode();

    encoded.rewind();

    StructDecoder decoder = struct.decoder(encoded);

    ByteBuffer blob = decoder.byteBuffer("blob");
    assertThat(blob.remaining(), is(128));
    for (int i = 0; i < 128; i++) {
      byte b = blob.get();
      assertThat(b, is((byte) 'x'));
    }
  }

  @Test
  public void testSkipByteBuffer() throws Exception {
    Struct struct = StructBuilder.newStructBuilder()
        .byteBuffer("blob1", 10)
        .byteBuffer("blob2", 20)
        .build();

    ByteBuffer encoded = struct.encoder()
        .byteBuffer("blob1", buffer(128, '1'))
        .byteBuffer("blob2", buffer(128, '2'))
        .encode();

    encoded.rewind();

    StructDecoder decoder = struct.decoder(encoded);

    ByteBuffer blob = decoder.byteBuffer("blob2");
    assertThat(blob.remaining(), is(128));
    for (int i = 0; i < 128; i++) {
      byte b = blob.get();
      assertThat(b, is((byte) '2'));
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
