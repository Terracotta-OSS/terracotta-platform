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
public class StringTest {

  @Test
  public void testStructWithString() throws Exception {
    Struct struct = StructBuilder.newStructBuilder()
        .string("name", 5)
        .strings("colors", 6)
        .string("address", 7)
        .build();

    ByteBuffer encoded = struct.encoder()
        .string("name", "john doe")
        .strings("colors")
          .value("red")
          .value("green")
          .value("blue")
        .end()
        .string("address", "my street")
        .encode();

    encoded.rewind();

    StructDecoder decoder = struct.decoder(encoded);

    assertThat(decoder.string("name"), is("john doe"));
    ArrayDecoder<String> ad = decoder.strings("colors");
    assertThat(ad.length(), is(3));
    assertThat(ad.value(), is("red"));
    assertThat(ad.value(), is("green"));
    assertThat(ad.value(), is("blue"));
    ad.end();
    assertThat(decoder.string("address"), is("my street"));
  }

  @Test
  public void testSkipString() throws Exception {
    Struct struct = StructBuilder.newStructBuilder()
        .string("name", 5)
        .strings("colors", 6)
        .string("address", 7)
        .build();

    ByteBuffer encoded = struct.encoder()
        .string("name", "john doe")
        .strings("colors")
          .value("red")
          .value("green")
          .value("blue")
        .end()
        .string("address", "my street")
        .encode();

    encoded.rewind();

    StructDecoder decoder = struct.decoder(encoded);

    ArrayDecoder<String> ad = decoder.strings("colors");
    assertThat(ad.length(), is(3));
    assertThat(ad.value(), is("red"));
    assertThat(ad.value(), is("green"));
    ad.end();
    assertThat(decoder.string("address"), is("my street"));
  }

}
