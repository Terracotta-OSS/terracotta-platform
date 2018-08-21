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
package org.terracotta.runnel.utils;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Ludovic Orban
 */
public class VLQTest {

  @Test
  public void testEncodeNegativeFails() throws Exception {
    assertThrows(IllegalArgumentException.class, () -> {
      VLQ.encode(-1, ByteBuffer.allocate(8));
    });
  }

  @Test
  public void testEncodedSizeNegativeValueFails() throws Exception {
    assertThrows(IllegalArgumentException.class, () -> {
      VLQ.encodedSize(-1);
    });
  }

  @Test
  public void testEncode() throws Exception {
    checkEncoding(0x7F, 0x7F);
    checkEncoding(0x80, 0x81, 0x00);
    checkEncoding(0x2000, 0xC0, 0x00);
    checkEncoding(0x3FFF, 0xFF, 0x7F);
    checkEncoding(0x4000, 0x81, 0x80, 0x00);
    checkEncoding(0x1FFFFF, 0xFF, 0xFF, 0x7F);
    checkEncoding(0x200000, 0x81, 0x80, 0x80, 0x00);
    checkEncoding(0x08000000, 0xC0, 0x80, 0x80, 0x00);
    checkEncoding(0x0FFFFFFF, 0xFF, 0xFF, 0xFF, 0x7F);
    checkEncoding(Integer.MAX_VALUE, 0x87, 0xFF, 0xFF, 0xFF, 0x7F);
  }

  @Test
  public void testDecode() throws Exception {
    checkDecoding(0x7F, 0x7F);
    checkDecoding(0x80, 0x81, 0x00);
    checkDecoding(0x2000, 0xC0, 0x00);
    checkDecoding(0x3FFF, 0xFF, 0x7F);
    checkDecoding(0x4000, 0x81, 0x80, 0x00);
    checkDecoding(0x1FFFFF, 0xFF, 0xFF, 0x7F);
    checkDecoding(0x200000, 0x81, 0x80, 0x80, 0x00);
    checkDecoding(0x08000000, 0xC0, 0x80, 0x80, 0x00);
    checkDecoding(0x0FFFFFFF, 0xFF, 0xFF, 0xFF, 0x7F);
    checkDecoding(Integer.MAX_VALUE, 0x87, 0xFF, 0xFF, 0xFF, 0x7F);
  }

  @Test
  public void testEncodedSize() throws Exception {
    assertThat(VLQ.encodedSize(0x7F), is(1));
    assertThat(VLQ.encodedSize(0x80), is(2));
    assertThat(VLQ.encodedSize(0x2000), is(2));
    assertThat(VLQ.encodedSize(0x3FFF), is(2));
    assertThat(VLQ.encodedSize(0x4000), is(3));
    assertThat(VLQ.encodedSize(0x1FFFFF), is(3));
    assertThat(VLQ.encodedSize(0x200000), is(4));
    assertThat(VLQ.encodedSize(0x08000000), is(4));
    assertThat(VLQ.encodedSize(0x0FFFFFFF), is(4));
    assertThat(VLQ.encodedSize(Integer.MAX_VALUE), is(5));
  }

  private void checkDecoding(int value, int... bytes) {
    ByteBuffer bb = ByteBuffer.allocate(8);

    for (int aByte : bytes) {
      bb.put((byte) aByte);
    }
    bb.rewind();

    int decoded = VLQ.decode(new ReadBuffer(bb));
    assertThat(decoded, is(value));
  }

  private void checkEncoding(int value, int... bytes) {
    ByteBuffer bb = ByteBuffer.allocate(8);
    VLQ.encode(value, bb);
    assertThat(bb.position(), is(bytes.length));

    int[] ba = new int[bytes.length];
    bb.rewind();
    for (int i = 0; i < bytes.length; i++) {
      ba[i] = bb.get();
      if (ba[i] < 0) ba[i] += 256;
    }
    assertArrayEquals(bytes, ba);
  }
}
