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
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Ludovic Orban
 */
public class VersionCompatibilityTest {

  private enum TestEnum_v1 {
    A,B
  }

  private enum TestEnum_v2 {
    A,C
  }

  private static final Enm<TestEnum_v1> ENM_V1 = EnmBuilder.newEnumBuilder(TestEnum_v1.class)
      .mapping(TestEnum_v1.A, 10)
      .mapping(TestEnum_v1.B, 20)
      .build();

  private static final Enm<TestEnum_v2> ENM_V2 = EnmBuilder.newEnumBuilder(TestEnum_v2.class)
      .mapping(TestEnum_v2.A, 10)
      .mapping(TestEnum_v2.C, 30)
      .build();

  private static final Struct STRUCT_V1 = StructBuilder.newStructBuilder()
      .int32("age", 100)
      .int64("id", 200)
      .enm("letter", 300, ENM_V1)
      .build();

  private static final Struct STRUCT_V2 = StructBuilder.newStructBuilder()
      .int32("age", 100)
      .string("name", 150)
      .int64("id", 200)
      .enm("letter", 300, ENM_V2)
      .build();


  @Test
  public void testForward() throws Exception {
    ByteBuffer encoded_v2 = STRUCT_V2.encoder()
        .int32("age", 30)
        .string("name", "john doe")
        .int64("id", 1234L)
        .enm("letter", TestEnum_v2.C)
        .encode();

    encoded_v2.rewind();
    StructDecoder decoder_v1 = STRUCT_V1.decoder(encoded_v2);

    assertThat(decoder_v1.int32("age"), is(30));
    assertThat(decoder_v1.int64("id"), is(1234L));
    assertThat(decoder_v1.<TestEnum_v1>enm("letter"), is(nullValue()));
  }

  @Test
  public void testBackward() throws Exception {
    ByteBuffer encoded_v1 = STRUCT_V1.encoder()
        .int32("age", 30)
        .int64("id", 1234L)
        .enm("letter", TestEnum_v1.B)
        .encode();

    encoded_v1.rewind();
    StructDecoder decoder_v2 = STRUCT_V2.decoder(encoded_v1);

    assertThat(decoder_v2.int32("age"), is(30));
    assertThat(decoder_v2.string("name"), is(nullValue()));
    assertThat(decoder_v2.int64("id"), is(1234L));
    assertThat(decoder_v2.<TestEnum_v1>enm("letter"), is(nullValue()));
  }

}
