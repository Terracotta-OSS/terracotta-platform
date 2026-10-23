/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2026
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

import org.junit.Test;
import org.terracotta.runnel.utils.CorruptDataException;
import org.terracotta.runnel.utils.LimitReachedException;
import org.terracotta.runnel.utils.ReadBuffer;
import org.terracotta.runnel.utils.WriteBuffer;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.nio.ByteBuffer;
import java.util.Random;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.fail;

/**
 * Edge-case coverage of the string codec: the encoded bytes must match the JDK's modified
 * UTF-8 ({@link DataOutputStream#writeUTF}) payload byte-for-byte, and decoding must round-trip
 * through both the ASCII fast path and the modified-UTF path.
 */
public class StringEdgeCasesTest {

  private static byte[] encodeValue(String s) {
    StringDataHolder holder = new StringDataHolder(s, 1);
    ByteBuffer bb = ByteBuffer.allocate(holder.size(false));
    holder.encode(new WriteBuffer(bb), false);
    bb.flip();
    ReadBuffer rb = new ReadBuffer(bb);
    int size = rb.getVlqInt();
    assertThat(size, is(rb.remaining()));
    byte[] out = new byte[rb.remaining()];
    bb.get(out);
    return out;
  }

  private static byte[] referenceModifiedUtf8(String s) throws Exception {
    ByteArrayOutputStream bout = new ByteArrayOutputStream();
    DataOutputStream dos = new DataOutputStream(bout);
    dos.writeUTF(s);
    dos.flush();
    byte[] all = bout.toByteArray();
    byte[] payload = new byte[all.length - 2];
    System.arraycopy(all, 2, payload, 0, payload.length);
    return payload;
  }

  private static String decodeValue(byte[] bytes) {
    ReadBuffer rb = new ReadBuffer(ByteBuffer.wrap(bytes));
    return rb.getString(bytes.length);
  }

  private static void roundTripWithJdkReference(String s) throws Exception {
    byte[] actual = encodeValue(s);
    if (actual.length < 65536) {
      // JDK reference is limited to 64KiB payloads
      assertThat("byte-mismatch vs JDK modified UTF-8 for string of length " + s.length(),
                 actual, is(referenceModifiedUtf8(s)));
    }
    assertThat(decodeValue(actual), is(s));
  }

  @Test
  public void emptyString() throws Exception {
    roundTripWithJdkReference("");
  }

  @Test
  public void asciiOnly() throws Exception {
    roundTripWithJdkReference("hello world, this is a plain ascii string 0123456789");
  }

  @Test
  public void embeddedNul() throws Exception {
    String s = "ab\u0000c";
    byte[] encoded = encodeValue(s);
    // NUL must be encoded as the 2-byte modified-UTF form C0 80
    byte[] expected = {'a', 'b', (byte) 0xC0, (byte) 0x80, 'c'};
    assertThat(encoded, is(expected));
    assertThat(decodeValue(encoded), is(s));
  }

  @Test
  public void twoByteCharacters() throws Exception {
    roundTripWithJdkReference("Düsseldorf, été, \u00807F boundary");
  }

  @Test
  public void threeByteCharacters() throws Exception {
    roundTripWithJdkReference("中文测试数据北京上海");
  }

  @Test
  public void supplementaryCharactersAsSurrogatePairs() throws Exception {
    // each surrogate half encodes as a 3-byte sequence
    roundTripWithJdkReference("emoji: 😀😁😂 done");
  }

  @Test
  public void asciiBoundaryChar() throws Exception {
    roundTripWithJdkReference("a\u007Fb");
    roundTripWithJdkReference("a\u0080b");
  }

  @Test
  public void exactly64KAscii() throws Exception {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < 65535; i++) {
      sb.append((char) ('a' + i % 26));
    }
    roundTripWithJdkReference(sb.toString());
  }

  @Test
  public void exactly64KPlusOneAscii() throws Exception {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < 65536; i++) {
      sb.append((char) ('a' + i % 26));
    }
    roundTripWithJdkReference(sb.toString());
  }

  @Test
  public void nonAsciiStraddling64KBytes() throws Exception {
    StringBuilder sb = new StringBuilder();
    while (sb.length() < 65534) {
      sb.append('x');
    }
    sb.append('\u20AC'); // 3-byte char straddling the 65535 boundary
    roundTripWithJdkReference(sb.toString());
  }

  @Test
  public void asciiFastPathHasSameResultsAsGeneralPath() {
    // strings that just barely contain a non-ASCII byte must take the general path
    String asciiPart = "some ascii prefix ";
    String str = asciiPart + 'é' + " suffix";
    assertThat(decodeValue(encodeValue(str)), is(str));
    // ... and pure-ASCII of identical length takes the fast path
    String pure = asciiPart + 'e' + " suffix";
    assertThat(decodeValue(encodeValue(pure)), is(pure));
    assertThat(encodeValue(pure).length - encodeValue(str).length, is(-1));
  }

  @Test
  public void malformedDataThrowsCorruptData() {
    // a continuation byte (0x80) used as a lead byte is illegal in modified UTF-8
    byte[] malformed = {(byte) 0x80, 0x4A};
    ReadBuffer rb = new ReadBuffer(ByteBuffer.wrap(malformed));
    try {
      rb.getString(malformed.length);
      fail();
    } catch (CorruptDataException e) {
      // expected; carries the suppressed UTF failure
      assertThat(e.getSuppressed().length, is(1));
    }
  }

  @Test
  public void truncatedMultibyteSequenceThrowsBufferUnderflow() {
    // truncated 3-byte sequence: E4 4A (should be E4 xx xx) -- same behavior as the original
    // decoder, which lets ByteBuffer report the underflow
    byte[] truncated = {(byte) 0xE4, 0x4A};
    ReadBuffer rb = new ReadBuffer(ByteBuffer.wrap(truncated));
    try {
      rb.getString(truncated.length);
      fail();
    } catch (java.nio.BufferUnderflowException e) {
      // expected
    }
  }

  @Test
  public void overlongClaimsThrowLimitReached() {
    byte[] data = {'a', 'b'};
    ReadBuffer rb = new ReadBuffer(ByteBuffer.wrap(data));
    try {
      rb.getString(10);
      fail();
    } catch (LimitReachedException e) {
      // expected
    }
  }

  @Test
  public void failingDecodeLeavesPositionUntouched() {
    byte[] malformed = {(byte) 0x80, 0x4A};
    ByteBuffer bb = ByteBuffer.wrap(malformed);
    ReadBuffer rb = new ReadBuffer(bb);
    try {
      rb.getString(malformed.length);
      fail();
    } catch (CorruptDataException e) {
      // expected
    }
    assertThat(bb.position(), is(0));
  }

  @Test
  public void directBufferDecode() {
    ByteBuffer all = ByteBuffer.allocateDirect(64);
    String s = "ascii via direct buffer";
    StringDataHolder holder = new StringDataHolder(s, 1);
    ByteBuffer heap = ByteBuffer.allocate(holder.size(false));
    holder.encode(new WriteBuffer(heap), false);
    heap.flip();
    heap.get(); // size vlq (single byte)
    byte[] payload = new byte[heap.remaining()];
    heap.get(payload);
    all.put(payload);
    all.flip();

    ReadBuffer rb = new ReadBuffer(all);
    assertThat(rb.getString(payload.length), is(s));
  }

  private static ByteBuffer direct(byte[] bytes) {
    ByteBuffer bb = ByteBuffer.allocateDirect(bytes.length);
    bb.put(bytes).flip();
    return bb;
  }

  @Test
  public void directBufferNonAsciiDecode() {
    String s = "direct \u0000 café 中文 😀 end";
    byte[] payload = encodeValue(s);
    ByteBuffer bb = direct(payload);
    ReadBuffer rb = new ReadBuffer(bb);
    assertThat(rb.getString(payload.length), is(s));
    assertThat(bb.position(), is(payload.length));
  }

  @Test
  public void directBufferMalformedThrowsCorruptDataAndLeavesPositionUntouched() {
    byte[] malformed = {0x4A, (byte) 0x80, 0x4A};
    ByteBuffer bb = direct(malformed);
    ReadBuffer rb = new ReadBuffer(bb);
    try {
      rb.getString(malformed.length);
      fail();
    } catch (CorruptDataException e) {
      assertThat(e.getSuppressed().length, is(1));
    }
    assertThat(bb.position(), is(0));
  }

  @Test
  public void nonAsciiDecodeAtNonZeroPositionAndOffset() {
    // decode from the middle of a sliced (arrayOffset != 0) buffer, followed by trailing data
    String s = "préfixe 中";
    byte[] payload = encodeValue(s);
    byte[] backing = new byte[payload.length + 7];
    System.arraycopy(payload, 0, backing, 5, payload.length);
    ByteBuffer outer = ByteBuffer.wrap(backing);
    outer.position(2);
    ByteBuffer sliced = outer.slice(); // arrayOffset 2
    sliced.position(3);
    ReadBuffer rb = new ReadBuffer(sliced);
    assertThat(rb.getString(payload.length), is(s));
    assertThat(sliced.position(), is(3 + payload.length));
  }

  /**
   * Randomized equivalence check: for arbitrary strings (including NUL, U+007F, U+0080,
   * 2-byte and 3-byte characters and surrogate halves), the encoded bytes must match the
   * JDK's modified UTF-8 byte-for-byte (under writeUTF's 64KiB limit) and must round-trip
   * through both the ASCII fast path and the modified-UTF path.
   */
  @Test
  public void randomizedRoundTrip() throws Exception {
    Random r = new Random(987654321L);
    for (int i = 0; i < 1000; i++) {
      String s = randomString(r);
      byte[] encoded = encodeValue(s);
      assertThat("round-trip failed for \"" + s + "\"", decodeValue(encoded), is(s));
      if (encoded.length < 65536) {
        assertThat("byte mismatch vs JDK modified UTF-8 for \"" + s + "\"",
                   encoded, is(referenceModifiedUtf8(s)));
      }
    }
  }

  private static String randomString(Random r) {
    char[] c = new char[r.nextInt(200)];
    for (int i = 0; i < c.length; i++) {
      switch (r.nextInt(4)) {
        case 0: c[i] = (char) ('a' + r.nextInt(26)); break;                    // plain ascii
        case 1: c[i] = (char) r.nextInt(0x80); break;                          // ascii range, incl. NUL and 0x7F
        case 2: c[i] = (char) (0x80 + r.nextInt(0x800 - 0x80)); break;        // 2-byte range, incl. U+0080
        default: c[i] = (char) (0x800 + r.nextInt(0x10000 - 0x800)); break;    // 3-byte range, incl. surrogates
      }
    }
    return new String(c);
  }
}
