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
package org.terracotta.runnel.encoding.dataholders;

import org.junit.Test;
import org.terracotta.runnel.Struct;
import org.terracotta.runnel.StructBuilder;
import org.terracotta.runnel.decoding.StructDecoder;
import org.terracotta.runnel.utils.ReadBuffer;
import org.terracotta.runnel.utils.WriteBuffer;

import java.nio.ByteBuffer;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * @author Ludovic Orban
 */
public class StructTest {

  @Test
  public void testWithIndex() throws Exception {
    StructDataHolder structDataHolder = new StructDataHolder(Arrays.asList(new StringDataHolder("eins", 2), new Int64DataHolder(111L, 3), new StringDataHolder("deuxieme", 4)), 11);

    assertThat(structDataHolder.size(true), is(28));

    ByteBuffer bb = ByteBuffer.allocate(structDataHolder.size(true));
    structDataHolder.encode(new WriteBuffer(bb), true);
    assertThat(bb.position(), is(28));
    bb.rewind();
    ReadBuffer readBuffer = new ReadBuffer(bb);
    assertThat(readBuffer.getVlqInt(), is(11));
    assertThat(readBuffer.getVlqInt(), is(26));
    assertThat(readBuffer.getVlqInt(), is(2));
    assertThat(readBuffer.getVlqInt(), is(4));
    assertThat(readBuffer.getString(4), is("eins"));
    assertThat(readBuffer.getVlqInt(), is(3));
    assertThat(readBuffer.getVlqInt(), is(8));
    assertThat(readBuffer.getLong(), is(111L));
    assertThat(readBuffer.getVlqInt(), is(4));
    assertThat(readBuffer.getVlqInt(), is(8));
    assertThat(readBuffer.getString(8), is("deuxieme"));
  }

  @Test
  public void testWithoutIndex() throws Exception {
    StructDataHolder structDataHolder = new StructDataHolder(Arrays.asList(new StringDataHolder("eins", 2), new Int64DataHolder(111L, 3), new StringDataHolder("deuxieme", 4)), 11);

    assertThat(structDataHolder.size(false), is(27));

    ByteBuffer bb = ByteBuffer.allocate(structDataHolder.size(false));
    structDataHolder.encode(new WriteBuffer(bb), false);
    assertThat(bb.position(), is(27));
    bb.rewind();
    ReadBuffer readBuffer = new ReadBuffer(bb);
    assertThat(readBuffer.getVlqInt(), is(26));
    assertThat(readBuffer.getVlqInt(), is(2));
    assertThat(readBuffer.getVlqInt(), is(4));
    assertThat(readBuffer.getString(4), is("eins"));
    assertThat(readBuffer.getVlqInt(), is(3));
    assertThat(readBuffer.getVlqInt(), is(8));
    assertThat(readBuffer.getLong(), is(111L));
    assertThat(readBuffer.getVlqInt(), is(4));
    assertThat(readBuffer.getVlqInt(), is(8));
    assertThat(readBuffer.getString(8), is("deuxieme"));
  }

  @Test
  public void testFromDecoder() throws Exception {
    Struct messageBase = StructBuilder.newStructBuilder().int32("opcode", 10).build();
    Struct messageCacheCreate = StructBuilder.newStructBuilder().int32("opcode", 10).string("cache", 20).build();
    Struct messageGet = StructBuilder.newStructBuilder().int32("opcode", 10).int64("key", 20).build();


    ByteBuffer encoded1 = messageCacheCreate.encoder().int32("opcode", 24).string("cache", "oh my cache!").encode();
    encoded1.flip();

    ByteBuffer encoded2 = messageGet.encoder().int32("opcode", 42).int64("key", 42L).encode();
    encoded2.flip();


    // Decode base
    StructDecoder decoder = messageBase.decoder(encoded1);
    Integer opcode = decoder.int32("opcode");
    assertThat(opcode, is(24));

    // Transform decoder to handle cacheCreate
    StructDecoder cacheCreateDecoder = messageCacheCreate.fromDecoder(decoder);
    assertThat(cacheCreateDecoder.string("cache"), is("oh my cache!"));

    // Decode base
    decoder = messageBase.decoder(encoded2);
    opcode = decoder.int32("opcode");
    assertThat(opcode, is(42));

    // Transform decode to handle get
    StructDecoder getDecoder = messageGet.fromDecoder(decoder);
    assertThat(getDecoder.int64("key"), is(42L));
  }

  @Test
  public void testFromDecoderInvalid() throws Exception {
    Struct messageBase = StructBuilder.newStructBuilder().int32("opcode", 10).build();
    Struct messageCacheCreate = StructBuilder.newStructBuilder().int32("opcode", 10).string("cache", 20).build();


    ByteBuffer encoded1 = messageCacheCreate.encoder().int32("opcode", 24).string("cache", "oh my cache!").encode();
    encoded1.flip();

    StructDecoder decoder = messageBase.decoder(encoded1);
    Integer opcode = decoder.int32("opcode");

    try {
      decoder.string("cache");
      fail("Field should not be readable - messageBase does not define \"cache\"");
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), containsString("No such field"));
    }

    StructDecoder cacheCreateDecoder = messageCacheCreate.fromDecoder(decoder);
    try {
      cacheCreateDecoder.int32("opcode");
      fail("Field should no longer be readable - was read already");
    } catch (IllegalArgumentException e){
      assertThat(e.getMessage(), containsString("No such field left"));
    }
  }

}
