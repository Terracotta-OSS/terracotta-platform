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
public class CharTest {

  @Test
  public void testStructWithChar() throws Exception {
    Struct struct = StructBuilder.newStructBuilder()
        .chr("x", 1)
        .chrs("y", 2)
        .chr("z", 3)
        .build();

    ByteBuffer encoded = struct.encoder()
        .chr("x", 'a')
        .chrs("y")
          .value('1')
          .value('2')
          .value('3')
        .end()
        .chr("z", 'z')
        .encode();

    encoded.rewind();

    StructDecoder<Void> decoder = struct.decoder(encoded);

    assertThat(decoder.mandatoryChr("x"), is('a'));
    ArrayDecoder<Character, StructDecoder<Void>> ad = decoder.mandatoryChrs("y");

    assertThat(ad.hasNext(), is(true));
    assertThat(ad.next(), is('1'));

    assertThat(ad.hasNext(), is(true));
    assertThat(ad.next(), is('2'));

    assertThat(ad.hasNext(), is(true));
    assertThat(ad.next(), is('3'));

    assertThat(ad.hasNext(), is(false));
    ad.end();
    assertThat(decoder.mandatoryChr("z"), is('z'));
  }

  @Test
  public void testSkipChar() throws Exception {
    Struct struct = StructBuilder.newStructBuilder()
        .chr("x", 1)
        .chrs("y", 2)
        .chr("z", 3)
        .build();

    ByteBuffer encoded = struct.encoder()
        .chr("x", 'q')
        .chrs("y")
          .value('w')
          .value('e')
          .value('r')
        .end()
        .chr("z", 't')
        .encode();

    encoded.rewind();

    StructDecoder<Void> decoder = struct.decoder(encoded);

    ArrayDecoder<Character, StructDecoder<Void>> ad = decoder.mandatoryChrs("y");

    assertThat(ad.hasNext(), is(true));
    assertThat(ad.next(), is('w'));

    assertThat(ad.hasNext(), is(true));
    assertThat(ad.next(), is('e'));

    assertThat(ad.hasNext(), is(true));
    ad.end();
    assertThat(decoder.mandatoryChr("z"), is('t'));
  }

}
