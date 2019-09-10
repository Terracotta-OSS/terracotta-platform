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

import org.terracotta.runnel.utils.WriteBuffer;

import java.util.List;

/**
 * Encoding is:
 * <pre>
 *   index:size:[field1 index:size:value][field2 index:size:value]...
 * </pre>
 */
public class StructDataHolder extends AbstractDataHolder {

  private final List<? extends DataHolder> values;
  private int cacheSize = -1;

  public StructDataHolder(List<? extends DataHolder> values, int index) {
    super(index);
    this.values = values;
  }

  @Override
  protected int valueSize() {
    if (cacheSize < 0) {
      int size = 0;
      for (DataHolder value : values) {
        size += value.size(true);
      }
      cacheSize = size;
    }
    return cacheSize;
  }

  @Override
  protected void encodeValue(WriteBuffer writeBuffer) {
    for (DataHolder value : values) {
      value.encode(writeBuffer, true);
    }
  }
}
