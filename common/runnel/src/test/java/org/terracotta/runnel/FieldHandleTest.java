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
import org.terracotta.runnel.decoding.Enm;
import org.terracotta.runnel.decoding.StructDecoder;
import org.terracotta.runnel.decoding.fields.BoolField;
import org.terracotta.runnel.decoding.fields.EnumField;
import org.terracotta.runnel.decoding.fields.Int32Field;
import org.terracotta.runnel.decoding.fields.Int64Field;
import org.terracotta.runnel.decoding.fields.StringField;
import org.terracotta.runnel.encoding.StructEncoder;

import java.nio.ByteBuffer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.fail;

/**
 * Field handle (token) APIs must be byte-identical to the name-based ones on encode,
 * value-identical on decode, and must enforce the exact same ordering/type validations.
 */
public class FieldHandleTest {

  enum Colour { RED, GREEN, BLUE }

  private static final EnumMapping<Colour> COLOURS = EnumMappingBuilder.newEnumMappingBuilder(Colour.class)
      .mapping(Colour.RED, 0).mapping(Colour.GREEN, 1).mapping(Colour.BLUE, 2).build();

  private static final Struct STRUCT = StructBuilder.newStructBuilder()
      .string("name", 10)
      .int64("id", 20)
      .bool("flag", 30)
      .enm("colour", 40, COLOURS)
      .build();

  private static final Struct OTHER_STRUCT = StructBuilder.newStructBuilder()
      .int64("other", 10)
      .build();

  @Test
  public void tokenEncodeByteIdenticalToNameEncode() {
    ByteBuffer byName = STRUCT.encoder()
        .string("name", "alice").int64("id", 1234567890123L).bool("flag", true).enm("colour", Colour.BLUE)
        .encode();

    StringField name = STRUCT.field("name", StringField.class);
    Int64Field id = STRUCT.field("id", Int64Field.class);
    BoolField flag = STRUCT.field("flag", BoolField.class);
    @SuppressWarnings("unchecked")
    EnumField<Colour> colour = (EnumField<Colour>) STRUCT.field("colour", EnumField.class);

    ByteBuffer byToken = STRUCT.encoder()
        .string(name, "alice").int64(id, 1234567890123L).bool(flag, true).enm(colour, Colour.BLUE)
        .encode();

    assertThat(byName.position(), is(byToken.position()));
    byte[] a = new byte[byName.position()]; (byName.flip()).get(a);
    byte[] b = new byte[byToken.position()]; (byToken.flip()).get(b);
    assertArrayEquals(a, b);
  }

  @Test
  public void tokenDecodeMatchesNameDecode() {
    ByteBuffer encoded = STRUCT.encoder()
        .string("name", "alice").int64("id", 1234567890123L).bool("flag", true).enm("colour", Colour.BLUE)
        .encode();

    StructDecoder<?> d = STRUCT.decoder(encoded.duplicate().position(0));
    assertThat(d.string(STRUCT.field("name", StringField.class)), is("alice"));
    assertThat(d.int64(STRUCT.field("id", Int64Field.class)), is(1234567890123L));
    assertThat(d.bool(STRUCT.field("flag", BoolField.class)), is(true));
    Enm<Colour> c = d.enm(colourField());
    assertThat(c.get(), is(Colour.BLUE));
  }

  @SuppressWarnings("unchecked")
  private static EnumField<Colour> colourField() {
    return (EnumField<Colour>) STRUCT.field("colour", EnumField.class);
  }

  @Test
  public void primitiveTokenDecodesAvoidBoxingPathsButMatch() {
    ByteBuffer encoded = STRUCT.encoder()
        .string("name", "alice").int64("id", 1234567890123L).bool("flag", true).enm("colour", Colour.BLUE)
        .encode();

    StructDecoder<?> d = STRUCT.decoder(encoded.duplicate().position(0));
    assertThat(d.string("name"), is("alice"));
    assertThat(d.int64(STRUCT.field("id", Int64Field.class), -1L), is(1234567890123L));
    assertThat(d.bool(STRUCT.field("flag", BoolField.class), false), is(true));
    assertThat(d.enm(colourField(), Colour.RED), is(Colour.BLUE));
  }

  @Test
  public void primitiveDecodesReturnDefaultsWhenAbsent() {
    ByteBuffer encoded = STRUCT.encoder().string("name", "alice").encode();

    StructDecoder<?> d = STRUCT.decoder(encoded.duplicate().position(0));
    assertThat(d.string("name"), is("alice"));
    assertThat(d.int64("id", -42L), is(-42L));
    assertThat(d.bool("flag", true), is(true));
    assertThat(d.enm("colour", Colour.GREEN), is(Colour.GREEN)); // absent -> default

    // note: each field may only be queried once per decoder (ascending-index stream contract),
    // so the boxed-absent check uses a fresh decoder
    StructDecoder<?> d2 = STRUCT.decoder(encoded.duplicate().position(0));
    d2.string("name");
    assertThat(d2.int64("id"), nullValue());
  }

  @Test
  public void unmappableEnumDecodesToDefaultInPrimitiveVariant() {
    // encode 99 (unknown id) by hand: via a struct that maps BLUE to 99
    EnumMapping<Colour> other = EnumMappingBuilder.newEnumMappingBuilder(Colour.class)
        .mapping(Colour.RED, 0).mapping(Colour.GREEN, 1).mapping(Colour.BLUE, 99).build();
    Struct alienStruct = StructBuilder.newStructBuilder()
        .string("name", 10).int64("id", 20).bool("flag", 30).enm("colour", 40, other).build();
    ByteBuffer encoded = alienStruct.encoder()
        .string("name", "x").int64("id", 5L).bool("flag", false).enm("colour", Colour.BLUE).encode();

    StructDecoder<?> d = STRUCT.decoder(encoded.duplicate().position(0));
    assertThat(d.string("name"), is("x"));
    assertThat(d.int64("id", -1), is(5L));
    assertThat(d.bool("flag", true), is(false));
    assertThat(d.enm("colour", Colour.GREEN), is(Colour.GREEN)); // unmappable -> default

    // while the Enm-based decode still surfaces the raw value
    StructDecoder<?> d2 = STRUCT.decoder(encoded.duplicate().position(0));
    d2.string("name"); d2.int64("id"); d2.bool("flag");
    Enm<Colour> enm = d2.enm("colour");
    assertThat(enm.isFound(), is(true));
    assertThat(enm.isValid(), is(false));
    assertThat(enm.raw(), is(99));
  }

  @Test
  public void tokenApiEnforcesOrdering() {
    Int64Field id = STRUCT.field("id", Int64Field.class);
    StringField name = STRUCT.field("name", StringField.class);
    StructEncoder<Void> e = STRUCT.encoder();
    e.int64(id, 1L);
    try {
      e.string(name, "late"); // name (10) after id (20) -> must fail like name-based API
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  @Test
  public void tokenApiRejectsForeignFields() {
    Int64Field foreign = OTHER_STRUCT.field("other", Int64Field.class);
    try {
      STRUCT.encoder().int64(foreign, 1L);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  @Test
  public void handlesAreBoundToTheStructInstance() {
    // a twin struct built from the same definition has an equal field by name and index, but
    // handles are identity-bound: the name-based API accepts the twin, handles do not
    Struct twin = StructBuilder.newStructBuilder()
        .string("name", 10)
        .int64("id", 20)
        .bool("flag", 30)
        .enm("colour", 40, COLOURS)
        .build();
    Int64Field twinId = twin.field("id", Int64Field.class);
    Int64Field ownId = STRUCT.field("id", Int64Field.class);
    assertThat(twinId.name(), is(ownId.name()));
    assertThat(twinId.index(), is(ownId.index()));
    assertThat(twinId == ownId, is(false));

    ByteBuffer encoded = STRUCT.encoder().string("name", "n").int64("id", 7L).encode();

    try {
      STRUCT.encoder().string("name", "n").int64(twinId, 7L);
      fail();
    } catch (IllegalArgumentException expected) {
    }
    try {
      STRUCT.decoder(encoded.duplicate().position(0)).int64(twinId);
      fail();
    } catch (IllegalArgumentException expected) {
    }

    // the same values via the name-based API work across instances
    assertThat(STRUCT.decoder(encoded.duplicate().position(0)).int64("id"), is(7L));
    assertThat(twin.decoder(encoded.duplicate().position(0)).int64("id"), is(7L));
    assertThat(twin.decoder(encoded.duplicate().position(0)).int64(twinId), is(7L));
  }

  @Test
  public void tokenApiEnforcesType() {
    try {
      STRUCT.field("id", StringField.class);
      fail();
    } catch (IllegalArgumentException expected) {
    }
    try {
      STRUCT.field("missing", StringField.class);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  @Test
  public void decodeTokenApiEnforcesOrderingAndType() {
    ByteBuffer encoded = STRUCT.encoder()
        .string("name", "alice").int64("id", 7L).bool("flag", true).enm("colour", Colour.RED).encode();
    StructDecoder<?> d = STRUCT.decoder(encoded.duplicate().position(0));
    Int64Field id = STRUCT.field("id", Int64Field.class);
    StringField name = STRUCT.field("name", StringField.class);
    assertThat(d.int64(id), is(7L));
    try {
      d.string(name); // out of order
      fail();
    } catch (IllegalArgumentException expected) {
    }

    StructDecoder<?> d2 = STRUCT.decoder(encoded.duplicate().position(0));
    try {
      d2.int64(OTHER_STRUCT.field("other", Int64Field.class));
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  @Test
  public void mistypedHandleLookupIsRejectedAtResolution() {
    // the realistic mistyping route is the typed lookup itself: resolving a field with the
    // wrong expected class is rejected up front (a reference lyingly cast to another field
    // type cannot even be constructed in pure Java — the runtime checkcast throws first)
    try {
      STRUCT.field("name", Int32Field.class);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }
}
