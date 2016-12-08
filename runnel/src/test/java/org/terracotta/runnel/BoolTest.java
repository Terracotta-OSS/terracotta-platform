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
public class BoolTest {

  @Test
  public void testStructWithBool() throws Exception {
    Struct struct = StructBuilder.newStructBuilder()
        .bool("x", 1)
        .bools("y", 2)
        .bool("z", 3)
        .build();

    ByteBuffer encoded = struct.encoder()
        .bool("x", true)
        .bools("y")
          .value(false)
          .value(true)
          .value(false)
        .end()
        .bool("z", true)
        .encode();

    encoded.rewind();

    StructDecoder<Void> decoder = struct.decoder(encoded);

    assertThat(decoder.bool("x"), is(true));
    ArrayDecoder<Boolean, StructDecoder<Void>> ad = decoder.bools("y");
    assertThat(ad.length(), is(3));
    assertThat(ad.value(), is(false));
    assertThat(ad.value(), is(true));
    assertThat(ad.value(), is(false));
    ad.end();
    assertThat(decoder.bool("z"), is(true));
  }

  @Test
  public void testSkipBool() throws Exception {
    Struct struct = StructBuilder.newStructBuilder()
        .bool("x", 1)
        .bools("y", 2)
        .bool("z", 3)
        .build();

    ByteBuffer encoded = struct.encoder()
        .bool("x", false)
        .bools("y")
          .value(true)
          .value(false)
          .value(true)
        .end()
        .bool("z", false)
        .encode();

    encoded.rewind();

    StructDecoder<Void> decoder = struct.decoder(encoded);

    ArrayDecoder<Boolean, StructDecoder<Void>> ad = decoder.bools("y");
    assertThat(ad.length(), is(3));
    assertThat(ad.value(), is(true));
    assertThat(ad.value(), is(false));
    ad.end();
    assertThat(decoder.bool("z"), is(false));
  }

}
