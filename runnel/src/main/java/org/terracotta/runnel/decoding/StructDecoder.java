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

import org.terracotta.runnel.decoding.fields.ArrayField;
import org.terracotta.runnel.decoding.fields.ByteBufferField;
import org.terracotta.runnel.decoding.fields.FloatingPoint64Field;
import org.terracotta.runnel.decoding.fields.Int32Field;
import org.terracotta.runnel.decoding.fields.Int64Field;
import org.terracotta.runnel.decoding.fields.StringField;
import org.terracotta.runnel.decoding.fields.StructField;
import org.terracotta.runnel.metadata.FieldSearcher;
import org.terracotta.runnel.utils.ReadBuffer;

import java.nio.ByteBuffer;

/**
 * @author Ludovic Orban
 */
public class StructDecoder implements PrimitiveDecodingSupport {

  private final FieldSearcher fieldSearcher;
  private final ReadBuffer readBuffer;
  private final StructDecoder parent;

  public StructDecoder(StructField structField, ReadBuffer readBuffer) {
    this(structField, readBuffer, null);
  }

  private StructDecoder(StructField structField, ReadBuffer readBuffer, StructDecoder parent) {
    this.fieldSearcher = structField.getMetadata().fieldSearcher();
    this.parent = parent;
    int size = readBuffer.getVlqInt();
    this.readBuffer = readBuffer.limit(size);
  }

  @Override
  public Integer int32(String name) {
    Int32Field field = fieldSearcher.nextField(name, Int32Field.class, null, readBuffer);
    if (field == null) {
      return null;
    }
    return (Integer) field.decode(readBuffer);
  }

  @Override
  public Long int64(String name) {
    Int64Field field = fieldSearcher.nextField(name, Int64Field.class, null, readBuffer);
    if (field == null) {
      return null;
    }
    return (Long) field.decode(readBuffer);
  }

  @Override
  public Double fp64(String name) {
    FloatingPoint64Field field = fieldSearcher.nextField(name, FloatingPoint64Field.class, null, readBuffer);
    if (field == null) {
      return null;
    }
    return (Double) field.decode(readBuffer);
  }

  @Override
  public String string(String name) {
    StringField field = fieldSearcher.nextField(name, StringField.class, null, readBuffer);
    if (field == null) {
      return null;
    }
    return (String) field.decode(readBuffer);
  }

  @Override
  public ByteBuffer byteBuffer(String name) {
    ByteBufferField field = fieldSearcher.nextField(name, ByteBufferField.class, null, readBuffer);
    if (field == null) {
      return null;
    }
    return (ByteBuffer) field.decode(readBuffer);
  }

  public StructDecoder struct(String name) {
    StructField field = fieldSearcher.nextField(name, StructField.class, null, readBuffer);
    if (field == null) {
      return null;
    }
    return new StructDecoder(field, readBuffer, this);
  }

  public ArrayDecoder<Integer> int32s(String name) {
    ArrayField field = fieldSearcher.nextField(name, ArrayField.class, Int32Field.class, readBuffer);
    if (field == null) {
      return null;
    }
    return new ArrayDecoder<Integer>(field.subFields().get(0), readBuffer, this);
  }

  public ArrayDecoder<Long> int64s(String name) {
    ArrayField field = fieldSearcher.nextField(name, ArrayField.class, Int64Field.class, readBuffer);
    if (field == null) {
      return null;
    }
    return new ArrayDecoder<Long>(field.subFields().get(0), readBuffer, this);
  }

  public ArrayDecoder<Double> fp64s(String name) {
    ArrayField field = fieldSearcher.nextField(name, ArrayField.class, FloatingPoint64Field.class, readBuffer);
    if (field == null) {
      return null;
    }
    return new ArrayDecoder<Double>(field.subFields().get(0), readBuffer, this);
  }

  public ArrayDecoder<String> strings(String name) {
    ArrayField field = fieldSearcher.nextField(name, ArrayField.class, StringField.class, readBuffer);
    if (field == null) {
      return null;
    }
    return new ArrayDecoder<String>(field.subFields().get(0), readBuffer, this);
  }

  public StructArrayDecoder structs(String name) {
    ArrayField field = fieldSearcher.nextField(name, ArrayField.class, StructField.class, readBuffer);
    if (field == null) {
      return null;
    }
    return new StructArrayDecoder(((StructField) field.subFields().get(0)), readBuffer, this);
  }

  public StructDecoder end() {
    if (parent == null) {
      throw new IllegalStateException("Cannot end root decoder");
    }

    readBuffer.skipAll();

    return parent;
  }

}
