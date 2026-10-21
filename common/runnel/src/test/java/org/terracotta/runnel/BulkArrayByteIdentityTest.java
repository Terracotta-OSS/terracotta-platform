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
import org.terracotta.runnel.decoding.ArrayDecoder;
import org.terracotta.runnel.decoding.StructDecoder;
import org.terracotta.runnel.decoding.fields.ArrayField;
import org.terracotta.runnel.encoding.ArrayEncoder;
import org.terracotta.runnel.encoding.StructArrayEncoder;
import org.terracotta.runnel.encoding.StructEncoder;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.fail;

/**
 * The bulk array overloads must produce byte-identical output to the per-element encoding,
 * and the bulk decoders must return the same values as per-element decoding.
 */
public class BulkArrayByteIdentityTest {

  private static final Struct STRUCT = StructBuilder.newStructBuilder()
      .int32s("int32s", 10)
      .int64s("int64s", 20)
      .fp64s("fp64s", 30)
      .bools("bools", 40)
      .chrs("chrs", 50)
      .strings("strings", 60)
      .byteBuffers("byteBuffers", 70)
      .build();

  private static final int[] INT32S = {1, -5, Integer.MAX_VALUE, 0, -98765};
  private static final long[] INT64S = {0L, -1L, Long.MAX_VALUE, Long.MIN_VALUE, 123456789012345L};
  private static final double[] FP64S = {0.0, -1.5, Double.MAX_VALUE, Double.MIN_VALUE, Math.PI};
  private static final boolean[] BOOLS = {true, false, true, true};
  private static final char[] CHRS = {'a', 'Z', '0', '\u007F', '\uFFFF'};
  private static final String[] STRINGS = {"", "abc", "with é accented", "北京"};
  private static final ByteBuffer[] BYTEBUFFERS = {
      ByteBuffer.wrap(new byte[0]),
      ByteBuffer.wrap(new byte[]{1, 2, 3}),
      ByteBuffer.wrap(new byte[]{(byte) 0xFF, 0, 42}),
  };

  private static ByteBuffer encodeElementWise() {
    StructEncoder<Void> e = STRUCT.encoder();
    ArrayEncoder<Integer, ?> a1 = e.int32s("int32s");
    for (int v : INT32S) a1.value(v);
    a1.end();
    ArrayEncoder<Long, ?> a2 = e.int64s("int64s");
    for (long v : INT64S) a2.value(v);
    a2.end();
    ArrayEncoder<Double, ?> a3 = e.fp64s("fp64s");
    for (double v : FP64S) a3.value(v);
    a3.end();
    ArrayEncoder<Boolean, ?> a4 = e.bools("bools");
    for (boolean v : BOOLS) a4.value(v);
    a4.end();
    ArrayEncoder<Character, ?> a5 = e.chrs("chrs");
    for (char v : CHRS) a5.value(v);
    a5.end();
    ArrayEncoder<String, ?> a6 = e.strings("strings");
    for (String v : STRINGS) a6.value(v);
    a6.end();
    ArrayEncoder<ByteBuffer, ?> a7 = e.byteBuffers("byteBuffers");
    for (ByteBuffer v : BYTEBUFFERS) a7.value(v.duplicate());
    a7.end();
    return e.encode();
  }

  private static ByteBuffer encodeBulk() {
    return STRUCT.encoder()
        .int32s("int32s", INT32S)
        .int64s("int64s", INT64S)
        .fp64s("fp64s", FP64S)
        .bools("bools", BOOLS)
        .chrs("chrs", CHRS)
        .strings("strings", STRINGS)
        .byteBuffers("byteBuffers", duplicates(BYTEBUFFERS))
        .encode();
  }

  private static ByteBuffer[] duplicates(ByteBuffer[] in) {
    ByteBuffer[] out = new ByteBuffer[in.length];
    for (int i = 0; i < in.length; i++) out[i] = in[i].duplicate();
    return out;
  }

  @Test
  public void bulkOutputIsByteIdentical() {
    ByteBuffer elementWise = encodeElementWise();
    ByteBuffer bulk = encodeBulk();
    assertThat(bulk.position(), is(elementWise.position()));
    byte[] a = new byte[elementWise.position()];
    (elementWise.flip()).get(a);
    byte[] b = new byte[bulk.position()];
    (bulk.flip()).get(b);
    assertArrayEquals(a, b);
  }

  @Test
  public void bulkDecodeMatchesElementWiseDecode() {
    ByteBuffer encoded = encodeElementWise();

    StructDecoder<?> d1 = STRUCT.decoder(encoded.duplicate().position(0));
    ArrayDecoder<Long, ?> a = d1.int64s("int64s"); // must skip int32s
    long[] viaElements = new long[a.length()];
    for (int i = 0; i < viaElements.length; i++) viaElements[i] = a.value();
    a.end();

    StructDecoder<?> d2 = STRUCT.decoder(encoded.duplicate().position(0));
    d2.int32s("int32s").end();
    long[] viaBulk = d2.int64sArray("int64s");

    assertArrayEquals(viaElements, viaBulk);
  }

  @Test
  public void allBulkDecodersRoundTrip() {
    ByteBuffer encoded = encodeBulk();
    StructDecoder<?> d = STRUCT.decoder(encoded.duplicate().position(0));
    assertArrayEquals(INT32S, d.int32sArray("int32s"));
    assertArrayEquals(INT64S, d.int64sArray("int64s"));
    assertArrayEquals(FP64S, d.fp64sArray("fp64s"), 0.0);
    boolean[] bools = d.boolsArray("bools");
    assertThat(bools.length, is(BOOLS.length));
    for (int i = 0; i < BOOLS.length; i++) assertThat(bools[i], is(BOOLS[i]));
    assertArrayEquals(CHRS, d.chrsArray("chrs"));
    assertArrayEquals(STRINGS, d.stringsArray("strings"));
    ByteBuffer[] bbs = d.byteBuffersArray("byteBuffers");
    assertThat(bbs.length, is(BYTEBUFFERS.length));
    for (int i = 0; i < BYTEBUFFERS.length; i++) {
      ByteBuffer expected = BYTEBUFFERS[i].duplicate().position(0);
      assertThat(bbs[i].remaining(), is(expected.remaining()));
      byte[] x = new byte[bbs[i].remaining()]; bbs[i].get(x);
      byte[] y = new byte[expected.remaining()]; expected.get(y);
      assertArrayEquals(x, y);
    }
  }

  @Test
  public void absentArrayFieldsReturnNullFromBulkDecoders() {
    ByteBuffer encoded = STRUCT.encoder().int64s("int64s", INT64S).encode();
    StructDecoder<?> d = STRUCT.decoder(encoded.duplicate().position(0));
    assertThat(d.int32sArray("int32s") == null, is(true)); // skipped over
    assertThat(d.int64sArray("int64s"), is(INT64S));
    StructDecoder<?> d2 = STRUCT.decoder(encoded.duplicate().position(0));
    d2.int64s("int64s").end();
    assertThat(d2.stringsArray("strings") == null, is(true)); // absent at end
  }

  @Test
  public void encodedSizeMatchesActualOutput() {
    StructEncoder<Void> e1 = encodeElementWiseProducer();
    int declared = e1.encodedSize();
    ByteBuffer bb = e1.encode();
    assertThat(bb.position(), is(declared));
    assertThat(e1.encode().position(), is(declared));
  }

  @Test
  public void encodingAfterSizedStructureWasModifiedFailsLoudly() {
    Struct element = StructBuilder.newStructBuilder().int32("x", 1).build();
    Struct struct = StructBuilder.newStructBuilder().structs("elements", 1, element).int32("after", 2).build();

    StructEncoder<Void> encoder = struct.encoder();
    StructArrayEncoder<StructEncoder<Void>> elements = encoder.structs("elements");
    elements.add().int32("x", 1);
    elements.add().int32("x", 2);
    encoder.encodedSize(); // memoizes the nested array size...
    elements.add().int32("x", 3); // ...which this makes stale
    elements.end().int32("after", 42);

    // a roomy (e.g. pooled) target buffer must not silently receive a corrupt frame
    try {
      encoder.encode(ByteBuffer.allocate(1024));
      fail();
    } catch (IllegalStateException expected) {
    }
    try {
      encoder.encode();
      fail();
    } catch (IllegalStateException | BufferOverflowException expected) {
    }
  }

  @Test
  public void addingScalarFieldsAfterEncodedSizeStillEncodesCorrectly() {
    // encodedSize()'s result goes stale when fields are added afterwards, but encode()
    // recalculates: the frame must come out valid and byte-identical to a fresh build
    StructEncoder<Void> e = STRUCT.encoder().int32s("int32s", INT32S);
    int stale = e.encodedSize();
    e.int64s("int64s", INT64S);

    ByteBuffer bb = e.encode();
    assertThat(bb.position() > stale, is(true));

    StructEncoder<Void> fresh = STRUCT.encoder().int32s("int32s", INT32S).int64s("int64s", INT64S);
    ByteBuffer expected = fresh.encode();
    assertThat(bb.position(), is(expected.position()));
    byte[] a = new byte[bb.position()]; (bb.flip()).get(a);
    byte[] b = new byte[expected.position()]; (expected.flip()).get(b);
    assertArrayEquals(b, a);
  }

  private static StructEncoder<Void> encodeElementWiseProducer() {
    return STRUCT.encoder()
        .int32s("int32s", INT32S)
        .int64s("int64s", INT64S)
        .fp64s("fp64s", FP64S)
        .bools("bools", BOOLS)
        .chrs("chrs", CHRS)
        .strings("strings", STRINGS);
  }

  @Test
  public void emptyArraysByteIdentical() {
    ByteBuffer a = STRUCT.encoder().int32s("int32s", new int[0]).encode();
    StructEncoder<Void> e2 = STRUCT.encoder();
    Object end = e2.int32s("int32s").end();
    @SuppressWarnings("unchecked")
    ByteBuffer b = ((StructEncoder<Void>) end).encode();
    assertThat(a.position(), is(b.position()));
    byte[] x = new byte[a.position()]; (a.flip()).get(x);
    byte[] y = new byte[b.position()]; (b.flip()).get(y);
    assertArrayEquals(x, y);

    StructDecoder<?> d = STRUCT.decoder(ByteBuffer.wrap(x));
    assertThat(Arrays.equals(d.int32sArray("int32s"), new int[0]), is(true));
  }

  @Test
  public void tokenKeyedBulkEncodeIsByteIdentical() {
    ArrayField i32 = STRUCT.field("int32s", ArrayField.class);
    ArrayField i64 = STRUCT.field("int64s", ArrayField.class);
    ArrayField f64 = STRUCT.field("fp64s", ArrayField.class);
    ArrayField bs = STRUCT.field("bools", ArrayField.class);
    ArrayField cs = STRUCT.field("chrs", ArrayField.class);
    ArrayField ss = STRUCT.field("strings", ArrayField.class);
    ArrayField bbs = STRUCT.field("byteBuffers", ArrayField.class);
    ByteBuffer bulkByToken = STRUCT.encoder()
        .int32s(i32, INT32S)
        .int64s(i64, INT64S)
        .fp64s(f64, FP64S)
        .bools(bs, BOOLS)
        .chrs(cs, CHRS)
        .strings(ss, STRINGS)
        .byteBuffers(bbs, duplicates(BYTEBUFFERS))
        .encode();
    ByteBuffer byName = encodeBulk();
    byte[] x = new byte[byName.position()]; (byName.flip()).get(x);
    byte[] y = new byte[bulkByToken.position()]; (bulkByToken.flip()).get(y);
    assertArrayEquals(x, y);
  }
}
