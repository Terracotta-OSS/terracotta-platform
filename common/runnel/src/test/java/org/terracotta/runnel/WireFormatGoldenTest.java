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
package org.terracotta.runnel;

import org.junit.Test;
import org.terracotta.runnel.decoding.StructDecoder;
import org.terracotta.runnel.decoding.fields.ArrayField;
import org.terracotta.runnel.decoding.fields.BoolField;
import org.terracotta.runnel.decoding.fields.ByteBufferField;
import org.terracotta.runnel.decoding.fields.CharField;
import org.terracotta.runnel.decoding.fields.EnumField;
import org.terracotta.runnel.decoding.fields.FloatingPoint64Field;
import org.terracotta.runnel.decoding.fields.Int32Field;
import org.terracotta.runnel.decoding.fields.Int64Field;
import org.terracotta.runnel.decoding.fields.StringField;
import org.terracotta.runnel.decoding.fields.StructField;
import org.terracotta.runnel.encoding.ArrayEncoder;
import org.terracotta.runnel.encoding.StructArrayEncoder;
import org.terracotta.runnel.encoding.StructEncoder;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

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
  private static final Struct KITCHEN_CELL = StructBuilder.newStructBuilder()
      .string("name", 10).int64("value", 20).bool("flag", 30).build();
  private static final Struct KITCHEN_SINK = StructBuilder.newStructBuilder()
      .string("name", 10).chr("grade", 20).int32("port", 30).int64("msn", 40)
      .fp64("load", 50).bool("alive", 60).enm("kind", 70, KINDS)
      .byteBuffer("payload", 80)
      .int32s("int32s", 90).int64s("int64s", 100).fp64s("fp64s", 110)
      .bools("bools", 120).chrs("chrs", 130).strings("strings", 140)
      .byteBuffers("byteBuffers", 150)
      .struct("inner", 160, KITCHEN_CELL).structs("cells", 170, KITCHEN_CELL).build();

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

  /**
   * A kitchen-sink message using every field type (all scalars, all seven array kinds, a
   * nested struct and a struct array). The golden bytes were emitted by the PRE-change code
   * through the old (name-based, per-element) API, and the same source generator was verified
   * to produce identical bytes on both the baseline and current trees. The tests below pin
   * that the current old API AND the new APIs (field handles, bulk arrays, primitive decodes,
   * decodeAll) all produce and consume exactly these bytes.
   */
  private static final String GOLDEN_KITCHEN_SINK =
      "82630a186b69746368656e2d73696e6b20e4b8adeda0bdedb880c08014024e2d1e040005"
          + "cafe2808112210f47de981153208400921fb54442d183c0101460109500400012aff5a1a"
          + "05048000000004ffffffff04000000000400000007047fffffff642e0508800000000000"
          + "000008ffffffffffffffff08000000000000000008112210f47de98115087fffffffffff"
          + "ffff6e2e0508000000000000000008bff8000000000000087ff8000000000000087ff000"
          + "000000000008400921fb54442d1878070301010100010181020d0402006102007f02ffff"
          + "024e2d810c27040005617363696912c3bc6ec3af63c3b664c3a920e4b8ade696870b7769"
          + "746820c080204e554c81160903000301020302ff008120140a05696e6e65721408ffffff"
          + "ffffffffd61e0100812a3a03120a03632d30140800000000000000011e0101120a03632d"
          + "311408fffffffffffffffe1e0100120a03632d3214087fffffffffffffff1e0101";

  // kitchen-sink values: encoded identically by every path under test
  private static final String KS_NAME = "kitchen-sink \u4e2d\uD83D\uDE00\u0000";
  private static final char KS_GRADE_CHAR = '\u4e2d';
  private static final int KS_PORT = 0x5CAFE;
  private static final long KS_MSN = 1234567890123456789L;
  private static final double KS_LOAD = Math.PI;
  private static final byte[] KS_PAYLOAD = {0x00, 0x01, 0x2A, (byte) 0xFF};
  private static final int[] KS_INT32S = {Integer.MIN_VALUE, -1, 0, 7, Integer.MAX_VALUE};
  private static final long[] KS_INT64S = {Long.MIN_VALUE, -1L, 0L, 1234567890123456789L, Long.MAX_VALUE};
  private static final double[] KS_FP64S = {0.0, -1.5, Double.NaN, Double.POSITIVE_INFINITY, Math.PI};
  private static final boolean[] KS_BOOLS = {true, false, true};
  private static final char[] KS_CHRS = {'a', '\u007F', '\uFFFF', '\u4e2d'};
  private static final String[] KS_STRINGS = {"", "ascii", "\u00fcn\u00efc\u00f6d\u00e9 \u4e2d\u6587", "with \u0000 NUL"};

  // kitchen-sink field handles: exercising every handle-keyed overload against the golden
  private static final StringField KS_NAME_F = KITCHEN_SINK.field("name", StringField.class);
  private static final CharField KS_GRADE_F = KITCHEN_SINK.field("grade", CharField.class);
  private static final Int32Field KS_PORT_F = KITCHEN_SINK.field("port", Int32Field.class);
  private static final Int64Field KS_MSN_F = KITCHEN_SINK.field("msn", Int64Field.class);
  private static final FloatingPoint64Field KS_LOAD_F = KITCHEN_SINK.field("load", FloatingPoint64Field.class);
  private static final BoolField KS_ALIVE_F = KITCHEN_SINK.field("alive", BoolField.class);
  @SuppressWarnings("unchecked")
  private static final EnumField<Kind> KS_KIND_F = (EnumField<Kind>) KITCHEN_SINK.field("kind", EnumField.class);
  private static final ByteBufferField KS_PAYLOAD_F = KITCHEN_SINK.field("payload", ByteBufferField.class);
  private static final ArrayField KS_INT32S_F = KITCHEN_SINK.field("int32s", ArrayField.class);
  private static final ArrayField KS_INT64S_F = KITCHEN_SINK.field("int64s", ArrayField.class);
  private static final ArrayField KS_FP64S_F = KITCHEN_SINK.field("fp64s", ArrayField.class);
  private static final ArrayField KS_BOOLS_F = KITCHEN_SINK.field("bools", ArrayField.class);
  private static final ArrayField KS_CHRS_F = KITCHEN_SINK.field("chrs", ArrayField.class);
  private static final ArrayField KS_STRINGS_F = KITCHEN_SINK.field("strings", ArrayField.class);
  private static final ArrayField KS_BYTE_BUFFERS_F = KITCHEN_SINK.field("byteBuffers", ArrayField.class);
  private static final StructField KS_INNER_F = KITCHEN_SINK.field("inner", StructField.class);
  private static final ArrayField KS_CELLS_F = KITCHEN_SINK.field("cells", ArrayField.class);
  private static final StringField KC_NAME_F = KITCHEN_CELL.field("name", StringField.class);
  private static final Int64Field KC_VALUE_F = KITCHEN_CELL.field("value", Int64Field.class);
  private static final BoolField KC_FLAG_F = KITCHEN_CELL.field("flag", BoolField.class);

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

  private static ByteBuffer[] kitchenSinkByteBuffers() {
    return new ByteBuffer[] {
        ByteBuffer.wrap(new byte[0]),
        ByteBuffer.wrap(new byte[] {1, 2, 3}),
        ByteBuffer.wrap(new byte[] {(byte) 0xFF, 0x00})};
  }

  /**
   * Encodes the kitchen sink through the OLD API only (names, per-element arrays, nested
   * structs by name) - the exact shape of the pre-change generator that produced
   * {@link #GOLDEN_KITCHEN_SINK}.
   */
  private static StructEncoder<Void> kitchenSinkEncoderOldApi() {
    StructEncoder<Void> e = KITCHEN_SINK.encoder();
    e.string("name", KS_NAME).chr("grade", KS_GRADE_CHAR).int32("port", KS_PORT).int64("msn", KS_MSN)
        .fp64("load", KS_LOAD).bool("alive", true).enm("kind", Kind.PONG)
        .byteBuffer("payload", ByteBuffer.wrap(KS_PAYLOAD));

    ArrayEncoder<Integer, StructEncoder<Void>> a1 = e.int32s("int32s");
    for (int v : KS_INT32S) {
      a1.value(v);
    }
    a1.end();

    ArrayEncoder<Long, StructEncoder<Void>> a2 = e.int64s("int64s");
    for (long v : KS_INT64S) {
      a2.value(v);
    }
    a2.end();

    ArrayEncoder<Double, StructEncoder<Void>> a3 = e.fp64s("fp64s");
    for (double v : KS_FP64S) {
      a3.value(v);
    }
    a3.end();

    ArrayEncoder<Boolean, StructEncoder<Void>> a4 = e.bools("bools");
    for (boolean v : KS_BOOLS) {
      a4.value(v);
    }
    a4.end();

    ArrayEncoder<Character, StructEncoder<Void>> a5 = e.chrs("chrs");
    for (char v : KS_CHRS) {
      a5.value(v);
    }
    a5.end();

    ArrayEncoder<String, StructEncoder<Void>> a6 = e.strings("strings");
    for (String v : KS_STRINGS) {
      a6.value(v);
    }
    a6.end();

    ArrayEncoder<ByteBuffer, StructEncoder<Void>> a7 = e.byteBuffers("byteBuffers");
    for (ByteBuffer v : kitchenSinkByteBuffers()) {
      a7.value(v);
    }
    a7.end();

    e.struct("inner").string("name", "inner").int64("value", -42L).bool("flag", false).end();

    StructArrayEncoder<StructEncoder<Void>> cells = e.structs("cells");
    cells.add().string("name", "c-0").int64("value", 1L).bool("flag", true);
    cells.add().string("name", "c-1").int64("value", -2L).bool("flag", false);
    cells.add().string("name", "c-2").int64("value", Long.MAX_VALUE).bool("flag", true);
    return cells.end();
  }

  private static ByteBuffer encodeKitchenSinkOldApi() {
    return kitchenSinkEncoderOldApi().encode();
  }

  /**
   * Encodes the same kitchen sink exclusively through the NEW APIs: every scalar via its
   * field handle, every array via the bulk overload keyed by an {@link ArrayField} handle,
   * the nested struct via the {@link StructField} handle and the struct array via the
   * {@link ArrayField} handle.
   */
  private static ByteBuffer encodeKitchenSinkNewApi() {
    StructEncoder<Void> e = KITCHEN_SINK.encoder();
    e.string(KS_NAME_F, KS_NAME).chr(KS_GRADE_F, KS_GRADE_CHAR).int32(KS_PORT_F, KS_PORT).int64(KS_MSN_F, KS_MSN)
        .fp64(KS_LOAD_F, KS_LOAD).bool(KS_ALIVE_F, true).enm(KS_KIND_F, Kind.PONG)
        .byteBuffer(KS_PAYLOAD_F, ByteBuffer.wrap(KS_PAYLOAD));

    e.int32s(KS_INT32S_F, KS_INT32S);
    e.int64s(KS_INT64S_F, KS_INT64S);
    e.fp64s(KS_FP64S_F, KS_FP64S);
    e.bools(KS_BOOLS_F, KS_BOOLS);
    e.chrs(KS_CHRS_F, KS_CHRS);
    e.strings(KS_STRINGS_F, KS_STRINGS);
    e.byteBuffers(KS_BYTE_BUFFERS_F, kitchenSinkByteBuffers());

    e.struct(KS_INNER_F).string(KC_NAME_F, "inner").int64(KC_VALUE_F, -42L).bool(KC_FLAG_F, false).end();

    StructArrayEncoder<StructEncoder<Void>> cells = e.structs(KS_CELLS_F);
    cells.add().string(KC_NAME_F, "c-0").int64(KC_VALUE_F, 1L).bool(KC_FLAG_F, true);
    cells.add().string(KC_NAME_F, "c-1").int64(KC_VALUE_F, -2L).bool(KC_FLAG_F, false);
    cells.add().string(KC_NAME_F, "c-2").int64(KC_VALUE_F, Long.MAX_VALUE).bool(KC_FLAG_F, true);
    return cells.end().encode();
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

  @Test
  public void kitchenSinkGoldenOldApi() {
    assertGolden(GOLDEN_KITCHEN_SINK, encodeKitchenSinkOldApi());
  }

  @Test
  public void kitchenSinkGoldenNewApi() {
    // every new encoding path (handles, bulk arrays, struct/structs handles) must emit
    // exactly the bytes the pre-change code emitted for the same values
    assertGolden(GOLDEN_KITCHEN_SINK, encodeKitchenSinkNewApi());
  }

  @Test
  public void kitchenSinkEncodedSizeMatchesGolden() {
    StructEncoder<Void> e = kitchenSinkEncoderOldApi();
    int declared = e.encodedSize();
    assertThat(e.encode().position(), is(declared));
    assertThat(declared, is(GOLDEN_KITCHEN_SINK.length() / 2));
  }

  @Test
  public void kitchenSinkGoldenDecodeNewApi() {
    StructDecoder<?> d = KITCHEN_SINK.decoder(fromHex(GOLDEN_KITCHEN_SINK));
    // scalar fields via handles: boxed where the null/absent distinction matters,
    // primitive default-value overloads where it does not
    assertThat(d.string(KS_NAME_F), is(KS_NAME));
    assertThat(d.chr(KS_GRADE_F), is(KS_GRADE_CHAR));
    assertThat(d.int32(KS_PORT_F, -1), is(KS_PORT));
    assertThat(d.int64(KS_MSN_F, -1L), is(KS_MSN));
    assertThat(d.fp64(KS_LOAD_F, -1.0), is(KS_LOAD));
    assertThat(d.bool(KS_ALIVE_F, false), is(true));
    assertThat(d.enm(KS_KIND_F, Kind.PING), is(Kind.PONG));
    ByteBuffer payload = d.byteBuffer(KS_PAYLOAD_F);
    assertThat(payload.remaining(), is(4));
    byte[] payloadBytes = new byte[4];
    payload.get(payloadBytes);
    assertThat(payloadBytes, is(KS_PAYLOAD));

    // all seven bulk array decoders against the pre-change bytes
    assertThat(d.int32sArray(KS_INT32S_F), is(KS_INT32S));
    assertThat(d.int64sArray(KS_INT64S_F), is(KS_INT64S));
    double[] fp64s = d.fp64sArray(KS_FP64S_F);
    assertThat(fp64s.length, is(KS_FP64S.length));
    for (int i = 0; i < KS_FP64S.length; i++) {
      // bit comparison: NaN and infinities must survive the round trip exactly
      assertThat(Double.doubleToLongBits(fp64s[i]), is(Double.doubleToLongBits(KS_FP64S[i])));
    }
    assertThat(d.boolsArray(KS_BOOLS_F), is(KS_BOOLS));
    assertThat(d.chrsArray(KS_CHRS_F), is(KS_CHRS));
    assertThat(d.stringsArray(KS_STRINGS_F), is(KS_STRINGS));
    ByteBuffer[] byteBuffers = d.byteBuffersArray(KS_BYTE_BUFFERS_F);
    assertThat(byteBuffers.length, is(3));
    assertThat(byteBuffers[0].remaining(), is(0));
    byte[] second = new byte[3];
    byteBuffers[1].get(second);
    assertThat(second, is(new byte[] {1, 2, 3}));
    byte[] third = new byte[2];
    byteBuffers[2].get(third);
    assertThat(third, is(new byte[] {(byte) 0xFF, 0x00}));

    // nested struct via its handle, decoded with cell handles + primitive overloads
    StructDecoder<?> inner = d.struct(KS_INNER_F);
    assertThat(inner.string(KC_NAME_F), is("inner"));
    assertThat(inner.int64(KC_VALUE_F, 0L), is(-42L));
    assertThat(inner.bool(KC_FLAG_F, true), is(false));
    inner.end();

    // struct array via its handle, decoded with decodeAll + primitive overloads
    List<String> cells = d.structs(KS_CELLS_F).decodeAll(
        cell -> cell.string(KC_NAME_F) + ":" + cell.int64(KC_VALUE_F, Long.MIN_VALUE) + ":" + cell.bool(KC_FLAG_F, false));
    assertThat(cells, is(Arrays.asList("c-0:1:true", "c-1:-2:false", "c-2:9223372036854775807:true")));
  }

  @Test
  public void kitchenSinkGoldenDecodeNameBasedPrimitives() {
    // the name-based primitive/default-value overloads, against the pre-change bytes
    StructDecoder<?> d = KITCHEN_SINK.decoder(fromHex(GOLDEN_KITCHEN_SINK));
    assertThat(d.string("name"), is(KS_NAME));
    assertThat(d.chr("grade", 'x'), is(KS_GRADE_CHAR));
    assertThat(d.int32("port", -1), is(KS_PORT));
    assertThat(d.int64("msn", -1L), is(KS_MSN));
    assertThat(d.fp64("load", -1.0), is(KS_LOAD));
    assertThat(d.bool("alive", false), is(true));
    assertThat(d.enm("kind", Kind.PING), is(Kind.PONG));
    assertThat(d.byteBuffer("payload").remaining(), is(4));
  }
}
