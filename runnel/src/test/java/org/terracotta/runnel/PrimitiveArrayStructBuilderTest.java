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

import java.nio.ByteBuffer;

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

    StructDecoder decoder = struct.decoder(bb);

    assertThat(decoder.string("name"), is("joe"));

    ArrayDecoder<Long> adi = decoder.int64s("ids");
    assertThat(adi.length(), is(3));
    assertThat(adi.value(), is(4L));
    assertThat(adi.value(), is(5L));
    assertThat(adi.value(), is(6L));
    adi.end();

    assertThat(decoder.string("address"), is("my street"));

    ArrayDecoder<String> ads = decoder.strings("colors");
    assertThat(ads.length(), is(2));
    assertThat(ads.value(), is("blue"));
    assertThat(ads.value(), is("green"));
    ads.end();

    assertThat(decoder.int64("age"), is(30L));
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

    assertThat(decoder.string("name"), is("joe"));
    assertThat(decoder.string("address"), is("my street"));
    assertThat(decoder.int64("age"), is(30L));
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

    StructDecoder decoder = struct.decoder(bb);

    assertThat(decoder.string("name"), is("joe"));

    ArrayDecoder<Long> ad = decoder.int64s("ids");
    assertThat(ad.length(), is(3));
    assertThat(ad.value(), is(4L));
    ad.end();

    assertThat(decoder.string("address"), is("my street"));

    ArrayDecoder<String> ads = decoder.strings("colors");
    assertThat(ads.length(), is(2));
    assertThat(ads.value(), is("blue"));
    ads.end();

    assertThat(decoder.int64("age"), is(30L));
  }

}
