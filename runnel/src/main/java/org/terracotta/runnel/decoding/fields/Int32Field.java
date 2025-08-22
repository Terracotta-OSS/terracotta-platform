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
package org.terracotta.runnel.decoding.fields;

import org.terracotta.runnel.utils.CorruptDataException;
import org.terracotta.runnel.utils.ReadBuffer;

/**
 * @author Ludovic Orban
 */
public class Int32Field extends AbstractValueField<Integer> {

  public Int32Field(String name, int index) {
    super(name, index);
  }

  @Override
  public Integer decode(ReadBuffer readBuffer) {
    int size = readBuffer.getVlqInt();
    if (size != 4) {
      throw new CorruptDataException("Expected field size of 4, read : " + size);
    }
    return readBuffer.getInt();
  }

}
