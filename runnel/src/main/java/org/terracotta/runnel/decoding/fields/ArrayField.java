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

import org.terracotta.runnel.utils.ReadBuffer;

import java.io.PrintStream;

/**
 * @author Ludovic Orban
 */
public class ArrayField extends AbstractField {

  private final Field arrayedField;

  public ArrayField(String name, int index, Field arrayedField) {
    super(name, index);
    this.arrayedField = arrayedField;
  }

  public Field subField() {
    return arrayedField;
  }

  @Override
  public void dump(ReadBuffer parentBuffer, PrintStream out, int depth) {
    int fieldSize = parentBuffer.getVlqInt();
    out.append(" size: ").append(Integer.toString(fieldSize));
    ReadBuffer readBuffer = parentBuffer.limit(fieldSize);

    out.append(" type: ").append(getClass().getSimpleName());

    int length = readBuffer.getVlqInt();
    out.append(" length: ").append(Integer.toString(length));

    Field subField = subField();
    for (int i = 0; i < length; i++) {
      out.append("\n  "); for (int j = 0; j < depth; j++) out.append("  ");
      subField.dump(readBuffer, out, depth + 1);
    }
  }
}
