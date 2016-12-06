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
import org.terracotta.runnel.decoding.fields.StructField;
import org.terracotta.runnel.utils.ReadBuffer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Ludovic Orban
 */
public class Metadata {

  private final List<Field> fields = new ArrayList<Field>();
  private final Map<String, Field> fieldsByName = new HashMap<String, Field>();
  private volatile boolean initialized = false;
  private volatile boolean initializationFullyChecked = false;
  private final ThreadLocal<Boolean> checkingForFullInitialization = new ThreadLocal<Boolean>();

  public Metadata() {
  }

  public void addField(Field field) {
    if (initialized) {
      throw new IllegalStateException("Metadata already initialized");
    }
    fields.add(field);
  }

  public void init() {
    if (initialized) {
      throw new IllegalStateException("Metadata already initialized");
    }
    for (Field field : fields) {
      fieldsByName.put(field.name(), field);
    }
    initialized = true;
  }

  public FieldSearcher fieldSearcher() {
    return new FieldSearcher(this);
  }

  public void checkFullyInitialized() throws IllegalStateException {
    if (initializationFullyChecked || Boolean.TRUE.equals(checkingForFullInitialization.get())) {
      return; // already visited this graph vertex, or graph already fully checked
    }

    if (!initialized) {
      throw new IllegalStateException("Metadata not yet initialized");
    }

    checkingForFullInitialization.set(Boolean.TRUE); // save the fact that the check has been performed on this vertex
    try {
      for (Field field : fields) {
        if (field instanceof StructField) {
          ((StructField) field).getMetadata().checkFullyInitialized();
        }
      }
      initializationFullyChecked = true;
    } finally {
      checkingForFullInitialization.remove();
    }
  }

  public FieldDecoder fieldDecoder(ReadBuffer readBuffer) {
    return new FieldDecoder(this, readBuffer);
  }

  public Map<Integer, Field> buildFieldsByIndexMap() {
    Map<Integer, Field> map = new HashMap<Integer, Field>();
    for (Field field : fieldsByName.values()) {
      map.put(field.index(), field);
    }
    return map;
  }

  Field getFieldByName(String name) {
    return fieldsByName.get(name);
  }

}
