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
import org.terracotta.runnel.utils.VLQ;

/**
 * @author Ludovic Orban
 */
public class FieldDecoder {

  private final Metadata metadata;
  private ReadBuffer readBuffer;
  private int lastIndex = -1;

  FieldDecoder(Metadata metadata, ReadBuffer readBuffer) {
    this.metadata = metadata;
    this.readBuffer = readBuffer;
  }

  public StructArrayDecoder decodeStructArray(String name, StructDecoder parent) {
    ArrayField field = nextField(name, ArrayField.class, StructField.class);
    if (field == null) {
      return null;
    }
    return new StructArrayDecoder(((StructField) field.subField()), readBuffer, parent);
  }

  public StructDecoder decodeStruct(String name, StructDecoder parent) {
    StructField field = nextField(name, StructField.class, null);
    if (field == null) {
      return null;
    }
    return new StructDecoder(field, readBuffer, parent);
  }

  public <T> ArrayDecoder<T> decodeValueArray(String name, Class<? extends ValueField<T>> clazz, StructDecoder parent) {
    ArrayField field = nextField(name, ArrayField.class, clazz);
    if (field == null) {
      return null;
    }
    return new ArrayDecoder<T>((ValueField<T>) field.subField(), readBuffer, parent);
  }

  public <T> T decodeValue(String name, Class<? extends ValueField<T>> clazz) {
    ValueField<T> field = nextField(name, clazz, null);
    if (field == null) {
      return null;
    }
    return field.decode(readBuffer);
  }


  public void reset(ReadBuffer readBuffer) {
    this.lastIndex = -1;
    this.readBuffer = readBuffer;
  }

  private  <T extends Field, S extends Field> T nextField(String name, Class<T> fieldClazz, Class<S> subFieldClazz) {
    Metadata.FieldWithIndex fieldWithIndex = findFieldWithIndex(name, fieldClazz, subFieldClazz);
    if (readBuffer.limitReached()) {
      return null;
    }

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

  private <T extends Field, S extends Field> Metadata.FieldWithIndex findFieldWithIndex(String name, Class<T> fieldClazz, Class<S> subFieldClazz) {
    Metadata.FieldWithIndex fieldWithIndex = metadata.getFieldWithIndexByName(name);
    if (fieldWithIndex == null) {
      throw new IllegalArgumentException("No such field : " + name);
    }
    if (fieldWithIndex.fieldIndex <= lastIndex) {
      throw new IllegalArgumentException("No such field left : '" + name + "'");
    }
    lastIndex = fieldWithIndex.fieldIndex;

    if (fieldWithIndex.field.getClass() != fieldClazz) {
      throw new IllegalArgumentException("Invalid type for field '" + name + "', expected : '" + fieldClazz.getSimpleName() + "' but was '" + fieldWithIndex.field.getClass().getSimpleName() + "'");
    }
    if (subFieldClazz != null) {
      ArrayField arrayField = (ArrayField) fieldWithIndex.field;
      Field nextSubField = arrayField.subField();
      if (!nextSubField.getClass().equals(subFieldClazz)) {
        throw new IllegalArgumentException("Invalid subtype for field '" + name + "', expected : '" + subFieldClazz.getSimpleName() + "' but was '" + nextSubField.getClass().getSimpleName() + "'");
      }
    }
    return fieldWithIndex;
  }

}
