/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2026
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

import org.terracotta.runnel.decoding.fields.ArrayField;
import org.terracotta.runnel.decoding.fields.Field;

/**
 * @author Ludovic Orban
 */
public class FieldSearcher {

  private final Metadata metadata;
  private int lastIndex = -1;

  FieldSearcher(Metadata metadata) {
    this.metadata = metadata;
  }

  public <T extends Field, S extends Field> T findField(String name, Class<T> fieldClazz, Class<S> subFieldClazz) {
    @SuppressWarnings("unchecked")
    T field = (T) metadata.getFieldByName(name);
    if (field == null) {
      throw new IllegalArgumentException("No such field : " + name);
    }
    if (field.index() <= lastIndex) {
      throw new IllegalArgumentException("No such field left : '" + name + "'");
    }
    lastIndex = field.index();

    checkType(field, fieldClazz, subFieldClazz);
    return field;
  }

  /**
   * Variant of {@link #findField(String, Class, Class)} keyed by a pre-resolved field handle,
   * skipping the name lookup. The same ordering and type validations are enforced.
   * @param field the field handle (must belong to this struct's metadata)
   * @param fieldClazz the expected field class
   * @param subFieldClazz the expected array sub-field class, or null for non-array fields
   * @return the field
   */
  public <T extends Field, S extends Field> T findField(T field, Class<T> fieldClazz, Class<S> subFieldClazz) {
    if (!metadata.hasField(field)) {
      throw new IllegalArgumentException("Field '" + field.name() + "' does not belong to this struct");
    }
    if (field.index() <= lastIndex) {
      throw new IllegalArgumentException("No such field left : '" + field.name() + "'");
    }
    lastIndex = field.index();

    checkType(field, fieldClazz, subFieldClazz);
    return field;
  }

  private <T extends Field, S extends Field> void checkType(T field, Class<T> fieldClazz, Class<S> subFieldClazz) {
    if (field.getClass() != fieldClazz) {
      throw new IllegalArgumentException("Invalid type for field '" + field.name() + "', expected : '" + fieldClazz.getSimpleName() + "' but was '" + field.getClass().getSimpleName() + "'");
    }
    if (subFieldClazz != null) {
      ArrayField arrayField = (ArrayField) field;
      Field nextSubField = arrayField.subField();
      if (!nextSubField.getClass().equals(subFieldClazz)) {
        throw new IllegalArgumentException("Invalid subtype for field '" + field.name() + "', expected : '" + subFieldClazz.getSimpleName() + "' but was '" + nextSubField.getClass().getSimpleName() + "'");
      }
    }
  }

}
