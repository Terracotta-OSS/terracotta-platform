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

import java.util.List;

/**
 * Encoding is:
 * <pre>
 *   index:size:[field1 index:size:value][field2 index:size:value]...
 * </pre>
 */
public class StructDataHolder implements DataHolder {

  private final List<? extends DataHolder> values;
  private final int index;

  public StructDataHolder(List<? extends DataHolder> values, int index) {
    this.values = values;
    this.index = index;
  }

  public int size(boolean withIndex) {
    int size = 0;

    for (DataHolder value : values) {
      size += value.size(true);
    }

    size += VLQ.encodedSize(size);

    if (withIndex) {
      size += VLQ.encodedSize(index);
    }

    return size;
  }

  public void encode(WriteBuffer writeBuffer, boolean withIndex) {
    if (withIndex) {
      writeBuffer.putVlqInt(index);
    }

    int size = 0;
    for (DataHolder value : values) {
      size += value.size(true);
    }
    writeBuffer.putVlqInt(size);

    for (DataHolder value : values) {
      value.encode(writeBuffer, true);
    }
  }
}
