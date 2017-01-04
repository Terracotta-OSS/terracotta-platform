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
import java.util.AbstractMap;
import java.util.Map;

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

  @Test(expected = IllegalStateException.class)
  public void testCannotEncodeNonRoot() throws Exception {
    struct.encoder()
        .struct("mapEntry")
          .string("key", "1")
          .string("value", "one")
        .encode();
  }

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

    StructDecoder<Void> decoder = struct.decoder(bb);

    assertThat(decoder.string("name"), is("joe"));

    StructDecoder<StructDecoder<Void>> d2 = decoder.struct("mapEntry");
    assertThat(d2.string("key"), is("1"));
    assertThat(d2.string("value"), is("one"));
    decoder = d2.end();

    assertThat(decoder.int64("id"), is(999L));
  }

  @Test
  public void testReadAll_withLambda() throws Exception {
    ByteBuffer bb = struct.encoder()
        .string("name", "joe")
        .struct("mapEntry", new AbstractMap.SimpleEntry<String, String>("1", "one"), new StructEncoderFunction<Map.Entry<String, String>>() {
          @Override
          public void encode(StructEncoder<?> encoder, Map.Entry<String, String> entry) {
            encoder.string("key", entry.getKey())
                .string("value", entry.getValue());
          }
        })
        .int64("id", 999L)
        .encode();

    bb.rewind();

    StructDecoder<Void> decoder = struct.decoder(bb);

    assertThat(decoder.string("name"), is("joe"));

    StructDecoder<StructDecoder<Void>> d2 = decoder.struct("mapEntry");
    assertThat(d2.string("key"), is("1"));
    assertThat(d2.string("value"), is("one"));
    decoder = d2.end();

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

    StructDecoder<Void> decoder = struct.decoder(bb);

    StructDecoder<StructDecoder<Void>> d2 = decoder.struct("mapEntry");
    assertThat(d2.string("key"), is("1"));
    assertThat(d2.string("value"), is("one"));
    decoder = d2.end();

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

    StructDecoder<Void> decoder = struct.decoder(bb);

    assertThat(decoder.string("name"), is("joe"));

    StructDecoder<StructDecoder<Void>> d2 = decoder.struct("mapEntry");
    decoder = d2.end();

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

    StructDecoder<Void> decoder = struct.decoder(bb);

    assertThat(decoder.string("name"), is("joe"));

    StructDecoder<StructDecoder<Void>> d2 = decoder.struct("mapEntry");
    assertThat(d2.string("value"), is("one"));
    decoder = d2.end();

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

    StructDecoder<Void> decoder = struct.decoder(bb);

    assertThat(decoder.string("name"), is("joe"));

    StructDecoder<StructDecoder<Void>> d2 = decoder.struct("mapEntry");
    assertThat(d2.string("key"), is("1"));
    decoder = d2.end();

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

    StructDecoder<Void> decoder = struct.decoder(bb);

    assertThat(decoder.string("name"), is("joe"));
    StructDecoder<StructDecoder<Void>> d2 = decoder.struct("mapEntry");
    assertThat(d2.string("key"), is(nullValue()));
    assertThat(d2.string("value"), is("one"));
    decoder = d2.end();
    assertThat(decoder.int64("id"), is(999L));
  }

}
