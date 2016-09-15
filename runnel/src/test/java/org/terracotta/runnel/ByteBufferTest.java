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

import org.junit.Test;
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
        .string("name", 5)
        .byteBuffer("blob", 10)
        .build();

    ByteBuffer encoded = struct.encoder()
        .string("name", "john doe")
        .byteBuffer("blob", buffer(128))
        .encode();

    encoded.rewind();

    StructDecoder decoder = struct.decoder(encoded);

    assertThat(decoder.string("name"), is("john doe"));
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
        .byteBuffer("blob", 10)
        .string("name", 15)
        .build();

    ByteBuffer encoded = struct.encoder()
        .byteBuffer("blob", buffer(128))
        .string("name", "john doe")
        .encode();

    encoded.rewind();

    StructDecoder decoder = struct.decoder(encoded);

    assertThat(decoder.string("name"), is("john doe"));
  }

  private static ByteBuffer buffer(int size) {
    ByteBuffer byteBuffer = ByteBuffer.allocate(size);
    for (int i = 0; i < size; i++) {
      byteBuffer.put((byte) 'x');
    }
    byteBuffer.rewind();
    return byteBuffer;
  }
}
