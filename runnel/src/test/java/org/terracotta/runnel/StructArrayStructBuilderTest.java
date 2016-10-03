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
import org.terracotta.runnel.encoding.StructArrayEncoderFunction;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

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

  private final Struct mapEntry = StructBuilder.newStructBuilder()
      .string("key", 1)
      .enm("type", 2, typeEnumMapping)
      .string("string", 10)
      .string("int", 11)
      .build();

  private final Struct struct = StructBuilder.newStructBuilder()
      .string("name", 1)
      .structs("mapEntry", 2, mapEntry)
      .int64("id", 3)
      .build();

  @Test
  public void testReadAll() throws Exception {
    ByteBuffer bb = struct.encoder()
        .string("name", "joe")
        .structs("mapEntry")
          .string("key", "1")
          .enm("type", Type.STRING)
          .string("string", "one")
        .next()
          .string("key", "2")
          .enm("type", Type.STRING)
          .string("string", "two")
        .end()
        .int64("id", 999L)
        .encode();

    bb.rewind();

    StructDecoder decoder = struct.decoder(bb);

    assertThat(decoder.string("name"), is("joe"));

    StructArrayDecoder sad = decoder.structs("mapEntry");
    assertThat(sad.length(), is(2));
    assertThat(sad.string("key"), is("1"));
    assertThat(sad.enm("type").get(), Is.<Object>is(Type.STRING));
    assertThat(sad.string("string"), is("one"));
    sad.next();
    assertThat(sad.string("key"), is("2"));
    assertThat(sad.enm("type").get(), Is.<Object>is(Type.STRING));
    assertThat(sad.string("string"), is("two"));
    sad.end();

    assertThat(decoder.int64("id"), is(999L));
  }

  @Test
  public void testReadAll_withLambda() throws Exception {
    ByteBuffer bb = struct.encoder()
        .string("name", "joe")
        .structs("mapEntry", new StructArrayEncoderFunction() {
          @Override
          public void encode(StructArrayEncoder encoder) {
            encoder
                  .string("key", "1")
                  .enm("type", Type.STRING)
                  .string("string", "one")
                .next()
                  .string("key", "2")
                  .enm("type", Type.STRING)
                  .string("string", "two");
          }
        })
        .int64("id", 999L)
        .encode();

    bb.rewind();

    StructDecoder decoder = struct.decoder(bb);

    assertThat(decoder.string("name"), is("joe"));

    StructArrayDecoder sad = decoder.structs("mapEntry");
    assertThat(sad.length(), is(2));
    assertThat(sad.string("key"), is("1"));
    assertThat(sad.enm("type").get(), Is.<Object>is(Type.STRING));
    assertThat(sad.string("string"), is("one"));
    sad.next();
    assertThat(sad.string("key"), is("2"));
    assertThat(sad.enm("type").get(), Is.<Object>is(Type.STRING));
    assertThat(sad.string("string"), is("two"));
    sad.end();

    assertThat(decoder.int64("id"), is(999L));
  }

  @Test
  public void testDump() throws Exception {
    ByteBuffer bb = struct.encoder()
        .string("name", "joe")
        .structs("mapEntry")
          .string("key", "1")
          .enm("type", Type.STRING)
          .string("string", "one")
        .next()
          .string("key", "2")
          .string("string", "two")
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
        .structs("mapEntry")
          .string("key", "1")
          .enm("type", Type.STRING)
          .string("string", "one")
        .next()
          .string("key", "2")
          .enm("type", Type.STRING)
          .string("string", "two")
        .end()
        .int64("id", 999L)
        .encode();

    bb.rewind();

    StructDecoder decoder = struct.decoder(bb);

    assertThat(decoder.string("name"), is("joe"));

    StructArrayDecoder sad = decoder.structs("mapEntry");
    assertThat(sad.length(), is(2));
    assertThat(sad.string("key"), is("1"));
    assertThat(sad.enm("type").get(), Is.<Object>is(Type.STRING));
    assertThat(sad.string("string"), is("one"));
    sad.end();

    assertThat(decoder.int64("id"), is(999L));
  }

  @Test
  public void testSkipStructEntry() throws Exception {
    ByteBuffer bb = struct.encoder()
        .string("name", "joe")
        .structs("mapEntry")
          .string("key", "1")
          .enm("type", Type.STRING)
          .string("string", "one")
        .next()
          .string("key", "2")
          .string("string", "two")
        .end()
        .int64("id", 999L)
        .encode();

    bb.rewind();

    StructDecoder decoder = struct.decoder(bb);

    assertThat(decoder.string("name"), is("joe"));

    StructArrayDecoder sad = decoder.structs("mapEntry");
    assertThat(sad.length(), is(2));
    assertThat(sad.string("key"), is("1"));
    sad.next();
    assertThat(sad.string("string"), is("two"));
    sad.end();

    assertThat(decoder.int64("id"), is(999L));
  }

  @Test
  public void testSkipStructsContents() throws Exception {
    ByteBuffer bb = struct.encoder()
        .string("name", "joe")
        .structs("mapEntry")
          .string("key", "1")
          .enm("type", Type.STRING)
          .string("string", "one")
        .next()
          .string("key", "2")
          .enm("type", Type.STRING)
          .string("string", "two")
        .end()
        .int64("id", 999L)
        .encode();

    bb.rewind();

    StructDecoder decoder = struct.decoder(bb);

    assertThat(decoder.string("name"), is("joe"));

    StructArrayDecoder sad = decoder.structs("mapEntry");
    assertThat(sad.length(), is(2));
    sad.end();

    assertThat(decoder.int64("id"), is(999L));
  }

  @Test
  public void testSkipArray() throws Exception {
    ByteBuffer bb = struct.encoder()
        .string("name", "joe")
        .structs("mapEntry")
          .string("key", "1")
          .enm("type", Type.STRING)
          .string("string", "one")
        .next()
          .string("key", "2")
          .string("string", "two")
        .end()
        .int64("id", 999L)
        .encode();

    bb.rewind();

    StructDecoder decoder = struct.decoder(bb);

    assertThat(decoder.string("name"), is("joe"));
    assertThat(decoder.int64("id"), is(999L));
  }

  @Test
  public void testNextBeforeEnd() throws Exception {
    ByteBuffer bb = struct.encoder()
        .string("name", "joe")
        .structs("mapEntry")
          .string("key", "1")
          .enm("type", Type.STRING)
          .string("string", "one")
        .next()
          .string("key", "2")
          .string("string", "two")
        .next()
        .end()
        .int64("id", 999L)
        .encode();

    bb.rewind();

    StructDecoder decoder = struct.decoder(bb);

    assertThat(decoder.string("name"), is("joe"));
    StructArrayDecoder sad = decoder.structs("mapEntry");
    assertThat(sad.length(), is(2));
    assertThat(sad.string("key"), is("1"));
    assertThat(sad.enm("type").get(), Is.<Object>is(Type.STRING));
    assertThat(sad.string("string"), is("one"));
    sad.next();
    assertThat(sad.string("key"), is("2"));
    assertThat(sad.enm("type").isValid(), is(false));
    assertThat(sad.string("string"), is("two"));
    sad.next();
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

    StructDecoder decoder = struct.decoder(bb);

    StructArrayDecoder sad = decoder.structs("mapEntry");
    assertThat(sad.length(), is(0));
    sad.end();
  }

  @Test
  public void testStructArrayEmptyFirstField() throws Exception {
    ByteBuffer bb = struct.encoder()
        .string("name", "joe")
        .structs("mapEntry")
          .string("string", "a")
        .next()
          .string("string", "b")
        .next()
          .string("string", "c")
        .end()
        .int64("id", 999L)
        .encode();

    bb.rewind();

    StructDecoder decoder = struct.decoder(bb);

    assertThat(decoder.string("name"), is("joe"));

    StructArrayDecoder sad = decoder.structs("mapEntry");
    assertThat(sad.length(), is(3));
    assertThat(sad.string("key"), is(nullValue()));
    assertThat(sad.enm("type").isFound(), is(false));
    assertThat(sad.string("string"), is("a"));
    sad.next();
    assertThat(sad.string("key"), is(nullValue()));
    assertThat(sad.enm("type").isFound(), is(false));
    assertThat(sad.string("string"), is("b"));
    sad.next();
    assertThat(sad.string("key"), is(nullValue()));
    assertThat(sad.enm("type").isFound(), is(false));
    assertThat(sad.string("string"), is("c"));
    sad.end();

    assertThat(decoder.int64("id"), is(999L));
  }

}
