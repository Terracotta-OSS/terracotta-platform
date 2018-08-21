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

import org.junit.jupiter.api.Test;
import org.terracotta.runnel.utils.ReadBuffer;
import org.terracotta.runnel.utils.WriteBuffer;

import java.nio.ByteBuffer;
import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * @author Ludovic Orban
 */
public class ArrayTest {

  @Test
  public void testWithIndex() throws Exception {
    ArrayDataHolder arrayDataHolder = new ArrayDataHolder(Arrays.asList(new StringDataHolder("one", 3), new StringDataHolder("two", 6), new StringDataHolder("three", 9)), 7);

    assertThat(arrayDataHolder.size(true), is(17));

    ByteBuffer bb = ByteBuffer.allocate(arrayDataHolder.size(true));
    arrayDataHolder.encode(new WriteBuffer(bb), true);
    assertThat(bb.position(), is(17));
    bb.rewind();
    ReadBuffer readBuffer = new ReadBuffer(bb);
    assertThat(readBuffer.getVlqInt(), is(7));
    assertThat(readBuffer.getVlqInt(), is(15));
    assertThat(readBuffer.getVlqInt(), is(3));
    assertThat(readBuffer.getVlqInt(), is(3));
    assertThat(readBuffer.getString(3), is("one"));
    assertThat(readBuffer.getVlqInt(), is(3));
    assertThat(readBuffer.getString(3), is("two"));
    assertThat(readBuffer.getVlqInt(), is(5));
    assertThat(readBuffer.getString(5), is("three"));
  }

  @Test
  public void testWithoutIndex() throws Exception {
    ArrayDataHolder arrayDataHolder = new ArrayDataHolder(Arrays.asList(new StringDataHolder("one", 3), new StringDataHolder("two", 6), new StringDataHolder("three", 9)), 7);

    assertThat(arrayDataHolder.size(false), is(16));

    ByteBuffer bb = ByteBuffer.allocate(arrayDataHolder.size(true));
    arrayDataHolder.encode(new WriteBuffer(bb), false);
    assertThat(bb.position(), is(16));
    bb.rewind();
    ReadBuffer readBuffer = new ReadBuffer(bb);
    assertThat(readBuffer.getVlqInt(), is(15));
    assertThat(readBuffer.getVlqInt(), is(3));
    assertThat(readBuffer.getVlqInt(), is(3));
    assertThat(readBuffer.getString(3), is("one"));
    assertThat(readBuffer.getVlqInt(), is(3));
    assertThat(readBuffer.getString(3), is("two"));
    assertThat(readBuffer.getVlqInt(), is(5));
    assertThat(readBuffer.getString(5), is("three"));
  }

}
