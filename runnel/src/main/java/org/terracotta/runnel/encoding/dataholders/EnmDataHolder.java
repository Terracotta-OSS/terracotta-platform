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

import org.terracotta.runnel.Enm;
import org.terracotta.runnel.utils.VLQ;
import org.terracotta.runnel.utils.WriteBuffer;

/**
 * @author Ludovic Orban
 */
public class EnmDataHolder<E extends Enum<E>> implements DataHolder {

  private final E value;
  private final int index;
  private final Enm<E> enm;

  public EnmDataHolder(E value, int index, Enm<E> enm) {
    this.value = value;
    this.index = index;
    this.enm = enm;
    if (!enm.maps(value)) {
      throw new IllegalArgumentException("Illegal enum value '" + value + "'");
    }
  }

  public int size(boolean withIndex) {
    return 4 + VLQ.encodedSize(4) + (withIndex ? VLQ.encodedSize(index) : 0);
  }

  public void encode(WriteBuffer writeBuffer, boolean withIndex) {
    if (withIndex) {
      writeBuffer.putVlqInt(index);
    }
    writeBuffer.putVlqInt(4);
    writeBuffer.putInt(enm.toInt(value));
  }
}
