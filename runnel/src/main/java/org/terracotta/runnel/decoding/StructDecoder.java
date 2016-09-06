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

import org.terracotta.runnel.metadata.ArrayField;
import org.terracotta.runnel.metadata.ByteBufferField;
import org.terracotta.runnel.metadata.Field;
import org.terracotta.runnel.metadata.Int32Field;
import org.terracotta.runnel.metadata.Int64Field;
import org.terracotta.runnel.metadata.StringField;
import org.terracotta.runnel.metadata.StructField;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * @author Ludovic Orban
 */
public class StructDecoder {

  private final List<? extends Field> metadata;
  private final ByteBuffer byteBuffer;
  private final int maxSize;
  private final StructDecoder parent;
  private int i = 0;
  private int currentlyRead = 0;

  public StructDecoder(List<? extends Field> metadata, ByteBuffer byteBuffer) {
    this(metadata, byteBuffer, null);
  }

  private StructDecoder(List<? extends Field> metadata, ByteBuffer byteBuffer, StructDecoder parent) {
    this.metadata = metadata;
    this.byteBuffer = byteBuffer;
    this.maxSize = byteBuffer.getInt();
    this.parent = parent;
    currentlyRead += 4;
  }

  public Integer int32(String name) {
    Int32Field field = findField(name, Int32Field.class);
    if (field == null) {
      return null;
    }
    int before = byteBuffer.position();
    Integer decoded = (Integer) field.decode(byteBuffer);
    int after = byteBuffer.position();
    currentlyRead += (after - before);
    return decoded;
  }

  public Long int64(String name) {
    Int64Field field = findField(name, Int64Field.class);
    if (field == null) {
      return null;
    }
    int before = byteBuffer.position();
    Long decoded = (Long) field.decode(byteBuffer);
    int after = byteBuffer.position();
    currentlyRead += (after - before);
    return decoded;
  }

  public String string(String name) {
    StringField field = findField(name, StringField.class);
    if (field == null) {
      return null;
    }
    int before = byteBuffer.position();
    String decoded = (String) field.decode(byteBuffer);
    int after = byteBuffer.position();
    currentlyRead += (after - before);
    return decoded;
  }

  public ByteBuffer byteBuffer(String name) {
    ByteBufferField field = findField(name, ByteBufferField.class);
    if (field == null) {
      return null;
    }
    int before = byteBuffer.position();
    ByteBuffer decoded = (ByteBuffer) field.decode(byteBuffer);
    int after = byteBuffer.position();
    currentlyRead += (after - before);
    return decoded;
  }

  public StructDecoder struct(String name) {
    StructField field = findField(name, StructField.class);
    if (field == null) {
      return null;
    } else {
      return new StructDecoder(field.subFields(), byteBuffer, this);
    }
  }

  public ArrayDecoder<Integer> int32s(String name) {
    ArrayField field = findField(name, ArrayField.class);
    if (field == null) {
      return null;
    } else {
      return new ArrayDecoder<Integer>(field.subFields().get(0), byteBuffer, this);
    }
  }

  public ArrayDecoder<Long> int64s(String name) {
    ArrayField field = findField(name, ArrayField.class);
    if (field == null) {
      return null;
    } else {
      return new ArrayDecoder<Long>(field.subFields().get(0), byteBuffer, this);
    }
  }

  public ArrayDecoder<String> strings(String name) {
    ArrayField field = findField(name, ArrayField.class);
    if (field == null) {
      return null;
    } else {
      return new ArrayDecoder<String>(field.subFields().get(0), byteBuffer, this);
    }
  }

  public StructArrayDecoder structs(String name) {
    ArrayField field = findField(name, ArrayField.class);
    if (field == null) {
      return null;
    } else {
      return new StructArrayDecoder(field.subFields().get(0), byteBuffer, this);
    }
  }

  public StructDecoder end() {
    if (parent == null) {
      throw new RuntimeException("Cannot end root decoder");
    }

    byteBuffer.position(byteBuffer.position() + (maxSize - currentlyRead));

    return parent;
  }

  private <F extends Field> F findField(String name, Class<F> fieldClazz) {
    F field = findMetadataFor(name, fieldClazz);
    if (currentlyRead + 4 > maxSize) {
      return null;
    }
    int index = byteBuffer.getInt();
    currentlyRead += 4;
    if (currentlyRead == maxSize) {
      return field;
    }

    while (index < field.index()) {
      currentlyRead += findMetadataFor(index).skip(byteBuffer);
      if (currentlyRead + 4 > maxSize) {
        return null;
      }
      index = byteBuffer.getInt();
      currentlyRead += 4;
    }

    if (index > field.index()) {
      byteBuffer.position(byteBuffer.position() - 4);
      currentlyRead -= 4;
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
