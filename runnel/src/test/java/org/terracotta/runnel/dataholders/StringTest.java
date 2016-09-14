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

import java.io.UnsupportedEncodingException;
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

    assertThat(stringDataHolder.size(true), is(28));

    ByteBuffer bb = ByteBuffer.allocate(stringDataHolder.size(true));
    stringDataHolder.encode(new WriteBuffer(bb), true);
    assertThat(bb.position(), is(28));
    bb.rewind();
    ReadBuffer readBuffer = new ReadBuffer(bb);
    assertThat(readBuffer.getVlqInt(), is(5));
    assertThat(readBuffer.getVlqInt(), is(26));
    assertThat(readBuffer.getString(26), is("aNormalString"));
  }

  @Test
  public void testWithoutIndex() throws Exception {
    StringDataHolder stringDataHolder = new StringDataHolder("aNormalString", 5);

    assertThat(stringDataHolder.size(false), is(27));

    ByteBuffer bb = ByteBuffer.allocate(stringDataHolder.size(true));
    stringDataHolder.encode(new WriteBuffer(bb), false);
    assertThat(bb.position(), is(27));
    bb.rewind();
    ReadBuffer readBuffer = new ReadBuffer(bb);
    assertThat(readBuffer.getVlqInt(), is(26));
    String s = readBuffer.getString(26);
    assertThat(s, is("aNormalString"));
  }

}
