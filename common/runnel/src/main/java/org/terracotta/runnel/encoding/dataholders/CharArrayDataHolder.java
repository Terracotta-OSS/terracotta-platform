/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2026
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
 * Holds a whole char array field, encoding byte-identically to per-element
 * {@link CharDataHolder}s but without one holder object (or boxed value) per element.
 * The array is defensively copied at construction.
 */
public final class CharArrayDataHolder extends AbstractDataHolder {

  private final char[] values;

  public CharArrayDataHolder(char[] values, int index) {
    super(index);
    this.values = values.clone();
  }

  @Override
  protected int valueSize() {
    return VLQ.encodedSize(values.length) + values.length * (1 + 2);
  }

  @Override
  protected void encodeValue(WriteBuffer writeBuffer) {
    writeBuffer.putVlqInt(values.length);
    for (char value : values) {
      writeBuffer.putVlqInt(2);
      writeBuffer.putChar(value);
    }
  }
}
