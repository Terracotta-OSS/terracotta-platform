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
public class EnumMappingTest {

  @Test
  public void testStructWithEnum() throws Exception {
    EnumMapping<TestEnum1> enm1 = EnumMappingBuilder.newEnumMappingBuilder(TestEnum1.class)
        .mapping(TestEnum1.A, 1)
        .mapping(TestEnum1.B, 2)
        .mapping(TestEnum1.C, 3)
        .build();

    EnumMapping<Character> enm2 = EnumMappingBuilder.newEnumMappingBuilder(Character.class)
        .mapping(W, 1)
        .mapping(X, 2)
        .mapping(Y, 3)
        .mapping(Z, 4)
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
        .enm("myEnum2", W)
        .struct("z")
          .enm("myEnum1", TestEnum1.A)
        .end()
        .enm("myEnum2bis", X)
        .encode();

    encoded.rewind();

    StructDecoder<Void> decoder = struct.decoder(encoded);

    assertThat(decoder.<Character>enm("myEnum2").get(), is(W));
    StructDecoder<StructDecoder<Void>> sd = decoder.struct("z");
    assertThat(sd.<TestEnum1>enm("myEnum1").get(), is(TestEnum1.A));
    sd.end();
    assertThat(decoder.<Character>enm("myEnum2bis").get(), is(X));
  }

  @Test
  public void testSkipEnum() throws Exception {
    EnumMapping<TestEnum1> enm1 = EnumMappingBuilder.newEnumMappingBuilder(TestEnum1.class)
        .mapping(TestEnum1.A, 1)
        .mapping(TestEnum1.B, 2)
        .mapping(TestEnum1.C, 3)
        .build();

    EnumMapping<Character> enm2 = EnumMappingBuilder.newEnumMappingBuilder(Character.class)
        .mapping(W, 1)
        .mapping(X, 2)
        .mapping(Y, 3)
        .mapping(Z, 4)
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
        .enm("myEnum2", W)
        .struct("z")
          .enm("myEnum1", TestEnum1.A)
        .end()
        .enm("myEnum2bis", X)
        .encode();

    encoded.rewind();

    StructDecoder<Void> decoder = struct.decoder(encoded);

    assertThat(decoder.<Character>enm("myEnum2bis").get(), is(X));
  }

  private enum TestEnum1 {
    A,
    B,
    C,
  }

  private static final Character W = 'w';
  private static final Character X = 'x';
  private static final Character Y = 'y';
  private static final Character Z = 'z';

}
