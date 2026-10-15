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

/**
 * Holds a whole string array field, encoding byte-identically to per-element
 * {@link StringDataHolder}s but without one holder object per element.
 * Each element is encoded to bytes at construction.
 */
public final class StringArrayDataHolder extends AbstractDataHolder {

  private final byte[][] encodedElements;
  private int cacheValueSize = -1;

  public StringArrayDataHolder(String[] values, int index) {
    super(index);
    this.encodedElements = new byte[values.length][];
    for (int i = 0; i < values.length; i++) {
      encodedElements[i] = StringDataHolder.encodeString(values[i]);
    }
  }

  @Override
  protected int valueSize() {
    if (cacheValueSize < 0) {
      int size = VLQ.encodedSize(encodedElements.length);
      for (byte[] element : encodedElements) {
        size += VLQ.encodedSize(element.length) + element.length;
      }
      cacheValueSize = size;
    }
    return cacheValueSize;
  }

  @Override
  protected void encodeValue(WriteBuffer writeBuffer) {
    writeBuffer.putVlqInt(encodedElements.length);
    for (byte[] element : encodedElements) {
      writeBuffer.putVlqInt(element.length);
      writeBuffer.putBytes(element, 0, element.length);
    }
  }
}
