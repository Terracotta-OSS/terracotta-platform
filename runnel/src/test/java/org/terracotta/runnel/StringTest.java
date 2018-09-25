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
import org.terracotta.runnel.encoding.StructEncoder;

import java.nio.ByteBuffer;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.fail;

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

    StructDecoder<Void> decoder = struct.decoder(encoded);

    assertThat(decoder.mandatoryString("name"), is("john doe"));
    ArrayDecoder<String, StructDecoder<Void>> ad = decoder.mandatoryStrings("colors");

    assertThat(ad.hasNext(), is(true));
    assertThat(ad.next(), is("red"));

    assertThat(ad.hasNext(), is(true));
    assertThat(ad.next(), is("green"));

    assertThat(ad.hasNext(), is(true));
    assertThat(ad.next(), is("blue"));

    assertThat(ad.hasNext(), is(false));
    ad.end();
    assertThat(decoder.mandatoryString("address"), is("my street"));
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

    StructDecoder<Void> decoder = struct.decoder(encoded);

    ArrayDecoder<String, StructDecoder<Void>> ad = decoder.mandatoryStrings("colors");

    assertThat(ad.hasNext(), is(true));
    assertThat(ad.next(), is("red"));

    assertThat(ad.hasNext(), is(true));
    assertThat(ad.next(), is("green"));

    assertThat(ad.hasNext(), is(true));
    ad.end();
    assertThat(decoder.mandatoryString("address"), is("my street"));
  }

  @Test
  public void testNullString() throws Exception {
    Struct struct = StructBuilder.newStructBuilder()
        .string("name", 5)
        .string("address", 7)
        .build();

    ByteBuffer encoded = struct.encoder()
        .string("name", null)
        .string("address", "my street")
        .encode();

    encoded.rewind();

    StructDecoder decoder = struct.decoder(encoded);
    assertThat(decoder.optionalString("name"), is(Optional.empty()));
    assertThat(decoder.optionalString("address"), is(Optional.of("my street")));
  }

  @Test
  public void testNullStringCannotBeEncodedTwice() throws Exception {
    Struct struct = StructBuilder.newStructBuilder()
        .string("name", 5)
        .string("address", 7)
        .build();

    StructEncoder encoder = struct.encoder()
        .string("name", null);

    try {
      encoder.string("name", "joe");
      fail("expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // expected
    }
  }

}
