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

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * @author Ludovic Orban
 */
public class PrimitiveStructBuilderTest {

  private final Struct struct = StructBuilder.newStructBuilder()
      .string("name", 1)
      .int64("age", 2)
      .int64("id", 3)
      .build();

  @Test
  public void testLastFieldNull_readAll() throws Exception {
    ByteBuffer bb = ByteBuffer.allocate(1024);

    struct.encoder()
        .string("name", "joe")
        .int64("age", 30)
        .encode(bb);

    bb.rewind();

    StructDecoder decoder = struct.decoder(bb);

    assertThat(decoder.string("name"), is("joe"));
    assertThat(decoder.int64("age"), is(30L));
    assertThat(decoder.int64("id"), is(nullValue()));
  }

  @Test
  public void testLastFieldNull_skipMiddleField() throws Exception {
    ByteBuffer bb = ByteBuffer.allocate(1024);

    struct.encoder()
        .string("name", "joe")
        .int64("age", 30)
        .encode(bb);

    bb.rewind();

    StructDecoder decoder = struct.decoder(bb);

    assertThat(decoder.string("name"), is("joe"));
    assertThat(decoder.int64("id"), is(nullValue()));
  }

}
