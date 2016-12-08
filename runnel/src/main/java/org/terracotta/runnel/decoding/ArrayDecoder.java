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

import org.terracotta.runnel.decoding.fields.ValueField;
import org.terracotta.runnel.utils.ReadBuffer;

/**
 * @author Ludovic Orban
 */
public class ArrayDecoder<T, P> {

  private final ValueField<T> arrayedField;
  private final ReadBuffer readBuffer;
  private final P parent;
  private final int length;

  public ArrayDecoder(ValueField<T> arrayedField, ReadBuffer readBuffer, P parent) {
    this.arrayedField = arrayedField;
    this.parent = parent;
    int size = readBuffer.getVlqInt();
    this.readBuffer = readBuffer.limit(size);

    this.length = readBuffer.getVlqInt();
  }

  public int length() {
    return length;
  }

  public T value() {
    return arrayedField.decode(readBuffer);
  }

  public P end() {
    readBuffer.skipAll();

    return parent;
  }

}
