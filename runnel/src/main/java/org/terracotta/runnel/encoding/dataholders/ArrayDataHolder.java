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
public class ArrayDataHolder extends AbstractDataHolder {

  private final List<? extends DataHolder> values;
  private int cacheSize = -1;

  public ArrayDataHolder(List<? extends DataHolder> values, int index) {
    super(index);
    this.values = values;
  }

  @Override
  public int size(int valueSize) {
    int lengthFieldSize = VLQ.encodedSize(values.size()); // length field
    this.cacheSize = valueSize + lengthFieldSize;
    return super.size(valueSize) + lengthFieldSize;
  }

  @Override
  protected int valueSize() {
    if (cacheSize < 0) {
      throw new AssertionError();
    } else {
      return cacheSize;
    }
  }

  @Override
  protected void encodeValue(WriteBuffer writeBuffer, int indentation) {
    writeBuffer.putVlqInt(values.size()); // length field
    for (DataHolder value : values) {
      value.encode(writeBuffer, false, indentation + 2);
    }
  }
}
