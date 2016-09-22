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
package org.terracotta.runnel.decoding.fields;

import org.terracotta.runnel.metadata.Metadata;
import org.terracotta.runnel.utils.ReadBuffer;

import java.io.PrintStream;
import java.util.List;
import java.util.Map;

/**
 * @author Ludovic Orban
 */
public class StructField extends AbstractField {

  private final List<? extends Field> subFields;
  private final Metadata metadata;

  public StructField(String name, int index, List<? extends Field> subFields) {
    super(name, index);
    this.subFields = subFields;
    this.metadata = new Metadata(subFields);
  }

  public Metadata getMetadata() {
    return metadata;
  }

  public List<? extends Field> subFields() {
    return subFields;
  }

  @Override
  public void dump(ReadBuffer parentBuffer, PrintStream out, int depth) {
    int fieldSize = parentBuffer.getVlqInt();
    out.append(" size: ").append(Integer.toString(fieldSize));
    ReadBuffer readBuffer = parentBuffer.limit(fieldSize);

    out.append(" type: ").append(getClass().getSimpleName());
    out.append(" name: ").append(name());

    Map<Integer, Field> fieldsByInteger = getMetadata().buildFieldsByIndexMap();
    while (!readBuffer.limitReached()) {
      out.append("\n  "); for (int j = 0; j < depth; j++) out.append("  ");
      int index = readBuffer.getVlqInt();
      out.append(" index: ").append(Integer.toString(index));
      Field subField = fieldsByInteger.get(index);
      subField.dump(readBuffer, out, depth + 1);
    }
  }
}
