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

  private final Struct mapEntry = StructBuilder.newStructBuilder()
      .string("key", 1)
      .string("value", 2)
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
          .string("value", "one")
        .next()
          .string("key", "2")
          .string("value", "two")
        .end()
        .int64("id", 999L)
        .encode();

    bb.rewind();

    StructDecoder decoder = struct.decoder(bb);

    assertThat(decoder.string("name"), is("joe"));

    StructArrayDecoder sad = decoder.structs("mapEntry");
    assertThat(sad.length(), is(2));
    assertThat(sad.string("key"), is("1"));
    assertThat(sad.string("value"), is("one"));
    sad.next();
    assertThat(sad.string("key"), is("2"));
    assertThat(sad.string("value"), is("two"));
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
                  .string("value", "one")
                .next()
                  .string("key", "2")
                  .string("value", "two");
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
    assertThat(sad.string("value"), is("one"));
    sad.next();
    assertThat(sad.string("key"), is("2"));
    assertThat(sad.string("value"), is("two"));
    sad.end();

    assertThat(decoder.int64("id"), is(999L));
  }

  @Test
  public void testDump() throws Exception {
    ByteBuffer bb = struct.encoder()
        .string("name", "joe")
        .structs("mapEntry")
        .string("key", "1")
        .string("value", "one")
        .next()
        .string("key", "2")
        .string("value", "two")
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
          .string("value", "one")
        .next()
          .string("key", "2")
          .string("value", "two")
        .end()
        .int64("id", 999L)
        .encode();

    bb.rewind();

    StructDecoder decoder = struct.decoder(bb);

    assertThat(decoder.string("name"), is("joe"));

    StructArrayDecoder sad = decoder.structs("mapEntry");
    assertThat(sad.length(), is(2));
    assertThat(sad.string("key"), is("1"));
    assertThat(sad.string("value"), is("one"));
    sad.end();

    assertThat(decoder.int64("id"), is(999L));
  }

  @Test
  public void testSkipStructEntry() throws Exception {
    ByteBuffer bb = struct.encoder()
        .string("name", "joe")
        .structs("mapEntry")
          .string("key", "1")
          .string("value", "one")
        .next()
          .string("key", "2")
          .string("value", "two")
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
    assertThat(sad.string("value"), is("two"));
    sad.end();

    assertThat(decoder.int64("id"), is(999L));
  }

  @Test
  public void testSkipStructsContents() throws Exception {
    ByteBuffer bb = struct.encoder()
        .string("name", "joe")
        .structs("mapEntry")
          .string("key", "1")
          .string("value", "one")
        .next()
          .string("key", "2")
          .string("value", "two")
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
          .string("value", "one")
        .next()
          .string("key", "2")
          .string("value", "two")
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
        .string("value", "one")
        .next()
        .string("key", "2")
        .string("value", "two")
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
    assertThat(sad.string("value"), is("one"));
    sad.next();
    assertThat(sad.string("key"), is("2"));
    assertThat(sad.string("value"), is("two"));
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
          .string("value", "a")
        .next()
          .string("value", "b")
        .next()
          .string("value", "c")
        .end()
        .int64("id", 999L)
        .encode();

    bb.rewind();

    StructDecoder decoder = struct.decoder(bb);

    assertThat(decoder.string("name"), is("joe"));

    StructArrayDecoder sad = decoder.structs("mapEntry");
    assertThat(sad.length(), is(3));
    assertThat(sad.string("key"), is(nullValue()));
    assertThat(sad.string("value"), is("a"));
    sad.next();
    assertThat(sad.string("key"), is(nullValue()));
    assertThat(sad.string("value"), is("b"));
    sad.next();
    assertThat(sad.string("key"), is(nullValue()));
    assertThat(sad.string("value"), is("c"));
    sad.end();

    assertThat(decoder.int64("id"), is(999L));
  }

}
