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
package org.terracotta.runnel.decoding;

import org.terracotta.runnel.metadata.Field;

import java.nio.ByteBuffer;

/**
 * @author Ludovic Orban
 */
public class ArrayDecoder<T> {

  private final Field arrayedField;
  private final ByteBuffer byteBuffer;
  private final StructDecoder parent;
  private final int size;
  private int i = 0;

  ArrayDecoder(Field arrayedField, ByteBuffer byteBuffer, StructDecoder parent) {
    this.arrayedField = arrayedField;
    this.byteBuffer = byteBuffer;
    this.parent = parent;
    this.size = byteBuffer.getInt();
  }

  public int size() {
    return size;
  }

  public T value() {
    if (i >= size) {
      throw new RuntimeException("Array end reached");
    }
    i++;
    return (T) arrayedField.decode(byteBuffer);
  }

  public StructDecoder end() {
    for (; i < size; i++) {
      arrayedField.skip(byteBuffer);
    }

    return parent;
  }

}
