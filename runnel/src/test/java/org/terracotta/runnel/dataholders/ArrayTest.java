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
package org.terracotta.runnel.dataholders;

import org.junit.Test;
import org.terracotta.runnel.utils.ReadBuffer;
import org.terracotta.runnel.utils.WriteBuffer;

import java.nio.ByteBuffer;
import java.util.Arrays;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author Ludovic Orban
 */
public class ArrayTest {

  @Test
  public void testWithIndex() throws Exception {
    ArrayDataHolder arrayDataHolder = new ArrayDataHolder(Arrays.asList(new StringDataHolder("one", 3), new StringDataHolder("two", 6), new StringDataHolder("three", 9)), 7);

    assertThat(arrayDataHolder.size(true), is(27));

    ByteBuffer bb = ByteBuffer.allocate(arrayDataHolder.size(true));
    arrayDataHolder.encode(new WriteBuffer(bb), true);
    assertThat(bb.position(), is(27));
    bb.rewind();
    ReadBuffer readBuffer = new ReadBuffer(bb);
    assertThat(readBuffer.getVlqInt(), is(7));
    assertThat(readBuffer.getVlqInt(), is(3));
    assertThat(readBuffer.getVlqInt(), is(6));
    assertThat(readBuffer.getString(6), is("one"));
    assertThat(readBuffer.getVlqInt(), is(6));
    assertThat(readBuffer.getString(6), is("two"));
    assertThat(readBuffer.getVlqInt(), is(10));
    assertThat(readBuffer.getString(10), is("three"));
  }

  @Test
  public void testWithoutIndex() throws Exception {
    ArrayDataHolder arrayDataHolder = new ArrayDataHolder(Arrays.asList(new StringDataHolder("one", 3), new StringDataHolder("two", 6), new StringDataHolder("three", 9)), 7);

    assertThat(arrayDataHolder.size(false), is(26));

    ByteBuffer bb = ByteBuffer.allocate(arrayDataHolder.size(true));
    arrayDataHolder.encode(new WriteBuffer(bb), false);
    assertThat(bb.position(), is(26));
    bb.rewind();
    ReadBuffer readBuffer = new ReadBuffer(bb);
    assertThat(readBuffer.getVlqInt(), is(3));
    assertThat(readBuffer.getVlqInt(), is(6));
    assertThat(readBuffer.getString(6), is("one"));
    assertThat(readBuffer.getVlqInt(), is(6));
    assertThat(readBuffer.getString(6), is("two"));
    assertThat(readBuffer.getVlqInt(), is(10));
    assertThat(readBuffer.getString(10), is("three"));
  }

}
