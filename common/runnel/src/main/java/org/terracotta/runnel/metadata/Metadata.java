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
package org.terracotta.runnel.metadata;

import org.terracotta.runnel.decoding.fields.Field;
import org.terracotta.runnel.decoding.fields.StructField;
import org.terracotta.runnel.utils.ReadBuffer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Ludovic Orban
 */
public class Metadata {

  private final List<Field> fields = new ArrayList<>();
  private final Map<String, Field> fieldsByName = new HashMap<>();
  private final Set<Field> fieldsByIdentity = Collections.newSetFromMap(new IdentityHashMap<>());
  private volatile Map<Integer, Field> fieldsByIndex;
  private volatile boolean initialized = false;
  private volatile boolean initializationFullyChecked = false;
  private final ThreadLocal<Boolean> checkingForFullInitialization = new ThreadLocal<>();

  public Metadata() {
  }

  public void addField(Field field) {
    if (initialized) {
      throw new IllegalStateException("Metadata already initialized");
    }
    fields.add(field);
    fieldsByIdentity.add(field);
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
    return new HashMap<>(fieldsByIndex());
  }

  /**
   * Returns the field-by-index map of this metadata, building and caching it on first use.
   * The returned map must not be mutated.
   * <p>
   * The map is only cached once this metadata is {@link #init() initialized}: fields can no
   * longer be added afterwards, so the cache cannot go stale. Before initialization a fresh,
   * necessarily empty map is returned on every call (as {@link #buildFieldsByIndexMap()} always
   * did), instead of caching an empty map forever.
   * @return the cached field-by-index map
   */
  public Map<Integer, Field> fieldsByIndex() {
    Map<Integer, Field> map = fieldsByIndex;
    if (map == null) {
      map = new HashMap<>();
      for (Field field : fieldsByName.values()) {
        map.put(field.index(), field);
      }
      if (initialized) {
        fieldsByIndex = map;
      }
    }
    return map;
  }

  /**
   * @return the number of fields registered in this metadata
   */
  public int fieldCount() {
    return fields.size();
  }

  /**
   * Checks by identity whether the given field is registered in this metadata.
   * @param field the field to look up
   * @return true if this exact field instance belongs to this metadata
   */
  public boolean hasField(Field field) {
    return fieldsByIdentity.contains(field);
  }

  /**
   * Looks up a field by name and validates its type.
   * @param name the field name
   * @param fieldClazz the expected field class
   * @return the field
   * @throws IllegalArgumentException if the field does not exist or has a different type
   */
  @SuppressWarnings("unchecked")
  public <T extends Field> T getFieldByName(String name, Class<T> fieldClazz) {
    Field field = getFieldByName(name);
    if (field == null) {
      throw new IllegalArgumentException("No such field : " + name);
    }
    if (field.getClass() != fieldClazz) {
      throw new IllegalArgumentException("Invalid type for field '" + name + "', expected : '" + fieldClazz.getSimpleName() + "' but was '" + field.getClass().getSimpleName() + "'");
    }
    return (T) field;
  }

  public Field getFieldByName(String name) {
    return fieldsByName.get(name);
  }

}
