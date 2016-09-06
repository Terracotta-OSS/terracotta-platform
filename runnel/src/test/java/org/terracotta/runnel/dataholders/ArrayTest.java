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

import java.nio.ByteBuffer;
import java.util.Arrays;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.terracotta.runnel.dataholders.StringTest.readString;

/**
 * @author Ludovic Orban
 */
public class ArrayTest {

  @Test
  public void testWithIndex() throws Exception {
    // (index)4 + (array len)4 + 10 + 10 + 14
    ArrayDataHolder arrayDataHolder = new ArrayDataHolder(Arrays.asList(new StringDataHolder("one", 3), new StringDataHolder("two", 6), new StringDataHolder("three", 9)), 7);

    assertThat(arrayDataHolder.size(true), is(42));

    ByteBuffer bb = ByteBuffer.allocate(arrayDataHolder.size(true));
    arrayDataHolder.encode(bb, true);
    assertThat(bb.position(), is(42));
    bb.rewind();
    assertThat(bb.getInt(), is(7));
    assertThat(bb.getInt(), is(3));
    assertThat(bb.getInt(), is(6));
    assertThat(readString(bb, 6), is("one"));
    assertThat(bb.getInt(), is(6));
    assertThat(readString(bb, 6), is("two"));
    assertThat(bb.getInt(), is(10));
    assertThat(readString(bb, 10), is("three"));
  }

  @Test
  public void testWithoutIndex() throws Exception {
    // (array len)4 + 10 + 10 + 14
    ArrayDataHolder arrayDataHolder = new ArrayDataHolder(Arrays.asList(new StringDataHolder("one", 3), new StringDataHolder("two", 6), new StringDataHolder("three", 9)), 7);

    assertThat(arrayDataHolder.size(false), is(38));

    ByteBuffer bb = ByteBuffer.allocate(arrayDataHolder.size(true));
    arrayDataHolder.encode(bb, false);
    assertThat(bb.position(), is(38));
    bb.rewind();
    assertThat(bb.getInt(), is(3));
    assertThat(bb.getInt(), is(6));
    assertThat(readString(bb, 6), is("one"));
    assertThat(bb.getInt(), is(6));
    assertThat(readString(bb, 6), is("two"));
    assertThat(bb.getInt(), is(10));
    assertThat(readString(bb, 10), is("three"));
  }

}
