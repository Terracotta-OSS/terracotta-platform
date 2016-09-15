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

import org.terracotta.runnel.utils.ReadBuffer;
import org.terracotta.runnel.utils.VLQ;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Ludovic Orban
 */
public class Metadata {

  private final Map<String, Field> fieldsByName = new HashMap<String, Field>();
  private int lastIndex = -1;

  public Metadata(List<? extends Field> metadata) {
    for (Field field : metadata) {
      fieldsByName.put(field.name(), field);
    }
  }

  public <T extends Field, S extends Field> T nextField(String name, Class<T> fieldClazz, Class<S> subFieldClazz, ReadBuffer readBuffer) {
    T field = this.findField(name, fieldClazz, subFieldClazz);
    if (readBuffer.limitReached()) {
      return null;
    }
    int index = readBuffer.getVlqInt();
    if (readBuffer.limitReached()) {
      return null;
    }

    while (index < field.index()) {
      int fieldSize = readBuffer.getVlqInt();
      readBuffer.skip(fieldSize);
      if (readBuffer.limitReached()) {
        return null;
      }
      index = readBuffer.getVlqInt();
    }

    if (index > field.index()) {
      readBuffer.rewind(VLQ.encodedSize(index));
      return null;
    } else if (index != field.index()) {
      return null;
    } else {
      return field;
    }
  }

  public <T extends Field, S extends Field> T findField(String name, Class<T> typeClass, Class<S> subTypeClass) {
    Field field = getByName(name);
    if (field.getClass() != typeClass) {
      throw new RuntimeException("Invalid type for field '" + name + "', expected : '" + typeClass.getSimpleName() + "' but was '" + field.getClass().getSimpleName() + "'");
    }
    if (subTypeClass != null && field.subFields().get(0).getClass() != subTypeClass) {
      throw new RuntimeException("Invalid subtype for field '" + name + "', expected : '" + subTypeClass.getSimpleName() + "' but was '" + field.subFields().get(0).getClass().getSimpleName() + "'");
    }
    return (T) field;
  }

  private Field getByName(String name) {
    Field field = fieldsByName.get(name);
    if (field.index() <= lastIndex) {
      throw new RuntimeException("No such field left : '" + name + "'");
    }
    lastIndex = field.index();
    return field;
  }

  public void reset() {
    this.lastIndex = -1;
  }
}
