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
public class Fp64Test {

  @Test
  public void testStructWithFp64() throws Exception {
    Struct struct = StructBuilder.newStructBuilder()
        .fp64("x", 1)
        .fp64s("y", 2)
        .fp64("z", 3)
        .build();

    ByteBuffer encoded = struct.encoder()
        .fp64("x", -1.0)
        .fp64s("y")
          .value(1.0)
          .value(-0.123)
          .value(Double.NaN)
          .value(Double.NEGATIVE_INFINITY)
          .value(Double.POSITIVE_INFINITY)
        .end()
        .fp64("z", 2.0)
        .encode();

    encoded.rewind();

    StructDecoder<Void> decoder = struct.decoder(encoded);

    assertThat(decoder.fp64("x"), is(-1.0));
    ArrayDecoder<Double, StructDecoder<Void>> ad = decoder.fp64s("y");
    assertThat(ad.length(), is(5));
    assertThat(ad.value(), is(1.0));
    assertThat(ad.value(), is(-0.123));
    assertThat(ad.value(), is(Double.NaN));
    assertThat(ad.value(), is(Double.NEGATIVE_INFINITY));
    assertThat(ad.value(), is(Double.POSITIVE_INFINITY));
    ad.end();
    assertThat(decoder.fp64("z"), is(2.0));
  }

  @Test
  public void testSkipFp64() throws Exception {
    Struct struct = StructBuilder.newStructBuilder()
        .fp64("x", 1)
        .fp64s("y", 2)
        .fp64("z", 3)
        .build();

    ByteBuffer encoded = struct.encoder()
        .fp64("x", -1.0)
        .fp64s("y")
          .value(1.0)
          .value(-0.123)
          .value(Double.NaN)
          .value(Double.NEGATIVE_INFINITY)
          .value(Double.POSITIVE_INFINITY)
        .end()
        .fp64("z", 2.0)
        .encode();

    encoded.rewind();

    StructDecoder<Void> decoder = struct.decoder(encoded);

    ArrayDecoder<Double, StructDecoder<Void>> ad = decoder.fp64s("y");
    assertThat(ad.length(), is(5));
    assertThat(ad.value(), is(1.0));
    assertThat(ad.value(), is(-0.123));
    assertThat(ad.value(), is(Double.NaN));
    ad.end();
    assertThat(decoder.fp64("z"), is(2.0));
  }

}
