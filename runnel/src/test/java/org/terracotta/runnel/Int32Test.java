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
public class Int32Test {

  @Test
  public void testStructWithInt32() throws Exception {
    Struct struct = StructBuilder.newStructBuilder()
        .int32("x", 1)
        .int32s("y", 2)
        .int32("z", 3)
        .build();

    ByteBuffer encoded = struct.encoder()
        .int32("x", -1)
        .int32s("y")
          .value(-10)
          .value(-15)
          .value(-20)
        .end()
        .int32("z", Integer.MAX_VALUE)
        .encode();

    encoded.rewind();

    StructDecoder<Void> decoder = struct.decoder(encoded);

    assertThat(decoder.int32("x"), is(-1));
    ArrayDecoder<Integer, StructDecoder<Void>> ad = decoder.int32s("y");
    assertThat(ad.length(), is(3));
    assertThat(ad.value(), is(-10));
    assertThat(ad.value(), is(-15));
    assertThat(ad.value(), is(-20));
    ad.end();
    assertThat(decoder.int32("z"), is(Integer.MAX_VALUE));
  }

  @Test
  public void testSkipInt32() throws Exception {
    Struct struct = StructBuilder.newStructBuilder()
        .int32("x", 1)
        .int32s("y", 2)
        .int32("z", 3)
        .build();

    ByteBuffer encoded = struct.encoder()
        .int32("x", -1)
        .int32s("y")
          .value(-10)
          .value(-15)
          .value(-20)
        .end()
        .int32("z", Integer.MAX_VALUE)
        .encode();

    encoded.rewind();

    StructDecoder<Void> decoder = struct.decoder(encoded);

    ArrayDecoder<Integer, StructDecoder<Void>> ad = decoder.int32s("y");
    assertThat(ad.length(), is(3));
    assertThat(ad.value(), is(-10));
    assertThat(ad.value(), is(-15));
    ad.end();
    assertThat(decoder.int32("z"), is(Integer.MAX_VALUE));
  }

}
