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
public class Int64Test {

  @Test
  public void testStructWithInt64() throws Exception {
    Struct struct = StructBuilder.newStructBuilder()
        .int64("x", 1)
        .int64s("y", 2)
        .int64("z", 3)
        .build();

    ByteBuffer encoded = struct.encoder()
        .int64("x", -1)
        .int64s("y")
          .value(-10L)
          .value(-15L)
          .value(-20L)
        .end()
        .int64("z", Long.MAX_VALUE)
        .encode();

    encoded.rewind();

    StructDecoder<Void> decoder = struct.decoder(encoded);

    assertThat(decoder.int64("x"), is(-1L));
    ArrayDecoder<Long, StructDecoder<Void>> ad = decoder.int64s("y");
    assertThat(ad.length(), is(3));
    assertThat(ad.value(), is(-10L));
    assertThat(ad.value(), is(-15L));
    assertThat(ad.value(), is(-20L));
    ad.end();
    assertThat(decoder.int64("z"), is(Long.MAX_VALUE));
  }

  @Test
  public void testSkipInt64() throws Exception {
    Struct struct = StructBuilder.newStructBuilder()
        .int64("x", 1)
        .int64s("y", 2)
        .int64("z", 3)
        .build();

    ByteBuffer encoded = struct.encoder()
        .int64("x", -1)
        .int64s("y")
          .value(-10L)
          .value(-15L)
          .value(-20L)
        .end()
        .int64("z", Long.MAX_VALUE)
        .encode();

    encoded.rewind();

    StructDecoder<Void> decoder = struct.decoder(encoded);

    ArrayDecoder<Long, StructDecoder<Void>> ad = decoder.int64s("y");
    assertThat(ad.length(), is(3));
    assertThat(ad.value(), is(-10L));
    assertThat(ad.value(), is(-15L));
    ad.end();
    assertThat(decoder.int64("z"), is(Long.MAX_VALUE));
  }

}
