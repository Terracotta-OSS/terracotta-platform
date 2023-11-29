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
import org.terracotta.runnel.utils.ReadBuffer;
import org.terracotta.runnel.utils.WriteBuffer;

import java.nio.ByteBuffer;
import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

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

}
