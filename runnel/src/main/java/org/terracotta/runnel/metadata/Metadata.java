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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Ludovic Orban
 */
public class Metadata {

  private final List<? extends Field> metadata;
  private Map<String, Field> fieldsByName;

  public Metadata(List<? extends Field> metadata) {
    this.metadata = metadata;
  }

  private void initFields(List<? extends Field> metadata) {
    if (fieldsByName == null) {
      fieldsByName = new HashMap<String, Field>(metadata.size(), 1.0F);
      for (Field field : metadata) {
        fieldsByName.put(field.name(), field);
      }
    }
  }

  public FieldSearcher fieldSearcher() {
    return new FieldSearcher(this);
  }

  public FieldDecoder fieldDecoder(ReadBuffer readBuffer) {
    return new FieldDecoder(this, readBuffer);
  }

  public Map<Integer, Field> buildFieldsByIndexMap() {
    initFields(metadata);
    Map<Integer, Field> map = new HashMap<Integer, Field>();
    for (Field field : fieldsByName.values()) {
      map.put(field.index(), field);
    }
    return map;
  }

  Field getFieldByName(String name) {
    initFields(metadata);
    return fieldsByName.get(name);
  }

}
