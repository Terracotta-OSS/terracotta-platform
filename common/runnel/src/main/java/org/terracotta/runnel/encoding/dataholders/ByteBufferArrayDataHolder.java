/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
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
 * Holds a whole byte buffer array field, encoding byte-identically to per-element
 * {@link ByteBufferDataHolder}s but without one holder object per element.
 * <p>
 * The element references are snapshotted at construction (the array is cloned); the buffers
 * themselves are referenced, not copied, consistent with {@link ByteBufferDataHolder}. Each
 * buffer's position is consumed at encode time, and {@code valueSize()} reads each buffer's
 * {@code remaining()} during size computation: do not advance, reuse or share a buffer between
 * construction and encoding — the holder is a single-use write.
 */
public final class ByteBufferArrayDataHolder extends AbstractDataHolder {

  private final ByteBuffer[] values;

  public ByteBufferArrayDataHolder(ByteBuffer[] values, int index) {
    super(index);
    this.values = values.clone();
  }

  @Override
  protected int valueSize() {
    int size = VLQ.encodedSize(values.length);
    for (ByteBuffer value : values) {
      size += VLQ.encodedSize(value.remaining()) + value.remaining();
    }
    return size;
  }

  @Override
  protected void encodeValue(WriteBuffer writeBuffer) {
    writeBuffer.putVlqInt(values.length);
    for (ByteBuffer value : values) {
      writeBuffer.putVlqInt(value.remaining());
      writeBuffer.putByteBuffer(value);
    }
  }
}
