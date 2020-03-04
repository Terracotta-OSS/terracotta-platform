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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.fail;

/**
 * @author Ludovic Orban
 */
@SuppressWarnings("rawtypes")
public class PrimitiveStructBuilderTest {

  private final Struct struct = StructBuilder.newStructBuilder()
      .string("name", 1)
      .int64("age", 2)
      .byteBuffer("blob", 3)
      .int32("id", 4)
      .build();

  @Test(expected = IllegalArgumentException.class)
  public void testEncodingNonExistentNameThrows() throws Exception {
    struct.encoder()
        .string("unknown", "joe")
    ;
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEncodingInvalidTypeThrows() throws Exception {
    struct.encoder()
        .int64("name", 1L)
    ;
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEncodingNotInIndexOrderThrows() throws Exception {
    struct.encoder()
        .int32("id", 1)
        .int64("age", 40L)
    ;
  }

  @Test
  public void testDecodingNonExistentNameThrows() throws Exception {
    ByteBuffer bb = struct.encoder()
        .encode();

    bb.rewind();

    StructDecoder decoder = struct.decoder(bb);
    try {
      decoder.int64("unknown");
      fail("expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // expected
    }
  }

  @Test
  public void testDecodingNotInIndexOrderThrows() throws Exception {
    ByteBuffer bb = struct.encoder()
        .encode();

    bb.rewind();

    StructDecoder decoder = struct.decoder(bb);
    decoder.int64("age");
    try {
      decoder.string("name");
      fail("expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // expected
    }
  }

  @Test
  public void testDecodingInvalidTypeThrows() throws Exception {
    ByteBuffer bb = struct.encoder()
        .encode();

    bb.rewind();

    StructDecoder decoder = struct.decoder(bb);
    try {
      decoder.byteBuffer("id");
      fail("expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // expected
    }
  }

  @Test
  public void testReadAll() throws Exception {
    ByteBuffer bb = struct.encoder()
        .string("name", "joe")
        .int64("age", 30)
        .byteBuffer("blob", buffer(4096, 'X'))
        .int32("id", Integer.MIN_VALUE)
        .encode();

    bb.rewind();

    StructDecoder decoder = struct.decoder(bb);

    assertThat(decoder.string("name"), is("joe"));
    assertThat(decoder.int64("age"), is(30L));
    ByteBuffer blob = decoder.byteBuffer("blob");
    assertThat(blob.remaining(), is(4096));
    for (int i = 0; i < 4096; i++) {
      byte b = blob.get();
      assertThat(b, is((byte) 'X'));
    }
    assertThat(decoder.int32("id"), is(Integer.MIN_VALUE));
  }

  @Test
  public void testDump() throws Exception {
    ByteBuffer bb = struct.encoder()
        .string("name", "joe")
        .int64("age", 30)
        .byteBuffer("blob", buffer(4096, 'X'))
        .int32("id", Integer.MIN_VALUE)
        .encode();

    bb.rewind();

    struct.dump(bb, new PrintStream(new ByteArrayOutputStream()));
  }

  @Test
  public void testLastFieldNull_readAll() throws Exception {
    ByteBuffer bb = struct.encoder()
        .string("name", "joe")
        .int64("age", 30)
        .byteBuffer("blob", buffer(4096, 'X'))
        .encode();

    bb.rewind();

    StructDecoder decoder = struct.decoder(bb);

    assertThat(decoder.string("name"), is("joe"));
    assertThat(decoder.int64("age"), is(30L));
    ByteBuffer blob = decoder.byteBuffer("blob");
    assertThat(blob.remaining(), is(4096));
    for (int i = 0; i < 4096; i++) {
      byte b = blob.get();
      assertThat(b, is((byte) 'X'));
    }
    assertThat(decoder.int32("id"), is(nullValue()));
  }

  @Test
  public void testMiddleFieldNull_readAll() throws Exception {
    ByteBuffer bb = struct.encoder()
        .string("name", "joe")
        .byteBuffer("blob", buffer(4096, 'X'))
        .int32("id", Integer.MIN_VALUE)
        .encode();

    bb.rewind();

    StructDecoder decoder = struct.decoder(bb);

    assertThat(decoder.string("name"), is("joe"));
    assertThat(decoder.int64("age"), is(nullValue()));
    ByteBuffer blob = decoder.byteBuffer("blob");
    assertThat(blob.remaining(), is(4096));
    for (int i = 0; i < 4096; i++) {
      byte b = blob.get();
      assertThat(b, is((byte) 'X'));
    }
    assertThat(decoder.int32("id"), is(Integer.MIN_VALUE));
  }

  @Test
  public void testLastFieldNull_skipMiddleFields() throws Exception {
    ByteBuffer bb = struct.encoder()
        .string("name", "joe")
        .int64("age", 30)
        .byteBuffer("blob", buffer(4096, 'X'))
        .int32("id", Integer.MIN_VALUE)
        .encode();

    bb.rewind();

    StructDecoder decoder = struct.decoder(bb);

    assertThat(decoder.string("name"), is("joe"));
    assertThat(decoder.int32("id"), is(Integer.MIN_VALUE));
  }

  @Test
  public void testLastFieldNull_skipToNullField() throws Exception {
    ByteBuffer bb = struct.encoder()
        .string("name", "joe")
        .int64("age", 30)
        .byteBuffer("blob", buffer(4096, 'X'))
        .encode();

    bb.rewind();

    StructDecoder decoder = struct.decoder(bb);

    assertThat(decoder.string("name"), is("joe"));
    assertThat(decoder.int32("id"), is(nullValue()));
  }


  private static ByteBuffer buffer(int size, char c) {
    ByteBuffer byteBuffer = ByteBuffer.allocate(size);
    for (int i = 0; i < size; i++) {
      byteBuffer.put((byte) c);
    }
    byteBuffer.rewind();
    return byteBuffer;
  }
}
