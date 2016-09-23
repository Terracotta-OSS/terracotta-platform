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
import org.terracotta.runnel.encoding.StructEncoder;
import org.terracotta.runnel.encoding.StructEncoderFunction;

import java.nio.ByteBuffer;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * @author Ludovic Orban
 */
public class StructStructBuilderTest {

  private final Struct mapEntry = StructBuilder.newStructBuilder()
      .string("key", 1)
      .string("value", 2)
      .build();

  private final Struct struct = StructBuilder.newStructBuilder()
      .string("name", 1)
      .struct("mapEntry", 2, mapEntry)
      .int64("id", 3)
      .build();

  @Test
  public void testReadAll() throws Exception {
    ByteBuffer bb = struct.encoder()
        .string("name", "joe")
        .struct("mapEntry")
          .string("key", "1")
          .string("value", "one")
        .end()
        .int64("id", 999L)
        .encode();

    bb.rewind();

    StructDecoder decoder = struct.decoder(bb);

    assertThat(decoder.string("name"), is("joe"));

    decoder = decoder.struct("mapEntry");
    assertThat(decoder.string("key"), is("1"));
    assertThat(decoder.string("value"), is("one"));
    decoder = decoder.end();

    assertThat(decoder.int64("id"), is(999L));
  }

  @Test
  public void testReadAll_withLambda() throws Exception {
    ByteBuffer bb = struct.encoder()
        .string("name", "joe")
        .struct("mapEntry", new StructEncoderFunction() {
          @Override
          public void encode(StructEncoder encoder) {
            encoder.string("key", "1")
                .string("value", "one");
          }
        })
        .int64("id", 999L)
        .encode();

    bb.rewind();

    StructDecoder decoder = struct.decoder(bb);

    assertThat(decoder.string("name"), is("joe"));

    decoder = decoder.struct("mapEntry");
    assertThat(decoder.string("key"), is("1"));
    assertThat(decoder.string("value"), is("one"));
    decoder = decoder.end();

    assertThat(decoder.int64("id"), is(999L));
  }

  @Test
  public void testSkipAllButLastField() throws Exception {
    ByteBuffer bb = struct.encoder()
        .string("name", "joe")
        .struct("mapEntry")
          .string("key", "1")
          .string("value", "one")
        .end()
        .int64("id", 999L)
        .encode();

    bb.rewind();

    StructDecoder decoder = struct.decoder(bb);

    assertThat(decoder.int64("id"), is(999L));
  }

  @Test
  public void testSkipFirstField() throws Exception {
    ByteBuffer bb = struct.encoder()
        .string("name", "joe")
        .struct("mapEntry")
          .string("key", "1")
          .string("value", "one")
        .end()
        .int64("id", 999L)
        .encode();

    bb.rewind();

    StructDecoder decoder = struct.decoder(bb);


    decoder = decoder.struct("mapEntry");
    assertThat(decoder.string("key"), is("1"));
    assertThat(decoder.string("value"), is("one"));
    decoder = decoder.end();

    assertThat(decoder.int64("id"), is(999L));
  }

  @Test
  public void testSkipStructContents() throws Exception {
    ByteBuffer bb = struct.encoder()
        .string("name", "joe")
        .struct("mapEntry")
          .string("key", "1")
          .string("value", "one")
        .end()
        .int64("id", 999L)
        .encode();

    bb.rewind();

    StructDecoder decoder = struct.decoder(bb);

    assertThat(decoder.string("name"), is("joe"));

    decoder = decoder.struct("mapEntry");
    decoder = decoder.end();

    assertThat(decoder.int64("id"), is(999L));
  }

  @Test
  public void testSkipStructFirstField() throws Exception {
    ByteBuffer bb = struct.encoder()
        .string("name", "joe")
        .struct("mapEntry")
          .string("key", "1")
          .string("value", "one")
        .end()
        .int64("id", 999L)
        .encode();

    bb.rewind();

    StructDecoder decoder = struct.decoder(bb);

    assertThat(decoder.string("name"), is("joe"));

    decoder = decoder.struct("mapEntry");
    assertThat(decoder.string("value"), is("one"));
    decoder = decoder.end();

    assertThat(decoder.int64("id"), is(999L));
  }

  @Test
  public void testSkipStructLastField() throws Exception {
    ByteBuffer bb = struct.encoder()
        .string("name", "joe")
        .struct("mapEntry")
          .string("key", "1")
          .string("value", "one")
        .end()
        .int64("id", 999L)
        .encode();

    bb.rewind();

    StructDecoder decoder = struct.decoder(bb);

    assertThat(decoder.string("name"), is("joe"));

    decoder = decoder.struct("mapEntry");
    assertThat(decoder.string("key"), is("1"));
    decoder = decoder.end();

    assertThat(decoder.int64("id"), is(999L));
  }

  @Test
  public void testSkipStruct() throws Exception {
    ByteBuffer bb = struct.encoder()
        .string("name", "joe")
        .struct("mapEntry")
          .string("key", "1")
          .string("value", "one")
        .end()
        .int64("id", 999L)
        .encode();

    bb.rewind();

    StructDecoder decoder = struct.decoder(bb);

    assertThat(decoder.string("name"), is("joe"));
    assertThat(decoder.int64("id"), is(999L));
  }

  @Test
  public void testEmptyFirstStructField() throws Exception {
    ByteBuffer bb = struct.encoder()
        .string("name", "joe")
        .struct("mapEntry")
          .string("value", "one")
        .end()
        .int64("id", 999L)
        .encode();

    bb.rewind();

    StructDecoder decoder = struct.decoder(bb);

    assertThat(decoder.string("name"), is("joe"));
    decoder = decoder.struct("mapEntry");
    assertThat(decoder.string("key"), is(nullValue()));
    assertThat(decoder.string("value"), is("one"));
    decoder = decoder.end();
    assertThat(decoder.int64("id"), is(999L));
  }

}
