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
package org.terracotta.runnel;

import org.junit.Test;
import org.terracotta.runnel.decoding.StructDecoder;
import org.terracotta.runnel.encoding.StructArrayEncoder;
import org.terracotta.runnel.encoding.StructEncoder;

import java.nio.ByteBuffer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Wire-format golden guard: the hex constants below are the exact bytes produced by the
 * pre-optimization code (verified byte-identical between the baseline and current trees,
 * per-element and bulk APIs alike). Any change that makes an encoder emit different bytes,
 * or a decoder accept these bytes differently, must come with a deliberate wire-format
 * version bump — this test is here to force that decision.
 * <p>
 * These complement {@link VersionCompatibilityTest}, which guards the semantic
 * skip-unknown-fields contract; this class guards byte-level framing.
 */
public class WireFormatGoldenTest {

  private enum Kind { PING, PONG }

  private static final EnumMapping<Kind> KINDS = EnumMappingBuilder.newEnumMappingBuilder(Kind.class)
      .mapping(Kind.PING, 7).mapping(Kind.PONG, 9).build();

  private static final Struct EMPTY = StructBuilder.newStructBuilder().build();
  private static final Struct LEASE = StructBuilder.newStructBuilder()
      .enm("kind", 10, KINDS).int64("msn", 20).int64("ts", 30).bool("ok", 40).build();
  private static final Struct MIXED = StructBuilder.newStructBuilder()
      .string("name", 10).int32("port", 20).fp64("load", 30).chr("grade", 40)
      .byteBuffer("payload", 50).build();
  private static final Struct STRINGS = StructBuilder.newStructBuilder()
      .string("empty", 10).string("ascii", 20).string("nul", 30)
      .string("twoByte", 40).string("threeByte", 50).string("emoji", 60).build();
  private static final Struct ARRAYS = StructBuilder.newStructBuilder()
      .int64s("longs", 10).strings("names", 20).build();
  private static final Struct CELL = StructBuilder.newStructBuilder()
      .string("id", 10).int64("v", 20).build();
  private static final Struct STRUCTS = StructBuilder.newStructBuilder()
      .structs("cells", 10, CELL).build();

  private static final String GOLDEN_EMPTY = "00";
  private static final String GOLDEN_LEASE =
      "1a0a0109140800000b3a73ce2ff21e08000001977420dc00280101";
  private static final String GOLDEN_MIXED =
      "210a066e6f64652d311404000024c21e083fea8f5c28f5c28f280200413203000102";
  private static final String GOLDEN_STRINGS =
      "200a001405706c61696e1e0461c080622802c3a93203e4b8ad3c06eda0bdedb880";
  private static final String GOLDEN_ARRAYS =
      "2b0a1c0308000000000000000708ffffffffffffffff087fffffffffffffff140b0300017806e4b8ade69687";
  private static final String GOLDEN_STRUCTS =
      "230a21020f0a03632d30140800000000000000010f0a03632d311408fffffffffffffffe";

  private static ByteBuffer encodeEmpty() {
    return EMPTY.encoder().encode();
  }

  private static ByteBuffer encodeLease() {
    return LEASE.encoder().enm("kind", Kind.PONG)
        .int64("msn", 12345678901234L).int64("ts", 1750000000000L).bool("ok", true).encode();
  }

  private static ByteBuffer encodeMixed() {
    return MIXED.encoder().string("name", "node-1").int32("port", 9410)
        .fp64("load", 0.83).chr("grade", 'A')
        .byteBuffer("payload", ByteBuffer.wrap(new byte[]{0, 1, 2})).encode();
  }

  private static ByteBuffer encodeStrings() {
    return STRINGS.encoder()
        .string("empty", "").string("ascii", "plain").string("nul", "a\u0000b")
        .string("twoByte", "\u00e9").string("threeByte", "\u4e2d").string("emoji", "\uD83D\uDE00").encode();
  }

  private static ByteBuffer encodeArrays() {
    // bulk overloads: byte-identical to the per-element encoding pinned by the golden
    return ARRAYS.encoder()
        .int64s("longs", new long[]{7L, -1L, Long.MAX_VALUE})
        .strings("names", new String[]{"", "x", "\u4e2d\u6587"}).encode();
  }

  private static ByteBuffer encodeStructs() {
    StructEncoder<Void> e = STRUCTS.encoder();
    StructArrayEncoder<StructEncoder<Void>> arr = e.structs("cells");
    arr.add().string("id", "c-0").int64("v", 1L);
    arr.add().string("id", "c-1").int64("v", -2L);
    return arr.end().encode();
  }

  private static String hex(ByteBuffer bb) {
    byte[] b = new byte[bb.position()];
    bb.flip().get(b);
    StringBuilder sb = new StringBuilder();
    for (byte x : b) {
      sb.append(Character.forDigit(x >> 4 & 0xF, 16)).append(Character.forDigit(x & 0xF, 16));
    }
    return sb.toString();
  }

  private static ByteBuffer fromHex(String h) {
    byte[] b = new byte[h.length() / 2];
    for (int i = 0; i < b.length; i++) {
      b[i] = (byte) Integer.parseInt(h.substring(2 * i, 2 * i + 2), 16);
    }
    return ByteBuffer.wrap(b);
  }

  private static void assertGolden(String golden, ByteBuffer encoded) {
    assertThat(hex(encoded), is(golden));
  }

  @Test
  public void emptyStructGolden() {
    assertGolden(GOLDEN_EMPTY, encodeEmpty());
    // a lone size prefix: the frame decodes as a struct with no fields to read
    EMPTY.decoder(fromHex(GOLDEN_EMPTY));
    assertGolden(GOLDEN_EMPTY, encodeEmpty());
  }

  @Test
  public void leaseGolden() {
    assertGolden(GOLDEN_LEASE, encodeLease());
    StructDecoder<?> d = LEASE.decoder(fromHex(GOLDEN_LEASE));
    assertThat(d.<Kind>enm("kind").get(), is(Kind.PONG));
    assertThat(d.int64("msn"), is(12345678901234L));
    assertThat(d.int64("ts"), is(1750000000000L));
    assertThat(d.bool("ok"), is(true));
    assertGolden(GOLDEN_LEASE, encodeLease());
  }

  @Test
  public void mixedGolden() {
    assertGolden(GOLDEN_MIXED, encodeMixed());
    StructDecoder<?> d = MIXED.decoder(fromHex(GOLDEN_MIXED));
    assertThat(d.string("name"), is("node-1"));
    assertThat(d.int32("port"), is(9410));
    assertThat(d.fp64("load"), is(0.83));
    assertThat(d.chr("grade"), is('A'));
    ByteBuffer payload = d.byteBuffer("payload");
    assertThat(payload.remaining(), is(3));
    assertThat(payload.get(), is((byte) 0));
    assertThat(payload.get(), is((byte) 1));
    assertThat(payload.get(), is((byte) 2));
    assertGolden(GOLDEN_MIXED, encodeMixed());
  }

  @Test
  public void stringsGolden() {
    assertGolden(GOLDEN_STRINGS, encodeStrings());
    StructDecoder<?> d = STRINGS.decoder(fromHex(GOLDEN_STRINGS));
    assertThat(d.string("empty"), is(""));
    assertThat(d.string("ascii"), is("plain"));
    assertThat(d.string("nul"), is("a\u0000b"));
    assertThat(d.string("twoByte"), is("\u00e9"));
    assertThat(d.string("threeByte"), is("\u4e2d"));
    assertThat(d.string("emoji"), is("\uD83D\uDE00"));
    assertGolden(GOLDEN_STRINGS, encodeStrings());
  }

  @Test
  public void arraysGolden() {
    assertGolden(GOLDEN_ARRAYS, encodeArrays());
    StructDecoder<?> d = ARRAYS.decoder(fromHex(GOLDEN_ARRAYS));
    assertThat(d.int64sArray("longs"), is(new long[]{7L, -1L, Long.MAX_VALUE}));
    assertThat(d.stringsArray("names"), is(new String[]{"", "x", "\u4e2d\u6587"}));
    assertGolden(GOLDEN_ARRAYS, encodeArrays());
  }

  @Test
  public void structArrayGolden() {
    assertGolden(GOLDEN_STRUCTS, encodeStructs());
    StructDecoder<?> d = STRUCTS.decoder(fromHex(GOLDEN_STRUCTS));
    java.util.List<String> ids = d.structs("cells").decodeAll(
        cell -> cell.string("id") + ":" + cell.int64("v", Long.MIN_VALUE));
    assertThat(ids, is(java.util.Arrays.asList("c-0:1", "c-1:-2")));
    assertGolden(GOLDEN_STRUCTS, encodeStructs());
  }
}
