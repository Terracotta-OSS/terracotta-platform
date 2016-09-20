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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Ludovic Orban
 */
public class Metadata {

  // yes, you need this weirdo holder class because every Field.index() call is megamorphic, hence very expensive
  static class FieldWithIndex {
    FieldWithIndex(Field field) {
      this.field = field;
      this.fieldIndex = field.index();
    }

    Field field;
    int fieldIndex;
  }

  private final Map<String, FieldWithIndex> fieldsByName;

  public Metadata(List<? extends Field> metadata) {
    fieldsByName = new HashMap<String, FieldWithIndex>();
    for (Field field : metadata) {
      fieldsByName.put(field.name(), new FieldWithIndex(field));
    }
  }

  public FieldSearcher fieldSearcher() {
    return new FieldSearcher(this);
  }

  public Map<Integer, Field> buildFieldsByIndexMap() {
    Map<Integer, Field> map = new HashMap<Integer, Field>();
    for (FieldWithIndex fieldWithIndex : fieldsByName.values()) {
      map.put(fieldWithIndex.fieldIndex, fieldWithIndex.field);
    }
    return map;
  }

  FieldWithIndex getFieldWithIndexByName(String name) {
    return fieldsByName.get(name);
  }

}
