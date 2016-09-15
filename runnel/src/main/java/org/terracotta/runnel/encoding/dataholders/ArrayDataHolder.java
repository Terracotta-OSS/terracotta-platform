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
 *   index:size:length:[field1 size:value][field2 size:value][field3 size:value]...
 * </pre>
 */
public class ArrayDataHolder implements DataHolder {

  private final List<? extends DataHolder> values;
  private int index;

  public ArrayDataHolder(List<? extends DataHolder> values, int index) {
    this.values = values;
    this.index = index;
  }

  public int size(boolean withIndex) {
    int size = 0;

    for (DataHolder value : values) {
      size += value.size(false);
    }

    size += VLQ.encodedSize(size);
    size += VLQ.encodedSize(values.size());

    if (withIndex) {
      size += VLQ.encodedSize(index);
    }

    return size;
  }

  public void encode(WriteBuffer byteBuffer, boolean withIndex) {
    if (withIndex) {
      byteBuffer.putVlqInt(index);
    }

    int size = 0;
    for (DataHolder value : values) {
      size += value.size(false);
    }
    size += VLQ.encodedSize(values.size());

    byteBuffer.putVlqInt(size);
    byteBuffer.putVlqInt(values.size());
    for (DataHolder value : values) {
      value.encode(byteBuffer, false);
    }
  }
}
