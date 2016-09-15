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
package org.terracotta.runnel.metadata;

import org.terracotta.runnel.decoding.fields.Field;
import org.terracotta.runnel.utils.ReadBuffer;
import org.terracotta.runnel.utils.VLQ;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Ludovic Orban
 */
public class Metadata {

  // yes, you need this weirdo holder class because every Field.index() call is megamorphic, hence very expensive
  private static class FieldWithIndex {
    FieldWithIndex(Field field) {
      this.field = field;
      this.fieldIndex = field.index();
    }

    Field field;
    int fieldIndex;
  }

  private final Map<String, FieldWithIndex> fieldsByName;
  private int lastIndex = -1;

  public Metadata(List<? extends Field> metadata) {
    fieldsByName = new HashMap<String, FieldWithIndex>();
    for (Field field : metadata) {
      fieldsByName.put(field.name(), new FieldWithIndex(field));
    }
  }

  public <T extends Field, S extends Field> T nextField(String name, Class<T> fieldClazz, Class<S> subFieldClazz, ReadBuffer readBuffer) {
    if (readBuffer.limitReached()) {
      return null;
    }
    FieldWithIndex fieldWithIndex = findFieldWithIndex(name, fieldClazz, subFieldClazz);

    int index = readBuffer.getVlqInt();
    // skip all fields with a lower index than the requested field's
    while (index < fieldWithIndex.fieldIndex) {
      int fieldSize = readBuffer.getVlqInt();
      readBuffer.skip(fieldSize);
      if (readBuffer.limitReached()) {
        return null;
      }
      index = readBuffer.getVlqInt();
    }

    if (index > fieldWithIndex.fieldIndex) {
      readBuffer.rewind(VLQ.encodedSize(index));
      return null;
    } else if (index != fieldWithIndex.fieldIndex) {
      return null;
    } else {
      return (T) fieldWithIndex.field;
    }
  }

  public <T extends Field, S extends Field> T findField(String name, Class<T> fieldClazz, Class<S> subFieldClazz) {
    FieldWithIndex fieldWithIndex = findFieldWithIndex(name, fieldClazz, subFieldClazz);
    return (T) fieldWithIndex.field;
  }

  private <T extends Field, S extends Field> FieldWithIndex findFieldWithIndex(String name, Class<T> fieldClazz, Class<S> subFieldClazz) {
    FieldWithIndex fieldWithIndex = fieldsByName.get(name);
    if (fieldWithIndex.fieldIndex <= lastIndex) {
      throw new RuntimeException("No such field left : '" + name + "'");
    }
    lastIndex = fieldWithIndex.fieldIndex;

    if (fieldWithIndex.field.getClass() != fieldClazz) {
      throw new RuntimeException("Invalid type for field '" + name + "', expected : '" + fieldClazz.getSimpleName() + "' but was '" + fieldWithIndex.field.getClass().getSimpleName() + "'");
    }
    if (subFieldClazz != null && fieldWithIndex.field.subFields().get(0).getClass() != subFieldClazz) {
      throw new RuntimeException("Invalid subtype for field '" + name + "', expected : '" + subFieldClazz.getSimpleName() + "' but was '" + fieldWithIndex.field.subFields().get(0).getClass().getSimpleName() + "'");
    }
    return fieldWithIndex;
  }

  public void reset() {
    this.lastIndex = -1;
  }
}
