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

import java.nio.ByteBuffer;
import java.util.List;

/**
 * @author Ludovic Orban
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

    size += 4;

    if (withIndex) {
      size += 4;
    }

    return size;
  }

  public void encode(ByteBuffer byteBuffer, boolean withIndex) {
    if (withIndex) {
      byteBuffer.putInt(index);
    }

    byteBuffer.putInt(values.size());
    for (DataHolder value : values) {
      value.encode(byteBuffer, false);
    }
  }
}
