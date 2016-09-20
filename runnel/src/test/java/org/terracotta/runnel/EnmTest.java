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

/**
 * @author Ludovic Orban
 */
public class EnmTest {

  @Test
  public void testStructWithEnum() throws Exception {
    Enm<TestEnum1> enm1 = EnmBuilder.<TestEnum1>newEnumBuilder()
        .mapping(TestEnum1.A, 1)
        .mapping(TestEnum1.B, 2)
        .mapping(TestEnum1.C, 3)
        .build();

    Enm<TestEnum2> enm2 = EnmBuilder.<TestEnum2>newEnumBuilder()
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
        .build();

    ByteBuffer encoded = struct.encoder()
        .enm("myEnum2", TestEnum2.W)
        .struct("z")
          .enm("myEnum1", TestEnum1.A)
        .end()
        .encode();

    encoded.rewind();

    StructDecoder decoder = struct.decoder(encoded);

    TestEnum2 myEnum2 = decoder.<TestEnum2>enm("myEnum2");
  }

  enum TestEnum1 {
    A,
    B,
    C,
  }

  enum TestEnum2 {
    W,
    X,
    Y,
    Z,
  }

}
