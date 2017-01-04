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

import org.terracotta.runnel.decoding.ArrayDecoder;
import org.terracotta.runnel.decoding.StructArrayDecoder;
import org.terracotta.runnel.decoding.StructDecoder;
import org.terracotta.runnel.decoding.fields.ArrayField;
import org.terracotta.runnel.decoding.fields.Field;
import org.terracotta.runnel.decoding.fields.StructField;
import org.terracotta.runnel.decoding.fields.ValueField;
import org.terracotta.runnel.utils.ReadBuffer;

/**
 * @author Ludovic Orban
 */
public class FieldDecoder {

  private final Metadata metadata;
  private final ReadBuffer readBuffer;
  private int lastIndex = -1;
  private int readAheadIndex = -1;

  FieldDecoder(Metadata metadata, ReadBuffer readBuffer) {
    this.metadata = metadata;
    this.readBuffer = readBuffer;
  }

  public <P> StructArrayDecoder<P> decodeStructArray(String name, P parent) {
    ArrayField field = nextField(name, ArrayField.class, StructField.class);
    if (field == null) {
      return null;
    }
    return new StructArrayDecoder<P>(((StructField) field.subField()), readBuffer, parent);
  }

  public <P> StructDecoder<P> decodeStruct(String name, P parent) {
    StructField field = nextField(name, StructField.class, null);
    if (field == null) {
      return null;
    }
    return new StructDecoder<P>(field, readBuffer, parent);
  }

  public <T, P> ArrayDecoder<T, P> decodeValueArray(String name, Class<? extends ValueField<T>> clazz, P parent) {
    ArrayField field = nextField(name, ArrayField.class, clazz);
    if (field == null) {
      return null;
    }
    return new ArrayDecoder<T, P>((ValueField<T>) field.subField(), readBuffer, parent);
  }

  public <T> T decodeValue(String name, Class<? extends ValueField<T>> clazz) {
    ValueField<T> field = nextField(name, clazz, null);
    if (field == null) {
      return null;
    }
    return field.decode(readBuffer);
  }

  private  <T extends Field, S extends Field> T nextField(String name, Class<T> fieldClazz, Class<S> subFieldClazz) {
    Field field = findFieldWithIndex(name, fieldClazz, subFieldClazz);
    if (readBuffer.limitReached()) {
      return null;
    }

    int index = readAheadIndex > 0 ? readAheadIndex : readBuffer.getVlqInt();
    readAheadIndex = -1;
    // skip all fields with a lower index than the requested field's
    while (index < field.index()) {
      int fieldSize = readBuffer.getVlqInt();
      readBuffer.skip(fieldSize);
      if (readBuffer.limitReached()) {
        return null;
      }
      index = readBuffer.getVlqInt();
    }

    if (index > field.index()) {
      readAheadIndex = index;
      return null;
    } else if (index != field.index()) {
      return null;
    } else {
      return (T) field;
    }
  }

  private <T extends Field, S extends Field> Field findFieldWithIndex(String name, Class<T> fieldClazz, Class<S> subFieldClazz) {
    Field field = metadata.getFieldByName(name);
    if (field == null) {
      throw new IllegalArgumentException("No such field : " + name);
    }
    if (field.index() <= lastIndex) {
      throw new IllegalArgumentException("No such field left : '" + name + "'");
    }
    lastIndex = field.index();

    if (field.getClass() != fieldClazz) {
      throw new IllegalArgumentException("Invalid type for field '" + name + "', expected : '" + fieldClazz.getSimpleName() + "' but was '" + field.getClass().getSimpleName() + "'");
    }
    if (subFieldClazz != null) {
      ArrayField arrayField = (ArrayField) field;
      Field nextSubField = arrayField.subField();
      if (!nextSubField.getClass().equals(subFieldClazz)) {
        throw new IllegalArgumentException("Invalid subtype for field '" + name + "', expected : '" + subFieldClazz.getSimpleName() + "' but was '" + nextSubField.getClass().getSimpleName() + "'");
      }
    }
    return field;
  }

}
