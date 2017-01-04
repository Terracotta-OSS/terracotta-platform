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

import org.hamcrest.core.Is;
import org.junit.Test;
import org.terracotta.runnel.decoding.StructArrayDecoder;
import org.terracotta.runnel.decoding.StructDecoder;
import org.terracotta.runnel.encoding.StructArrayEncoder;
import org.terracotta.runnel.encoding.StructEncoder;
import org.terracotta.runnel.encoding.StructEncoderFunction;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.fail;

/**
 * @author Ludovic Orban
 */
public class StructArrayStructBuilderTest {

  enum Type {
    STRING, INT
  }

  private final EnumMapping<Type> typeEnumMapping = EnumMappingBuilder.newEnumMappingBuilder(Type.class)
      .mapping(Type.STRING, 0)
      .mapping(Type.INT, 1)
      .build();

  private final Struct anotherStruct = StructBuilder.newStructBuilder()
      .int32("aaa", 1)
      .build();

  private final Struct mapEntry = StructBuilder.newStructBuilder()
      .string("key", 1)
      .enm("type", 2, typeEnumMapping)
      .string("string", 10)
      .string("int", 11)
      .struct("anotherStruct", 12, anotherStruct)
      .build();

  private final Struct struct = StructBuilder.newStructBuilder()
      .string("name", 1)
      .structs("mapEntry", 2, mapEntry)
      .int64("id", 3)
      .build();

  @Test
  public void testReadAll() throws Exception {
    ByteBuffer bb =
      struct.encoder()
        .string("name", "joe")
        .structs("mapEntry")
          .add()
            .string("key", "1")
            .enm("type", Type.STRING)
            .string("string", "one")
            .struct("anotherStruct")
              .int32("aaa", 1)
              .end()
            .end()
          .add()
            .string("key", "2")
            .enm("type", Type.STRING)
            .string("string", "two")
            .end()
          .end()
        .int64("id", 999L)
        .encode();

    bb.rewind();

    StructDecoder<Void> decoder = struct.decoder(bb);

    assertThat(decoder.string("name"), is("joe"));

    StructArrayDecoder<StructDecoder<Void>> sad = decoder.structs("mapEntry");
    assertThat(sad.length(), is(2));
    StructDecoder<StructArrayDecoder<StructDecoder<Void>>> first = sad.next();
    assertThat(first.string("key"), is("1"));
    assertThat(first.<Type>enm("type").get(), is(Type.STRING));
    assertThat(first.string("string"), is("one"));
    StructDecoder<StructDecoder<StructArrayDecoder<StructDecoder<Void>>>> anotherStruct = first.struct("anotherStruct");
    assertThat(anotherStruct.int32("aaa"), is(1));
    anotherStruct.end();
    StructDecoder<StructArrayDecoder<StructDecoder<Void>>> second = sad.next();
    assertThat(second.string("key"), is("2"));
    assertThat(second.<Type>enm("type").get(), is(Type.STRING));
    assertThat(second.string("string"), is("two"));
    sad.end();

    assertThat(decoder.int64("id"), is(999L));
  }

  @Test
  public void testReadAll_withLambda() throws Exception {
    Map<String, String> stuff = new LinkedHashMap<String, String>();
    stuff.put("1", "one");
    stuff.put("2", "two");

    ByteBuffer bb = struct.encoder()
        .string("name", "joe")
        .structs("mapEntry", stuff.entrySet(), new StructEncoderFunction<Map.Entry<String, String>>() {
          @Override
          public void encode(StructEncoder<?> encoder, Map.Entry<String, String> entry) {
            encoder
                .string("key", entry.getKey())
                .enm("type", Type.STRING)
                .string("string", entry.getValue());
          }
        })
        .int64("id", 999L)
        .encode();

    bb.rewind();

    StructDecoder<Void> decoder = struct.decoder(bb);

    assertThat(decoder.string("name"), is("joe"));

    StructArrayDecoder<StructDecoder<Void>> sad = decoder.structs("mapEntry");
    assertThat(sad.length(), is(2));
    StructDecoder<StructArrayDecoder<StructDecoder<Void>>> first = sad.next();
    assertThat(first.string("key"), is("1"));
    assertThat(first.enm("type").get(), Is.<Object>is(Type.STRING));
    assertThat(first.string("string"), is("one"));
    StructDecoder<StructArrayDecoder<StructDecoder<Void>>> second = sad.next();
    assertThat(second.string("key"), is("2"));
    assertThat(second.enm("type").get(), Is.<Object>is(Type.STRING));
    assertThat(second.string("string"), is("two"));
    sad.end();

    assertThat(decoder.int64("id"), is(999L));
  }

  @Test
  public void testDump() throws Exception {
    ByteBuffer bb = struct.encoder()
        .string("name", "joe")
        .structs("mapEntry").add()
          .string("key", "1")
          .enm("type", Type.STRING)
          .string("string", "one")
          .end()
        .add()
          .string("key", "2")
          .string("string", "two")
          .end()
        .end()
        .int64("id", 999L)
        .encode();

    bb.rewind();

    struct.dump(bb, new PrintStream(new ByteArrayOutputStream()));
  }

  @Test
  public void testSkipArrayEntry() throws Exception {
    ByteBuffer bb = struct.encoder()
        .string("name", "joe")
        .structs("mapEntry").add()
          .string("key", "1")
          .enm("type", Type.STRING)
          .string("string", "one")
          .end()
        .add()
          .string("key", "2")
          .enm("type", Type.STRING)
          .string("string", "two")
          .end()
        .end()
        .int64("id", 999L)
        .encode();

    bb.rewind();

    StructDecoder<Void> decoder = struct.decoder(bb);

    assertThat(decoder.string("name"), is("joe"));

    StructArrayDecoder<StructDecoder<Void>> sad = decoder.structs("mapEntry");
    assertThat(sad.length(), is(2));
    StructDecoder<StructArrayDecoder<StructDecoder<Void>>> first = sad.next();
    assertThat(first.string("key"), is("1"));
    assertThat(first.enm("type").get(), Is.<Object>is(Type.STRING));
    assertThat(first.string("string"), is("one"));
    sad.end();

    assertThat(decoder.int64("id"), is(999L));
  }

  @Test
  public void testSkipStructEntry() throws Exception {
    ByteBuffer bb = struct.encoder()
        .string("name", "joe")
        .structs("mapEntry").add()
          .string("key", "1")
          .enm("type", Type.STRING)
          .string("string", "one")
          .end()
        .add()
          .string("key", "2")
          .string("string", "two")
          .end()
        .end()
        .int64("id", 999L)
        .encode();

    bb.rewind();

    StructDecoder<Void> decoder = struct.decoder(bb);

    assertThat(decoder.string("name"), is("joe"));

    StructArrayDecoder sad = decoder.structs("mapEntry");
    assertThat(sad.length(), is(2));
    StructDecoder<StructArrayDecoder<StructDecoder<Void>>> first = sad.next();
    assertThat(first.string("key"), is("1"));
    StructDecoder<StructArrayDecoder<StructDecoder<Void>>> second = sad.next();
    assertThat(second.string("string"), is("two"));
    sad.end();

    assertThat(decoder.int64("id"), is(999L));
  }

  @Test
  public void testSkipStructsContents() throws Exception {
    ByteBuffer bb = struct.encoder()
        .string("name", "joe")
        .structs("mapEntry").add()
          .string("key", "1")
          .enm("type", Type.STRING)
          .string("string", "one")
          .end()
        .add()
          .string("key", "2")
          .enm("type", Type.STRING)
          .string("string", "two")
          .end()
        .end()
        .int64("id", 999L)
        .encode();

    bb.rewind();

    StructDecoder<Void> decoder = struct.decoder(bb);

    assertThat(decoder.string("name"), is("joe"));

    StructArrayDecoder<StructDecoder<Void>> sad = decoder.structs("mapEntry");
    assertThat(sad.length(), is(2));
    sad.end();

    assertThat(decoder.int64("id"), is(999L));
  }

  @Test
  public void testSkipArray() throws Exception {
    ByteBuffer bb = struct.encoder()
        .string("name", "joe")
        .structs("mapEntry").add()
          .string("key", "1")
          .enm("type", Type.STRING)
          .string("string", "one")
          .end()
        .add()
          .string("key", "2")
          .string("string", "two")
          .end()
        .end()
        .int64("id", 999L)
        .encode();

    bb.rewind();

    StructDecoder<Void> decoder = struct.decoder(bb);

    assertThat(decoder.string("name"), is("joe"));
    assertThat(decoder.int64("id"), is(999L));
  }

  @Test
  public void testNextBeforeEnd() throws Exception {

    StructArrayEncoder<StructEncoder<Void>> arrayEncoder = struct.encoder()
            .string("name", "joe")
            .structs("mapEntry");

    arrayEncoder.add()
      .string("key", "1")
      .enm("type", Type.STRING)
      .string("string", "one")
      .end()
    .add()
      .string("key", "2")
      .string("string", "two");

    ByteBuffer bb = arrayEncoder.add().end()
        .end()
        .int64("id", 999L)
        .encode();

    bb.rewind();

    StructDecoder<Void> decoder = struct.decoder(bb);

    assertThat(decoder.string("name"), is("joe"));
    StructArrayDecoder<StructDecoder<Void>> sad = decoder.structs("mapEntry");
    assertThat(sad.length(), is(2));
    StructDecoder<StructArrayDecoder<StructDecoder<Void>>> first = sad.next();
    assertThat(first.string("key"), is("1"));
    assertThat(first.enm("type").get(), Is.<Object>is(Type.STRING));
    assertThat(first.string("string"), is("one"));
    StructDecoder<StructArrayDecoder<StructDecoder<Void>>> second = sad.next();
    assertThat(second.string("key"), is("2"));
    assertThat(second.enm("type").isValid(), is(false));
    assertThat(second.string("string"), is("two"));
    assertThat(sad.hasNext(), is(false));
    try {
      sad.next();
      fail("Expected NoSuchElementException");
    } catch (NoSuchElementException e) {
      //expected
    }
    sad.end();
    assertThat(decoder.int64("id"), is(999L));
  }

  @Test
  public void testEmptyStructArray() throws Exception {
    Struct struct = StructBuilder.newStructBuilder()
        .structs("mapEntry", 2, mapEntry)
        .build();

    ByteBuffer bb = struct.encoder()
        .structs("mapEntry")
        .end()
        .encode();

    bb.rewind();

    StructDecoder<Void> decoder = struct.decoder(bb);

    StructArrayDecoder<StructDecoder<Void>> sad = decoder.structs("mapEntry");
    assertThat(sad.length(), is(0));
    sad.end();
  }

  @Test
  public void testStructArrayEmptyFirstField() throws Exception {
    ByteBuffer bb = struct.encoder()
        .string("name", "joe")
        .structs("mapEntry").add()
          .string("string", "a").end()
        .add()
          .string("string", "b").end()
        .add()
          .string("string", "c").end()
        .end()
        .int64("id", 999L)
        .encode();

    bb.rewind();

    StructDecoder<Void> decoder = struct.decoder(bb);

    assertThat(decoder.string("name"), is("joe"));

    StructArrayDecoder<StructDecoder<Void>> sad = decoder.structs("mapEntry");
    assertThat(sad.length(), is(3));
    StructDecoder<StructArrayDecoder<StructDecoder<Void>>> first = sad.next();
    assertThat(first.string("key"), is(nullValue()));
    assertThat(first.enm("type").isFound(), is(false));
    assertThat(first.string("string"), is("a"));
    StructDecoder<StructArrayDecoder<StructDecoder<Void>>> second = sad.next();
    assertThat(second.string("key"), is(nullValue()));
    assertThat(second.enm("type").isFound(), is(false));
    assertThat(second.string("string"), is("b"));
    StructDecoder<StructArrayDecoder<StructDecoder<Void>>> third = sad.next();
    assertThat(third.string("key"), is(nullValue()));
    assertThat(third.enm("type").isFound(), is(false));
    assertThat(third.string("string"), is("c"));
    sad.end();

    assertThat(decoder.int64("id"), is(999L));
  }

}
