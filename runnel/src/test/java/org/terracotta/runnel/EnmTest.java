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

import java.nio.ByteBuffer;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Ludovic Orban
 */
public class EnmTest {

  @Test
  public void testStructWithEnum() throws Exception {
    Enm<TestEnum1> enm1 = EnmBuilder.newEnumBuilder(TestEnum1.class)
        .mapping(TestEnum1.A, 1)
        .mapping(TestEnum1.B, 2)
        .mapping(TestEnum1.C, 3)
        .build();

    Enm<TestEnum2> enm2 = EnmBuilder.newEnumBuilder(TestEnum2.class)
        .mapping(TestEnum2.W, 1)
        .mapping(TestEnum2.X, 2)
        .mapping(TestEnum2.Y, 3)
        .mapping(TestEnum2.Z, 4)
        .build();

    Struct subStruct = StructBuilder.newStructBuilder()
        .enm("myEnum1", 1, enm1)
        .build();

    Struct struct = StructBuilder.newStructBuilder()
        .enm("myEnum2", 1, enm2)
        .struct("z", 3, subStruct)
        .enm("myEnum2bis", 5, enm2)
        .build();

    ByteBuffer encoded = struct.encoder()
        .enm("myEnum2", TestEnum2.W)
        .struct("z")
          .enm("myEnum1", TestEnum1.A)
        .end()
        .enm("myEnum2bis", TestEnum2.X)
        .encode();

    encoded.rewind();

    StructDecoder decoder = struct.decoder(encoded);

    assertThat(decoder.<TestEnum2>enm("myEnum2"), is(TestEnum2.W));
    StructDecoder sd = decoder.struct("z");
    assertThat(sd.<TestEnum1>enm("myEnum1"), is(TestEnum1.A));
    sd.end();
    assertThat(decoder.<TestEnum2>enm("myEnum2bis"), is(TestEnum2.X));
  }

  @Test
  public void testSkipEnum() throws Exception {
    Enm<TestEnum1> enm1 = EnmBuilder.newEnumBuilder(TestEnum1.class)
        .mapping(TestEnum1.A, 1)
        .mapping(TestEnum1.B, 2)
        .mapping(TestEnum1.C, 3)
        .build();

    Enm<TestEnum2> enm2 = EnmBuilder.newEnumBuilder(TestEnum2.class)
        .mapping(TestEnum2.W, 1)
        .mapping(TestEnum2.X, 2)
        .mapping(TestEnum2.Y, 3)
        .mapping(TestEnum2.Z, 4)
        .build();

    Struct subStruct = StructBuilder.newStructBuilder()
        .enm("myEnum1", 1, enm1)
        .build();

    Struct struct = StructBuilder.newStructBuilder()
        .enm("myEnum2", 1, enm2)
        .struct("z", 3, subStruct)
        .enm("myEnum2bis", 5, enm2)
        .build();

    ByteBuffer encoded = struct.encoder()
        .enm("myEnum2", TestEnum2.W)
        .struct("z")
          .enm("myEnum1", TestEnum1.A)
        .end()
        .enm("myEnum2bis", TestEnum2.X)
        .encode();

    encoded.rewind();

    StructDecoder decoder = struct.decoder(encoded);

    assertThat(decoder.<TestEnum2>enm("myEnum2bis"), is(TestEnum2.X));
  }

  private enum TestEnum1 {
    A,
    B,
    C,
  }

  private enum TestEnum2 {
    W,
    X,
    Y,
    Z,
  }

}
