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
  private final Field structField;
  private final List<? extends Field> metadata;
  private final ReadBuffer readBuffer;
  private final StructDecoder parent;
  private final int size;
  private int maxSize;
  private int currentlyRead = 0;
  private int i = 0;
  private int arrayIndex = 0;

  StructArrayDecoder(Field structField, ReadBuffer readBuffer, StructDecoder parent) {
    this.structField = structField;
    this.metadata = structField.subFields();
    this.readBuffer = readBuffer;
    this.parent = parent;
    this.size = readBuffer.getVlqInt();
    if (this.size > 0) {
      this.maxSize = readBuffer.getVlqInt();
      currentlyRead += VLQ.encodedSize(maxSize);
    }
  }

  public Integer int32(String name) {
    Int32Field field = findField(name, Int32Field.class);
    if (field == null) {
      return null;
    }
    int before = readBuffer.position();
    Integer decoded = (Integer) field.decode(readBuffer);
    int after = readBuffer.position();
    currentlyRead += (after - before);
    return decoded;
  }

  public Long int64(String name) {
    Int64Field field = findField(name, Int64Field.class);
    if (field == null) {
      return null;
    }
    int before = readBuffer.position();
    Long decoded = (Long) field.decode(readBuffer);
    int after = readBuffer.position();
    currentlyRead += (after - before);
    return decoded;
  }

  public String string(String name) {
    StringField field = findField(name, StringField.class);
    if (field == null) {
      return null;
    }
    int before = readBuffer.position();
    String decoded = (String) field.decode(readBuffer);
    int after = readBuffer.position();
    currentlyRead += (after - before);
    return decoded;
  }

  public ByteBuffer byteBuffer(String name) {
    ByteBufferField field = findField(name, ByteBufferField.class);
    if (field == null) {
      return null;
    }
    int before = readBuffer.position();
    ByteBuffer decoded = (ByteBuffer) field.decode(readBuffer);
    int after = readBuffer.position();
    currentlyRead += (after - before);
    return decoded;
  }

  public int size() {
    return size;
  }

  public StructDecoder end() {
    while (arrayIndex < size-1) {
      next();
    }
    if (currentlyRead != maxSize) {
      readBuffer.skip(maxSize - currentlyRead);
    }
    return parent;
  }

  public void next() {
    if (arrayIndex >= size) {
      throw new RuntimeException("Last array element reached");
    }
    arrayIndex++;

    if (currentlyRead != maxSize) {
      readBuffer.skip(maxSize - currentlyRead);
    }

    if (arrayIndex < size) {
      currentlyRead = 0;
      maxSize = readBuffer.getVlqInt();
      currentlyRead += VLQ.encodedSize(maxSize);

      i = 0;
    }
  }


  private <F extends Field> F findField(String name, Class<F> fieldClazz) {
    F field = findMetadataFor(name, fieldClazz);
    if (currentlyRead >= maxSize) {
      return null;
    }
    int index = readBuffer.getVlqInt();
    currentlyRead += VLQ.encodedSize(index);

    while (index < field.index()) {
      currentlyRead += findMetadataFor(index).skip(readBuffer);
      if (currentlyRead >= maxSize) {
        return null;
      }
      index = readBuffer.getVlqInt();
      currentlyRead += VLQ.encodedSize(index);
    }

    if (index > field.index()) {
      readBuffer.rewind(VLQ.encodedSize(index));
      currentlyRead -= VLQ.encodedSize(index);
      return null;
    } else if (index != field.index()) {
      return null;
    } else {
      return field;
    }
  }

  private Field findMetadataFor(int index) {
    for (Field field : metadata) {
      if (field.index() == index) {
        return field;
      }
    }
    throw new RuntimeException("No field with index [" + index + "]");
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
