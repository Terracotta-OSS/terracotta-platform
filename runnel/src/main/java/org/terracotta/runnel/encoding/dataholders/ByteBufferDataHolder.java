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

import org.terracotta.runnel.utils.VLQ;
import org.terracotta.runnel.utils.WriteBuffer;

import java.nio.ByteBuffer;

/**
 * @author Ludovic Orban
 */
public class ByteBufferDataHolder implements DataHolder {

  private final ByteBuffer value;
  private final int index;

  public ByteBufferDataHolder(ByteBuffer value, int index) {
    this.value = value;
    this.index = index;
  }

  public int size(boolean withIndex) {
    int len = value.remaining();
    return VLQ.encodedSize(len) + len + (withIndex ? VLQ.encodedSize(index) : 0);
  }

  public void encode(WriteBuffer writeBuffer, boolean withIndex) {
    if (withIndex) {
      writeBuffer.putVlqInt(index);
    }
    writeBuffer.putVlqInt(value.remaining());
    writeBuffer.putByteBuffer(value);
  }
}
