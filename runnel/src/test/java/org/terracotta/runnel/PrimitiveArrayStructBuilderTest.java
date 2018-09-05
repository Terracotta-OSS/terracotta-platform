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
import org.terracotta.runnel.decoding.ArrayDecoder;
import org.terracotta.runnel.decoding.StructDecoder;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * @author Ludovic Orban
 */
public class PrimitiveArrayStructBuilderTest {

  private final Struct struct = StructBuilder.newStructBuilder()
      .string("name", 1)
      .int64s("ids", 2)
      .string("address", 3)
      .strings("colors", 4)
      .int64("age", 5)
      .build();

  @Test
  public void testEncodeNullArray() throws Exception {
    ByteBuffer bb = ByteBuffer.allocate(1024);

    struct.encoder()
        .int64("age", 30)
        .encode(bb);

    bb.rewind();

    StructDecoder decoder = struct.decoder(bb);

    assertThat(decoder.optionalInt64s("ids"), is(Optional.empty()));
  }

  @Test
  public void testEncodeEmptyArray() throws Exception {
    ByteBuffer bb = ByteBuffer.allocate(1024);

    struct.encoder()
        .int64s("ids")
        .end()
        .encode(bb);

    bb.rewind();

    StructDecoder<Void> decoder = struct.decoder(bb);

    ArrayDecoder<Long, StructDecoder<Void>> ids = decoder.optionalInt64s("ids").get();
    assertThat(ids.hasNext(), is(false));
  }


  @Test
  public void testReadAll() throws Exception {
    ByteBuffer bb = ByteBuffer.allocate(1024);

    struct.encoder()
        .string("name", "joe")
        .int64s("ids")
          .value(4L)
          .value(5L)
          .value(6L)
        .end()
        .string("address", "my street")
        .strings("colors")
          .value("blue")
          .value("green")
        .end()
        .int64("age", 30)
        .encode(bb);

    bb.rewind();

    StructDecoder<Void> decoder = struct.decoder(bb);

    assertThat(decoder.mandatoryString("name"), is("joe"));

    ArrayDecoder<Long, StructDecoder<Void>> adi = decoder.mandatoryInt64s("ids");

    assertThat(adi.hasNext(), is(true));
    assertThat(adi.next(), is(4L));

    assertThat(adi.hasNext(), is(true));
    assertThat(adi.next(), is(5L));

    assertThat(adi.hasNext(), is(true));
    assertThat(adi.next(), is(6L));

    assertThat(adi.hasNext(), is(false));
    adi.end();

    assertThat(decoder.optionalString("address"), is(Optional.of("my street")));

    ArrayDecoder<String, StructDecoder<Void>> ads = decoder.mandatoryStrings("colors");

    assertThat(ads.hasNext(), is(true));
    assertThat(ads.next(), is("blue"));

    assertThat(ads.hasNext(), is(true));
    assertThat(ads.next(), is("green"));

    assertThat(ads.hasNext(), is(false));
    ads.end();

    assertThat(decoder.optionalInt64("age"), is(Optional.of(30L)));
  }

  @Test
  public void testDump() throws Exception {
    ByteBuffer bb = ByteBuffer.allocate(1024);

    struct.encoder()
        .string("name", "joe")
        .int64s("ids")
        .value(4L)
        .value(5L)
        .value(6L)
        .end()
        .string("address", "my street")
        .strings("colors")
        .value("blue")
        .value("green")
        .end()
        .int64("age", 30)
        .encode(bb);

    bb.rewind();

    struct.dump(bb, new PrintStream(new ByteArrayOutputStream()));
  }

  @Test
  public void testSkipArrays() throws Exception {
    ByteBuffer bb = ByteBuffer.allocate(1024);

    struct.encoder()
        .string("name", "joe")
        .int64s("ids")
          .value(4L)
          .value(5L)
          .value(6L)
        .end()
        .string("address", "my street")
        .strings("colors")
          .value("blue")
          .value("green")
        .end()
        .int64("age", 30)
        .encode(bb);

    bb.rewind();

    StructDecoder decoder = struct.decoder(bb);

    assertThat(decoder.mandatoryString("name"), is("joe"));
    assertThat(decoder.optionalString("address"), is(Optional.of("my street")));
    assertThat(decoder.optionalInt64("age"), is(Optional.of(30L)));
  }

  @Test
  public void testSkipSomeArrayContent() throws Exception {
    ByteBuffer bb = ByteBuffer.allocate(1024);

    struct.encoder()
        .string("name", "joe")
        .int64s("ids")
          .value(4L)
          .value(5L)
          .value(6L)
        .end()
        .string("address", "my street")
        .strings("colors")
          .value("blue")
          .value("green")
        .end()
        .int64("age", 30)
        .encode(bb);

    bb.rewind();

    StructDecoder<Void> decoder = struct.decoder(bb);

    assertThat(decoder.mandatoryString("name"), is("joe"));

    ArrayDecoder<Long, StructDecoder<Void>> ad = decoder.mandatoryInt64s("ids");

    assertThat(ad.hasNext(), is(true));
    assertThat(ad.next(), is(4L));

    assertThat(ad.hasNext(), is(true));
    ad.end();

    assertThat(decoder.mandatoryString("address"), is("my street"));

    ArrayDecoder<String, StructDecoder<Void>> ads = decoder.mandatoryStrings("colors");

    assertThat(ads.hasNext(), is(true));
    assertThat(ads.next(), is("blue"));

    assertThat(ads.hasNext(), is(true));
    ads.end();

    assertThat(decoder.mandatoryInt64("age"), is(30L));
  }

}
