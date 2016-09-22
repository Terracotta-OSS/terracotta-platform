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

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author Ludovic Orban
 */
public class StringTest {

  @Test
  public void testWithIndex() throws Exception {
    StringDataHolder stringDataHolder = new StringDataHolder("aNormalString", 5);

    assertThat(stringDataHolder.size(true), is(15));

    ByteBuffer bb = ByteBuffer.allocate(stringDataHolder.size(true));
    stringDataHolder.encode(new WriteBuffer(bb), true);
    assertThat(bb.position(), is(15));
    bb.rewind();
    ReadBuffer readBuffer = new ReadBuffer(bb);
    assertThat(readBuffer.getVlqInt(), is(5));
    assertThat(readBuffer.getVlqInt(), is(13));
    assertThat(readBuffer.getString(13), is("aNormalString"));
  }

  @Test
  public void testWithoutIndex() throws Exception {
    StringDataHolder stringDataHolder = new StringDataHolder("aNormalString", 5);

    assertThat(stringDataHolder.size(false), is(14));

    ByteBuffer bb = ByteBuffer.allocate(stringDataHolder.size(true));
    stringDataHolder.encode(new WriteBuffer(bb), false);
    assertThat(bb.position(), is(14));
    bb.rewind();
    ReadBuffer readBuffer = new ReadBuffer(bb);
    assertThat(readBuffer.getVlqInt(), is(13));
    String s = readBuffer.getString(13);
    assertThat(s, is("aNormalString"));
  }

}
