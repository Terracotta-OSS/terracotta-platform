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
package org.terracotta.runnel.decoding;

import org.terracotta.runnel.metadata.ByteBufferField;
import org.terracotta.runnel.metadata.Field;
import org.terracotta.runnel.metadata.Int32Field;
import org.terracotta.runnel.metadata.Int64Field;
import org.terracotta.runnel.metadata.StringField;
import org.terracotta.runnel.utils.ReadBuffer;
import org.terracotta.runnel.utils.VLQ;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * @author Ludovic Orban
 */
public class StructArrayDecoder {
  private final List<? extends Field> metadata;
  private final ReadBuffer arrayReadBuffer;
  private final StructDecoder parent;
  private final int arrayLength;

  private ReadBuffer structReadBuffer;
  private int i = 0;

  StructArrayDecoder(Field structField, ReadBuffer readBuffer, StructDecoder parent) {
    this.metadata = structField.subFields();
    this.parent = parent;

    int arraySize = readBuffer.getVlqInt();
    this.arrayReadBuffer = readBuffer.limit(arraySize);
    this.arrayLength = readBuffer.getVlqInt();

    if (this.arrayLength > 0) {
      int structSize = readBuffer.getVlqInt();
      structReadBuffer = arrayReadBuffer.limit(structSize);
    } else {
      structReadBuffer = arrayReadBuffer.limit(0);
    }
  }

  public Integer int32(String name) {
    Int32Field field = findField(name, Int32Field.class);
    if (field == null) {
      return null;
    }
    return (Integer) field.decode(arrayReadBuffer);
  }

  public Long int64(String name) {
    Int64Field field = findField(name, Int64Field.class);
    if (field == null) {
      return null;
    }
    return (Long) field.decode(arrayReadBuffer);
  }

  public String string(String name) {
    StringField field = findField(name, StringField.class);
    if (field == null) {
      return null;
    }
    return (String) field.decode(arrayReadBuffer);
  }

  public ByteBuffer byteBuffer(String name) {
    ByteBufferField field = findField(name, ByteBufferField.class);
    if (field == null) {
      return null;
    }
    return (ByteBuffer) field.decode(arrayReadBuffer);
  }

  public int length() {
    return arrayLength;
  }

  public StructDecoder end() {
    arrayReadBuffer.skipAll();
    structReadBuffer = null;
    return parent;
  }

  public void next() {
    if (arrayReadBuffer.limitReached()) {
      return;
    }

    structReadBuffer.skipAll();
    int structSize = arrayReadBuffer.getVlqInt();
    structReadBuffer = arrayReadBuffer.limit(structSize);

    i = 0;
  }


  private <F extends Field> F findField(String name, Class<F> fieldClazz) {
    F field = findMetadataFor(name, fieldClazz);
    if (arrayReadBuffer.limitReached()) {
      return null;
    }
    int index = arrayReadBuffer.getVlqInt();

    while (index < field.index()) {
      int fieldSize = arrayReadBuffer.getVlqInt();
      arrayReadBuffer.skip(fieldSize);
      if (arrayReadBuffer.limitReached()) {
        return null;
      }
      index = arrayReadBuffer.getVlqInt();
    }

    if (index > field.index()) {
      arrayReadBuffer.rewind(VLQ.encodedSize(index));
      return null;
    } else if (index != field.index()) {
      return null;
    } else {
      return field;
    }
  }

  private <F extends Field> F findMetadataFor(String name, Class<F> clazz) {
    for (; i < metadata.size(); i++) {
      Field field = metadata.get(i);
      if (field.name().equals(name)) {
        if (field.getClass() != clazz) {
          throw new RuntimeException("Invalid type for field '" + name + "', expected : '" + clazz.getSimpleName() + "' but was '" + field.getClass().getSimpleName() + "'");
        }
        return (F) field;
      }
    }
    throw new RuntimeException("No such field left : '" + name + "'");
  }

}
