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
import org.terracotta.runnel.decoding.Enm;
import org.terracotta.runnel.decoding.StructDecoder;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.util.NoSuchElementException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

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

  private static final EnumMapping<TestEnum_v1> ENM_V1 = EnumMappingBuilder.newEnumMappingBuilder(TestEnum_v1.class)
      .mapping(TestEnum_v1.A, 10)
      .mapping(TestEnum_v1.B, 20)
      .build();

  private static final EnumMapping<TestEnum_v2> ENM_V2 = EnumMappingBuilder.newEnumMappingBuilder(TestEnum_v2.class)
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
      .enm("2ndLetter", 400, ENM_V2)
      .build();


  @Test
  public void testForward() throws Exception {
    ByteBuffer encoded_v2 = STRUCT_V2.encoder()
        .int32("age", 30)
        .string("name", "john doe")
        .int64("id", 1234L)
        .enm("letter", TestEnum_v2.C)
        .enm("2ndLetter", TestEnum_v2.A)
        .encode();

    encoded_v2.rewind();
    StructDecoder decoder_v1 = STRUCT_V1.decoder(encoded_v2);

    assertThat(decoder_v1.int32("age"), is(30));
    assertThat(decoder_v1.int64("id"), is(1234L));
    Enm<TestEnum_v1> letter = decoder_v1.enm("letter");
    try {
      letter.get();
      fail();
    } catch (NoSuchElementException e) {
      // expected
    }
    assertThat(letter.isValid(), is(false));
    assertThat(letter.isFound(), is(true));
    assertThat(letter.raw(), is(30));

    encoded_v2.rewind();
    STRUCT_V1.dump(encoded_v2, new PrintStream(new ByteArrayOutputStream()));
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
    Enm<TestEnum_v1> letter = decoder_v2.enm("letter");
    try {
      letter.get();
      fail();
    } catch (NoSuchElementException e) {
      // expected
    }
    assertThat(letter.isValid(), is(false));
    assertThat(letter.isFound(), is(true));
    assertThat(letter.raw(), is(20));

    Enm<Object> secondLetter = decoder_v2.enm("2ndLetter");
    try {
      secondLetter.get();
      fail();
    } catch (NoSuchElementException e) {
      // expected
    }
    try {
      secondLetter.raw();
      fail();
    } catch (NoSuchElementException e) {
      // expected
    }
    assertThat(secondLetter.isFound(), is(false));
    assertThat(secondLetter.isValid(), is(false));

    encoded_v1.rewind();
    STRUCT_V2.dump(encoded_v1, new PrintStream(new ByteArrayOutputStream()));
  }

}
