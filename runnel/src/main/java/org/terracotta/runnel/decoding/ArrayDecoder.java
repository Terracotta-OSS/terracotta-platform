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
import org.terracotta.runnel.utils.ReadBuffer;
import org.terracotta.runnel.utils.VLQ;

/**
 * @author Ludovic Orban
 */
public class ArrayDecoder<T> {

  private final Field arrayedField;
  private final ReadBuffer readBuffer;
  private final StructDecoder parent;
  private final int length;
  private final int size;
  private int i = 0;
  private int currentlyRead = 0;

  ArrayDecoder(Field arrayedField, ReadBuffer readBuffer, StructDecoder parent) {
    this.arrayedField = arrayedField;
    this.readBuffer = readBuffer;
    this.parent = parent;
    this.size = readBuffer.getVlqInt();
    this.length = readBuffer.getVlqInt();
    currentlyRead += VLQ.encodedSize(length);
  }

  public int length() {
    return length;
  }

  public T value() {
    if (i >= length) {
      throw new RuntimeException("Array end reached");
    }
    i++;
    int before = readBuffer.position();
    T decoded = (T) arrayedField.decode(readBuffer);
    int after = readBuffer.position();
    currentlyRead += (after - before);
    return decoded;
  }

  public StructDecoder end() {
    readBuffer.skip(size - currentlyRead);

    return parent;
  }

}
