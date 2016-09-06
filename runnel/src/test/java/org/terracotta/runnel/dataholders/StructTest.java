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
public class StructTest {

  @Test
  public void testWithIndex() throws Exception {
    // 4(index) + 4(size) + 4(idx1) + 12 + 4(idx2) + 8 + 4(idx4) + 20
    StructDataHolder structDataHolder = new StructDataHolder(Arrays.asList(new StringDataHolder("eins", 2), new Int64DataHolder(111L, 3), new StringDataHolder("deuxieme", 4)), 11);

    assertThat(structDataHolder.size(true), is(60));

    ByteBuffer bb = ByteBuffer.allocate(structDataHolder.size(true));
    structDataHolder.encode(bb, true);
    assertThat(bb.position(), is(60));
    bb.rewind();
    assertThat(bb.getInt(), is(11));
    assertThat(bb.getInt(), is(56));
    assertThat(bb.getInt(), is(2));
    assertThat(bb.getInt(), is(8));
    assertThat(readString(bb, 8), is("eins"));
    assertThat(bb.getInt(), is(3));
    assertThat(bb.getLong(), is(111L));
    assertThat(bb.getInt(), is(4));
    assertThat(bb.getInt(), is(16));
    assertThat(readString(bb, 16), is("deuxieme"));
  }

  @Test
  public void testWithoutIndex() throws Exception {
    // 4(index) + 4(size) + 4(idx1) + 12 + 4(idx2) + 8 + 4(idx4) + 20
    StructDataHolder structDataHolder = new StructDataHolder(Arrays.asList(new StringDataHolder("eins", 2), new Int64DataHolder(111L, 3), new StringDataHolder("deuxieme", 4)), 11);

    assertThat(structDataHolder.size(false), is(56));

    ByteBuffer bb = ByteBuffer.allocate(structDataHolder.size(false));
    structDataHolder.encode(bb, false);
    assertThat(bb.position(), is(56));
    bb.rewind();
    assertThat(bb.getInt(), is(56));
    assertThat(bb.getInt(), is(2));
    assertThat(bb.getInt(), is(8));
    assertThat(readString(bb, 8), is("eins"));
    assertThat(bb.getInt(), is(3));
    assertThat(bb.getLong(), is(111L));
    assertThat(bb.getInt(), is(4));
    assertThat(bb.getInt(), is(16));
    assertThat(readString(bb, 16), is("deuxieme"));
  }

}
